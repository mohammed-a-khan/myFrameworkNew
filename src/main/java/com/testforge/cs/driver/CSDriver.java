package com.testforge.cs.driver;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.exceptions.CSDriverException;
import com.testforge.cs.reporting.CSReportManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production-ready wrapper for Selenium WebDriver
 * Provides complete abstraction - users never see WebDriver directly
 */
public class CSDriver {
    private static final Logger logger = LoggerFactory.getLogger(CSDriver.class);
    
    private final WebDriver driver;
    private final CSConfigManager config;
    private final CSReportManager reportManager;
    private final WebDriverWait wait;
    
    // Configuration
    private final int defaultTimeout;
    private final boolean captureNetworkTraffic;
    private final boolean enablePerformanceMetrics;
    
    public CSDriver(WebDriver driver) {
        this.driver = driver;
        this.config = CSConfigManager.getInstance();
        this.reportManager = CSReportManager.getInstance();
        
        // Load configuration
        this.defaultTimeout = config.getInt("cs.wait.timeout", 30);
        this.captureNetworkTraffic = config.getBoolean("cs.driver.capture.network", false);
        this.enablePerformanceMetrics = config.getBoolean("cs.driver.performance.metrics", false);
        
        // Initialize wait
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
    }
    
    /**
     * Navigate to URL
     */
    public void get(String url) {
        logger.info("Navigating to URL: {}", url);
        try {
            driver.get(url);
            reportManager.logStep("Navigated to: " + url, true);
        } catch (Exception e) {
            reportManager.logStep("Failed to navigate to: " + url, false);
            throw new CSDriverException("Failed to navigate to URL: " + url, e);
        }
    }
    
    /**
     * Navigate to URL (alias for get)
     */
    public void navigateTo(String url) {
        get(url);
    }
    
    /**
     * Get current URL
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
    
    /**
     * Get page title
     */
    public String getTitle() {
        return driver.getTitle();
    }
    
    /**
     * Get page source
     */
    public String getPageSource() {
        return driver.getPageSource();
    }
    
    /**
     * Find single element
     */
    public CSElement findElement(By locator) {
        return new CSElement(driver, locator, locator.toString());
    }
    
    /**
     * Find single element with description
     */
    public CSElement findElement(By locator, String description) {
        return new CSElement(driver, locator, description);
    }
    
    /**
     * Find element using locator string and description
     */
    public CSElement findElement(String locatorString, String description) {
        By by = parseLocatorString(locatorString);
        return new CSElement(driver, by, description);
    }
    
    /**
     * Find multiple elements
     */
    public List<CSElement> findElements(By locator) {
        List<WebElement> webElements = driver.findElements(locator);
        return webElements.stream()
            .map(we -> new CSElement(driver, locator, locator.toString()))
            .collect(Collectors.toList());
    }
    
    /**
     * Refresh page
     */
    public void refresh() {
        logger.info("Refreshing page");
        driver.navigate().refresh();
        reportManager.logStep("Page refreshed", true);
    }
    
    /**
     * Navigate back
     */
    public void back() {
        logger.info("Navigating back");
        driver.navigate().back();
        reportManager.logStep("Navigated back", true);
    }
    
    /**
     * Navigate forward
     */
    public void forward() {
        logger.info("Navigating forward");
        driver.navigate().forward();
        reportManager.logStep("Navigated forward", true);
    }
    
    /**
     * Close current window
     */
    public void close() {
        logger.info("Closing current window");
        driver.close();
        reportManager.logStep("Window closed", true);
    }
    
    /**
     * Quit driver and close all windows
     */
    public void quit() {
        logger.info("Quitting driver");
        try {
            driver.quit();
            reportManager.logStep("Driver quit successfully", true);
        } catch (Exception e) {
            logger.error("Error quitting driver", e);
        }
    }
    
    /**
     * Switch to window by title
     */
    public void switchToWindow(String windowTitle) {
        logger.info("Switching to window: {}", windowTitle);
        String currentWindow = driver.getWindowHandle();
        
        for (String windowHandle : driver.getWindowHandles()) {
            driver.switchTo().window(windowHandle);
            if (driver.getTitle().equals(windowTitle)) {
                reportManager.logStep("Switched to window: " + windowTitle, true);
                return;
            }
        }
        
        driver.switchTo().window(currentWindow);
        throw new CSDriverException("Window not found with title: " + windowTitle);
    }
    
    /**
     * Switch to window by index
     */
    public void switchToWindow(int index) {
        logger.info("Switching to window index: {}", index);
        List<String> windows = List.copyOf(driver.getWindowHandles());
        
        if (index < 0 || index >= windows.size()) {
            throw new CSDriverException("Invalid window index: " + index);
        }
        
        driver.switchTo().window(windows.get(index));
        reportManager.logStep("Switched to window index: " + index, true);
    }
    
    /**
     * Get window handles
     */
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }
    
    /**
     * Get current window handle
     */
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }
    
    /**
     * Switch to frame by index
     */
    public void switchToFrame(int index) {
        logger.info("Switching to frame index: {}", index);
        driver.switchTo().frame(index);
        reportManager.logStep("Switched to frame index: " + index, true);
    }
    
    /**
     * Switch to frame by name or ID
     */
    public void switchToFrame(String nameOrId) {
        logger.info("Switching to frame: {}", nameOrId);
        driver.switchTo().frame(nameOrId);
        reportManager.logStep("Switched to frame: " + nameOrId, true);
    }
    
    /**
     * Switch to frame by element
     */
    public void switchToFrame(CSElement frameElement) {
        logger.info("Switching to frame element");
        driver.switchTo().frame(frameElement.getElement());
        reportManager.logStep("Switched to frame element", true);
    }
    
    /**
     * Switch to default content
     */
    public void switchToDefaultContent() {
        logger.info("Switching to default content");
        driver.switchTo().defaultContent();
        reportManager.logStep("Switched to default content", true);
    }
    
    /**
     * Accept alert
     */
    public void acceptAlert() {
        logger.info("Accepting alert");
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            alert.accept();
            reportManager.logStep("Accepted alert: " + alertText, true);
        } catch (NoAlertPresentException e) {
            throw new CSDriverException("No alert present to accept", e);
        }
    }
    
    /**
     * Dismiss alert
     */
    public void dismissAlert() {
        logger.info("Dismissing alert");
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            alert.dismiss();
            reportManager.logStep("Dismissed alert: " + alertText, true);
        } catch (NoAlertPresentException e) {
            throw new CSDriverException("No alert present to dismiss", e);
        }
    }
    
    /**
     * Get alert text
     */
    public String getAlertText() {
        logger.info("Getting alert text");
        try {
            Alert alert = driver.switchTo().alert();
            return alert.getText();
        } catch (NoAlertPresentException e) {
            throw new CSDriverException("No alert present", e);
        }
    }
    
    /**
     * Send text to alert
     */
    public void sendKeysToAlert(String text) {
        logger.info("Sending text to alert: {}", text);
        try {
            Alert alert = driver.switchTo().alert();
            alert.sendKeys(text);
            reportManager.logStep("Sent text to alert: " + text, true);
        } catch (NoAlertPresentException e) {
            throw new CSDriverException("No alert present", e);
        }
    }
    
    /**
     * Execute JavaScript
     */
    public Object executeScript(String script, Object... args) {
        logger.debug("Executing JavaScript: {}", script);
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(script, args);
            reportManager.logStep("Executed JavaScript", true);
            return result;
        } catch (Exception e) {
            reportManager.logStep("Failed to execute JavaScript", false);
            throw new CSDriverException("Failed to execute JavaScript", e);
        }
    }
    
    /**
     * Execute async JavaScript
     */
    public Object executeAsyncScript(String script, Object... args) {
        logger.debug("Executing async JavaScript: {}", script);
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeAsyncScript(script, args);
            reportManager.logStep("Executed async JavaScript", true);
            return result;
        } catch (Exception e) {
            reportManager.logStep("Failed to execute async JavaScript", false);
            throw new CSDriverException("Failed to execute async JavaScript", e);
        }
    }
    
    /**
     * Take screenshot
     */
    public byte[] takeScreenshot() {
        logger.info("Taking screenshot");
        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            byte[] screenshotBytes = screenshot.getScreenshotAs(OutputType.BYTES);
            reportManager.logStep("Screenshot captured", true);
            return screenshotBytes;
        } catch (Exception e) {
            logger.error("Failed to take screenshot", e);
            throw new CSDriverException("Failed to take screenshot", e);
        }
    }
    
    /**
     * Maximize window
     */
    public void maximize() {
        logger.info("Maximizing window");
        driver.manage().window().maximize();
        reportManager.logStep("Window maximized", true);
    }
    
    /**
     * Set window size
     */
    public void setWindowSize(int width, int height) {
        logger.info("Setting window size: {}x{}", width, height);
        driver.manage().window().setSize(new Dimension(width, height));
        reportManager.logStep("Window size set to: " + width + "x" + height, true);
    }
    
    /**
     * Get window size
     */
    public Dimension getWindowSize() {
        return driver.manage().window().getSize();
    }
    
    /**
     * Set window position
     */
    public void setWindowPosition(int x, int y) {
        logger.info("Setting window position: {},{}", x, y);
        driver.manage().window().setPosition(new Point(x, y));
        reportManager.logStep("Window position set to: " + x + "," + y, true);
    }
    
    /**
     * Get window position
     */
    public Point getWindowPosition() {
        return driver.manage().window().getPosition();
    }
    
    /**
     * Delete all cookies
     */
    public void deleteAllCookies() {
        logger.info("Deleting all cookies");
        driver.manage().deleteAllCookies();
        reportManager.logStep("All cookies deleted", true);
    }
    
    /**
     * Delete cookie by name
     */
    public void deleteCookie(String name) {
        logger.info("Deleting cookie: {}", name);
        driver.manage().deleteCookieNamed(name);
        reportManager.logStep("Cookie deleted: " + name, true);
    }
    
    /**
     * Add cookie
     */
    public void addCookie(String name, String value) {
        logger.info("Adding cookie: {}={}", name, value);
        Cookie cookie = new Cookie(name, value);
        driver.manage().addCookie(cookie);
        reportManager.logStep("Cookie added: " + name, true);
    }
    
    /**
     * Get cookie by name
     */
    public Cookie getCookie(String name) {
        return driver.manage().getCookieNamed(name);
    }
    
    /**
     * Get all cookies
     */
    public Set<Cookie> getCookies() {
        return driver.manage().getCookies();
    }
    
    /**
     * Wait for page load
     */
    public void waitForPageLoad() {
        logger.debug("Waiting for page load");
        wait.until(driver -> {
            String readyState = (String) ((JavascriptExecutor) driver)
                .executeScript("return document.readyState");
            return "complete".equals(readyState);
        });
        logger.debug("Page load complete");
    }
    
    /**
     * Wait for jQuery to complete
     */
    public void waitForJQuery() {
        logger.debug("Waiting for jQuery");
        wait.until(driver -> {
            Boolean jQueryComplete = (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return jQuery.active == 0");
            return jQueryComplete != null && jQueryComplete;
        });
        logger.debug("jQuery complete");
    }
    
    /**
     * Wait for Angular to complete
     */
    public void waitForAngular() {
        logger.debug("Waiting for Angular");
        wait.until(driver -> {
            Boolean angularReady = (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return window.getAllAngularTestabilities().findIndex(x => !x.isStable()) === -1");
            return angularReady != null && angularReady;
        });
        logger.debug("Angular complete");
    }
    
    /**
     * Get underlying WebDriver (should be used sparingly)
     */
    public WebDriver getWebDriver() {
        return driver;
    }
    
    /**
     * Check if driver is active
     */
    public boolean isActive() {
        try {
            driver.getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Parse locator string to By object
     */
    private By parseLocatorString(String locatorString) {
        if (locatorString.contains(":")) {
            String[] parts = locatorString.split(":", 2);
            String type = parts[0].toLowerCase();
            String value = parts[1];
            
            switch (type) {
                case "id":
                    return By.id(value);
                case "name":
                    return By.name(value);
                case "css":
                    return By.cssSelector(value);
                case "xpath":
                    return By.xpath(value);
                case "class":
                    return By.className(value);
                case "tag":
                    return By.tagName(value);
                case "link":
                    return By.linkText(value);
                case "partial":
                    return By.partialLinkText(value);
                default:
                    throw new CSDriverException("Unknown locator type: " + type);
            }
        }
        
        // If no type specified, try to guess
        if (locatorString.startsWith("//") || locatorString.contains("[")) {
            return By.xpath(locatorString);
        } else if (locatorString.startsWith("#") || locatorString.contains(".") || locatorString.contains(":")) {
            return By.cssSelector(locatorString);
        } else {
            return By.id(locatorString);
        }
    }
    
    @Override
    public String toString() {
        return String.format("CSDriver[%s]", driver.getClass().getSimpleName());
    }
}