package com.testforge.cs.exceptions;

/**
 * Exception for WebDriver related errors
 */
public class CSWebDriverException extends CSFrameworkException {
    
    public CSWebDriverException(String message) {
        super("WEB_DRIVER_ERROR", "WebDriver", message);
    }
    
    public CSWebDriverException(String message, Throwable cause) {
        super("WEB_DRIVER_ERROR", "WebDriver", message, cause);
    }
    
    public CSWebDriverException(String errorCode, String message) {
        super(errorCode, "WebDriver", message);
    }
    
    public CSWebDriverException(String errorCode, String message, Throwable cause) {
        super(errorCode, "WebDriver", message, cause);
    }
}