package com.testforge.cs.exceptions;

/**
 * Exception for reporting related errors
 */
public class CSReportingException extends CSFrameworkException {
    
    private String reportType;
    private String reportPath;
    
    public CSReportingException(String message) {
        super("REPORT_ERROR", "Reporting", message);
    }
    
    public CSReportingException(String message, Throwable cause) {
        super("REPORT_ERROR", "Reporting", message, cause);
    }
    
    public CSReportingException(String reportType, String reportPath, String message) {
        super("REPORT_ERROR", "Reporting", message);
        this.reportType = reportType;
        this.reportPath = reportPath;
    }
    
    public CSReportingException(String reportType, String reportPath, String message, Throwable cause) {
        super("REPORT_ERROR", "Reporting", message, cause);
        this.reportType = reportType;
        this.reportPath = reportPath;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public String getReportPath() {
        return reportPath;
    }
}