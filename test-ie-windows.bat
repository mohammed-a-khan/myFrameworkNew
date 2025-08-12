@echo off
echo ==========================================
echo Testing IE Mode on Windows 11
echo ==========================================
echo.

:: First download drivers if needed
if not exist "drivers\IEDriverServer.exe" (
    echo Drivers not found. Downloading...
    call download-drivers.bat
)

:: Set driver paths
set PATH=%PATH%;%CD%\drivers

:: Run test with IE
echo Running test with IE browser...
mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -Dwebdriver.ie.driver=drivers\IEDriverServer.exe -Dwebdriver.edge.driver=drivers\msedgedriver.exe

echo.
echo Test complete. Check the logs above for results.
pause