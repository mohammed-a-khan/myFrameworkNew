# IE Testing on Windows - Complete Guide

## ⚠️ Important: Platform Requirements

### ❌ CANNOT Run IE Tests On:
- **WSL/WSL2** (Windows Subsystem for Linux)
- **Linux**
- **Mac**
- **Docker containers** (unless Windows-based)

### ✅ CAN Run IE Tests On:
- **Windows 10** (native)
- **Windows 11** (native - will use Edge IE mode)
- **Windows Server 2016/2019/2022**

## Quick Start for Windows

### Step 1: Download Drivers
Run as Administrator in Command Prompt:
```batch
download-drivers.bat
```

This will download:
- IEDriverServer.exe (32-bit, version 4.14.0)
- msedgedriver.exe (matching your Edge version)
- chromedriver.exe

### Step 2: Configure Windows for IE Testing

Run as Administrator:
```batch
setup-ie-mode-windows11.bat
```

This will:
1. Enable Internet Explorer 11 in Windows Features
2. Configure Internet Options (Protected Mode, Zoom, etc.)
3. Set up Edge IE mode compatibility
4. Add registry entries for IE mode

### Step 3: Run Your Tests

```batch
test-ie-windows.bat
```

Or manually:
```batch
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml
```

## How It Works on Windows 11

Since IE is retired on Windows 11, our framework:

1. **First Attempt**: Tries to use IEDriverServer with Edge IE mode
   - Uses `attachToEdgeChrome()` method
   - Configures `FORCE_CREATE_PROCESS` capability
   - Sets up all IE mode specific settings

2. **Automatic Fallback**: If IE mode fails (common issue)
   - Automatically switches to Edge browser
   - Tests continue without interruption
   - Clear logging shows what happened

## Common Issues and Solutions

### Issue 1: "Illegal key values seen in w3c capabilities"
**Solution**: Fixed in latest code - removed duplicate capabilities

### Issue 2: "Cannot download Edge driver" 
**Solution**: 
- Run `download-drivers.bat` to get drivers offline
- Or ensure internet connectivity to msedgedriver.azureedge.net

### Issue 3: "Protected Mode settings must be same"
**Solution**: 
1. Open Internet Options (inetcpl.cpl)
2. Security tab → Set Protected Mode same for all zones
3. Or run `setup-ie-mode-windows11.bat`

### Issue 4: Tests hang at "about:blank"
**Solution**: 
- Framework will timeout after 60 seconds
- Automatically fallback to Edge
- No manual intervention needed

### Issue 5: Running from WSL
**Solution**: 
- Exit WSL (`exit` command)
- Open Windows Command Prompt or PowerShell
- Run tests from there

## Test Configuration

In your test suite XML:
```xml
<parameter name="browser.name" value="ie"/>
```

The framework handles everything else automatically.

## What Happens Behind the Scenes

### On Windows 11:
```
1. Test requests IE browser
2. Framework detects Windows 11
3. Attempts IEDriverServer with Edge IE mode config
4. If successful → Tests run in Edge IE mode
5. If timeout → Automatically switches to Edge
6. Tests complete successfully either way
```

### On Windows 10:
```
1. Test requests IE browser
2. Framework uses standard IEDriverServer
3. Internet Explorer opens
4. Tests run normally
```

## Verification

To verify your setup:
```batch
mvn test -Dtest=CSBDDRunner -DsuiteXmlFile=suites/orangehrm-failure-test.xml
```

Check logs for:
- "Windows 11 detected" (if on Win11)
- "IE MODE INITIALIZATION" 
- "EDGE DRIVER READY - FALLBACK SUCCESSFUL" (if fallback occurred)

## Manual Driver Setup (Optional)

If automatic download fails:

1. Download IEDriverServer: https://selenium.dev/downloads/
   - Choose 32-bit version (more stable)
   - Version 4.14.0 or later

2. Download Edge Driver: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/
   - Match your Edge version

3. Place in `drivers/` folder:
   ```
   project-root/
   └── drivers/
       ├── IEDriverServer.exe
       ├── msedgedriver.exe
       └── chromedriver.exe
   ```

## Summary

- **Windows native required** - No WSL/Linux
- **Automatic handling** - Framework manages Windows 11 IE mode issues
- **Zero code changes** - Just use `browser.name=ie`
- **Fallback protection** - Tests won't fail due to IE issues

For any issues, check:
1. You're on native Windows (not WSL)
2. Drivers are downloaded (`drivers/` folder)
3. Windows is configured (`setup-ie-mode-windows11.bat`)
4. Test logs for specific error messages