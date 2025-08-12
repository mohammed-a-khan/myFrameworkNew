package com.testforge.cs.azuredevops.client;

import com.testforge.cs.azuredevops.config.CSADOConfiguration;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import com.testforge.cs.utils.CSJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced Azure DevOps client with retry logic, proxy support, and batch operations
 * Based on Playwright framework's ADOClient implementation
 */
public class CSEnhancedADOClient {
    private static final Logger logger = LoggerFactory.getLogger(CSEnhancedADOClient.class);
    private static CSEnhancedADOClient instance;
    
    private final CSADOConfiguration config;
    private final HttpClient httpClient;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final Map<String, CompletableFuture<?>> activeRequests = new ConcurrentHashMap<>();
    
    private CSEnhancedADOClient() {
        this.config = CSADOConfiguration.getInstance();
        this.httpClient = buildHttpClient();
        logger.info("Enhanced ADO client initialized");
    }
    
    public static synchronized CSEnhancedADOClient getInstance() {
        if (instance == null) {
            instance = new CSEnhancedADOClient();
        }
        return instance;
    }
    
    /**
     * Build HTTP client with proxy support
     */
    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getTimeout()))
            .followRedirects(HttpClient.Redirect.NORMAL);
        
        // Configure proxy if enabled
        if (config.getProxy() != null && config.getProxy().isEnabled()) {
            CSADOConfiguration.ProxyConfig proxyConfig = config.getProxy();
            builder.proxy(ProxySelector.of(
                new InetSocketAddress(proxyConfig.getServer(), proxyConfig.getPort())
            ));
            
            // Add authenticator for proxy if credentials provided
            if (proxyConfig.getUsername() != null && !proxyConfig.getUsername().isEmpty()) {
                builder.authenticator(new java.net.Authenticator() {
                    @Override
                    protected java.net.PasswordAuthentication getPasswordAuthentication() {
                        return new java.net.PasswordAuthentication(
                            proxyConfig.getUsername(),
                            proxyConfig.getPassword().toCharArray()
                        );
                    }
                });
            }
        }
        
        return builder.build();
    }
    
    /**
     * Execute request with retry logic
     */
    public <T> ADOResponse<T> request(ADORequestOptions options, Class<T> responseType) {
        String requestId = "req_" + requestCount.incrementAndGet();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("[{}] {} {}", requestId, options.method, options.url);
            
            ADOResponse<T> response = executeWithRetry(
                requestId, 
                options, 
                responseType,
                options.retryCount != null ? options.retryCount : config.getRetryCount()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] Completed in {}ms - Status: {}", 
                requestId, duration, response.status);
            
            return response;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}] Failed after {}ms", requestId, duration, e);
            throw new CSAzureDevOpsException("Request failed: " + e.getMessage(), e);
        } finally {
            activeRequests.remove(requestId);
        }
    }
    
    /**
     * Execute request with retry logic
     */
    private <T> ADOResponse<T> executeWithRetry(
            String requestId,
            ADORequestOptions options,
            Class<T> responseType,
            int retriesLeft) throws Exception {
        
        try {
            return executeRequest(requestId, options, responseType);
        } catch (Exception e) {
            if (retriesLeft > 0 && isRetryableError(e)) {
                int delay = config.getRetryDelay() * (config.getRetryCount() - retriesLeft + 1);
                logger.warn("[{}] Retrying after {}ms... ({} retries left)", 
                    requestId, delay, retriesLeft);
                
                Thread.sleep(delay);
                return executeWithRetry(requestId, options, responseType, retriesLeft - 1);
            }
            throw e;
        }
    }
    
    /**
     * Execute single request
     */
    private <T> ADOResponse<T> executeRequest(
            String requestId,
            ADORequestOptions options,
            Class<T> responseType) throws Exception {
        
        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(options.url))
            .timeout(Duration.ofMillis(config.getTimeout()));
        
        // Add headers
        Map<String, String> headers = new HashMap<>(config.getAuthHeaders());
        if (options.headers != null) {
            headers.putAll(options.headers);
        }
        headers.forEach(requestBuilder::header);
        
        // Set method and body
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
        if (options.body != null) {
            if (options.body instanceof byte[]) {
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray((byte[]) options.body);
            } else if (options.body instanceof String) {
                bodyPublisher = HttpRequest.BodyPublishers.ofString((String) options.body);
            } else {
                String json = CSJsonUtils.toJson(options.body);
                bodyPublisher = HttpRequest.BodyPublishers.ofString(json);
            }
        }
        
        switch (options.method) {
            case "GET":
                requestBuilder.GET();
                break;
            case "POST":
                requestBuilder.POST(bodyPublisher);
                break;
            case "PUT":
                requestBuilder.PUT(bodyPublisher);
                break;
            case "PATCH":
                requestBuilder.method("PATCH", bodyPublisher);
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default:
                throw new CSAzureDevOpsException("Unsupported HTTP method: " + options.method);
        }
        
        HttpRequest request = requestBuilder.build();
        
        // Execute request
        CompletableFuture<HttpResponse<String>> futureResponse = 
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        activeRequests.put(requestId, futureResponse);
        
        HttpResponse<String> httpResponse = futureResponse.get();
        
        // Parse response
        T data = null;
        if (httpResponse.body() != null && !httpResponse.body().isEmpty()) {
            if (responseType == String.class) {
                data = responseType.cast(httpResponse.body());
            } else if (responseType == byte[].class) {
                data = responseType.cast(httpResponse.body().getBytes(StandardCharsets.UTF_8));
            } else {
                data = CSJsonUtils.fromJson(httpResponse.body(), responseType);
            }
        }
        
        ADOResponse<T> response = new ADOResponse<>();
        response.status = httpResponse.statusCode();
        response.statusText = getStatusText(httpResponse.statusCode());
        response.headers = new HashMap<>();
        httpResponse.headers().map().forEach((key, value) -> 
            response.headers.put(key, String.join(",", value)));
        response.data = data;
        response.request = options;
        
        // Check for errors
        if (httpResponse.statusCode() >= 400) {
            String errorMsg = String.format("ADO request failed: %d %s", 
                httpResponse.statusCode(), response.statusText);
            ADOError error = new ADOError(errorMsg);
            error.status = httpResponse.statusCode();
            error.statusText = response.statusText;
            error.response = data;
            error.request = options;
            throw error;
        }
        
        return response;
    }
    
    /**
     * Check if error is retryable
     */
    private boolean isRetryableError(Exception e) {
        if (e instanceof IOException) {
            return true;
        }
        
        if (e instanceof ADOError) {
            ADOError adoError = (ADOError) e;
            return adoError.status != null && 
                Arrays.asList(408, 429, 500, 502, 503, 504).contains(adoError.status);
        }
        
        return false;
    }
    
    /**
     * Get status text from status code
     */
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "Unknown";
        }
    }
    
    // Convenience methods
    
    public <T> ADOResponse<T> get(String url, Class<T> responseType) {
        return request(new ADORequestOptions("GET", url), responseType);
    }
    
    public <T> ADOResponse<T> post(String url, Object body, Class<T> responseType) {
        ADORequestOptions options = new ADORequestOptions("POST", url);
        options.body = body;
        return request(options, responseType);
    }
    
    public <T> ADOResponse<T> put(String url, Object body, Class<T> responseType) {
        ADORequestOptions options = new ADORequestOptions("PUT", url);
        options.body = body;
        return request(options, responseType);
    }
    
    public <T> ADOResponse<T> patch(String url, Object body, Class<T> responseType) {
        ADORequestOptions options = new ADORequestOptions("PATCH", url);
        options.body = body;
        return request(options, responseType);
    }
    
    public <T> ADOResponse<T> delete(String url, Class<T> responseType) {
        return request(new ADORequestOptions("DELETE", url), responseType);
    }
    
    /**
     * Get list response
     */
    public <T> ADOListResponse<T> getList(String url, Class<T> itemType) {
        ADOResponse<Map> response = get(url, Map.class);
        
        ADOListResponse<T> listResponse = new ADOListResponse<>();
        listResponse.count = (Integer) response.data.get("count");
        
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.data.get("value");
        listResponse.value = new ArrayList<>();
        
        if (items != null) {
            for (Map<String, Object> item : items) {
                String json = CSJsonUtils.toJson(item);
                T typedItem = CSJsonUtils.fromJson(json, itemType);
                listResponse.value.add(typedItem);
            }
        }
        
        return listResponse;
    }
    
    /**
     * Get all items with pagination
     */
    public <T> List<T> getAll(String url, Class<T> itemType, int pageSize) {
        List<T> allItems = new ArrayList<>();
        int skip = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            String paginatedUrl = addQueryParams(url, Map.of(
                "$top", pageSize,
                "$skip", skip
            ));
            
            ADOListResponse<T> response = getList(paginatedUrl, itemType);
            
            if (response.value != null && !response.value.isEmpty()) {
                allItems.addAll(response.value);
                skip += response.value.size();
                hasMore = response.value.size() == pageSize;
            } else {
                hasMore = false;
            }
        }
        
        return allItems;
    }
    
    /**
     * Upload attachment
     */
    public ADOAttachmentResponse uploadAttachment(byte[] content, String fileName, String contentType) {
        String url = config.buildUrl(config.getEndpoints().getAttachments(), null);
        
        ADORequestOptions options = new ADORequestOptions("POST", url);
        options.body = content;
        options.headers = Map.of(
            "Content-Type", contentType != null ? contentType : "application/octet-stream",
            "Content-Length", String.valueOf(content.length)
        );
        
        ADOResponse<ADOAttachmentResponse> response = request(options, ADOAttachmentResponse.class);
        return response.data;
    }
    
    /**
     * Execute batch requests
     */
    public List<Map<String, Object>> executeBatch(List<BatchRequest> requests) {
        String batchUrl = config.getOrganizationUrl() + "/_apis/$batch";
        
        Map<String, Object> batchBody = new HashMap<>();
        List<Map<String, Object>> batchRequests = new ArrayList<>();
        
        for (int i = 0; i < requests.size(); i++) {
            BatchRequest req = requests.get(i);
            Map<String, Object> batchReq = new HashMap<>();
            batchReq.put("id", String.valueOf(i));
            batchReq.put("method", req.method);
            batchReq.put("url", req.uri);
            batchReq.put("headers", req.headers);
            batchReq.put("body", req.body);
            batchRequests.add(batchReq);
        }
        
        batchBody.put("requests", batchRequests);
        
        ADOResponse<Map> response = post(batchUrl, batchBody, Map.class);
        return (List<Map<String, Object>>) response.data.get("responses");
    }
    
    /**
     * Add query parameters to URL
     */
    private String addQueryParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        
        StringBuilder urlBuilder = new StringBuilder(url);
        String separator = url.contains("?") ? "&" : "?";
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            urlBuilder.append(separator)
                .append(entry.getKey())
                .append("=")
                .append(java.net.URLEncoder.encode(
                    entry.getValue().toString(), 
                    StandardCharsets.UTF_8));
            separator = "&";
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * Cancel all active requests
     */
    public void cancelAllRequests() {
        logger.info("Cancelling {} active requests", activeRequests.size());
        
        for (Map.Entry<String, CompletableFuture<?>> entry : activeRequests.entrySet()) {
            entry.getValue().cancel(true);
            logger.debug("Cancelled request: {}", entry.getKey());
        }
        
        activeRequests.clear();
    }
    
    /**
     * Get statistics
     */
    public RequestStatistics getStatistics() {
        RequestStatistics stats = new RequestStatistics();
        stats.totalRequests = requestCount.get();
        stats.activeRequests = activeRequests.size();
        return stats;
    }
    
    /**
     * Test connection
     */
    public boolean testConnection() {
        try {
            logger.info("Testing Azure DevOps connection...");
            
            String url = config.getOrganizationUrl() + "/_apis/projects";
            url = config.buildUrl(url, null);
            
            ADOResponse<Map> response = get(url, Map.class);
            
            boolean connected = response.status == 200;
            logger.info("Azure DevOps connection test: {}", connected ? "SUCCESS" : "FAILED");
            
            return connected;
        } catch (Exception e) {
            logger.error("Azure DevOps connection test failed", e);
            return false;
        }
    }
    
    // Inner classes
    
    public static class ADORequestOptions {
        public String method;
        public String url;
        public Map<String, String> headers;
        public Object body;
        public Integer timeout;
        public Integer retryCount;
        public boolean skipRetry;
        
        public ADORequestOptions(String method, String url) {
            this.method = method;
            this.url = url;
        }
    }
    
    public static class ADOResponse<T> {
        public int status;
        public String statusText;
        public Map<String, String> headers;
        public T data;
        public ADORequestOptions request;
    }
    
    public static class ADOListResponse<T> {
        public int count;
        public List<T> value;
    }
    
    public static class ADOError extends Exception {
        public Integer status;
        public String statusText;
        public Object response;
        public ADORequestOptions request;
        public String code;
        
        public ADOError(String message) {
            super(message);
        }
    }
    
    public static class ADOAttachmentResponse {
        public String id;
        public String url;
    }
    
    public static class BatchRequest {
        public String method;
        public String uri;
        public Map<String, String> headers;
        public Object body;
        
        public BatchRequest(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }
    }
    
    public static class RequestStatistics {
        public int totalRequests;
        public int activeRequests;
    }
}