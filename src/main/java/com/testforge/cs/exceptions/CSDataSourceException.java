package com.testforge.cs.exceptions;

/**
 * Exception thrown when data source issues occur
 */
public class CSDataSourceException extends CSFrameworkException {
    
    public CSDataSourceException(String message) {
        super(message);
    }
    
    public CSDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSDataSourceException(String errorCode, String component, String message) {
        super(errorCode, component, message);
    }
    
    public CSDataSourceException(String errorCode, String component, String message, Throwable cause) {
        super(errorCode, component, message, cause);
    }
}