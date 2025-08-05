package com.testforge.cs.exceptions;

/**
 * Exception for API testing related errors
 */
public class CSApiException extends CSFrameworkException {
    
    private int statusCode;
    private String endpoint;
    private String method;
    
    public CSApiException(String message) {
        super("API_ERROR", "API", message);
    }
    
    public CSApiException(String message, Throwable cause) {
        super("API_ERROR", "API", message, cause);
    }
    
    public CSApiException(String endpoint, String method, int statusCode, String message) {
        super("API_ERROR", "API", message);
        this.endpoint = endpoint;
        this.method = method;
        this.statusCode = statusCode;
    }
    
    public CSApiException(String endpoint, String method, int statusCode, String message, Throwable cause) {
        super("API_ERROR", "API", message, cause);
        this.endpoint = endpoint;
        this.method = method;
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getMethod() {
        return method;
    }
}