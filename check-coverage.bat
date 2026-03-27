@echo off
echo JaCoCo Coverage Check
echo ================================

REM First run tests to generate coverage data
echo Running tests and generating coverage data...
call ./mvnw clean test jacoco:report

if %ERRORLEVEL% neq 0 (
    echo Tests failed, cannot check coverage
    exit /b 1
)

echo.
echo Coverage report generated successfully
echo Report location: target\site\jacoco\index.html

REM Open the report in browser
start "" "target\site\jacoco\index.html"

echo.
echo Coverage report opened in browser
echo Please check the coverage manually:
echo - Line coverage should be above 70%%
echo - Branch coverage should be above 60%%
echo.
pause
