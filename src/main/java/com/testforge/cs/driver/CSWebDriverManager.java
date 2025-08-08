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

/**
 * WebDriver factory and manager for creating and managing browser instances
 * Supports Chrome, Firefox, Edge, Safari with local and remote execution
 */
public class CSWebDriverManager {
    private static final Logger logger = LoggerFactory.getLogger(CSWebDriverManager.class);
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
    private static final Map<String, WebDriver> driverPool = new ConcurrentHashMap<>();
    
    /**
     * Create WebDriver instance
     */
    public static WebDriver createDriver(String browserType, boolean headless, Map<String, Object> capabilities) {
        logger.info("Creating {} driver (headless: {})", browserType, headless);
        
        WebDriver driver;
        
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
        String threadId = String.valueOf(Thread.currentThread().getId());
        driverPool.put(threadId, driver);
        
        return driver;
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
        return threadLocalDriver.get();
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
            }
        }
    }
    
    /**
     * Quit all drivers
     */
    public static void quitAllDrivers() {
        logger.info("Quitting all {} drivers", driverPool.size());
        
        driverPool.forEach((threadId, driver) -> {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.error("Error quitting driver for thread {}", threadId, e);
            }
        });
        
        driverPool.clear();
        threadLocalDriver.remove();
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
            
            // Create directory if needed
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            // Write screenshot data to file
            Files.write(path, screenshotData);
            
            logger.info("Screenshot saved to: {}", filePath);
            return path.toFile();
            
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