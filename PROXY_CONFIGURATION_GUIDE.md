# Proxy Configuration Guide

## Overview
This guide explains how to configure the framework to work behind corporate proxy servers that require all internet connections to go through a proxy.

## Why Proxy Configuration is Needed

WebDriverManager (part of Selenium 4) automatically downloads browser drivers from:
- **ChromeDriver**: From Google's servers
- **EdgeDriver**: From Microsoft's Azure CDN
- **FirefoxDriver**: From Mozilla's servers
- **IEDriverServer**: From Selenium's GitHub releases

If your company network blocks direct internet access, you need to configure proxy settings.

## Quick Setup

### 1. Edit `application.properties`

Add these lines to `resources/config/application.properties`:

```properties
# Enable proxy
proxy.enabled=true

# Your company's proxy server
proxy.host=proxy.yourcompany.com
proxy.port=8080

# Optional - only if your proxy requires authentication
proxy.username=
proxy.password=
```

### 2. That's It!
The framework will automatically use the proxy for all driver downloads.

## Configuration Options

### Basic Configuration (Most Common)
```properties
# Enable proxy for driver downloads
proxy.enabled=true

# Proxy server details (REQUIRED when proxy.enabled=true)
proxy.host=proxy.company.com
proxy.port=8080
```

### With Authentication (If Required)
```properties
# Enable proxy
proxy.enabled=true

# Proxy server
proxy.host=proxy.company.com
proxy.port=8080

# Proxy credentials (optional - leave empty if not needed)
proxy.username=your.username
proxy.password=your.password
```

### With Non-Proxy Hosts
```properties
# Enable proxy
proxy.enabled=true

# Proxy server
proxy.host=proxy.company.com
proxy.port=8080

# Hosts that should bypass proxy (optional)
proxy.nonProxyHosts=localhost,127.0.0.1,*.internal.company.com,intranet
```

## How It Works

When `proxy.enabled=true`, the framework:

1. **Configures System Properties**
   - Sets `http.proxyHost` and `http.proxyPort`
   - Sets `https.proxyHost` and `https.proxyPort`
   - Configures non-proxy hosts if specified

2. **Configures WebDriverManager**
   - Uses WebDriverManager's proxy API
   - Adds authentication if provided
   - Applied to all driver downloads

3. **Automatic Fallback**
   - If proxy fails, tries local drivers in `drivers/` folder
   - Logs clear error messages

## Testing Your Proxy Configuration

### 1. Enable Proxy and Test
```properties
proxy.enabled=true
proxy.host=your.proxy.here
proxy.port=8080
```

### 2. Run a Simple Test
```bash
mvn test -Dtest=CSBDDRunner -DsuiteXmlFile=suites/test-proxy.xml
```

### 3. Check Logs
Look for these messages:
```
INFO  Configuring WebDriverManager to use proxy: proxy.company.com:8080
INFO  Proxy configuration completed. All driver downloads will use proxy
DEBUG WebDriverManager configured with proxy: http://proxy.company.com:8080
```

## Troubleshooting

### Issue: "UnknownHostException: msedgedriver.azureedge.net"
**Cause**: Cannot reach Microsoft's servers to download Edge driver
**Solution**: Enable proxy configuration

### Issue: "Proxy is enabled but host or port is not configured"
**Cause**: `proxy.enabled=true` but `proxy.host` or `proxy.port` is empty
**Solution**: Provide both proxy.host and proxy.port values

### Issue: "407 Proxy Authentication Required"
**Cause**: Your proxy requires username/password
**Solution**: Set `proxy.username` and `proxy.password`

### Issue: Driver download still fails with proxy
**Solutions**:
1. Verify proxy settings are correct
2. Check if proxy allows HTTPS connections
3. Download drivers manually and place in `drivers/` folder
4. Run `download-drivers.bat` from a machine with internet access

## Manual Driver Download (Backup Option)

If proxy configuration doesn't work, manually download drivers:

1. Download drivers on a machine with internet access
2. Place them in the `drivers/` folder:
   ```
   project-root/
   └── drivers/
       ├── chromedriver.exe
       ├── msedgedriver.exe
       ├── geckodriver.exe
       └── IEDriverServer.exe
   ```
3. The framework will use local drivers automatically

## Command Line Override

You can also set proxy via command line:
```bash
mvn test -Dproxy.enabled=true -Dproxy.host=proxy.company.com -Dproxy.port=8080
```

## Environment-Specific Proxy

For different environments, create separate properties files:
- `application-dev.properties` - Development proxy
- `application-qa.properties` - QA proxy  
- `application-prod.properties` - Production proxy

## Security Notes

1. **Don't commit passwords**: Never commit proxy passwords to version control
2. **Use environment variables**: For passwords, consider using environment variables
3. **Encrypt sensitive data**: For production, consider encrypting proxy credentials

## Summary

- ✅ Simple configuration - just set host and port
- ✅ No code changes required
- ✅ Works with all browsers
- ✅ Automatic fallback to local drivers
- ✅ Clear error messages
- ✅ Optional authentication support

The framework handles all the complexity of proxy configuration internally. Just set `proxy.enabled=true` with your proxy details, and everything works!