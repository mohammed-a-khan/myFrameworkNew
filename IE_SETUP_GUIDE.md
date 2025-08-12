# Internet Explorer Setup Guide

## Overview
This framework now supports Internet Explorer (IE) for legacy application testing. IE requires specific configuration on Windows machines to work properly with Selenium.

## Prerequisites

### 1. Windows Configuration
Before running tests on IE, you must configure Windows settings:

#### Security Zones Configuration
1. Open Internet Explorer
2. Go to **Tools → Internet Options → Security tab**
3. For **ALL** zones (Internet, Local intranet, Trusted sites, Restricted sites):
   - Either enable Protected Mode for all zones
   - OR disable Protected Mode for all zones
   - **They must all be the same!**

#### Enhanced Protected Mode
1. Go to **Tools → Internet Options → Advanced tab**
2. Under Security section, **uncheck** "Enable Enhanced Protected Mode"
3. Click Apply and OK

#### Browser Zoom Level
1. Open Internet Explorer
2. Press `Ctrl + 0` to reset zoom to 100%
3. Or go to **View → Zoom → 100%**

#### Enable JavaScript
1. Go to **Tools → Internet Options → Security tab**
2. Click "Custom level..."
3. Under Scripting section:
   - Enable "Active scripting"
   - Enable "Allow Programmatic clipboard access"
   - Enable "Allow status bar updates via script"

### 2. IEDriverServer Setup
The framework uses WebDriverManager to automatically download IEDriverServer.exe. 

**Manual Setup (Optional):**
1. Download IEDriverServer from: https://www.selenium.dev/downloads/
2. Choose 32-bit or 64-bit based on your IE installation
3. Extract and place in a folder
4. Add to PATH or specify in `application.properties`:
   ```properties
   ie.driver.path=C:/drivers/IEDriverServer.exe
   ```

## Configuration Properties

Add these to your `application.properties`:

```properties
# Browser type
browser.name=ie

# IE-specific settings
ie.ignore.security.domains=true        # Ignore security domain settings
ie.ignore.zoom=true                    # Ignore zoom level requirements
ie.ignore.protected.mode=true          # Ignore protected mode settings
ie.require.window.focus=false          # Don't require window focus
ie.enable.persistent.hovering=true     # Enable persistent hovering
ie.native.events=true                  # Use native events
ie.ensure.clean.session=true           # Start with clean session
ie.enable.javascript=true              # Enable JavaScript
ie.page.load.strategy=normal           # Page load strategy (normal/eager/none)
```

## Usage

### Command Line
```bash
# Run tests with IE
mvn test -Dbrowser.name=ie

# Run specific test suite with IE
mvn test -DsuiteXmlFile=suites/test.xml -Dbrowser.name=ie

# Run with specific IE driver path
mvn test -Dbrowser.name=ie -Die.driver.path=C:/drivers/IEDriverServer.exe
```

### TestNG Suite XML
```xml
<suite name="IE Test Suite">
    <parameter name="browser.name" value="ie"/>
    <parameter name="browser.headless" value="false"/> <!-- IE doesn't support headless -->
    
    <test name="IE Test">
        <classes>
            <class name="com.test.TestClass"/>
        </classes>
    </test>
</suite>
```

### In Code
```java
@Test
@CSBrowser("ie")
public void testWithIE() {
    // Your test code
}
```

## Known Issues and Solutions

### Issue 1: Slow Typing
**Problem:** IE types very slowly in input fields.
**Solution:** Use 64-bit IEDriverServer instead of 32-bit.

### Issue 2: Click Not Working
**Problem:** Elements not clickable or clicks don't register.
**Solution:** 
- Enable `ie.require.window.focus=true`
- Use JavaScript clicks as fallback

### Issue 3: Security Certificate Errors
**Problem:** SSL certificate warnings block navigation.
**Solution:**
```java
driver.navigate().to("javascript:document.getElementById('overridelink').click()");
```

### Issue 4: Session Not Created
**Problem:** "Unable to create new session" error.
**Solution:** 
- Check all security zones have same Protected Mode setting
- Run IDE/command prompt as Administrator

### Issue 5: Timeout Issues
**Problem:** Pages take too long to load.
**Solution:**
```properties
ie.page.load.strategy=eager  # Don't wait for all resources
selenium.pageload.timeout=60  # Increase timeout
```

## Best Practices

1. **Always run tests on clean IE instance**
   ```properties
   ie.ensure.clean.session=true
   ```

2. **Use explicit waits** - IE is slower than modern browsers
   ```java
   WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
   ```

3. **Handle alerts carefully** - IE handles alerts differently
   ```java
   try {
       Alert alert = driver.switchTo().alert();
       alert.accept();
   } catch (NoAlertPresentException e) {
       // No alert present
   }
   ```

4. **Test on actual Windows machine** - IE doesn't work on Linux/Mac

5. **Consider using Edge IE Mode** for better compatibility:
   ```properties
   browser.name=edge
   edge.ie.mode=true  # Use Edge in IE compatibility mode
   ```

## Troubleshooting

### Enable Detailed Logging
```properties
# In application.properties
logging.level.org.openqa.selenium.ie=DEBUG
logging.level.com.testforge.cs.driver=DEBUG
```

### Check IE Version
```cmd
# In Command Prompt
reg query "HKEY_LOCAL_MACHINE\Software\Microsoft\Internet Explorer" /v Version
```

### Reset IE Settings
1. Open IE → Tools → Internet Options → Advanced tab
2. Click "Reset..." button
3. Check "Delete personal settings"
4. Click Reset and restart IE

## Alternative: Edge IE Mode
For better performance and compatibility, consider using Edge in IE mode:

```properties
browser.name=edge
edge.use.ie.mode=true
edge.ie.compatibility.version=11
```

This provides IE compatibility with better performance and modern browser features.

## Support
- Selenium IE Documentation: https://www.selenium.dev/documentation/webdriver/browsers/internet_explorer/
- IEDriverServer Downloads: https://www.selenium.dev/downloads/
- Microsoft IE Support: https://support.microsoft.com/en-us/topic/internet-explorer-help-23360e49-9cd3-4dda-ba52-705336cc0de2