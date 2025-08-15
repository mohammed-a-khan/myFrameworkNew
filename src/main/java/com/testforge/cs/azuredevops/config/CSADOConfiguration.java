package com.testforge.cs.azuredevops.config;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Azure DevOps configuration with comprehensive settings
 * Based on Playwright framework's ADOConfig implementation
 */
public class CSADOConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(CSADOConfiguration.class);
    private static CSADOConfiguration instance;
    
    // Core configuration
    private String organizationUrl;
    private String projectName;
    private String personalAccessToken;
    private String username;
    private String password;
    private AuthType authType;
    private String apiVersion;
    
    // Connection settings
    private int timeout;
    private int retryCount;
    private int retryDelay;
    
    // Test configuration
    private Integer testPlanId;
    private Integer testSuiteId;
    private String buildId;
    private String releaseId;
    private String environment;
    private String runName;
    private boolean automated;
    
    // Upload settings
    private boolean uploadAttachments;
    private boolean uploadScreenshots;
    private boolean uploadVideos;
    private boolean uploadLogs;
    private boolean updateTestCases;
    private boolean createBugsOnFailure;
    
    // Proxy configuration
    private ProxyConfig proxy;
    
    // Bug template
    private BugTemplate bugTemplate;
    
    // Custom fields
    private Map<String, Object> customFields;
    
    // Endpoints
    private ADOEndpoints endpoints;
    
    private boolean enabled;
    private boolean initialized = false;
    
    public enum AuthType {
        PAT("pat"),
        BASIC("basic"),
        OAUTH("oauth");
        
        private final String value;
        
        AuthType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static AuthType fromString(String value) {
            for (AuthType type : AuthType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return PAT;
        }
    }
    
    public static class ProxyConfig {
        private boolean enabled;
        private String server;
        private int port;
        private String username;
        private String password;
        private String[] bypass;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getServer() { return server; }
        public void setServer(String server) { this.server = server; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String[] getBypass() { return bypass; }
        public void setBypass(String[] bypass) { this.bypass = bypass; }
    }
    
    public static class BugTemplate {
        private String title;
        private String assignedTo;
        private String areaPath;
        private String iterationPath;
        private int priority;
        private String severity;
        private String[] tags;
        private Map<String, Object> customFields;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
        
        public String getAreaPath() { return areaPath; }
        public void setAreaPath(String areaPath) { this.areaPath = areaPath; }
        
        public String getIterationPath() { return iterationPath; }
        public void setIterationPath(String iterationPath) { this.iterationPath = iterationPath; }
        
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
        
        public Map<String, Object> getCustomFields() { return customFields; }
        public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }
    }
    
    public static class ADOEndpoints {
        private String testPlans;
        private String testSuites;
        private String testRuns;
        private String testResults;
        private String testCases;
        private String testPoints;
        private String attachments;
        private String workItems;
        private String builds;
        private String releases;
        
        // Getters
        public String getTestPlans() { return testPlans; }
        public String getTestSuites() { return testSuites; }
        public String getTestRuns() { return testRuns; }
        public String getTestResults() { return testResults; }
        public String getTestCases() { return testCases; }
        public String getTestPoints() { return testPoints; }
        public String getAttachments() { return attachments; }
        public String getWorkItems() { return workItems; }
        public String getBuilds() { return builds; }
        public String getReleases() { return releases; }
    }
    
    private CSADOConfiguration() {
        // Private constructor for singleton
    }
    
    public static synchronized CSADOConfiguration getInstance() {
        if (instance == null) {
            instance = new CSADOConfiguration();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Initialize configuration from properties
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            logger.info("Initializing Azure DevOps configuration...");
            
            CSConfigManager config = CSConfigManager.getInstance();
            
            // Check if enabled
            enabled = Boolean.parseBoolean(config.getProperty("cs.azure.devops.enabled", "false"));
            
            if (!enabled) {
                logger.info("Azure DevOps integration is disabled");
                initialized = true;
                return;
            }
            
            // Load core configuration
            loadCoreConfiguration(config);
            
            // Load connection settings
            loadConnectionSettings(config);
            
            // Load test configuration
            loadTestConfiguration(config);
            
            // Load upload settings
            loadUploadSettings(config);
            
            // Load proxy configuration
            loadProxyConfiguration(config);
            
            // Load bug template
            loadBugTemplate(config);
            
            // Load custom fields
            loadCustomFields(config);
            
            // Validate configuration
            validateConfiguration();
            
            // Build endpoints
            buildEndpoints();
            
            logger.info("Azure DevOps configuration initialized successfully");
            initialized = true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize Azure DevOps configuration", e);
            enabled = false;
            initialized = false;
            throw new CSAzureDevOpsException("Failed to initialize ADO configuration", e);
        }
    }
    
    private void loadCoreConfiguration(CSConfigManager config) {
        String orgUrl = config.getProperty("cs.azure.devops.organization.url", "");
        if (orgUrl.isEmpty()) {
            // Fallback to old property name for backward compatibility
            String org = config.getProperty("cs.azure.devops.organization", "");
            if (!org.isEmpty()) {
                orgUrl = "https://dev.azure.com/" + org;
            }
        }
        this.organizationUrl = orgUrl.replaceAll("/$", "");
        
        this.projectName = config.getProperty("cs.azure.devops.project", "");
        this.authType = AuthType.fromString(config.getProperty("cs.azure.devops.auth.type", "pat"));
        this.apiVersion = config.getProperty("cs.azure.devops.api.version", "7.1");
        
        switch (authType) {
            case PAT:
                this.personalAccessToken = config.getProperty("cs.azure.devops.token", "");
                break;
            case BASIC:
                this.username = config.getProperty("cs.azure.devops.username", "");
                this.password = config.getProperty("cs.azure.devops.password", "");
                break;
            case OAUTH:
                // OAuth implementation would go here
                break;
        }
    }
    
    private void loadConnectionSettings(CSConfigManager config) {
        this.timeout = Integer.parseInt(config.getProperty("cs.azure.devops.timeout", "60000"));
        this.retryCount = Integer.parseInt(config.getProperty("cs.azure.devops.retry.count", "3"));
        this.retryDelay = Integer.parseInt(config.getProperty("cs.azure.devops.retry.delay", "1000"));
    }
    
    private void loadTestConfiguration(CSConfigManager config) {
        String planId = config.getProperty("cs.azure.devops.test.plan.id", "");
        if (!planId.isEmpty()) {
            this.testPlanId = Integer.parseInt(planId);
        }
        
        String suiteId = config.getProperty("cs.azure.devops.test.suite.id", "");
        if (!suiteId.isEmpty()) {
            this.testSuiteId = Integer.parseInt(suiteId);
        }
        
        this.buildId = config.getProperty("cs.azure.devops.build.id", "");
        this.releaseId = config.getProperty("cs.azure.devops.release.id", "");
        this.environment = config.getProperty("cs.azure.devops.environment", "");
        this.runName = config.getProperty("cs.azure.devops.run.name", 
            "CS TestForge Automated Run - " + new java.util.Date());
        this.automated = Boolean.parseBoolean(config.getProperty("cs.azure.devops.automated", "true"));
    }
    
    private void loadUploadSettings(CSConfigManager config) {
        this.uploadAttachments = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.upload.attachments", "true"));
        this.uploadScreenshots = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.upload.screenshots", "true"));
        this.uploadVideos = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.upload.videos", "true"));
        this.uploadLogs = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.upload.logs", "true"));
        this.updateTestCases = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.update.testcases", "false"));
        this.createBugsOnFailure = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.create.bugs", "false"));
    }
    
    private void loadProxyConfiguration(CSConfigManager config) {
        boolean proxyEnabled = Boolean.parseBoolean(
            config.getProperty("cs.azure.devops.proxy.enabled", "false"));
        
        if (proxyEnabled) {
            proxy = new ProxyConfig();
            proxy.setEnabled(true);
            proxy.setServer(config.getProperty("cs.azure.devops.proxy.server", ""));
            proxy.setPort(Integer.parseInt(config.getProperty("cs.azure.devops.proxy.port", "8080")));
            proxy.setUsername(config.getProperty("cs.azure.devops.proxy.username", ""));
            proxy.setPassword(config.getProperty("cs.azure.devops.proxy.password", ""));
            
            String bypassList = config.getProperty("cs.azure.devops.proxy.bypass", "");
            if (!bypassList.isEmpty()) {
                proxy.setBypass(bypassList.split(","));
            }
        }
    }
    
    private void loadBugTemplate(CSConfigManager config) {
        if (createBugsOnFailure) {
            bugTemplate = new BugTemplate();
            bugTemplate.setTitle(config.getProperty("cs.azure.devops.bug.title.template", 
                "Test Failed: {testName}"));
            bugTemplate.setAssignedTo(config.getProperty("cs.azure.devops.bug.assigned.to", ""));
            bugTemplate.setAreaPath(config.getProperty("cs.azure.devops.bug.area.path", ""));
            bugTemplate.setIterationPath(config.getProperty("cs.azure.devops.bug.iteration.path", ""));
            bugTemplate.setPriority(Integer.parseInt(
                config.getProperty("cs.azure.devops.bug.priority", "2")));
            bugTemplate.setSeverity(config.getProperty("cs.azure.devops.bug.severity", "Medium"));
            
            String tags = config.getProperty("cs.azure.devops.bug.tags", "");
            if (!tags.isEmpty()) {
                bugTemplate.setTags(tags.split(","));
            }
        }
    }
    
    private void loadCustomFields(CSConfigManager config) {
        String customFieldsJson = config.getProperty("cs.azure.devops.custom.fields", "");
        if (!customFieldsJson.isEmpty()) {
            try {
                // Parse JSON custom fields - would need JSON parser
                customFields = new HashMap<>();
                logger.debug("Custom fields loaded");
            } catch (Exception e) {
                logger.warn("Failed to parse custom fields", e);
            }
        }
    }
    
    private void validateConfiguration() {
        if (!enabled) {
            return;
        }
        
        if (organizationUrl == null || organizationUrl.isEmpty()) {
            throw new CSAzureDevOpsException("Azure DevOps organization URL is required");
        }
        
        if (projectName == null || projectName.isEmpty()) {
            throw new CSAzureDevOpsException("Azure DevOps project name is required");
        }
        
        switch (authType) {
            case PAT:
                if (personalAccessToken == null || personalAccessToken.isEmpty()) {
                    throw new CSAzureDevOpsException("Personal Access Token is required for PAT authentication");
                }
                break;
            case BASIC:
                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    throw new CSAzureDevOpsException("Username and password are required for basic authentication");
                }
                break;
        }
        
        if (proxy != null && proxy.isEnabled()) {
            if (proxy.getServer() == null || proxy.getServer().isEmpty()) {
                throw new CSAzureDevOpsException("Proxy server is required when proxy is enabled");
            }
            if (proxy.getPort() < 1 || proxy.getPort() > 65535) {
                throw new CSAzureDevOpsException("Proxy port must be between 1 and 65535");
            }
        }
    }
    
    private void buildEndpoints() {
        if (organizationUrl == null || organizationUrl.isEmpty() || 
            projectName == null || projectName.isEmpty()) {
            return;
        }
        
        String baseUrl = organizationUrl + "/" + projectName + "/_apis";
        
        endpoints = new ADOEndpoints();
        endpoints.testPlans = baseUrl + "/test/plans";
        endpoints.testSuites = baseUrl + "/test/plans/{planId}/suites";
        endpoints.testRuns = baseUrl + "/test/runs";
        endpoints.testResults = baseUrl + "/test/runs/{runId}/results";
        endpoints.testCases = baseUrl + "/wit/workitems";
        endpoints.testPoints = baseUrl + "/test/plans/{planId}/suites/{suiteId}/points";
        endpoints.attachments = baseUrl + "/wit/attachments";
        endpoints.workItems = baseUrl + "/wit/workitems";
        endpoints.builds = baseUrl + "/build/builds";
        endpoints.releases = baseUrl + "/release/releases";
    }
    
    /**
     * Get authentication headers
     */
    public Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        switch (authType) {
            case PAT:
                String token = java.util.Base64.getEncoder().encodeToString(
                    (":" + personalAccessToken).getBytes());
                headers.put("Authorization", "Basic " + token);
                break;
            case BASIC:
                String creds = java.util.Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes());
                headers.put("Authorization", "Basic " + creds);
                break;
            case OAUTH:
                // OAuth implementation
                break;
        }
        
        return headers;
    }
    
    /**
     * Build URL with parameters
     */
    public String buildUrl(String endpoint, Map<String, Object> params) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new CSAzureDevOpsException("Endpoint cannot be null or empty");
        }
        
        String url = endpoint;
        
        // Replace path parameters
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", 
                    java.net.URLEncoder.encode(entry.getValue().toString(), 
                        java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        
        // Add API version
        String separator = url.contains("?") ? "&" : "?";
        url += separator + "api-version=" + apiVersion;
        
        return url;
    }
    
    /**
     * Format bug title with placeholders
     */
    public String formatBugTitle(String testName, String errorMessage) {
        if (bugTemplate == null || bugTemplate.getTitle() == null) {
            return "Test Failed: " + testName;
        }
        
        return bugTemplate.getTitle()
            .replace("{testName}", testName)
            .replace("{date}", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()))
            .replace("{time}", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()))
            .replace("{error}", errorMessage != null ? errorMessage : "Unknown error");
    }
    
    /**
     * Check if proxy should be bypassed for URL
     */
    public boolean shouldBypassProxy(String url) {
        if (proxy == null || !proxy.isEnabled() || proxy.getBypass() == null) {
            return false;
        }
        
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String hostname = urlObj.getHost().toLowerCase();
            
            for (String bypass : proxy.getBypass()) {
                String pattern = bypass.toLowerCase()
                    .replace(".", "\\.")
                    .replace("*", ".*");
                
                if (hostname.matches(pattern)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking proxy bypass", e);
        }
        
        return false;
    }
    
    // Getters
    public String getOrganizationUrl() { return organizationUrl; }
    public String getProjectName() { return projectName; }
    public String getPersonalAccessToken() { return personalAccessToken; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public AuthType getAuthType() { return authType; }
    public String getApiVersion() { return apiVersion; }
    public int getTimeout() { return timeout; }
    public int getRetryCount() { return retryCount; }
    public int getRetryDelay() { return retryDelay; }
    public String getTestPlanId() { return testPlanId != null ? testPlanId.toString() : null; }
    public String getTestSuiteId() { return testSuiteId != null ? testSuiteId.toString() : null; }
    public String getBuildId() { return buildId; }
    public String getReleaseId() { return releaseId; }
    public String getEnvironment() { return environment; }
    public String getRunName() { return runName; }
    public boolean isAutomated() { return automated; }
    public boolean isUploadAttachments() { return uploadAttachments; }
    public boolean isUploadScreenshots() { return uploadScreenshots; }
    public boolean isUploadVideos() { return uploadVideos; }
    public boolean isUploadLogs() { return uploadLogs; }
    public boolean isUpdateTestCases() { return updateTestCases; }
    public boolean isCreateBugsOnFailure() { return createBugsOnFailure; }
    public ProxyConfig getProxy() { return proxy; }
    public BugTemplate getBugTemplate() { return bugTemplate; }
    public Map<String, Object> getCustomFields() { return customFields; }
    public ADOEndpoints getEndpoints() { return endpoints; }
    public boolean isEnabled() { return enabled; }
    
    /**
     * Reset configuration (for testing)
     */
    public void reset() {
        logger.info("Resetting Azure DevOps configuration");
        initialized = false;
        enabled = false;
    }
}