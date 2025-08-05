package com.testforge.cs.exceptions;

/**
 * Exception thrown for element operation failures
 */
public class CSElementException extends CSFrameworkException {
    
    public CSElementException(String message) {
        super(message);
    }
    
    public CSElementException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSElementException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}