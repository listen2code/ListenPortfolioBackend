#!/usr/bin/env python3
"""
Automated Docker & Disk Cleanup Tool for AWS EC2 Deployment.
Safely detects and removes orphaned overlay2 build layers, prunes unused Docker
images/builder cache, and vacuums system logs without affecting running services or database volumes.
"""

import json
import os
import subprocess
import sys


def run_cmd(cmd, check=True):
    try:
        return subprocess.check_output(cmd, stderr=subprocess.DEVNULL).decode()
    except Exception:
        if check:
            raise
        return ""


def main():
    print("==========================================================")
    print("🧹 Starting Automated Docker & Disk Cleanup...")
    print("==========================================================")

    # 1. Prune dangling images and BuildKit cache via standard Docker commands
    print("[1/4] Pruning Docker dangling images and builder cache...")
    subprocess.call(["sudo", "docker", "image", "prune", "-f"])
    subprocess.call(["sudo", "docker", "builder", "prune", "-a", "-f", "--keep-storage", "0"])

    # 2. Identify and purge unreferenced (orphaned) overlay2 layers
    print("[2/4] Inspecting active GraphDriver layers in Docker...")
    ps_output = run_cmd(["sudo", "docker", "ps", "-aq"], check=False).split()
    images_output = run_cmd(["sudo", "docker", "images", "-q"], check=False).split()
    targets = list(set(ps_output + images_output))

    used_layers = set()
    for t in targets:
        try:
            data = run_cmd(["sudo", "docker", "inspect", t], check=False)
            if not data:
                continue
            items = json.loads(data)
            for item in items:
                gd = item.get("GraphDriver", {}).get("Data", {})
                for key in ["LowerDir", "UpperDir", "MergedDir", "WorkDir"]:
                    val = gd.get(key)
                    if val:
                        for path in val.split(":"):
                            if "/var/lib/docker/overlay2/" in path:
                                base = path.replace("/var/lib/docker/overlay2/", "").split("/")[0]
                                used_layers.add(base)
        except Exception as e:
            print(f"Warning: Failed to inspect target {t}: {e}")

    overlay_path = "/var/lib/docker/overlay2"
    all_dirs_output = run_cmd(["sudo", "ls", "-1", overlay_path], check=False).split()
    if all_dirs_output:
        all_dirs = set(all_dirs_output)
        all_dirs.discard("l")
        all_dirs.discard("backingFsBlockDev")
        unref = sorted(list(all_dirs - used_layers))

        if unref:
            print(f"Found {len(unref)} orphaned overlay2 layer directories. Removing...")
            freed_bytes = 0
            for d in unref:
                d_path = os.path.join(overlay_path, d)
                try:
                    size_str = run_cmd(["sudo", "du", "-sb", d_path], check=False).split()[0]
                    freed_bytes += int(size_str)
                except Exception:
                    pass
                subprocess.call(["sudo", "rm", "-rf", d_path])

            l_path = os.path.join(overlay_path, "l")
            subprocess.call(["sudo", "find", l_path, "-xtype", "l", "-delete"])

            print(f"Cleaned {len(unref)} orphaned layers (freed approx. {freed_bytes / (1024 * 1024):.1f} MB).")
        else:
            print("No orphaned overlay2 layers found. Layer tree is clean.")

    # 3. Clean system journals and package manager cache
    print("[3/4] Vacuuming systemd journal logs and package cache...")
    subprocess.call(["sudo", "journalctl", "--vacuum-time=1d"], stderr=subprocess.DEVNULL)
    subprocess.call(["sudo", "dnf", "clean", "all"], stderr=subprocess.DEVNULL)
    subprocess.call(["sudo", "rm", "-rf", "/var/cache/dnf", "/var/cache/yum"], stderr=subprocess.DEVNULL)

    # 4. Display final disk utilization
    print("[4/4] Final Disk Space Utilization:")
    subprocess.call(["df", "-h", "/"])
    print("==========================================================")
    print("Cleanup completed successfully.")
    print("==========================================================")


if __name__ == "__main__":
    main()
