package com.testforge.cs.exceptions;

/**
 * Exception thrown for visualization related errors
 */
public class CSVisualizationException extends RuntimeException {
    
    public CSVisualizationException(String message) {
        super(message);
    }
    
    public CSVisualizationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSVisualizationException(Throwable cause) {
        super(cause);
    }
}