package com.testforge.cs.exceptions;

/**
 * Exception thrown when element locator issues occur
 */
public class CSLocatorException extends CSFrameworkException {
    
    public CSLocatorException(String message) {
        super(message);
    }
    
    public CSLocatorException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSLocatorException(String errorCode, String component, String message) {
        super(errorCode, component, message);
    }
    
    public CSLocatorException(String errorCode, String component, String message, Throwable cause) {
        super(errorCode, component, message, cause);
    }
}