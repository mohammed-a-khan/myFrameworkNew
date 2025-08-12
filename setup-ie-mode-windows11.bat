@echo off
echo ==========================================
echo Windows 11 IE Mode Setup for Selenium
echo ==========================================
echo.

echo STEP 1: Enable Internet Explorer 11 in Windows Features
echo --------------------------------------------------------
echo Opening Windows Features dialog...
start /wait optionalfeatures
echo Please ensure "Internet Explorer 11" is CHECKED
echo.
pause

echo STEP 2: Configure Internet Options
echo -----------------------------------
echo Opening Internet Options...
start inetcpl.cpl
echo.
echo Please configure the following:
echo.
echo 1. Security Tab:
echo    - Select each zone (Internet, Local intranet, Trusted sites, Restricted)
echo    - Either enable or disable Protected Mode for ALL zones (must be same)
echo    - Add your test URL to Trusted Sites:
echo      * Click "Trusted sites" zone
echo      * Click "Sites" button
echo      * Add: https://opensource-demo.orangehrmlive.com
echo      * Add any other test URLs
echo.
echo 2. Advanced Tab:
echo    - UNCHECK "Enable Enhanced Protected Mode"
echo.
echo 3. General Tab:
echo    - Click "Settings" under Browsing history
echo    - Under "Check for newer versions of stored pages"
echo    - Select "Every time I visit the webpage"
echo.
pause

echo STEP 3: Configure Edge Settings
echo --------------------------------
echo Opening Edge settings...
start msedge://settings/defaultBrowser
echo.
echo 1. Go to "Default browser" settings
echo 2. Under "Internet Explorer compatibility"
echo 3. Set "Allow sites to be reloaded in Internet Explorer mode" to "Allow"
echo 4. Add your test sites to IE mode list if needed
echo.
pause

echo STEP 4: Registry Settings (Run as Administrator)
echo -------------------------------------------------
echo Adding registry entries for IE mode compatibility...
echo.

:: Enable IE mode in Edge
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Policies\Microsoft\Edge" /v "InternetExplorerIntegrationLevel" /t REG_DWORD /d 1 /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Policies\Microsoft\Edge" /v "InternetExplorerIntegrationSiteList" /t REG_SZ /d "" /f

:: Configure IE settings
reg add "HKEY_CURRENT_USER\Software\Microsoft\Internet Explorer\Main" /v "TabProcGrowth" /t REG_DWORD /d 0 /f
reg add "HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings\Zones\3" /v "2500" /t REG_DWORD /d 0 /f

echo Registry settings applied.
echo.

echo STEP 5: Verify Installation
echo ----------------------------
echo Checking if IE11 is enabled...
dism /online /get-features | findstr /i "Internet-Explorer"
echo.

echo STEP 6: Test IEDriverServer
echo ----------------------------
echo Please ensure:
echo 1. You are NOT running as Administrator
echo 2. IEDriverServer.exe is in your PATH or project
echo 3. Using IEDriverServer 4.8.1 or later (32-bit recommended)
echo 4. Using Selenium 4.8.1 or later
echo.

echo ==========================================
echo Setup Complete! 
echo ==========================================
echo.
echo IMPORTANT REMINDERS:
echo - DO NOT run your tests as Administrator
echo - Add test URLs to Trusted Sites
echo - Restart your computer if this is first time setup
echo.
pause