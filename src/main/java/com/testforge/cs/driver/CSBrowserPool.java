package com.testforge.cs.driver;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * Smart browser pool that ensures browsers are always utilized
 * and never remain idle when there's work to be done
 */
public class CSBrowserPool {
    private static final Logger logger = LoggerFactory.getLogger(CSBrowserPool.class);
    
    private static CSBrowserPool instance;
    
    // Pool of available browsers
    private final BlockingQueue<BrowserInstance> availableBrowsers;
    
    // Map of browsers currently in use
    private final Map<String, BrowserInstance> browsersInUse = new ConcurrentHashMap<>();
    
    // Browser creation semaphore
    private final Semaphore browserCreationSemaphore;
    
    // Statistics
    private final AtomicInteger totalBrowsersCreated = new AtomicInteger(0);
    private final AtomicInteger currentActiveBrowsers = new AtomicInteger(0);
    private final AtomicInteger totalTestsExecuted = new AtomicInteger(0);
    private final Map<String, Integer> threadTestCount = new ConcurrentHashMap<>();
    
    // Configuration
    private final int maxBrowsers;
    private final String browserType;
    private final boolean headless;
    
    private CSBrowserPool(int maxBrowsers, String browserType, boolean headless) {
        this.maxBrowsers = maxBrowsers;
        this.browserType = browserType;
        this.headless = headless;
        this.availableBrowsers = new LinkedBlockingQueue<>();
        this.browserCreationSemaphore = new Semaphore(maxBrowsers);
        
        logger.info("Browser pool initialized: max={}, type={}, headless={}", 
            maxBrowsers, browserType, headless);
    }
    
    /**
     * Get or create singleton instance
     */
    public static synchronized CSBrowserPool getInstance(int maxBrowsers, String browserType, boolean headless) {
        if (instance == null) {
            instance = new CSBrowserPool(maxBrowsers, browserType, headless);
        }
        return instance;
    }
    
    /**
     * Get a browser from the pool
     * This will either return an available browser or create a new one if under limit
     */
    public synchronized WebDriver acquireBrowser(String threadName) throws InterruptedException {
        logger.info("[{}] Requesting browser (available: {}, in-use: {}, max: {})", 
            threadName, availableBrowsers.size(), browsersInUse.size(), maxBrowsers);
        
        // First, try to get an available browser
        BrowserInstance browser = availableBrowsers.poll();
        
        if (browser == null) {
            // No available browser, try to create a new one if under limit
            if (browserCreationSemaphore.tryAcquire()) {
                try {
                    browser = createNewBrowser();
                    logger.info("[{}] Created new browser (total: {})", 
                        threadName, totalBrowsersCreated.get());
                } catch (Exception e) {
                    browserCreationSemaphore.release();
                    logger.error("[{}] Failed to create browser", threadName, e);
                    throw new RuntimeException("Failed to create browser", e);
                }
            } else {
                // Max browsers reached, wait for one to become available
                logger.info("[{}] Max browsers reached, waiting for available browser...", threadName);
                browser = availableBrowsers.take(); // This will block until available
                logger.info("[{}] Got browser from pool after waiting", threadName);
            }
        } else {
            logger.info("[{}] Reusing browser from pool", threadName);
        }
        
        // Mark browser as in use
        browser.assignToThread(threadName);
        browsersInUse.put(threadName, browser);
        currentActiveBrowsers.set(browsersInUse.size());
        
        // Update statistics
        threadTestCount.merge(threadName, 1, Integer::sum);
        totalTestsExecuted.incrementAndGet();
        
        logPoolStatus();
        
        return browser.getDriver();
    }
    
    /**
     * Release browser back to pool for reuse
     */
    public synchronized void releaseBrowser(String threadName) {
        BrowserInstance browser = browsersInUse.remove(threadName);
        
        if (browser != null) {
            try {
                // Clear browser state for next use
                browser.reset();
                
                // Put back in available pool
                availableBrowsers.offer(browser);
                currentActiveBrowsers.set(browsersInUse.size());
                
                logger.info("[{}] Released browser back to pool (available: {}, in-use: {})", 
                    threadName, availableBrowsers.size(), browsersInUse.size());
                
                // Notify waiting threads
                notifyAll();
                
            } catch (Exception e) {
                logger.error("[{}] Error resetting browser, will close it", threadName, e);
                closeBrowser(browser);
            }
        } else {
            logger.warn("[{}] No browser found to release", threadName);
        }
        
        logPoolStatus();
    }
    
    /**
     * Create a new browser instance
     */
    private BrowserInstance createNewBrowser() {
        WebDriver driver = CSWebDriverManager.createDriver(browserType, headless, null);
        if (driver == null) {
            throw new RuntimeException("Failed to create WebDriver");
        }
        
        totalBrowsersCreated.incrementAndGet();
        return new BrowserInstance(driver);
    }
    
    /**
     * Close a specific browser
     */
    private void closeBrowser(BrowserInstance browser) {
        try {
            browser.close();
            browserCreationSemaphore.release();
            logger.info("Closed browser and released semaphore");
        } catch (Exception e) {
            logger.error("Error closing browser", e);
        }
    }
    
    /**
     * Close all browsers in the pool
     */
    public synchronized void closeAllBrowsers() {
        logger.info("Closing all browsers in pool");
        
        // Close browsers in use
        browsersInUse.values().forEach(this::closeBrowser);
        browsersInUse.clear();
        
        // Close available browsers
        BrowserInstance browser;
        while ((browser = availableBrowsers.poll()) != null) {
            closeBrowser(browser);
        }
        
        currentActiveBrowsers.set(0);
        logger.info("All browsers closed. Total tests executed: {}", totalTestsExecuted.get());
        logStatistics();
    }
    
    /**
     * Log current pool status
     */
    private void logPoolStatus() {
        logger.debug("Browser Pool Status: Available={}, In-Use={}, Total Created={}, Max={}", 
            availableBrowsers.size(), browsersInUse.size(), totalBrowsersCreated.get(), maxBrowsers);
    }
    
    /**
     * Log final statistics
     */
    private void logStatistics() {
        logger.info("=== Browser Pool Statistics ===");
        logger.info("Total browsers created: {}", totalBrowsersCreated.get());
        logger.info("Total tests executed: {}", totalTestsExecuted.get());
        logger.info("Thread distribution:");
        threadTestCount.forEach((thread, count) -> 
            logger.info("  {}: {} tests", thread, count));
        logger.info("================================");
    }
    
    /**
     * Get pool statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("availableBrowsers", availableBrowsers.size());
        stats.put("browsersInUse", browsersInUse.size());
        stats.put("totalBrowsersCreated", totalBrowsersCreated.get());
        stats.put("totalTestsExecuted", totalTestsExecuted.get());
        stats.put("threadDistribution", new HashMap<>(threadTestCount));
        return stats;
    }
    
    /**
     * Browser instance wrapper
     */
    private static class BrowserInstance {
        private final WebDriver driver;
        private String currentThread;
        private int usageCount = 0;
        
        BrowserInstance(WebDriver driver) {
            this.driver = driver;
        }
        
        void assignToThread(String threadName) {
            this.currentThread = threadName;
            this.usageCount++;
        }
        
        void reset() {
            try {
                // Clear cookies and navigate to blank page
                driver.manage().deleteAllCookies();
                driver.navigate().to("about:blank");
            } catch (Exception e) {
                logger.warn("Error resetting browser", e);
            }
        }
        
        void close() {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.warn("Error closing browser", e);
            }
        }
        
        WebDriver getDriver() {
            return driver;
        }
        
        int getUsageCount() {
            return usageCount;
        }
    }
}