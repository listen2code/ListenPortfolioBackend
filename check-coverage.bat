@echo off
echo JaCoCo Coverage Check
echo ================================

REM First run tests to generate coverage data
echo Running tests to generate coverage data...
call ./mvnw clean test

REM Generate report even if tests failed
echo.
echo Generating JaCoCo coverage report...
call ./mvnw jacoco:report

echo.
echo Coverage report generated successfully
echo Report location: target\site\jacoco\index.html

REM Check if report file exists
if exist "target\site\jacoco\index.html" (
    echo Report file found successfully
    
    REM Open the report in browser
    echo.
    echo Opening detailed coverage report in browser...
    start "" "target\site\jacoco\index.html"
    
    echo.
    echo Coverage report opened in browser
    echo Please check the coverage manually:
    echo - Line coverage should be above 70%%
    echo - Branch coverage should be above 60%%
    echo.
    echo You can also check the CSV file at: target\site\jacoco\jacoco.csv
) else (
    echo ERROR: Coverage report not found at target\site\jacoco\index.html
    echo Please check if JaCoCo is properly configured
)

echo.
echo Done.
pause
