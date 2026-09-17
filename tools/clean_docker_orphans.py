#!/usr/bin/env python3
"""
===================================================================
AWS EC2 Docker 构建层与磁盘深度清理引擎 (Disk & Docker Cleanup Engine)
===================================================================
【设计背景与核心解决痛点】:
AWS EC2 t2.micro 实例仅配备 8GB EBS 根磁盘。在频繁执行 GitHub Actions CI/CD
自动构建部署时，Docker overlay2 驱动经常因构建中断、BuildKit 临时缓存丢失或镜像标签覆盖，
产生大量未被任何容器或镜像引用的“孤儿层 (Orphaned Layers)”物理目录。
此时即便运行 `docker system prune -a`，Docker 守护进程也无法识别并删除这些底层孤儿文件，
导致 `/dev/xvda1` 磁盘使用率飙升至 99%~100%（触发 "No space left on device" 部署失败）。

【核心算法与工作流程】:
1. 标准缓存清理：调用 Docker CLI 移除无标签悬空镜像 (dangling images) 与 BuildKit 构建器缓存。
2. 拓扑图深度反查 (GraphDriver Reverse Lookup)：
   - 抓取当前宿主机上所有的活跃/停止容器 ID (`docker ps -aq`) 与合法镜像 ID (`docker images -q`)。
   - 对每个目标执行 `docker inspect`，提取其 `GraphDriver.Data` 字典中的 `LowerDir`、`UpperDir`、
     `MergedDir` 和 `WorkDir` 路径，提取其对应的 overlay2 目录哈希，加入“白名单在用层集合 (used_layers)”。
3. 集合差集识别孤儿层：
   - 读取物理目录 `/var/lib/docker/overlay2` 下的所有文件夹名称。
   - 剔除软链接快捷目录 `l` 与元数据文件。
   - 差集运算 `unref = all_dirs - used_layers`，精准锁定 100% 无引用的孤儿物理目录。
4. 物理安全删除：
   - 逐个统计孤儿目录体积并调用 `rm -rf` 彻底释放磁盘空间。
   - 清理 `overlay2/l` 下指向已删除目录的死软链接 (`find -xtype l -delete`)。
5. 系统级日志与缓存真空压缩：
   - 清理 systemd journal 日志保留期至 1 天以内 (`journalctl --vacuum-time=1d`)。
   - 清理 DNF / YUM 包管理器缓存。
===================================================================
"""

import json
import os
import subprocess
import sys


def run_cmd(cmd, check=True):
    """
    辅助函数：安全执行 Shell 命令行并返回标准输出文本。
    
    :param cmd: 命令行列表，例如 ["sudo", "docker", "ps", "-aq"]
    :param check: 为 True 时抛出异常，为 False 时静默失败并返回空字符串
    :return: 命令输出的标准输出字符串 (UTF-8 解码)
    """
    try:
        return subprocess.check_output(cmd, stderr=subprocess.DEVNULL).decode()
    except Exception:
        if check:
            raise
        return ""


def main():
    print("==========================================================")
    print("🧹 开始执行 AWS EC2 自动化 Docker 与磁盘深度自愈清理...")
    print("==========================================================")

    # -------------------------------------------------------------------
    # 步骤 1：清理标准悬空镜像与 BuildKit 构建缓存
    # -------------------------------------------------------------------
    print("[1/4] 正在清理 Docker 悬空镜像 (dangling) 与 BuildKit 构建缓存...")
    # prune -f 仅清理未打标签的中间镜像，不影响任何运行中的业务容器
    subprocess.call(["sudo", "docker", "image", "prune", "-f"])
    # builder prune 清理 Docker BuildKit 编译时留下的临时缓存数据
    subprocess.call(["sudo", "docker", "builder", "prune", "-a", "-f", "--keep-storage", "0"])

    # -------------------------------------------------------------------
    # 步骤 2：拓扑反查 active GraphDriver，识别并物理清除 overlay2 孤儿层
    # -------------------------------------------------------------------
    print("[2/4] 正在反查 Docker 活跃容器与镜像的底层 GraphDriver 引用树...")
    # 获取宿主机上存在的所有容器 ID（包括运行中和已停止的容器）
    ps_output = run_cmd(["sudo", "docker", "ps", "-aq"], check=False).split()
    # 获取宿主机上存在的所有镜像 ID
    images_output = run_cmd(["sudo", "docker", "images", "-q"], check=False).split()
    # 合并为无重复的审查目标清单
    targets = list(set(ps_output + images_output))

    used_layers = set()
    for t in targets:
        try:
            # inspect 查询目标元数据，提取其 GraphDriver 引用的具体底层存储层
            data = run_cmd(["sudo", "docker", "inspect", t], check=False)
            if not data:
                continue
            items = json.loads(data)
            for item in items:
                gd = item.get("GraphDriver", {}).get("Data", {})
                # LowerDir 包含多层以冒号分隔的只读层；UpperDir 为容器读写层；MergedDir 为联合挂载点
                for key in ["LowerDir", "UpperDir", "MergedDir", "WorkDir"]:
                    val = gd.get(key)
                    if val:
                        for path in val.split(":"):
                            if "/var/lib/docker/overlay2/" in path:
                                # 提取 overlay2/ 下具体的层目录哈希名称
                                base = path.replace("/var/lib/docker/overlay2/", "").split("/")[0]
                                used_layers.add(base)
        except Exception as e:
            print(f"警告: 检查目标 {t} 时发生异常: {e}")

    overlay_path = "/var/lib/docker/overlay2"
    all_dirs_output = run_cmd(["sudo", "ls", "-1", overlay_path], check=False).split()
    if all_dirs_output:
        all_dirs = set(all_dirs_output)
        # 排除 overlay2 内置的短软链接索引目录 'l' 与底层块设备元数据文件
        all_dirs.discard("l")
        all_dirs.discard("backingFsBlockDev")
        # 核心差集运算：物理存在但未被任何容器/镜像引用的即为孤儿无用层
        unref = sorted(list(all_dirs - used_layers))

        if unref:
            print(f"🔍 识别到 {len(unref)} 个未被引用的 overlay2 孤儿物理目录，开始安全移除...")
            freed_bytes = 0
            for d in unref:
                d_path = os.path.join(overlay_path, d)
                try:
                    # 统计即将释放的磁盘字节大小
                    size_str = run_cmd(["sudo", "du", "-sb", d_path], check=False).split()[0]
                    freed_bytes += int(size_str)
                except Exception:
                    pass
                # 物理删除孤儿目录
                subprocess.call(["sudo", "rm", "-rf", d_path])

            # 清理短软链接目录 'l' 中所有指向已删除物理目录的失效死链接
            l_path = os.path.join(overlay_path, "l")
            subprocess.call(["sudo", "find", l_path, "-xtype", "l", "-delete"])

            print(f"✅ 成功清除 {len(unref)} 个孤儿层，释放约 {freed_bytes / (1024 * 1024):.1f} MB 物理磁盘空间。")
        else:
            print("✅ 未发现任何 overlay2 孤儿目录，GraphDriver 存储树处于健康紧凑状态。")

    # -------------------------------------------------------------------
    # 步骤 3：清理 systemd journal 系统日志与包管理器安装缓存
    # -------------------------------------------------------------------
    print("[3/4] 正在清理 systemd 系统日志 (vacuum <= 1天) 与 Linux 包管理缓存...")
    # 限制 journal 日志最多保留 1 天，释放可能占用数百兆的 system.journal
    subprocess.call(["sudo", "journalctl", "--vacuum-time=1d"], stderr=subprocess.DEVNULL)
    # 清理 DNF / YUM 元数据与包缓存
    subprocess.call(["sudo", "dnf", "clean", "all"], stderr=subprocess.DEVNULL)
    subprocess.call(["sudo", "rm", "-rf", "/var/cache/dnf", "/var/cache/yum"], stderr=subprocess.DEVNULL)

    # -------------------------------------------------------------------
    # 步骤 4：展示最终磁盘空间释放指标
    # -------------------------------------------------------------------
    print("[4/4] 当前根分区磁盘空间使用情况:")
    subprocess.call(["df", "-h", "/"])
    print("==========================================================")
    print("🎉 自动化清理流程执行完毕。")
    print("==========================================================")


if __name__ == "__main__":
    main()
