# Windows 11 Internet Explorer Issue

## Problem
On Windows 11, Internet Explorer has been retired and removed. When you try to use the IE WebDriver, Windows automatically redirects to Microsoft Edge. This causes tests to hang or fail because:
1. The IEDriverServer expects to communicate with Internet Explorer
2. Windows opens Edge instead
3. The driver cannot establish proper communication with Edge
4. Tests hang indefinitely waiting for the driver to initialize

## Symptoms
- Edge browser opens instead of IE when `browser.name=ie` is configured
- Test hangs after "Attempting to create InternetExplorerDriver instance..."
- Browser shows "Internet Explorer mode" in Edge
- URL is not loaded and test execution stops

## Root Cause
Microsoft has officially retired Internet Explorer on Windows 11. The OS automatically redirects IE requests to Edge in "IE mode". However, the IEDriverServer.exe is designed to work with actual Internet Explorer, not Edge in IE mode.

## Solutions

### Option 1: Use Windows 10 or Earlier
Internet Explorer still works properly on:
- Windows 10 (with IE 11)
- Windows Server 2019
- Windows Server 2016

### Option 2: Use Edge in IE Mode (Recommended for Windows 11)
Instead of using `browser.name=ie`, use Edge with IE compatibility:

```xml
<!-- In your TestNG suite XML -->
<parameter name="browser.name" value="edge"/>
<parameter name="edge.ie.mode" value="true"/>
```

Or in properties file:
```properties
browser.name=edge
edge.ie.mode=true
```

### Option 3: Use Edge Directly
For modern web applications, use Edge without IE mode:
```xml
<parameter name="browser.name" value="edge"/>
```

### Option 4: Virtual Machine with Windows 10
Run tests in a Windows 10 VM where actual IE is available.

## Framework Updates
The framework now includes:
1. **Timeout mechanism**: IE driver creation times out after 30 seconds to prevent hanging
2. **Synchronization**: Only one thread can create IE driver at a time
3. **Force sequential mode**: When IE is detected, parallel execution is disabled
4. **Better error messages**: Clear indication when Windows redirects to Edge

## Configuration for Edge IE Mode
If you need IE compatibility on Windows 11, configure Edge in IE mode:

```properties
# Use Edge browser
browser.name=edge

# Enable IE compatibility mode
edge.ie.mode=true

# IE compatibility settings
edge.ie.compatibility.version=11
edge.ie.ignore.protected.mode=true
edge.ie.ignore.zoom=true
```

## Testing Recommendations

### For Legacy Applications (requiring IE)
1. Use Windows 10 or Windows Server 2019
2. Or use Edge in IE mode on Windows 11
3. Test thoroughly as behavior may differ

### For Modern Applications
1. Use Chrome, Firefox, or Edge (without IE mode)
2. These browsers support parallel execution
3. Better performance and reliability

## Parallel Execution Note
Internet Explorer (and Edge in IE mode) do not handle parallel execution well. The framework automatically forces sequential execution when IE is detected. For parallel testing, use:
- Chrome
- Firefox
- Edge (regular mode)

## Error Messages Explained

### "IE driver creation timed out after 30 seconds"
- Windows is redirecting to Edge
- IEDriverServer cannot communicate with Edge
- Solution: Use Edge browser directly

### "Windows redirected to Edge instead of IE"
- Windows 11 automatic redirection occurred
- Tests may not work as expected
- Solution: Use Edge with IE mode configuration

## Checking Your Windows Version
```cmd
winver
```
- Windows 11: IE is not available, will redirect to Edge
- Windows 10: IE 11 is available and should work
- Windows Server 2019/2016: IE is available

## Microsoft's Official Position
As of June 15, 2022, Internet Explorer 11 has been permanently disabled on Windows 11 and certain versions of Windows 10. Microsoft recommends using Microsoft Edge with IE mode for legacy compatibility needs.

## References
- [Microsoft: Internet Explorer Retirement](https://docs.microsoft.com/en-us/lifecycle/announcements/internet-explorer-11-end-of-support)
- [Edge IE Mode Documentation](https://docs.microsoft.com/en-us/deployedge/edge-ie-mode)
- [Selenium IE Driver Documentation](https://www.selenium.dev/documentation/webdriver/browsers/internet_explorer/)