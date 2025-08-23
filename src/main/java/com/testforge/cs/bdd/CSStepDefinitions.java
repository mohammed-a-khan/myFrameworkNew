package com.testforge.cs.bdd;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.injection.CSSmartPageInjector;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for BDD step definitions
 * Provides common functionality and utilities for step definition classes
 */
public abstract class CSStepDefinitions {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final CSReportManager reportManager = CSReportManager.getInstance();
    protected final CSConfigManager config = CSConfigManager.getInstance();
    
    // Thread-local storage for test context
    private static final ThreadLocal<CSDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<CSTestContext> contextThreadLocal = new ThreadLocal<>();
    
    /**
     * Constructor that automatically processes @CSPageInjection annotations.
     * 
     * This constructor is called when step definition classes are instantiated
     * by the BDD framework (Cucumber, TestNG, etc.). It scans for fields annotated
     * with @CSPageInjection and sets up lazy proxies for automatic page initialization.
     * 
     * <p><b>Note:</b> Actual page creation is deferred until first access to ensure
     * WebDriver is properly initialized.</p>
     */
    public CSStepDefinitions() {
        try {
            logger.debug("Initializing step definitions: {}", this.getClass().getSimpleName());
            
            // Process @CSPageInjection annotations and provide guidance
            CSSmartPageInjector.processPageInjections(this);
            
            logger.debug("Step definitions initialization complete: {}", this.getClass().getSimpleName());
            
        } catch (Exception e) {
            logger.error("Failed to initialize step definitions: {}", this.getClass().getSimpleName(), e);
            // Don't throw here as it would prevent framework initialization
            // Errors will be caught when pages are actually accessed
        }
    }
    
    /**
     * Get the current driver instance
     */
    protected CSDriver getDriver() {
        CSDriver driver = driverThreadLocal.get();
        if (driver == null) {
            // Try to get from WebDriverManager as fallback
            WebDriver webDriver = CSWebDriverManager.getDriver();
            if (webDriver != null) {
                logger.warn("Driver not in ThreadLocal, using WebDriverManager driver for thread: {}", Thread.currentThread().getName());
                driver = new CSDriver(webDriver);
                driverThreadLocal.set(driver);
            } else {
                throw new RuntimeException("Driver not initialized. Make sure test extends CSBaseTest.");
            }
        }
        return driver;
    }
    
    /**
     * Set the driver for current thread
     */
    public static void setDriver(CSDriver driver) {
        driverThreadLocal.set(driver);
    }
    
    /**
     * Clear thread-local state to ensure scenario isolation
     */
    public static void clearThreadLocalState() {
        // Clear the thread-local driver if needed
        // This helps ensure complete isolation between scenarios
        // especially when running multiple feature files
        LoggerFactory.getLogger(CSStepDefinitions.class).debug("Clearing thread-local state for scenario isolation");
    }
    
    /**
     * Get page instance
     */
    protected <T extends CSBasePage> T getPage(Class<T> pageClass) {
        try {
            // Get the driver to ensure it's available
            CSDriver driver = getDriver();
            
            T page = pageClass.getDeclaredConstructor().newInstance();
            // Page initialization is handled in CSBasePage constructor
            // which already has access to driver, config, and reportManager
            return page;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create page instance: " + pageClass.getName(), e);
        }
    }
    
    /**
     * Navigate to URL
     */
    protected void navigateTo(String url) {
        getDriver().navigateTo(url);
    }
    
    /**
     * Capture screenshot
     */
    protected void captureScreenshot(String screenshotName) {
        byte[] screenshot = CSScreenshotUtils.captureScreenshot(getDriver().getWebDriver());
        reportManager.attachScreenshot(screenshot, screenshotName);
    }
    
    /**
     * Find element using locator string and description
     */
    protected com.testforge.cs.elements.CSElement findElement(String locatorString, String description) {
        return getDriver().findElement(locatorString, description);
    }
    
    /**
     * Assert methods for BDD steps
     */
    protected void assertTrue(boolean condition, String message) {
        Assert.assertTrue(condition, message);
    }
    
    protected void assertFalse(boolean condition, String message) {
        Assert.assertFalse(condition, message);
    }
    
    protected void assertEquals(Object actual, Object expected, String message) {
        Assert.assertEquals(actual, expected, message);
    }
    
    protected void assertNotNull(Object object, String message) {
        Assert.assertNotNull(object, message);
    }
    
    /**
     * Test context for sharing data between steps
     */
    public static class CSTestContext {
        private final java.util.Map<String, Object> data = new java.util.concurrent.ConcurrentHashMap<>();
        
        public void put(String key, Object value) {
            data.put(key, value);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            return (T) data.get(key);
        }
        
        public boolean containsKey(String key) {
            return data.containsKey(key);
        }
        
        public void clear() {
            data.clear();
        }
    }
    
    /**
     * Get test context
     */
    protected CSTestContext getContext() {
        CSTestContext context = contextThreadLocal.get();
        if (context == null) {
            context = new CSTestContext();
            contextThreadLocal.set(context);
        }
        return context;
    }
    
    /**
     * Page cache for lazy initialization
     */
    private final Map<Class<?>, Object> pageCache = new ConcurrentHashMap<>();
    
    /**
     * Get or create a page object with lazy initialization
     * This method caches page objects to avoid repeated initialization
     * 
     * @param pageClass The page class to get or create
     * @return The page object instance
     */
    @SuppressWarnings("unchecked")
    protected <T> T getOrCreatePage(Class<T> pageClass) {
        return (T) pageCache.computeIfAbsent(pageClass, clazz -> {
            try {
                logger.debug("Creating new instance of page: {}", clazz.getName());
                // Cast to the correct type for getPage
                @SuppressWarnings("rawtypes")
                Class pageClazz = clazz;
                return getPage(pageClazz);
            } catch (Exception e) {
                logger.error("Failed to create page instance: {}", clazz.getName(), e);
                throw new RuntimeException("Failed to initialize page: " + clazz.getName(), e);
            }
        });
    }
    
    /**
     * Clear the page cache (useful when driver is reset)
     */
    protected void clearPageCache() {
        pageCache.clear();
        logger.debug("Page cache cleared");
    }
    
    /**
     * Injects pages with @CSPageInjection annotations when WebDriver is ready.
     * This method should be called after WebDriver initialization but before test execution.
     */
    public void injectAnnotatedPages() {
        try {
            logger.debug("Injecting @CSPageInjection annotated pages for: {}", this.getClass().getSimpleName());
            CSSmartPageInjector.injectPages(this);
        } catch (Exception e) {
            logger.warn("Failed to inject annotated pages: {}", e.getMessage());
            // Don't fail the test - just log the warning
        }
    }
    
    /**
     * Get the current data row from scenario context
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String> getDataRow() {
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            Object dataRow = runner.getFromContext("dataRow");
            if (dataRow instanceof Map) {
                return (Map<String, String>) dataRow;
            }
        }
        return new java.util.HashMap<>();
    }
    
    /**
     * Get a specific value from the current data row
     */
    protected String getDataValue(String key) {
        return getDataRow().getOrDefault(key, "");
    }
    
    /**
     * Check if a data key exists in the current data row
     */
    protected boolean hasDataKey(String key) {
        return getDataRow().containsKey(key);
    }
    
    /**
     * Clean up thread-local resources including page cache
     */
    public static void cleanup() {
        driverThreadLocal.remove();
        contextThreadLocal.remove();
        
        // Clear all page injection caches to prevent memory leaks  
        CSSmartPageInjector.clearAllCaches();
    }
}