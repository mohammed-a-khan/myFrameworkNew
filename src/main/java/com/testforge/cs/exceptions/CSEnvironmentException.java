package com.testforge.cs.exceptions;

/**
 * Exception thrown for environment collection related errors
 */
public class CSEnvironmentException extends RuntimeException {
    
    public CSEnvironmentException(String message) {
        super(message);
    }
    
    public CSEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSEnvironmentException(Throwable cause) {
        super(cause);
    }
}