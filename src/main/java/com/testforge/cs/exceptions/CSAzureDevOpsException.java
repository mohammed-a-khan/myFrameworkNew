package com.testforge.cs.exceptions;

/**
 * Exception thrown for Azure DevOps related errors
 */
public class CSAzureDevOpsException extends RuntimeException {
    
    public CSAzureDevOpsException(String message) {
        super(message);
    }
    
    public CSAzureDevOpsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSAzureDevOpsException(Throwable cause) {
        super(cause);
    }
}