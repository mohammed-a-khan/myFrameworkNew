package com.testforge.cs.exceptions;

/**
 * Exception thrown when step execution fails
 */
public class CSStepExecutionException extends CSFrameworkException {
    
    public CSStepExecutionException(String message) {
        super(message);
    }
    
    public CSStepExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSStepExecutionException(String errorCode, String component, String message) {
        super(errorCode, component, message);
    }
    
    public CSStepExecutionException(String errorCode, String component, String message, Throwable cause) {
        super(errorCode, component, message, cause);
    }
}