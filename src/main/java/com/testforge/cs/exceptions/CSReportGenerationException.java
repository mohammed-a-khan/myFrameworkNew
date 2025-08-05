package com.testforge.cs.exceptions;

/**
 * Exception thrown when report generation fails
 */
public class CSReportGenerationException extends CSFrameworkException {
    
    public CSReportGenerationException(String message) {
        super(message);
    }
    
    public CSReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSReportGenerationException(String errorCode, String component, String message) {
        super(errorCode, component, message);
    }
    
    public CSReportGenerationException(String errorCode, String component, String message, Throwable cause) {
        super(errorCode, component, message, cause);
    }
}