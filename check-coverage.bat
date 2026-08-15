@echo off
echo JaCoCo Coverage Check
echo ================================

REM First run tests to generate coverage data
echo Running tests to generate coverage data...
call .\gradlew.bat test jacocoTestReport

echo.
echo Coverage report generated successfully
echo Report location: build\reports\jacoco\test\html\index.html

REM Check if report file exists
if exist "build\reports\jacoco\test\html\index.html" (
    echo Report file found successfully
    
    REM Open the report in browser
    echo.
    echo Opening detailed coverage report in browser...
    start "" "build\reports\jacoco\test\html\index.html"
    
    echo.
    echo Coverage report opened in browser
    echo Please check the coverage manually:
    echo - Line coverage should be above 70%%
    echo - Branch coverage should be above 60%%
) else (
    echo ERROR: Coverage report not found at build\reports\jacoco\test\html\index.html
    echo Please check if JaCoCo is properly configured
)

echo.
echo Done.
pause
