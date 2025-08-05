package com.testforge.cs.exceptions;

/**
 * Exception thrown when page initialization fails
 */
public class CSPageInitializationException extends CSFrameworkException {
    
    public CSPageInitializationException(String message) {
        super(message);
    }
    
    public CSPageInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSPageInitializationException(String errorCode, String component, String message) {
        super(errorCode, component, message);
    }
    
    public CSPageInitializationException(String errorCode, String component, String message, Throwable cause) {
        super(errorCode, component, message, cause);
    }
}