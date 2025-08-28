# Local WebDriver Executables

This directory is for storing local WebDriver executable files to enable offline testing without internet connectivity.

## Configuration

The framework will look for drivers in this directory before attempting to download them via WebDriverManager. This behavior can be configured in `application.properties`:

```properties
# Enable/disable local driver lookup
cs.driver.local.lookup.enabled=true

# Directory path for local drivers (relative to project root or absolute path)
cs.driver.local.path=server

# Driver executable names (customize if needed)
cs.driver.chrome.executable=chromedriver.exe
cs.driver.firefox.executable=geckodriver.exe
cs.driver.edge.executable=msedgedriver.exe
cs.driver.ie.executable=IEDriverServer.exe
cs.driver.safari.executable=safaridriver
```

## Driver Downloads

Place the appropriate driver executables in this directory:

### Chrome Driver
- Download from: https://chromedriver.chromium.org/
- File name: `chromedriver.exe` (Windows) or `chromedriver` (Linux/Mac)

### Firefox Driver (GeckoDriver)
- Download from: https://github.com/mozilla/geckodriver/releases
- File name: `geckodriver.exe` (Windows) or `geckodriver` (Linux/Mac)

### Edge Driver
- Download from: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/
- File name: `msedgedriver.exe` (Windows) or `msedgedriver` (Linux/Mac)

### IE Driver
- Download from: https://selenium-release.storage.googleapis.com/
- File name: `IEDriverServer.exe`

### Safari Driver
- Built into macOS, no download needed
- File name: `safaridriver`

## Usage Priority

1. **Local Drivers**: Framework checks this directory first
2. **WebDriverManager**: If local driver not found, attempts download
3. **Error**: If both fail, provides helpful error messages

## Benefits

- **Offline Testing**: No internet required once drivers are placed locally
- **Version Control**: Specific driver versions for consistent testing
- **Network Issues**: Bypass corporate firewalls and connectivity problems
- **Performance**: Faster test startup (no download time)

## File Permissions

Ensure driver executables have proper execution permissions:

```bash
# Linux/Mac
chmod +x chromedriver geckodriver msedgedriver safaridriver
```