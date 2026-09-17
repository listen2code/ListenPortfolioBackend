@echo off
REM ===================================================================
REM Portfolio 后端 SpotBugs 静态代码漏洞扫描自动化脚本
REM ===================================================================
REM 使用方式：双击运行或在终端执行 .\check-spotbugs.bat
REM 
REM 执行机制：
REM   1. 调用 Gradle Wrapper 编译主应用生产源码 (classes)
REM   2. 启动 SpotBugs 引擎进行深度字节码模式匹配分析 (过滤规则遵循 spotbugs-exclude.xml)
REM   3. 输出 HTML 与 XML 格式报告
REM   4. 若存在 HTML 漏洞大盘，自动在 Windows 默认浏览器中弹出展示
REM ===================================================================

echo ===================================================================
echo [SpotBugs] 正在启动静态代码分析与潜在漏洞扫描...
echo ===================================================================

REM 步骤 1：调用本地 Gradle Wrapper 运行分析任务
echo [1/3] 执行 Gradle SpotBugs 任务: gradlew.bat spotbugsMain
call .\gradlew.bat spotbugsMain

echo.
echo [2/3] SpotBugs 静态分析阶段已执行完毕
echo 报告输出相对路径: build\reports\spotbugs\main.html

REM 步骤 2：检查生成的 HTML 报告并自动调起浏览器
if exist "build\reports\spotbugs\main.html" (
    echo [3/3] [OK] 找到漏洞分析报告，正在调起默认浏览器查看...
    start "" "build\reports\spotbugs\main.html"
) else (
    echo [INFO] 未检测到报告文件，请确认是否编译失败。
)

echo.
echo 分析流程结束。
pause

