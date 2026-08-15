@echo off
echo SpotBugs Analysis
echo ================================

echo Running SpotBugs analysis...
call .\gradlew.bat spotbugsMain

echo.
echo SpotBugs analysis completed
echo Opening generated report...
if exist "build\reports\spotbugs\main.html" (
    start "" "build\reports\spotbugs\main.html"
)

echo.
pause
