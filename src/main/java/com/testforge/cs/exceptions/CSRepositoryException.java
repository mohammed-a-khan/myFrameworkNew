package com.testforge.cs.exceptions;

/**
 * Exception thrown for object repository related errors
 */
public class CSRepositoryException extends RuntimeException {
    
    public CSRepositoryException(String message) {
        super(message);
    }
    
    public CSRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSRepositoryException(Throwable cause) {
        super(cause);
    }
}