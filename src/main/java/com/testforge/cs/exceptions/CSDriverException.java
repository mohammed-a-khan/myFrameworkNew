package com.testforge.cs.exceptions;

/**
 * Exception thrown for driver-related errors
 */
public class CSDriverException extends CSFrameworkException {
    
    public CSDriverException(String message) {
        super(message);
    }
    
    public CSDriverException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSDriverException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}