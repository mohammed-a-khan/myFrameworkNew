package com.testforge.cs.api;

import com.testforge.cs.exceptions.CSApiException;
import com.testforge.cs.utils.CSJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client wrapper using Java 11+ native HTTP client
 * Supports REST API testing without third-party libraries
 */
public class CSHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(CSHttpClient.class);
    
    private final HttpClient httpClient;
    private final Map<String, String> defaultHeaders;
    private final Duration defaultTimeout;
    private String baseUrl;
    
    /**
     * Create HTTP client with default settings
     */
    public CSHttpClient() {
        this("", Duration.ofSeconds(30));
    }
    
    /**
     * Create HTTP client with base URL
     */
    public CSHttpClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(30));
    }
    
    /**
     * Create HTTP client with base URL and timeout
     */
    public CSHttpClient(String baseUrl, Duration timeout) {
        this(baseUrl, timeout, null, 0);
    }
    
    /**
     * Create HTTP client with base URL, timeout, and proxy configuration
     */
    public CSHttpClient(String baseUrl, Duration timeout, String proxyHost, int proxyPort) {
        this(baseUrl, timeout, proxyHost, proxyPort, null, null);
    }
    
    /**
     * Create HTTP client with base URL, timeout, proxy configuration, and authentication
     */
    public CSHttpClient(String baseUrl, Duration timeout, String proxyHost, int proxyPort, 
                       String proxyUsername, String proxyPassword) {
        this.baseUrl = baseUrl;
        this.defaultTimeout = timeout;
        this.defaultHeaders = new HashMap<>();
        
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NORMAL);
        
        // Configure proxy if provided
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            logger.info("Configuring HTTP client with proxy: {}:{}", proxyHost, proxyPort);
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
            clientBuilder.proxy(ProxySelector.of(proxyAddress));
            
            // Configure proxy authentication if provided
            if (proxyUsername != null && !proxyUsername.isEmpty() && 
                proxyPassword != null && !proxyPassword.isEmpty()) {
                logger.info("Configuring proxy authentication for user: {}", proxyUsername);
                clientBuilder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                });
            }
        }
        
        this.httpClient = clientBuilder.build();
        
        // Set default headers
        defaultHeaders.put("User-Agent", "CS-TestForge-Framework/1.0");
        defaultHeaders.put("Accept", "application/json");
    }
    
    /**
     * Create default HTTP client instance
     */
    public static CSHttpClient create() {
        return new CSHttpClient();
    }
    
    /**
     * Create HTTP client instance with base URL
     */
    public static CSHttpClient create(String baseUrl) {
        return new CSHttpClient(baseUrl);
    }
    
    /**
     * Set base URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Add default header
     */
    public void addDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }
    
    /**
     * Remove default header
     */
    public void removeDefaultHeader(String name) {
        defaultHeaders.remove(name);
    }
    
    /**
     * GET request builder
     */
    public CSHttpRequestBuilder get(String path) {
        return new CSHttpRequestBuilder(this, "GET", baseUrl + path);
    }
    
    /**
     * POST request builder
     */
    public CSHttpRequestBuilder post(String path) {
        return new CSHttpRequestBuilder(this, "POST", baseUrl + path);
    }
    
    /**
     * PUT request builder
     */
    public CSHttpRequestBuilder put(String path) {
        return new CSHttpRequestBuilder(this, "PUT", baseUrl + path);
    }
    
    /**
     * DELETE request builder
     */
    public CSHttpRequestBuilder delete(String path) {
        return new CSHttpRequestBuilder(this, "DELETE", baseUrl + path);
    }
    
    /**
     * PATCH request builder
     */
    public CSHttpRequestBuilder patch(String path) {
        return new CSHttpRequestBuilder(this, "PATCH", baseUrl + path);
    }
    
    /**
     * Execute GET request
     */
    public CSHttpResponse executeGet(String url, Map<String, String> headers) {
        HttpRequest request = buildRequest(url, "GET", null, headers);
        return executeRequest(request);
    }
    
    /**
     * Execute POST request
     */
    public CSHttpResponse executePost(String url, String body, Map<String, String> headers) {
        HttpRequest request = buildRequest(url, "POST", body, headers);
        return executeRequest(request);
    }
    
    /**
     * Execute PUT request
     */
    public CSHttpResponse executePut(String url, String body, Map<String, String> headers) {
        HttpRequest request = buildRequest(url, "PUT", body, headers);
        return executeRequest(request);
    }
    
    /**
     * Execute DELETE request
     */
    public CSHttpResponse executeDelete(String url, Map<String, String> headers) {
        HttpRequest request = buildRequest(url, "DELETE", null, headers);
        return executeRequest(request);
    }
    
    /**
     * Execute PATCH request
     */
    public CSHttpResponse executePatch(String url, String body, Map<String, String> headers) {
        HttpRequest request = buildRequest(url, "PATCH", body, headers);
        return executeRequest(request);
    }
    
    /**
     * GET request with query parameters
     */
    public CSHttpResponse get(String path, Map<String, String> queryParams) {
        return get(path, queryParams, null);
    }
    
    /**
     * GET request with query parameters and headers
     */
    public CSHttpResponse get(String path, Map<String, String> queryParams, Map<String, String> headers) {
        String url = buildUrl(path, queryParams);
        HttpRequest request = buildRequest(url, "GET", null, headers);
        return executeRequest(request);
    }
    
    /**
     * POST request with JSON body
     */
    public CSHttpResponse post(String path, Object body) {
        return post(path, body, null);
    }
    
    /**
     * POST request with JSON body and headers
     */
    public CSHttpResponse post(String path, Object body, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "POST", body, headers);
        return executeRequest(request);
    }
    
    /**
     * PUT request with JSON body
     */
    public CSHttpResponse put(String path, Object body) {
        return put(path, body, null);
    }
    
    /**
     * PUT request with JSON body and headers
     */
    public CSHttpResponse put(String path, Object body, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "PUT", body, headers);
        return executeRequest(request);
    }
    
    /**
     * PATCH request with JSON body
     */
    public CSHttpResponse patch(String path, Object body) {
        return patch(path, body, null);
    }
    
    /**
     * PATCH request with JSON body and headers
     */
    public CSHttpResponse patch(String path, Object body, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "PATCH", body, headers);
        return executeRequest(request);
    }
    
    
    /**
     * HEAD request
     */
    public CSHttpResponse head(String path) {
        return head(path, null);
    }
    
    /**
     * HEAD request with headers
     */
    public CSHttpResponse head(String path, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "HEAD", null, headers);
        return executeRequest(request);
    }
    
    /**
     * OPTIONS request
     */
    public CSHttpResponse options(String path) {
        return options(path, null);
    }
    
    /**
     * OPTIONS request with headers
     */
    public CSHttpResponse options(String path, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "OPTIONS", null, headers);
        return executeRequest(request);
    }
    
    /**
     * Async GET request
     */
    public CompletableFuture<CSHttpResponse> getAsync(String path) {
        return getAsync(path, null, null);
    }
    
    /**
     * Async GET request with query parameters and headers
     */
    public CompletableFuture<CSHttpResponse> getAsync(String path, Map<String, String> queryParams, Map<String, String> headers) {
        String url = buildUrl(path, queryParams);
        HttpRequest request = buildRequest(url, "GET", null, headers);
        return executeRequestAsync(request);
    }
    
    /**
     * Async POST request
     */
    public CompletableFuture<CSHttpResponse> postAsync(String path, Object body, Map<String, String> headers) {
        String url = buildUrl(path, null);
        HttpRequest request = buildRequest(url, "POST", body, headers);
        return executeRequestAsync(request);
    }
    
    /**
     * Build full URL
     */
    private String buildUrl(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder();
        
        // Add base URL if present
        if (baseUrl != null && !baseUrl.isEmpty()) {
            url.append(baseUrl);
            if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
                url.append("/");
            }
        }
        
        // Add path
        url.append(path);
        
        // Add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            queryParams.forEach((key, value) -> {
                url.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                url.append("=");
                url.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                url.append("&");
            });
            // Remove trailing &
            url.setLength(url.length() - 1);
        }
        
        return url.toString();
    }
    
    /**
     * Build HTTP request
     */
    private HttpRequest buildRequest(String url, String method, Object body, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(defaultTimeout);
        
        // Add default headers
        defaultHeaders.forEach(builder::header);
        
        // Add custom headers
        if (headers != null) {
            headers.forEach(builder::header);
        }
        
        // Set body
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
        if (body != null) {
            String jsonBody;
            if (body instanceof String) {
                jsonBody = (String) body;
            } else {
                jsonBody = CSJsonUtils.toJson(body);
            }
            bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonBody);
            
            // Set Content-Type if not already set
            if (headers == null || !headers.containsKey("Content-Type")) {
                builder.header("Content-Type", "application/json");
            }
        }
        
        // Set method
        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(bodyPublisher);
                break;
            case "PUT":
                builder.PUT(bodyPublisher);
                break;
            case "DELETE":
                if (body != null) {
                    builder.method("DELETE", bodyPublisher);
                } else {
                    builder.DELETE();
                }
                break;
            case "PATCH":
                builder.method("PATCH", bodyPublisher);
                break;
            case "HEAD":
                builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                break;
            case "OPTIONS":
                builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
                break;
            default:
                builder.method(method, bodyPublisher);
        }
        
        return builder.build();
    }
    
    /**
     * Execute HTTP request
     */
    private CSHttpResponse executeRequest(HttpRequest request) {
        try {
            logger.debug("Executing {} request to: {}", request.method(), request.uri());
            
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            
            CSHttpResponse csResponse = new CSHttpResponse(
                response.statusCode(),
                response.headers().map(),
                response.body(),
                endTime - startTime,
                request.uri().toString(),
                request.method()
            );
            
            logger.debug("Response: {} - {} ms", response.statusCode(), endTime - startTime);
            return csResponse;
            
        } catch (IOException | InterruptedException e) {
            throw new CSApiException("Failed to execute HTTP request: " + request.uri(), e);
        }
    }
    
    /**
     * Execute HTTP request asynchronously
     */
    private CompletableFuture<CSHttpResponse> executeRequestAsync(HttpRequest request) {
        logger.debug("Executing async {} request to: {}", request.method(), request.uri());
        
        long startTime = System.currentTimeMillis();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                long endTime = System.currentTimeMillis();
                
                CSHttpResponse csResponse = new CSHttpResponse(
                    response.statusCode(),
                    response.headers().map(),
                    response.body(),
                    endTime - startTime,
                    request.uri().toString(),
                    request.method()
                );
                
                logger.debug("Async response: {} - {} ms", response.statusCode(), endTime - startTime);
                return csResponse;
            })
            .exceptionally(throwable -> {
                throw new CSApiException("Failed to execute async HTTP request: " + request.uri(), throwable);
            });
    }
    
    /**
     * Get the underlying HTTP client
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
}