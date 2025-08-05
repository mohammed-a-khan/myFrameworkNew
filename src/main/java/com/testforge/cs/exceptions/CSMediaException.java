package com.testforge.cs.exceptions;

/**
 * Exception thrown for media recording related errors
 */
public class CSMediaException extends RuntimeException {
    
    public CSMediaException(String message) {
        super(message);
    }
    
    public CSMediaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSMediaException(Throwable cause) {
        super(cause);
    }
}