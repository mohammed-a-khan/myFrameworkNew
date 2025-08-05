package com.testforge.cs.bdd;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Step execution context for BDD tests
 */
public class CSStepContext {
    private final Map<String, Object> contextData = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> pageInstances = new ConcurrentHashMap<>();
    
    /**
     * Get page instance with caching
     */
    @SuppressWarnings("unchecked")
    public <T extends CSBasePage> T getPage(Class<T> pageClass) {
        return (T) pageInstances.computeIfAbsent(pageClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create page instance: " + clazz.getName(), e);
            }
        });
    }
    
    /**
     * Get the current WebDriver
     */
    public WebDriver getDriver() {
        return CSWebDriverManager.getDriver();
    }
    
    /**
     * Capture screenshot
     */
    public void captureScreenshot(String name) {
        byte[] screenshot = CSScreenshotUtils.captureScreenshot(getDriver());
        // Store in context for reporting
        contextData.put("screenshot_" + name, screenshot);
    }
    
    /**
     * Store data in context
     */
    public void put(String key, Object value) {
        contextData.put(key, value);
    }
    
    /**
     * Get data from context
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) contextData.get(key);
    }
    
    /**
     * Clear context
     */
    public void clear() {
        contextData.clear();
        pageInstances.clear();
    }
}