package com.testforge.cs.azuredevops;

import com.testforge.cs.api.CSHttpClient;
import com.testforge.cs.api.CSHttpResponse;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import com.testforge.cs.utils.CSJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Azure DevOps client for test result publishing
 * Uses native Java HTTP client without third-party dependencies
 */
public class CSAzureDevOpsClient {
    private static final Logger logger = LoggerFactory.getLogger(CSAzureDevOpsClient.class);
    private static final String API_VERSION = "7.1";
    
    private final String organization;
    private final String project;
    private final String personalAccessToken;
    private final CSHttpClient httpClient;
    private final String baseUrl;
    
    /**
     * Create Azure DevOps client
     */
    public CSAzureDevOpsClient(String organization, String project, String personalAccessToken) {
        this.organization = organization;
        this.project = project;
        this.personalAccessToken = personalAccessToken;
        this.baseUrl = String.format("https://dev.azure.com/%s/%s/_apis", organization, project);
        
        this.httpClient = new CSHttpClient(baseUrl, Duration.ofSeconds(60));
        
        // Set authentication header
        String authHeader = Base64.getEncoder().encodeToString(
            (":" + personalAccessToken).getBytes()
        );
        httpClient.addDefaultHeader("Authorization", "Basic " + authHeader);
        httpClient.addDefaultHeader("Content-Type", "application/json");
        httpClient.addDefaultHeader("Accept", "application/json");
    }
    
    /**
     * Create test run
     */
    public CSTestRun createTestRun(String name, String buildId, String planId) {
        logger.info("Creating test run: {}", name);
        
        Map<String, Object> runData = new HashMap<>();
        runData.put("name", name);
        runData.put("automated", true);
        runData.put("isAutomated", true);
        runData.put("state", "InProgress");
        
        if (buildId != null) {
            Map<String, Object> build = new HashMap<>();
            build.put("id", buildId);
            runData.put("build", build);
        }
        
        if (planId != null) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("id", planId);
            runData.put("plan", plan);
        }
        
        String endpoint = String.format("/test/runs?api-version=%s", API_VERSION);
        CSHttpResponse response = httpClient.post(endpoint)
                .body(CSJsonUtils.toJson(runData))
                .execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to create test run: " + response.getBody());
        }
        
        Map<String, Object> responseData = response.getBodyAsMap();
        return new CSTestRun(
            responseData.get("id").toString(),
            responseData.get("name").toString(),
            responseData.get("state").toString(),
            responseData.get("webAccessUrl").toString()
        );
    }
    
    /**
     * Add test results to a run
     */
    public void addTestResults(String runId, List<Map<String, Object>> testResults) {
        logger.info("Adding {} test results to run: {}", testResults.size(), runId);
        
        String endpoint = String.format("/test/runs/%s/results?api-version=%s", runId, API_VERSION);
        CSHttpResponse response = httpClient.post(endpoint)
                .body(CSJsonUtils.toJson(testResults))
                .execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to add test results: " + response.getBody());
        }
        
        logger.info("Successfully added test results to run: {}", runId);
    }
    
    /**
     * Update test run
     */
    public void updateTestRun(String runId, String state, String comment) {
        logger.info("Updating test run: {} to state: {}", runId, state);
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("state", state);
        
        if (comment != null) {
            updateData.put("comment", comment);
        }
        
        String endpoint = String.format("/test/runs/%s?api-version=%s", runId, API_VERSION);
        CSHttpResponse response = httpClient.patch(endpoint)
                .body(CSJsonUtils.toJson(updateData))
                .execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to update test run: " + response.getBody());
        }
        
        logger.info("Successfully updated test run: {}", runId);
    }
    
    /**
     * Complete test run
     */
    public void completeTestRun(String runId) {
        updateTestRun(runId, "Completed", "Test run completed by CS TestForge Framework");
    }
    
    /**
     * Get test run
     */
    public CSTestRun getTestRun(String runId) {
        logger.debug("Getting test run: {}", runId);
        
        String endpoint = String.format("/test/runs/%s?api-version=%s", runId, API_VERSION);
        CSHttpResponse response = httpClient.get(endpoint).execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to get test run: " + response.getBody());
        }
        
        Map<String, Object> responseData = response.getBodyAsMap();
        return new CSTestRun(
            responseData.get("id").toString(),
            responseData.get("name").toString(),
            responseData.get("state").toString(),
            responseData.get("webAccessUrl").toString()
        );
    }
    
    /**
     * Get test plans
     */
    public List<CSTestPlan> getTestPlans() {
        logger.debug("Getting test plans for project: {}", project);
        
        String endpoint = String.format("/test/plans?api-version=%s", API_VERSION);
        CSHttpResponse response = httpClient.get(endpoint).execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to get test plans: " + response.getBody());
        }
        
        Map<String, Object> responseData = response.getBodyAsMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> plans = (List<Map<String, Object>>) responseData.get("value");
        
        return plans.stream()
            .map(plan -> new CSTestPlan(
                plan.get("id").toString(),
                plan.get("name").toString(),
                plan.get("state").toString()
            ))
            .toList();
    }
    
    /**
     * Get builds
     */
    public List<CSBuild> getBuilds(int top) {
        logger.debug("Getting builds for project: {}", project);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("$top", String.valueOf(top));
        queryParams.put("api-version", API_VERSION);
        
        String endpoint = "/build/builds";
        CSHttpResponse response = httpClient.get(endpoint)
                .queryParams(queryParams)
                .execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to get builds: " + response.getBody());
        }
        
        Map<String, Object> responseData = response.getBodyAsMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> builds = (List<Map<String, Object>>) responseData.get("value");
        
        return builds.stream()
            .map(build -> new CSBuild(
                build.get("id").toString(),
                build.get("buildNumber").toString(),
                build.get("status").toString(),
                build.get("result") != null ? build.get("result").toString() : null
            ))
            .toList();
    }
    
    /**
     * Upload test attachment
     */
    public String uploadTestAttachment(String runId, String resultId, String fileName, byte[] content, String attachmentType) {
        logger.info("Uploading test attachment: {} to result: {}", fileName, resultId);
        
        // First create attachment
        Map<String, Object> attachmentData = new HashMap<>();
        attachmentData.put("fileName", fileName);
        attachmentData.put("comment", "Test attachment uploaded by CS TestForge Framework");
        attachmentData.put("attachmentType", attachmentType);
        
        String createEndpoint = String.format("/test/runs/%s/results/%s/attachments?api-version=%s", 
            runId, resultId, API_VERSION);
        
        CSHttpResponse createResponse = httpClient.post(createEndpoint)
                .body(CSJsonUtils.toJson(attachmentData))
                .execute();
        
        if (!createResponse.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to create attachment: " + createResponse.getBody());
        }
        
        Map<String, Object> attachmentInfo = createResponse.getBodyAsMap();
        String attachmentId = attachmentInfo.get("id").toString();
        
        // Upload attachment content (simplified - in real implementation would need multipart upload)
        logger.info("Successfully created test attachment: {}", attachmentId);
        
        return attachmentId;
    }
    
    /**
     * Create work item (bug)
     */
    public String createWorkItem(String title, String description, String workItemType, Map<String, Object> fields) {
        logger.info("Creating work item: {}", title);
        
        // Build JSON patch document for work item creation
        java.util.List<Map<String, Object>> patchDocument = new java.util.ArrayList<>();
        
        // Add title
        Map<String, Object> titleOp = new HashMap<>();
        titleOp.put("op", "add");
        titleOp.put("path", "/fields/System.Title");
        titleOp.put("value", title);
        patchDocument.add(titleOp);
        
        // Add description
        if (description != null) {
            Map<String, Object> descOp = new HashMap<>();
            descOp.put("op", "add");
            descOp.put("path", "/fields/System.Description");
            descOp.put("value", description);
            patchDocument.add(descOp);
        }
        
        // Add additional fields
        if (fields != null) {
            fields.forEach((fieldName, value) -> {
                Map<String, Object> fieldOp = new HashMap<>();
                fieldOp.put("op", "add");
                fieldOp.put("path", "/fields/" + fieldName);
                fieldOp.put("value", value);
                patchDocument.add(fieldOp);
            });
        }
        
        String endpoint = String.format("/wit/workitems/$%s?api-version=%s", workItemType, API_VERSION);
        
        // Use custom headers for JSON patch
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json-patch+json");
        
        CSHttpResponse response = httpClient.post(endpoint)
                .body(CSJsonUtils.toJson(patchDocument))
                .headers(headers)
                .execute();
        
        if (!response.isSuccessful()) {
            throw new CSAzureDevOpsException("Failed to create work item: " + response.getBody());
        }
        
        Map<String, Object> workItem = response.getBodyAsMap();
        String workItemId = workItem.get("id").toString();
        
        logger.info("Successfully created work item: {}", workItemId);
        return workItemId;
    }
    
    /**
     * Get organization and project info
     */
    public String getOrganization() {
        return organization;
    }
    
    public String getProject() {
        return project;
    }
    
    /**
     * Test connection
     */
    public boolean testConnection() {
        try {
            logger.info("Testing Azure DevOps connection...");
            
            String endpoint = String.format("/projects?api-version=%s", API_VERSION);
            CSHttpResponse response = httpClient.get(endpoint).execute();
            
            boolean connected = response.isSuccessful();
            logger.info("Azure DevOps connection test: {}", connected ? "SUCCESS" : "FAILED");
            
            return connected;
        } catch (Exception e) {
            logger.error("Azure DevOps connection test failed", e);
            return false;
        }
    }
}