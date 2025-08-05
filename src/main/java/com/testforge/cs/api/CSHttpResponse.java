package com.testforge.cs.api;

import com.testforge.cs.utils.CSJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an HTTP response
 */
public class CSHttpResponse {
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;
    private final long responseTime;
    private final String requestUrl;
    private final String requestMethod;
    
    public CSHttpResponse(int statusCode, Map<String, List<String>> headers, String body, 
                         long responseTime, String requestUrl, String requestMethod) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.responseTime = responseTime;
        this.requestUrl = requestUrl;
        this.requestMethod = requestMethod;
    }
    
    /**
     * Get status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Check if response is successful (2xx)
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Check if response is redirect (3xx)
     */
    public boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }
    
    /**
     * Check if response is client error (4xx)
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Check if response is server error (5xx)
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Get all headers
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    
    /**
     * Get header value
     */
    public Optional<String> getHeader(String name) {
        List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            return Optional.of(values.get(0));
        }
        return Optional.empty();
    }
    
    /**
     * Get all header values
     */
    public List<String> getHeaderValues(String name) {
        return headers.getOrDefault(name, List.of());
    }
    
    /**
     * Get response body as string
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Get response body as JSON object
     */
    public <T> T getBodyAsJson(Class<T> clazz) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.fromJson(body, clazz);
    }
    
    /**
     * Get response body as JSON with TypeReference
     */
    public <T> T getBodyAsJson(TypeReference<T> typeRef) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.fromJson(body, typeRef);
    }
    
    /**
     * Get response body as Map
     */
    public Map<String, Object> getBodyAsMap() {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.jsonToMap(body);
    }
    
    /**
     * Get response body as List of Maps
     */
    public List<Map<String, Object>> getBodyAsListOfMaps() {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.jsonToListOfMaps(body);
    }
    
    /**
     * Extract value from JSON response using JSON path
     */
    public String getJsonValue(String jsonPath) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.getValue(body, jsonPath);
    }
    
    /**
     * Extract object from JSON response using JSON path
     */
    public <T> T getJsonObject(String jsonPath, Class<T> clazz) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return CSJsonUtils.getObject(body, jsonPath, clazz);
    }
    
    /**
     * Get response time in milliseconds
     */
    public long getResponseTime() {
        return responseTime;
    }
    
    /**
     * Get request URL
     */
    public String getRequestUrl() {
        return requestUrl;
    }
    
    /**
     * Get request method
     */
    public String getRequestMethod() {
        return requestMethod;
    }
    
    /**
     * Assert status code
     */
    public CSHttpResponse assertStatusCode(int expectedCode) {
        if (statusCode != expectedCode) {
            throw new AssertionError(String.format(
                "Expected status code %d but got %d for %s %s\nResponse body: %s",
                expectedCode, statusCode, requestMethod, requestUrl, body
            ));
        }
        return this;
    }
    
    /**
     * Assert response is successful
     */
    public CSHttpResponse assertSuccessful() {
        if (!isSuccessful()) {
            throw new AssertionError(String.format(
                "Expected successful response but got %d for %s %s\nResponse body: %s",
                statusCode, requestMethod, requestUrl, body
            ));
        }
        return this;
    }
    
    /**
     * Assert header exists
     */
    public CSHttpResponse assertHeaderExists(String name) {
        if (!headers.containsKey(name)) {
            throw new AssertionError(String.format(
                "Expected header '%s' to exist but it was not found",
                name
            ));
        }
        return this;
    }
    
    /**
     * Assert header value
     */
    public CSHttpResponse assertHeaderValue(String name, String expectedValue) {
        Optional<String> actualValue = getHeader(name);
        if (!actualValue.isPresent()) {
            throw new AssertionError(String.format(
                "Expected header '%s' to exist but it was not found",
                name
            ));
        }
        if (!actualValue.get().equals(expectedValue)) {
            throw new AssertionError(String.format(
                "Expected header '%s' to have value '%s' but got '%s'",
                name, expectedValue, actualValue.get()
            ));
        }
        return this;
    }
    
    /**
     * Assert response body contains text
     */
    public CSHttpResponse assertBodyContains(String text) {
        if (body == null || !body.contains(text)) {
            throw new AssertionError(String.format(
                "Expected response body to contain '%s' but it didn't\nActual body: %s",
                text, body
            ));
        }
        return this;
    }
    
    /**
     * Assert JSON value
     */
    public CSHttpResponse assertJsonValue(String jsonPath, String expectedValue) {
        String actualValue = getJsonValue(jsonPath);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(String.format(
                "Expected JSON path '%s' to have value '%s' but got '%s'",
                jsonPath, expectedValue, actualValue
            ));
        }
        return this;
    }
    
    /**
     * Assert response time is less than threshold
     */
    public CSHttpResponse assertResponseTime(long maxMillis) {
        if (responseTime > maxMillis) {
            throw new AssertionError(String.format(
                "Expected response time to be less than %d ms but was %d ms",
                maxMillis, responseTime
            ));
        }
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s - %d (%d ms)\n%s", 
            requestMethod, requestUrl, statusCode, responseTime,
            body != null ? body.substring(0, Math.min(body.length(), 500)) : "<no body>");
    }
}