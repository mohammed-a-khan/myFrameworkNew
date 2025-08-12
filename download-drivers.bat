@echo off
echo ==========================================
echo Downloading WebDriver Files for Windows
echo ==========================================
echo.

:: Create drivers directory
if not exist "drivers" mkdir drivers

echo Downloading IEDriverServer (32-bit)...
powershell -Command "& {Invoke-WebRequest -Uri 'https://github.com/SeleniumHQ/selenium/releases/download/selenium-4.14.0/IEDriverServer_Win32_4.14.0.zip' -OutFile 'IEDriverServer.zip'}"
powershell -Command "& {Expand-Archive -Path 'IEDriverServer.zip' -DestinationPath 'drivers' -Force}"
del IEDriverServer.zip
echo IEDriverServer downloaded to drivers\IEDriverServer.exe

echo.
echo Downloading EdgeDriver...
:: Detect Edge version
for /f "tokens=3" %%i in ('reg query "HKEY_CURRENT_USER\Software\Microsoft\Edge\BLBeacon" /v version 2^>nul ^| findstr version') do set EDGE_VERSION=%%i
echo Detected Edge version: %EDGE_VERSION%

:: Download matching EdgeDriver
set EDGE_MAJOR=%EDGE_VERSION:~0,3%
echo Downloading EdgeDriver for version %EDGE_MAJOR%...
powershell -Command "& {Invoke-WebRequest -Uri 'https://msedgedriver.azureedge.net/%EDGE_VERSION%/edgedriver_win64.zip' -OutFile 'edgedriver.zip'}"
powershell -Command "& {Expand-Archive -Path 'edgedriver.zip' -DestinationPath 'temp_edge' -Force}"
move /Y temp_edge\msedgedriver.exe drivers\msedgedriver.exe
rmdir /S /Q temp_edge
del edgedriver.zip
echo EdgeDriver downloaded to drivers\msedgedriver.exe

echo.
echo Downloading ChromeDriver...
powershell -Command "& {Invoke-WebRequest -Uri 'https://storage.googleapis.com/chrome-for-testing-public/131.0.6778.204/win64/chromedriver-win64.zip' -OutFile 'chromedriver.zip'}"
powershell -Command "& {Expand-Archive -Path 'chromedriver.zip' -DestinationPath 'temp_chrome' -Force}"
move /Y temp_chrome\chromedriver-win64\chromedriver.exe drivers\chromedriver.exe
rmdir /S /Q temp_chrome
del chromedriver.zip
echo ChromeDriver downloaded to drivers\chromedriver.exe

echo.
echo ==========================================
echo All drivers downloaded successfully!
echo ==========================================
echo.
echo Drivers are located in: %CD%\drivers
echo.
pause