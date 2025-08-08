package com.testforge.cs.api;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP request builder for fluent API
 */
public class CSHttpRequestBuilder {
    private final CSHttpClient client;
    private final String method;
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private String body;
    private String contentType;
    
    public CSHttpRequestBuilder(CSHttpClient client, String method, String url) {
        this.client = client;
        this.method = method;
        this.url = url;
    }
    
    public CSHttpRequestBuilder header(String name, String value) {
        headers.put(name, value);
        return this;
    }
    
    public CSHttpRequestBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }
    
    public CSHttpRequestBuilder queryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }
    
    public CSHttpRequestBuilder queryParams(Map<String, String> params) {
        this.queryParams.putAll(params);
        return this;
    }
    
    public CSHttpRequestBuilder body(String body) {
        this.body = body;
        return this;
    }
    
    public CSHttpRequestBuilder contentType(String contentType) {
        this.contentType = contentType; // Store for potential future use
        header("Content-Type", contentType);
        return this;
    }
    
    public CSHttpRequestBuilder withBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        header("Authorization", "Basic " + encoded);
        return this;
    }
    
    public CSHttpRequestBuilder withBearerToken(String token) {
        header("Authorization", "Bearer " + token);
        return this;
    }
    
    public CSHttpResponse execute() {
        // Build final URL with query params
        String finalUrl = buildUrlWithParams();
        
        // Execute based on method
        switch (method.toUpperCase()) {
            case "GET":
                return client.executeGet(finalUrl, headers);
                
            case "POST":
                return client.executePost(finalUrl, body, headers);
                
            case "PUT":
                return client.executePut(finalUrl, body, headers);
                
            case "DELETE":
                return client.executeDelete(finalUrl, headers);
                
            case "PATCH":
                return client.executePatch(finalUrl, body, headers);
                
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
    
    private String buildUrlWithParams() {
        if (queryParams.isEmpty()) {
            return url;
        }
        
        StringBuilder urlBuilder = new StringBuilder(url);
        if (url.contains("?")) {
            urlBuilder.append("&");
        } else {
            urlBuilder.append("?");
        }
        
        queryParams.forEach((key, value) -> 
            urlBuilder.append(key).append("=")
                     .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8))
                     .append("&")
        );
        
        // Remove trailing &
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&') {
            urlBuilder.setLength(urlBuilder.length() - 1);
        }
        
        return urlBuilder.toString();
    }
}