package com.testforge.cs.driver;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSFrameworkException;
import com.testforge.cs.exceptions.CSWebDriverException;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
 * WebDriver factory and manager for creating and managing browser instances
 * Supports Chrome, Firefox, Edge, Safari with local and remote execution
 */
public class CSWebDriverManager {
    private static final Logger logger = LoggerFactory.getLogger(CSWebDriverManager.class);
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private static final Map<String, WebDriver> driverPool = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger browserCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private static volatile int maxBrowsersAllowed = Integer.MAX_VALUE;
    private static Semaphore browserSemaphore = new Semaphore(Integer.MAX_VALUE);
    private static final Object ieDriverLock = new Object(); // Synchronization for IE driver creation
    
    // Register shutdown hook to ensure all browsers are closed
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutdown detected - cleaning up all browsers");
            quitAllDrivers();
        }));
        
        // Configure proxy for WebDriverManager if enabled
        configureWebDriverManagerProxy();
    }
    
    /**
     * Configure proxy for WebDriverManager to download drivers through corporate proxy
     */
    private static void configureWebDriverManagerProxy() {
        try {
            boolean proxyEnabled = config.getBooleanProperty("cs.proxy.enabled", false);
            
            if (proxyEnabled) {
                String proxyHost = config.getProperty("cs.proxy.host", "");
                String proxyPort = config.getProperty("cs.proxy.port", "");
                
                if (proxyHost.isEmpty() || proxyPort.isEmpty()) {
                    logger.warn("Proxy is enabled but host or port is not configured. Proxy will not be used.");
                    return;
                }
                
                // Set proxy for WebDriverManager
                String proxyUrl = proxyHost + ":" + proxyPort;
                logger.info("Configuring WebDriverManager to use proxy: {}", proxyUrl);
                
                // Configure proxy using system properties (WebDriverManager respects these)
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", proxyPort);
                
                // Optional: Set non-proxy hosts
                String nonProxyHosts = config.getProperty("cs.proxy.nonProxyHosts", "localhost,127.0.0.1");
                if (!nonProxyHosts.isEmpty()) {
                    System.setProperty("http.nonProxyHosts", nonProxyHosts);
                    System.setProperty("https.nonProxyHosts", nonProxyHosts);
                }
                
                // Optional: Set proxy authentication if provided
                String proxyUsername = config.getProperty("cs.proxy.username", "");
                String proxyPassword = config.getProperty("cs.proxy.password", "");
                
                if (!proxyUsername.isEmpty() && !proxyPassword.isEmpty()) {
                    logger.info("Proxy authentication configured for user: {}", proxyUsername);
                    
                    // Set authenticator for proxy
                    java.net.Authenticator.setDefault(new java.net.Authenticator() {
                        @Override
                        protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestorType() == RequestorType.PROXY) {
                                return new java.net.PasswordAuthentication(
                                    proxyUsername, 
                                    proxyPassword.toCharArray()
                                );
                            }
                            return null;
                        }
                    });
                    
                    // Also set for WebDriverManager specifically
                    System.setProperty("http.proxyUser", proxyUsername);
                    System.setProperty("http.proxyPassword", proxyPassword);
                    System.setProperty("https.proxyUser", proxyUsername);
                    System.setProperty("https.proxyPassword", proxyPassword);
                }
                
                logger.info("Proxy configuration completed. All driver downloads will use proxy: {}:{}", 
                    proxyHost, proxyPort);
                
            } else {
                logger.debug("Proxy is disabled. WebDriverManager will connect directly to internet.");
            }
            
        } catch (Exception e) {
            logger.error("Failed to configure proxy for WebDriverManager: {}", e.getMessage());
            logger.warn("WebDriverManager will attempt to connect without proxy.");
        }
    }
    
    /**
     * Set maximum number of browsers allowed
     */
    public static synchronized void setMaxBrowsersAllowed(int max) {
        maxBrowsersAllowed = max;
        browserSemaphore = new Semaphore(max);
        logger.info("Maximum browsers allowed set to: {} (Semaphore permits: {})", max, browserSemaphore.availablePermits());
    }
    
    /**
     * Setup WebDriverManager with proxy configuration if needed
     * This method ensures proxy is properly configured for each driver download
     */
    private static void setupWebDriverManager(WebDriverManager manager) {
        // The proxy is already configured via system properties in static block
        // But we can add additional WebDriverManager-specific proxy settings here if needed
        
        boolean proxyEnabled = config.getBooleanProperty("cs.proxy.enabled", false);
        if (proxyEnabled) {
            String proxyHost = config.getProperty("cs.proxy.host", "");
            String proxyPort = config.getProperty("cs.proxy.port", "");
            
            if (!proxyHost.isEmpty() && !proxyPort.isEmpty()) {
                // WebDriverManager also supports proxy configuration via its own API
                String proxyUrl = "http://" + proxyHost + ":" + proxyPort;
                manager.proxy(proxyUrl);
                
                // Set proxy user/pass if provided
                String proxyUser = config.getProperty("cs.proxy.username", "");
                String proxyPass = config.getProperty("cs.proxy.password", "");
                if (!proxyUser.isEmpty() && !proxyPass.isEmpty()) {
                    manager.proxyUser(proxyUser);
                    manager.proxyPass(proxyPass);
                }
                
                logger.debug("WebDriverManager configured with proxy: {}", proxyUrl);
            }
        }
        
        // Now setup the driver
        manager.setup();
    }
    
    /**
     * Create WebDriver instance
     * Synchronized to prevent race conditions during parallel browser creation
     */
    public static synchronized WebDriver createDriver(String browserType, boolean headless, Map<String, Object> capabilities) {
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        
        // Check if this thread already has a driver
        WebDriver existingDriver = threadLocalDriver.get();
        if (existingDriver != null) {
            try {
                // Check if the driver is still valid
                existingDriver.getTitle();
                logger.warn("Thread {} (ID: {}) already has an ACTIVE driver! Returning existing driver.", threadName, threadId);
                return existingDriver;
            } catch (Exception e) {
                logger.info("Thread {} (ID: {}) has a stale driver, will create new one", threadName, threadId);
                threadLocalDriver.remove();
                String threadIdKey = String.valueOf(threadId);
                driverPool.remove(threadIdKey);
            }
        }
        
        // Try to acquire a permit to create a browser
        boolean acquired = false;
        try {
            logger.info("Thread {} attempting to acquire browser permit. Available permits: {}", 
                threadName, browserSemaphore.availablePermits());
            acquired = browserSemaphore.tryAcquire();
            if (!acquired) {
                logger.error("!!! BROWSER LIMIT REACHED !!! Thread {} cannot create browser. Max allowed: {}", 
                    threadName, maxBrowsersAllowed);
                return null;
            }
            logger.info("Thread {} acquired browser permit successfully", threadName);
        } catch (Exception e) {
            logger.error("Error acquiring browser permit", e);
            return null;
        }
        
        int currentCount = browserCount.incrementAndGet();
        logger.error("!!! BROWSER #{} BEING CREATED !!! Thread: {} (ID: {}), Type: {}", 
            currentCount, threadName, threadId, browserType);
        logger.error("Current driver pool size before creation: {}", driverPool.size());
        
        WebDriver driver = null;
        
        try {
            // Check if remote execution is enabled
            String remoteUrl = config.getProperty("cs.selenium.remote.url");
            if (remoteUrl != null && !remoteUrl.isEmpty()) {
                driver = createRemoteDriver(browserType, remoteUrl, headless, capabilities);
            } else {
                driver = createLocalDriver(browserType, headless, capabilities);
            }
            
            // Configure driver
            configureDriver(driver);
            
            // Store in thread local
            threadLocalDriver.set(driver);
            
            // Store in pool with thread ID
            String threadIdKey = String.valueOf(threadId);
            driverPool.put(threadIdKey, driver);
            
            logger.info("Browser successfully created for thread {}. Active browsers: {}", 
                threadName, driverPool.size());
            
            return driver;
        } catch (Exception e) {
            // If driver creation fails, release the permit
            if (acquired) {
                browserSemaphore.release();
                logger.info("Released browser permit due to creation failure");
            }
            throw e;
        }
    }
    
    /**
     * Create local WebDriver
     */
    private static WebDriver createLocalDriver(String browserType, boolean headless, Map<String, Object> capabilities) {
        switch (browserType.toLowerCase()) {
            case "chrome":
                return createChromeDriver(headless, capabilities);
            case "firefox":
                return createFirefoxDriver(headless, capabilities);
            case "edge":
                return createEdgeDriver(headless, capabilities);
            case "safari":
                return createSafariDriver(capabilities);
            case "ie":
            case "internetexplorer":
            case "internet explorer":
                // Try IE driver first, fallback to Edge on Windows 11 if it fails
                try {
                    return createInternetExplorerDriver(capabilities);
                } catch (CSWebDriverException e) {
                    // Check if we're on Windows 11 and should fallback to Edge
                    if (isWindows11() && e.getMessage().contains("Windows 11")) {
                        logger.warn("=================================================");
                        logger.warn("IE DRIVER FAILED - AUTOMATICALLY SWITCHING TO EDGE");
                        logger.warn("=================================================");
                        logger.warn("Reason: {}", e.getMessage());
                        logger.warn("Attempting to use Edge browser with IE mode support...");
                        
                        // Create Edge with IE mode instead
                        return createEdgeWithIEMode(capabilities);
                    }
                    throw e; // Re-throw if not Windows 11 related
                }
            default:
                throw new CSFrameworkException("Unsupported browser type: " + browserType);
        }
    }
    
    /**
     * Create Chrome driver
     */
    private static WebDriver createChromeDriver(boolean headless, Map<String, Object> capabilities) {
        // Setup ChromeDriver using WebDriverManager
        setupWebDriverManager(WebDriverManager.chromedriver());
        
        ChromeOptions options = new ChromeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Add default arguments
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach((key, value) -> {
                if (value instanceof String) {
                    options.addArguments(key + "=" + value);
                } else {
                    options.setCapability(key, value);
                }
            });
        }
        
        // Add preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        String downloadPath = config.getProperty("cs.browser.download.directory", System.getProperty("user.dir") + "/downloads");
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);
        
        return new ChromeDriver(options);
    }
    
    /**
     * Create Firefox driver
     */
    private static WebDriver createFirefoxDriver(boolean headless, Map<String, Object> capabilities) {
        // Setup FirefoxDriver using WebDriverManager with proxy support
        setupWebDriverManager(WebDriverManager.firefoxdriver());
        
        FirefoxOptions options = new FirefoxOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        // Add preferences
        String downloadPath = config.getProperty("cs.browser.download.directory", System.getProperty("user.dir") + "/downloads");
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadPath);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", 
            "application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }
        
        return new FirefoxDriver(options);
    }
    
    /**
     * Create Edge driver
     */
    private static WebDriver createEdgeDriver(boolean headless, Map<String, Object> capabilities) {
        // Setup EdgeDriver using WebDriverManager with proxy support
        setupWebDriverManager(WebDriverManager.edgedriver());
        
        EdgeOptions options = new EdgeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Add default arguments (similar to Chrome)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }
        
        return new EdgeDriver(options);
    }
    
    /**
     * Create Safari driver
     */
    private static WebDriver createSafariDriver(Map<String, Object> capabilities) {
        SafariOptions options = new SafariOptions();
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }
        
        return new SafariDriver(options);
    }
    
    /**
     * Create Internet Explorer driver
     * Note: IE requires specific setup:
     * 1. Enable Protected Mode must be same for all zones
     * 2. Enhanced Protected Mode must be disabled
     * 3. Zoom level must be 100%
     * 4. IEDriverServer.exe must be in PATH or use WebDriverManager
     */
    private static WebDriver createInternetExplorerDriver(Map<String, Object> capabilities) {
        // Check if running on Windows
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("windows")) {
            logger.error("========================================");
            logger.error("IE DRIVER CANNOT RUN ON THIS PLATFORM");
            logger.error("========================================");
            logger.error("Current OS: {}", System.getProperty("os.name"));
            logger.error("Current Platform: {}", System.getProperty("os.version"));
            
            if (osName.contains("linux") && System.getProperty("os.version").contains("WSL")) {
                logger.error("");
                logger.error("You are running in WSL (Windows Subsystem for Linux)");
                logger.error("Internet Explorer driver ONLY works on native Windows");
                logger.error("");
                logger.error("SOLUTIONS:");
                logger.error("1. Run tests directly on Windows (not in WSL)");
                logger.error("2. Use Chrome or Edge browser in WSL");
                logger.error("3. Set up Remote WebDriver to connect to Windows host");
                
                throw new CSWebDriverException(
                    "IE driver cannot run in WSL/Linux. Please run tests on Windows or use Chrome/Edge browser."
                );
            }
            
            throw new CSWebDriverException(
                "Internet Explorer driver only runs on Windows. Current OS: " + System.getProperty("os.name")
            );
        }
        
        // IE doesn't handle parallel driver creation well, so synchronize
        synchronized (ieDriverLock) {
            logger.info("Thread {} acquiring IE driver creation lock", Thread.currentThread().getName());
            
            // Check Windows version
            boolean isWindows11 = isWindows11();
            
            if (isWindows11) {
                logger.warn("Windows 11 detected - Edge will open in IE compatibility mode");
                logger.warn("Note: Navigation may be slower as IEDriverServer communicates through IE mode");
            }
            
            // Setup IEDriver using WebDriverManager with proxy support
            // CRITICAL: Use IEDriver 4.8.1 or later for Windows 11 compatibility
            logger.info("Setting up IEDriverServer (version 4.8.1+ required for Windows 11)");
            setupWebDriverManager(WebDriverManager.iedriver().driverVersion("4.14.0").arch32());
            
            InternetExplorerOptions options = new InternetExplorerOptions();
            
            // For Windows 11 - Enable Edge IE mode compatibility (Microsoft + QAF approach)
            if (isWindows11) {
                logger.info("Configuring for Windows 11 Edge IE mode...");
                
                // STEP 1: Find Edge executable
                String edgePath = findEdgeExecutablePath();
                if (edgePath == null) {
                    edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
                }
                logger.info("Using Edge executable: {}", edgePath);
                
                // STEP 2: Core IE mode configuration (Microsoft recommended)
                options.attachToEdgeChrome();
                options.withEdgeExecutablePath(edgePath);
                
                // STEP 3: QAF-style force process creation (CRITICAL for Windows 11)
                options.setCapability(InternetExplorerDriver.FORCE_CREATE_PROCESS, true);
                options.setCapability("ie.forceCreateProcessApi", true);
                
                // STEP 4: IE mode specific capabilities
                options.setCapability("ie.edgechromium", true);
                options.setCapability("ie.edgepath", edgePath);
                
                // STEP 5: Disable native events to prevent hanging
                options.setCapability("nativeEvents", false);
                
                // STEP 6: Required compatibility settings
                options.ignoreZoomSettings();
                options.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
                options.introduceFlakinessByIgnoringSecurityDomains();
                options.setCapability("ignoreProtectedModeSettings", true);
                
                // STEP 7: Performance and stability settings
                options.setCapability(InternetExplorerDriver.REQUIRE_WINDOW_FOCUS, false);
                options.setCapability("requireWindowFocus", false);
                options.setCapability(InternetExplorerDriver.ENABLE_PERSISTENT_HOVERING, false);
                options.setCapability("enablePersistentHover", false);
                
                // STEP 8: Clean session for reliability (use IE-specific constant)
                options.setCapability(InternetExplorerDriver.IE_ENSURE_CLEAN_SESSION, true);
                
                // STEP 9: Additional Edge IE mode settings (avoid duplicates)
                options.setCapability("se:ieOptions", new HashMap<String, Object>() {{
                    put("ie.edgechromium", true);
                    put("ignoreProtectedModeSettings", true);
                    put("ignoreZoomSetting", true);
                    put("ie.ensureCleanSession", true);
                }});
                
                // STEP 10: Set initial URL to about:blank
                options.withInitialBrowserUrl("about:blank");
                
                logger.info("IE mode configuration complete with all QAF + Microsoft settings");
            }
            
            // Use IE-specific methods where available
            if (config.getBooleanProperty("cs.ie.ignore.security.domains", true)) {
                options.introduceFlakinessByIgnoringSecurityDomains();
            }
            
            if (config.getBooleanProperty("cs.ie.ignore.zoom", true)) {
                options.ignoreZoomSettings();
            }
            
            if (config.getBooleanProperty("cs.ie.require.window.focus", false)) {
                options.requireWindowFocus();
            }
            
            if (config.getBooleanProperty("cs.ie.enable.persistent.hovering", true)) {
                options.enablePersistentHovering();
            }
            
            // Don't use native events with Edge redirection
            if (config.getBooleanProperty("cs.ie.native.events", false)) {
                options.usePerProcessProxy();
            }
            
            // This option doesn't have a dedicated method, but is still needed for IE
            if (config.getBooleanProperty("cs.ie.ignore.protected.mode", true)) {
                options.setCapability("ignoreProtectedModeSettings", true);
            }
            
            // Don't clear session when using Edge redirection - it might cause issues
            if (config.getBooleanProperty("cs.ie.ensure.clean.session", false)) {
                options.destructivelyEnsureCleanSession();
            }
            
            // Set initial browser URL - use blank page
            options.withInitialBrowserUrl("about:blank");
        
        // Set page load strategy
        String pageLoadStrategy = config.getProperty("cs.ie.page.load.strategy", "normal");
        options.setPageLoadStrategy(PageLoadStrategy.valueOf(pageLoadStrategy.toUpperCase()));
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }
        
        logger.info("Creating Internet Explorer driver with security domains ignored: {}", 
            config.getBooleanProperty("cs.ie.ignore.security.domains", true));
        
            try {
                // Small delay to prevent initialization conflicts
                Thread.sleep(500);
                
                logger.info("Creating InternetExplorerDriver instance...");
                logger.info("Options configured: attachToEdgeChrome={}, forceCreateProcess={}", 
                    isWindows11, 
                    options.getCapability(InternetExplorerDriver.FORCE_CREATE_PROCESS));
                
                if (isWindows11) {
                    logger.warn("===== WINDOWS 11 IE MODE INITIALIZATION =====");
                    logger.warn("Edge will open in IE compatibility mode");
                    logger.warn("This may take 15-30 seconds to establish connection");
                    logger.warn("If it hangs, ensure:");
                    logger.warn("1. You are NOT running as Administrator");
                    logger.warn("2. Protected Mode is same for all zones");
                    logger.warn("3. Enhanced Protected Mode is disabled");
                    logger.warn("4. Zoom level is 100%");
                }
                
                // Create driver with robust error handling
                InternetExplorerDriver driver = null;
                
                if (isWindows11) {
                    // Windows 11: Create with timeout protection
                    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
                    java.util.concurrent.Future<InternetExplorerDriver> future = executor.submit(() -> {
                        try {
                            logger.info("[IE MODE] Starting driver creation with QAF configuration...");
                            
                            // Log capabilities info for debugging
                            logger.debug("[IE MODE] Configuration includes:");
                            logger.debug("  attachToEdgeChrome = true");
                            logger.debug("  forceCreateProcess = {}", options.getCapability(InternetExplorerDriver.FORCE_CREATE_PROCESS));
                            logger.debug("  ie.edgechromium = {}", options.getCapability("ie.edgechromium"));
                            
                            logger.info("[IE MODE] Invoking InternetExplorerDriver constructor...");
                            InternetExplorerDriver ieDriver = new InternetExplorerDriver(options);
                            logger.info("[IE MODE] Constructor completed successfully!");
                            
                            return ieDriver;
                        } catch (Exception e) {
                            logger.error("[IE MODE] Failed to create driver: {}", e.getMessage());
                            logger.error("[IE MODE] Stack trace:", e);
                            throw new RuntimeException("IE driver creation failed: " + e.getMessage(), e);
                        }
                    });
                    
                    try {
                        // Wait up to 60 seconds for Windows 11 IE mode
                        logger.info("[IE MODE] Waiting for driver initialization (timeout: 60 seconds)...");
                        driver = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
                        logger.info("[IE MODE] Driver created successfully!");
                        
                    } catch (java.util.concurrent.TimeoutException e) {
                        logger.error("========================================");
                        logger.error("IE DRIVER CREATION TIMED OUT AFTER 60 SECONDS");
                        logger.error("========================================");
                        logger.error("This indicates IEDriverServer cannot communicate with Edge IE mode");
                        logger.error("");
                        logger.error("POSSIBLE SOLUTIONS:");
                        logger.error("1. Run the setup script: setup-ie-mode-windows11.bat (as Administrator)");
                        logger.error("2. Use Edge browser directly: browser.name=edge");
                        logger.error("3. Check Windows Event Viewer for more details");
                        logger.error("");
                        
                        future.cancel(true);
                        executor.shutdownNow();
                        
                        // Clean up hanging processes
                        try {
                            logger.info("Cleaning up hanging processes...");
                            Runtime.getRuntime().exec("taskkill /F /IM IEDriverServer.exe");
                            Runtime.getRuntime().exec("taskkill /F /IM msedge.exe");
                            Thread.sleep(1000);
                        } catch (Exception ex) {
                            logger.debug("Process cleanup: {}", ex.getMessage());
                        }
                        
                        throw new CSWebDriverException(
                            "IEDriverServer cannot establish connection with Edge IE mode on Windows 11. " +
                            "This is a known limitation. Please use 'browser.name=edge' instead."
                        );
                        
                    } catch (java.util.concurrent.ExecutionException e) {
                        logger.error("Driver creation failed with exception: {}", e.getCause().getMessage());
                        throw new CSWebDriverException("Failed to create IE driver: " + e.getCause().getMessage(), e.getCause());
                        
                    } finally {
                        executor.shutdown();
                    }
                    
                } else {
                    // Windows 10 and earlier: Create normally
                    logger.info("Creating standard InternetExplorerDriver for Windows 10 or earlier...");
                    driver = new InternetExplorerDriver(options);
                }
                
                logger.info("Driver instance created");
                
                // Wait for the browser to stabilize
                Thread.sleep(2000);
                
                // Set timeouts with longer values for IE mode
                try {
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120)); // Longer timeout for IE mode
                    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
                    driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(60));
                    logger.info("Timeouts configured for IE mode");
                } catch (Exception e) {
                    logger.warn("Failed to set timeouts: {}", e.getMessage());
                }
                
                // Try to navigate away from about:blank to establish connection
                try {
                    logger.info("Establishing connection with browser...");
                    driver.get("about:blank");
                    Thread.sleep(1000);
                    logger.info("Connection established");
                } catch (Exception e) {
                    logger.warn("Initial navigation warning (expected on Windows 11): {}", e.getMessage());
                }
                
                // Get browser info
                try {
                    String browserName = driver.getCapabilities().getBrowserName();
                    logger.info("Browser connected: {}", browserName);
                    
                    if (browserName != null && browserName.toLowerCase().contains("edge")) {
                        logger.info("Confirmed: Running on Edge in IE compatibility mode");
                    }
                } catch (Exception e) {
                    logger.debug("Could not get browser info: {}", e.getMessage());
                }
                
                logger.info("InternetExplorerDriver ready for use");
                logger.info("Thread {} releasing IE driver creation lock", Thread.currentThread().getName());
                return driver;
            } catch (Exception e) {
                logger.error("Failed to create Internet Explorer driver: {}", e.getMessage());
                logger.error("Common causes:");
                logger.error("1. Protected Mode settings are not the same for all zones");
                logger.error("2. Enhanced Protected Mode is enabled");
                logger.error("3. Browser zoom level is not 100%");
                logger.error("4. IE is not installed or IEDriverServer.exe is not compatible");
                logger.info("Thread {} releasing IE driver creation lock after error", Thread.currentThread().getName());
                throw new CSWebDriverException("Failed to create Internet Explorer driver: " + e.getMessage(), e);
            }
        } // End of synchronized block
    }
    
    /**
     * Check if running on Windows 11
     */
    private static boolean isWindows11() {
        try {
            // Try to get Windows version using command
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "ver");
            Process p = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Windows 11 shows as version 10.0 with build >= 22000
                if (line.contains("Version 10.0") && line.contains("22")) {
                    return true;
                }
            }
            
            // Alternative check using system properties
            String osName = System.getProperty("os.name");
            if (osName != null && osName.contains("Windows 11")) {
                return true;
            }
            
            // Check using Windows registry if available
            try {
                ProcessBuilder regPb = new ProcessBuilder("reg", "query", 
                    "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "/v", "CurrentBuild");
                Process regP = regPb.start();
                java.io.BufferedReader regReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(regP.getInputStream()));
                String regLine;
                while ((regLine = regReader.readLine()) != null) {
                    if (regLine.contains("CurrentBuild")) {
                        String[] parts = regLine.trim().split("\\s+");
                        if (parts.length > 2) {
                            int build = Integer.parseInt(parts[parts.length - 1]);
                            if (build >= 22000) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not check registry: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.debug("Could not determine Windows version: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Create Edge driver with IE mode for Windows 11
     * This is the fallback when IEDriverServer fails on Windows 11
     */
    private static WebDriver createEdgeWithIEMode(Map<String, Object> capabilities) {
        logger.info("=================================================");
        logger.info("CREATING EDGE DRIVER AS IE MODE FALLBACK");
        logger.info("=================================================");
        logger.info("This provides the best compatibility for legacy IE applications on Windows 11");
        
        // Setup EdgeDriver with offline fallback and proxy support
        try {
            setupWebDriverManager(WebDriverManager.edgedriver());
        } catch (Exception e) {
            logger.warn("Could not download Edge driver, trying offline mode: {}", e.getMessage());
            // Try to use system Edge driver if available
            String edgePath = System.getProperty("webdriver.edge.driver");
            if (edgePath == null || edgePath.isEmpty()) {
                // Look for Edge driver in common locations
                String[] possiblePaths = {
                    "drivers/msedgedriver.exe",
                    "C:\\drivers\\msedgedriver.exe",
                    System.getProperty("user.dir") + "\\drivers\\msedgedriver.exe"
                };
                
                for (String path : possiblePaths) {
                    if (new java.io.File(path).exists()) {
                        System.setProperty("webdriver.edge.driver", path);
                        logger.info("Using local Edge driver: {}", path);
                        break;
                    }
                }
            }
        }
        
        EdgeOptions options = new EdgeOptions();
        
        // Add standard Edge arguments
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // Note: Edge runs in standard mode but can handle most IE-specific content
        // For true IE mode, sites need to be configured in Edge settings
        logger.info("Note: Edge will run in standard mode.");
        logger.info("For sites requiring IE mode:");
        logger.info("  1. Open Edge Settings > Default Browser");
        logger.info("  2. Add your sites to the IE mode list");
        logger.info("  3. Or use Group Policy for enterprise deployment");
        
        // Add custom capabilities
        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }
        
        try {
            logger.info("Creating Edge driver...");
            EdgeDriver driver = new EdgeDriver(options);
            logger.info("Edge driver created successfully");
            
            // Set timeouts
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            
            logger.info("=================================================");
            logger.info("EDGE DRIVER READY - FALLBACK SUCCESSFUL");
            logger.info("=================================================");
            logger.info("Your tests will run in Edge (Chromium-based browser)");
            logger.info("Most web applications will work correctly");
            
            return driver;
        } catch (Exception e) {
            throw new CSWebDriverException("Failed to create Edge driver as fallback: " + e.getMessage(), e);
        }
    }
    
    
    /**
     * Find Edge executable path on Windows
     */
    private static String findEdgeExecutablePath() {
        String[] possiblePaths = {
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            System.getenv("PROGRAMFILES(X86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
            System.getenv("PROGRAMFILES") + "\\Microsoft\\Edge\\Application\\msedge.exe"
        };
        
        for (String path : possiblePaths) {
            if (path != null && new java.io.File(path).exists()) {
                return path;
            }
        }
        
        logger.warn("Edge executable not found in standard locations");
        return null;
    }
    
    /**
     * Create remote WebDriver
     */
    private static WebDriver createRemoteDriver(String browserType, String remoteUrl, 
                                               boolean headless, Map<String, Object> capabilities) {
        try {
            URL url = new URL(remoteUrl);
            
            switch (browserType.toLowerCase()) {
                case "chrome":
                    ChromeOptions chromeOptions = new ChromeOptions();
                    if (headless) chromeOptions.addArguments("--headless=new");
                    if (capabilities != null) capabilities.forEach(chromeOptions::setCapability);
                    return new RemoteWebDriver(url, chromeOptions);
                    
                case "firefox":
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (headless) firefoxOptions.addArguments("--headless");
                    if (capabilities != null) capabilities.forEach(firefoxOptions::setCapability);
                    return new RemoteWebDriver(url, firefoxOptions);
                    
                case "edge":
                    EdgeOptions edgeOptions = new EdgeOptions();
                    if (headless) edgeOptions.addArguments("--headless=new");
                    if (capabilities != null) capabilities.forEach(edgeOptions::setCapability);
                    return new RemoteWebDriver(url, edgeOptions);
                    
                case "safari":
                    SafariOptions safariOptions = new SafariOptions();
                    if (capabilities != null) capabilities.forEach(safariOptions::setCapability);
                    return new RemoteWebDriver(url, safariOptions);
                    
                default:
                    throw new CSFrameworkException("Unsupported browser type for remote execution: " + browserType);
            }
        } catch (Exception e) {
            throw new CSFrameworkException("Failed to create remote driver", e);
        }
    }
    
    /**
     * Configure driver with common settings
     */
    private static void configureDriver(WebDriver driver) {
        // Set timeouts
        Duration implicitWait = Duration.ofSeconds(config.getIntProperty("cs.wait.implicit", 10));
        Duration pageLoadTimeout = Duration.ofSeconds(config.getIntProperty("cs.wait.pageload.timeout", 30));
        Duration scriptTimeout = Duration.ofSeconds(config.getIntProperty("cs.wait.script.timeout", 30));
        
        driver.manage().timeouts().implicitlyWait(implicitWait);
        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        driver.manage().timeouts().scriptTimeout(scriptTimeout);
        
        // Maximize window
        if (config.getBooleanProperty("cs.browser.maximize", true)) {
            driver.manage().window().maximize();
        }
        
        // Delete cookies
        if (config.getBooleanProperty("cs.browser.delete.cookies", true)) {
            driver.manage().deleteAllCookies();
        }
    }
    
    /**
     * Get current thread's driver
     */
    public static WebDriver getDriver() {
        WebDriver driver = threadLocalDriver.get();
        logger.debug("Getting driver for thread {} (ID: {}): {}", 
            Thread.currentThread().getName(), 
            Thread.currentThread().getId(),
            driver != null ? "FOUND" : "NULL");
        return driver;
    }
    
    /**
     * Set driver for current thread
     */
    public static void setDriver(WebDriver driver) {
        threadLocalDriver.set(driver);
        String threadId = String.valueOf(Thread.currentThread().getId());
        driverPool.put(threadId, driver);
    }
    
    /**
     * Quit current thread's driver
     */
    public static void quitDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Driver quit successfully");
            } catch (Exception e) {
                logger.error("Error quitting driver", e);
            } finally {
                threadLocalDriver.remove();
                String threadId = String.valueOf(Thread.currentThread().getId());
                driverPool.remove(threadId);
                // Release the semaphore permit
                browserSemaphore.release();
                logger.info("Released browser permit. Available permits: {}", browserSemaphore.availablePermits());
            }
        }
    }
    
    /**
     * Quit all drivers - more robust cleanup
     */
    public static synchronized void quitAllDrivers() {
        logger.info("Quitting all {} drivers in pool", driverPool.size());
        
        // Track successfully closed drivers
        int closedCount = 0;
        int alreadyClosedCount = 0;
        
        // Create a copy to avoid concurrent modification
        Map<String, WebDriver> driversCopy = new HashMap<>(driverPool);
        
        for (Map.Entry<String, WebDriver> entry : driversCopy.entrySet()) {
            String threadId = entry.getKey();
            WebDriver driver = entry.getValue();
            
            try {
                if (driver != null) {
                    // Check if driver session is still active
                    try {
                        // Try to get window handle to check if browser is still alive
                        String windowHandle = driver.getWindowHandle();
                        if (windowHandle != null) {
                            logger.info("Closing driver for thread {} (window: {})", threadId, windowHandle);
                            
                            // Close all windows first
                            for (String handle : driver.getWindowHandles()) {
                                driver.switchTo().window(handle);
                                driver.close();
                            }
                            
                            // Now quit the driver
                            driver.quit();
                            closedCount++;
                            
                            // Release semaphore permit for each closed driver
                            browserSemaphore.release();
                            logger.info("Successfully closed driver for thread {}", threadId);
                        }
                    } catch (Exception sessionError) {
                        // Driver session already closed or not responsive
                        logger.debug("Driver session already closed or unresponsive for thread {}: {}", 
                            threadId, sessionError.getMessage());
                        alreadyClosedCount++;
                        
                        // Try force quit anyway
                        try {
                            driver.quit();
                        } catch (Exception e) {
                            // Ignore - driver was already closed
                        }
                        
                        // Still release the permit
                        browserSemaphore.release();
                    }
                }
            } catch (Exception e) {
                logger.error("Error quitting driver for thread {}: {}", threadId, e.getMessage());
            }
        }
        
        logger.info("Driver cleanup complete - Closed: {}, Already closed: {}", 
            closedCount, alreadyClosedCount);
        
        // Clear all references
        driverPool.clear();
        
        // Also check and clear ThreadLocal for current thread
        WebDriver localDriver = threadLocalDriver.get();
        if (localDriver != null) {
            try {
                localDriver.quit();
                logger.debug("Closed thread-local driver for current thread");
            } catch (Exception e) {
                logger.debug("Thread-local driver already closed: {}", e.getMessage());
            }
            threadLocalDriver.remove();
        }
        
        // Reset browser count
        browserCount.set(0);
        
        logger.info("All test browser instances have been closed");
    }
    
    /**
     * Take screenshot
     */
    public static File takeScreenshot(String filePath) {
        try {
            WebDriver driver = getDriver();
            byte[] screenshotData = CSScreenshotUtils.captureScreenshot(driver);
            
            if (screenshotData == null || screenshotData.length == 0) {
                logger.warn("Screenshot data is empty");
                return null;
            }
            
            // Check if we should skip saving to file (when embedding is enabled)
            CSConfigManager config = CSConfigManager.getInstance();
            boolean embedScreenshots = Boolean.parseBoolean(
                config.getProperty("cs.report.screenshots.embed", "false")
            );
            
            if (embedScreenshots) {
                // When embedding, still create a temporary file for processing
                // but it will be deleted after being embedded in the report
                Path path = Paths.get(filePath);
                Files.createDirectories(path.getParent());
                Files.write(path, screenshotData);
                logger.debug("Screenshot temporarily saved for embedding: {}", filePath);
                return path.toFile();
            } else {
                // Normal file saving when not embedding
                Path path = Paths.get(filePath);
                Files.createDirectories(path.getParent());
                Files.write(path, screenshotData);
                logger.info("Screenshot saved to: {}", filePath);
                return path.toFile();
            }
            
        } catch (IOException e) {
            logger.error("Failed to save screenshot", e);
            return null;
        }
    }
    
    /**
     * Take screenshot as bytes
     */
    public static byte[] takeScreenshot() {
        WebDriver driver = getDriver();
        return CSScreenshotUtils.captureScreenshot(driver);
    }
    
    /**
     * Check if driver is active
     */
    public static boolean isDriverActive() {
        WebDriver driver = getDriver();
        if (driver == null) {
            return false;
        }
        
        try {
            driver.getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get driver pool size
     */
    public static int getDriverPoolSize() {
        return driverPool.size();
    }
}