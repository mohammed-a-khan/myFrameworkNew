package com.testforge.cs.exceptions;

/**
 * Exception thrown when element is not found on the page
 */
public class CSElementNotFoundException extends CSFrameworkException {
    
    private String locator;
    private long timeoutSeconds;
    
    public CSElementNotFoundException(String locator, long timeoutSeconds) {
        super("ELEMENT_NOT_FOUND", "WebDriver", 
              String.format("Element not found with locator: %s after %d seconds", locator, timeoutSeconds));
        this.locator = locator;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public CSElementNotFoundException(String locator, long timeoutSeconds, Throwable cause) {
        super("ELEMENT_NOT_FOUND", "WebDriver", 
              String.format("Element not found with locator: %s after %d seconds", locator, timeoutSeconds), cause);
        this.locator = locator;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public String getLocator() {
        return locator;
    }
    
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}