package com.testforge.cs.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder pattern for constructing REST API requests
 * Provides fluent interface for API testing
 */
public class CSRestApiBuilder {
    private final CSHttpClient httpClient;
    private String baseUrl;
    private String path;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Object body;
    private Duration timeout;
    
    /**
     * Create new REST API builder
     */
    public CSRestApiBuilder() {
        this.httpClient = new CSHttpClient();
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.method = "GET";
        this.timeout = Duration.ofSeconds(30);
    }
    
    /**
     * Create builder with base URL
     */
    public CSRestApiBuilder(String baseUrl) {
        this();
        this.baseUrl = baseUrl;
        this.httpClient.setBaseUrl(baseUrl);
    }
    
    /**
     * Set base URL
     */
    public CSRestApiBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient.setBaseUrl(baseUrl);
        return this;
    }
    
    /**
     * Set path
     */
    public CSRestApiBuilder path(String path) {
        this.path = path;
        return this;
    }
    
    /**
     * Set timeout
     */
    public CSRestApiBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * Set method to GET
     */
    public CSRestApiBuilder get() {
        this.method = "GET";
        return this;
    }
    
    /**
     * Set method to POST
     */
    public CSRestApiBuilder post() {
        this.method = "POST";
        return this;
    }
    
    /**
     * Set method to PUT
     */
    public CSRestApiBuilder put() {
        this.method = "PUT";
        return this;
    }
    
    /**
     * Set method to PATCH
     */
    public CSRestApiBuilder patch() {
        this.method = "PATCH";
        return this;
    }
    
    /**
     * Set method to DELETE
     */
    public CSRestApiBuilder delete() {
        this.method = "DELETE";
        return this;
    }
    
    /**
     * Set method to HEAD
     */
    public CSRestApiBuilder head() {
        this.method = "HEAD";
        return this;
    }
    
    /**
     * Set method to OPTIONS
     */
    public CSRestApiBuilder options() {
        this.method = "OPTIONS";
        return this;
    }
    
    /**
     * Add header
     */
    public CSRestApiBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }
    
    /**
     * Add multiple headers
     */
    public CSRestApiBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }
    
    /**
     * Add query parameter
     */
    public CSRestApiBuilder queryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }
    
    /**
     * Add multiple query parameters
     */
    public CSRestApiBuilder queryParams(Map<String, String> params) {
        this.queryParams.putAll(params);
        return this;
    }
    
    /**
     * Set request body
     */
    public CSRestApiBuilder body(Object body) {
        this.body = body;
        return this;
    }
    
    /**
     * Set JSON body
     */
    public CSRestApiBuilder jsonBody(Object body) {
        this.body = body;
        header("Content-Type", "application/json");
        return this;
    }
    
    /**
     * Set XML body
     */
    public CSRestApiBuilder xmlBody(String xml) {
        this.body = xml;
        header("Content-Type", "application/xml");
        return this;
    }
    
    /**
     * Set form body
     */
    public CSRestApiBuilder formBody(Map<String, String> formData) {
        StringBuilder form = new StringBuilder();
        formData.forEach((key, value) -> {
            if (form.length() > 0) {
                form.append("&");
            }
            form.append(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8));
            form.append("=");
            form.append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        });
        this.body = form.toString();
        header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }
    
    /**
     * Set basic authentication
     */
    public CSRestApiBuilder basicAuth(String username, String password) {
        String auth = username + ":" + password;
        String encodedAuth = java.util.Base64.getEncoder()
            .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        header("Authorization", "Basic " + encodedAuth);
        return this;
    }
    
    /**
     * Set bearer token
     */
    public CSRestApiBuilder bearerToken(String token) {
        header("Authorization", "Bearer " + token);
        return this;
    }
    
    /**
     * Set API key header
     */
    public CSRestApiBuilder apiKey(String headerName, String apiKey) {
        header(headerName, apiKey);
        return this;
    }
    
    /**
     * Accept JSON response
     */
    public CSRestApiBuilder acceptJson() {
        header("Accept", "application/json");
        return this;
    }
    
    /**
     * Accept XML response
     */
    public CSRestApiBuilder acceptXml() {
        header("Accept", "application/xml");
        return this;
    }
    
    /**
     * Build and execute request
     */
    public CSHttpResponse execute() {
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("Path not set");
        }
        
        // Create new HTTP client with timeout
        CSHttpClient client = new CSHttpClient(baseUrl != null ? baseUrl : "", timeout);
        
        // Add default headers to client
        headers.forEach(client::addDefaultHeader);
        
        // Execute request based on method
        switch (method.toUpperCase()) {
            case "GET":
                return client.get(path, queryParams.isEmpty() ? null : queryParams, null);
            
            case "POST":
                return client.post(path, body, null);
            
            case "PUT":
                return client.put(path, body, null);
            
            case "PATCH":
                return client.patch(path, body, null);
            
            case "DELETE":
                return client.delete(path).execute();
            
            case "HEAD":
                return client.head(path, null);
            
            case "OPTIONS":
                return client.options(path, null);
            
            default:
                throw new IllegalStateException("Unsupported method: " + method);
        }
    }
    
    /**
     * Execute and get response body as type
     */
    public <T> T executeAndGetBody(Class<T> responseType) {
        CSHttpResponse response = execute();
        return response.getBodyAsJson(responseType);
    }
    
    /**
     * Execute and get response body as map
     */
    public Map<String, Object> executeAndGetBodyAsMap() {
        CSHttpResponse response = execute();
        return response.getBodyAsMap();
    }
    
    /**
     * Execute and assert successful
     */
    public CSHttpResponse executeAndAssertSuccess() {
        return execute().assertSuccessful();
    }
    
    /**
     * Execute and assert status code
     */
    public CSHttpResponse executeAndAssertStatus(int expectedStatus) {
        return execute().assertStatusCode(expectedStatus);
    }
    
    /**
     * Create a copy of this builder
     */
    public CSRestApiBuilder copy() {
        CSRestApiBuilder copy = new CSRestApiBuilder(this.baseUrl);
        copy.path = this.path;
        copy.method = this.method;
        copy.headers = new HashMap<>(this.headers);
        copy.queryParams = new HashMap<>(this.queryParams);
        copy.body = this.body;
        copy.timeout = this.timeout;
        return copy;
    }
}