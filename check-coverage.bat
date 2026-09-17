@echo off
REM ===================================================================
REM Portfolio 后端 JaCoCo 测试代码覆盖率自动化检查与报告查看脚本
REM ===================================================================
REM 使用方式：双击运行或在终端执行 .\check-coverage.bat
REM 
REM 执行机制：
REM   1. 调用 Gradle Wrapper 运行全量单元测试与集成测试 (排除高耗时性能测试)
REM   2. 自动触发 jacocoTestReport 生成基于 JVM 字节码插桩的覆盖率报表 (HTML + XML)
REM   3. 检查 HTML 报告文件是否存在，并在 Windows 默认浏览器中自动弹出可视化详情大盘
REM   4. 提示团队设定的覆盖率质量红线门禁 (行覆盖率 >= 70%，分支覆盖率 >= 60%)
REM ===================================================================

echo ===================================================================
echo [JaCoCo] 正在启动单元测试并收集代码覆盖率统计数据...
echo ===================================================================

REM 步骤 1：调用本地 Gradle Wrapper 编译运行测试并生成报告
echo [1/3] 执行 Gradle 测试与报告生成任务: gradlew.bat test jacocoTestReport
call .\gradlew.bat test jacocoTestReport

echo.
echo [2/3] 正在校验覆盖率报告生成结果...
echo 报告输出相对路径: build\reports\jacoco\test\html\index.html

REM 步骤 2：检查生成的 HTML 报告文件是否存在
if exist "build\reports\jacoco\test\html\index.html" (
    echo [OK] 成功找到覆盖率报告网页文件！
    
    REM 步骤 3：调用 Windows 默认浏览器自动打开可视化覆盖率大盘
    echo.
    echo [3/3] 正在调起系统默认浏览器打开详细报告...
    start "" "build\reports\jacoco\test\html\index.html"
    
    echo.
    echo ===================================================================
    echo [质量门禁提示] 请在打开的浏览器报告中人工核对以下指标：
    echo   - 指令/行覆盖率 (Line Coverage)  : 建议保持在 70%% 以上
    echo   - 分支判断覆盖率 (Branch Coverage): 建议保持在 60%% 以上
    echo   - 业务核心 Service 层 (Service)  : 建议核心路径达到 80%% 以上
    echo ===================================================================
) else (
    echo [ERROR] 未能在目标路径找到生成的覆盖率报告！
    echo 缺失文件: build\reports\jacoco\test\html\index.html
    echo 请检查 build.gradle 中 jacoco 插件配置是否正常，或检查测试阶段是否编译报错中断。
)

echo.
echo 执行流程结束。
pause

