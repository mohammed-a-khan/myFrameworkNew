package com.testforge.cs.repository;

import java.util.Map;

/**
 * Environment configuration definition for repository management
 */
public class CSEnvironmentConfig {
    private String name;
    private String description;
    private String baseUrl;
    private Map<String, String> urls;
    private Map<String, String> credentials;
    private Map<String, String> properties;
    private Map<String, String> databaseConfig;
    private Map<String, String> apiConfig;
    private String browserType;
    private boolean headless;
    private String version;
    
    // Default constructor for JSON deserialization
    public CSEnvironmentConfig() {}
    
    public CSEnvironmentConfig(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public Map<String, String> getUrls() {
        return urls;
    }
    
    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }
    
    public Map<String, String> getCredentials() {
        return credentials;
    }
    
    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public Map<String, String> getDatabaseConfig() {
        return databaseConfig;
    }
    
    public void setDatabaseConfig(Map<String, String> databaseConfig) {
        this.databaseConfig = databaseConfig;
    }
    
    public Map<String, String> getApiConfig() {
        return apiConfig;
    }
    
    public void setApiConfig(Map<String, String> apiConfig) {
        this.apiConfig = apiConfig;
    }
    
    public String getBrowserType() {
        return browserType;
    }
    
    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }
    
    public boolean isHeadless() {
        return headless;
    }
    
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    // Utility methods
    
    /**
     * Get URL by key
     */
    public String getUrl(String key) {
        return urls != null ? urls.get(key) : null;
    }
    
    /**
     * Get credential by key
     */
    public String getCredential(String key) {
        return credentials != null ? credentials.get(key) : null;
    }
    
    /**
     * Get property by key
     */
    public String getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
    
    /**
     * Get database config by key
     */
    public String getDatabaseProperty(String key) {
        return databaseConfig != null ? databaseConfig.get(key) : null;
    }
    
    /**
     * Get API config by key
     */
    public String getApiProperty(String key) {
        return apiConfig != null ? apiConfig.get(key) : null;
    }
    
    /**
     * Get username credential
     */
    public String getUsername() {
        return getCredential("username");
    }
    
    /**
     * Get password credential
     */
    public String getPassword() {
        return getCredential("password");
    }
    
    /**
     * Get database URL
     */
    public String getDatabaseUrl() {
        return getDatabaseProperty("url");
    }
    
    /**
     * Get API base URL
     */
    public String getApiBaseUrl() {
        return getApiProperty("baseUrl");
    }
    
    @Override
    public String toString() {
        return String.format("CSEnvironmentConfig{name='%s', baseUrl='%s', browser='%s', headless=%s}", 
            name, baseUrl, browserType, headless);
    }
}