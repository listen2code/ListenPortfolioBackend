@echo off
echo SpotBugs Analysis
echo ================================

echo Running SpotBugs analysis...
call ./mvnw spotbugs:check -q

echo.
echo SpotBugs analysis completed

REM Try to open existing report first

echo generating new report...
call ./mvnw spotbugs:spotbugs -q
echo Opening generated report...
start "" "target\spotbugs.html"
echo Report generated and opened

echo.
pause
