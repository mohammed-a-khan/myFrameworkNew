package com.testforge.cs.exceptions;

/**
 * Base exception class for CS Framework
 * All custom exceptions should extend this class
 */
public class CSFrameworkException extends RuntimeException {
    
    private String errorCode;
    private String component;
    
    public CSFrameworkException(String message) {
        super(message);
    }
    
    public CSFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CSFrameworkException(String errorCode, String component, String message) {
        super(message);
        this.errorCode = errorCode;
        this.component = component;
    }
    
    public CSFrameworkException(String errorCode, String component, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.component = component;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getComponent() {
        return component;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        if (component != null) {
            sb.append(" in ").append(component);
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}