package com.testforge.cs.exceptions;

/**
 * Exception for data-related errors (Excel, CSV, JSON, Database)
 */
public class CSDataException extends CSFrameworkException {
    
    private String dataSource;
    private String dataType;
    
    public CSDataException(String message) {
        super("DATA_ERROR", "Data", message);
    }
    
    public CSDataException(String message, Throwable cause) {
        super("DATA_ERROR", "Data", message, cause);
    }
    
    public CSDataException(String dataSource, String dataType, String message) {
        super("DATA_ERROR", "Data", message);
        this.dataSource = dataSource;
        this.dataType = dataType;
    }
    
    public CSDataException(String dataSource, String dataType, String message, Throwable cause) {
        super("DATA_ERROR", "Data", message, cause);
        this.dataSource = dataSource;
        this.dataType = dataType;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public String getDataType() {
        return dataType;
    }
}