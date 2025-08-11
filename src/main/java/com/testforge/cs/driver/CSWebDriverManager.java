package com.testforge.cs.driver;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSFrameworkException;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
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
    
    // Register shutdown hook to ensure all browsers are closed
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutdown detected - cleaning up all browsers");
            quitAllDrivers();
        }));
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
     * Create WebDriver instance
     */
    public static WebDriver createDriver(String browserType, boolean headless, Map<String, Object> capabilities) {
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
            String remoteUrl = config.getProperty("selenium.remote.url");
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
            default:
                throw new CSFrameworkException("Unsupported browser type: " + browserType);
        }
    }
    
    /**
     * Create Chrome driver
     */
    private static WebDriver createChromeDriver(boolean headless, Map<String, Object> capabilities) {
        // Setup ChromeDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();
        
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
        String downloadPath = config.getProperty("download.directory", System.getProperty("user.dir") + "/downloads");
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
        // Setup FirefoxDriver using WebDriverManager
        WebDriverManager.firefoxdriver().setup();
        
        FirefoxOptions options = new FirefoxOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        // Add preferences
        String downloadPath = config.getProperty("download.directory", System.getProperty("user.dir") + "/downloads");
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
        // Setup EdgeDriver using WebDriverManager
        WebDriverManager.edgedriver().setup();
        
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
        Duration implicitWait = Duration.ofSeconds(config.getIntProperty("selenium.implicit.wait", 10));
        Duration pageLoadTimeout = Duration.ofSeconds(config.getIntProperty("selenium.pageload.timeout", 30));
        Duration scriptTimeout = Duration.ofSeconds(config.getIntProperty("selenium.script.timeout", 30));
        
        driver.manage().timeouts().implicitlyWait(implicitWait);
        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        driver.manage().timeouts().scriptTimeout(scriptTimeout);
        
        // Maximize window
        if (config.getBooleanProperty("browser.maximize", true)) {
            driver.manage().window().maximize();
        }
        
        // Delete cookies
        if (config.getBooleanProperty("browser.delete.cookies", true)) {
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
        
        // Create a copy to avoid concurrent modification
        Map<String, WebDriver> driversCopy = new HashMap<>(driverPool);
        
        driversCopy.forEach((threadId, driver) -> {
            try {
                if (driver != null) {
                    // Check if driver session is still active
                    try {
                        driver.getTitle(); // Test if driver is still responsive
                        logger.info("Closing driver for thread {}", threadId);
                        driver.quit();
                        // Release semaphore permit for each closed driver
                        browserSemaphore.release();
                        logger.info("Released browser permit for thread {}", threadId);
                    } catch (Exception sessionError) {
                        // Driver session already closed, just log it
                        logger.debug("Driver session already closed for thread {}", threadId);
                        // Still release the permit
                        browserSemaphore.release();
                    }
                }
            } catch (Exception e) {
                logger.error("Error quitting driver for thread {}", threadId, e);
            }
        });
        
        // Clear all references
        driverPool.clear();
        
        // Also check and clear ThreadLocal
        WebDriver localDriver = threadLocalDriver.get();
        if (localDriver != null) {
            try {
                localDriver.quit();
            } catch (Exception e) {
                logger.debug("Error quitting thread-local driver", e);
            }
            threadLocalDriver.remove();
        }
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