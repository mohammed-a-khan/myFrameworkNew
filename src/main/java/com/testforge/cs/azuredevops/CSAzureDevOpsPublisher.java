package com.testforge.cs.azuredevops;

import com.testforge.cs.azuredevops.client.CSEnhancedADOClient;
import com.testforge.cs.azuredevops.config.CSADOConfiguration;
import com.testforge.cs.azuredevops.managers.CSTestRunManager;
import com.testforge.cs.azuredevops.managers.CSTestSuiteManager;
import com.testforge.cs.azuredevops.managers.CSEvidenceUploader;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.config.CSConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Enhanced Azure DevOps publisher using comprehensive ADO integration
 * Based on Playwright framework's implementation
 */
public class CSAzureDevOpsPublisher {
    private static final Logger logger = LoggerFactory.getLogger(CSAzureDevOpsPublisher.class);
    private static CSAzureDevOpsPublisher instance;
    
    // Enhanced ADO components
    private CSEnhancedADOClient client;
    private CSADOConfiguration config;
    private CSTestRunManager testRunManager;
    private CSTestSuiteManager testSuiteManager;
    private CSEvidenceUploader evidenceUploader;
    
    private boolean enabled;
    
    private CSAzureDevOpsPublisher() {
        initialize();
    }
    
    public static synchronized CSAzureDevOpsPublisher getInstance() {
        if (instance == null) {
            instance = new CSAzureDevOpsPublisher();
        }
        return instance;
    }
    
    /**
     * Initialize enhanced Azure DevOps integration
     */
    private void initialize() {
        try {
            logger.info("Initializing Azure DevOps integration...");
            
            // Initialize configuration
            config = CSADOConfiguration.getInstance();
            enabled = config.isEnabled();
            
            if (!enabled) {
                logger.info("Azure DevOps integration is disabled");
                return;
            }
            
            // Initialize enhanced components
            client = CSEnhancedADOClient.getInstance();
            testRunManager = CSTestRunManager.getInstance();
            testSuiteManager = CSTestSuiteManager.getInstance();
            evidenceUploader = CSEvidenceUploader.getInstance();
            
            // Test connection
            if (client.testConnection()) {
                logger.info("Azure DevOps integration initialized successfully");
                logger.info("Organization: {}", config.getOrganizationUrl());
                logger.info("Project: {}", config.getProjectName());
                
                // Initialize test points if configured
                testSuiteManager.initializeTestPoints();
                
                // Log test plan and suite configuration
                if (config.getTestPlanId() != null) {
                    logger.info("Test Plan ID: {}", config.getTestPlanId());
                }
                if (config.getTestSuiteId() != null) {
                    logger.info("Test Suite ID: {}", config.getTestSuiteId());
                }
            } else {
                logger.error("Azure DevOps connection test failed - disabling integration");
                enabled = false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize Azure DevOps integration", e);
            enabled = false;
        }
    }
    
    /**
     * Start test run
     */
    public String startTestRun(String runName, String buildId, String planId) {
        if (!enabled) {
            logger.debug("Azure DevOps integration disabled - skipping test run creation");
            return null;
        }
        
        try {
            CSTestRunManager.TestRunCreateRequest request = new CSTestRunManager.TestRunCreateRequest();
            request.name = runName;
            request.buildId = buildId;
            
            // Parse planId if it's numeric
            if (planId != null && planId.matches("\\d+")) {
                request.planId = Integer.parseInt(planId);
            } else if (config.getTestPlanId() != null) {
                request.planId = Integer.parseInt(config.getTestPlanId());
            }
            
            // Use configured suite ID if available
            if (config.getTestSuiteId() != null) {
                request.suiteId = Integer.parseInt(config.getTestSuiteId());
            }
            
            CSTestRunManager.TestRun testRun = testRunManager.createTestRun(request);
            
            logger.info("Started Azure DevOps test run: {} (ID: {})", 
                testRun.name, testRun.id);
            if (testRun.webAccessUrl != null && !testRun.webAccessUrl.isEmpty()) {
                logger.info("Test run URL: {}", testRun.webAccessUrl);
            }
            
            return testRun.id;
            
        } catch (Exception e) {
            logger.error("Failed to start Azure DevOps test run", e);
            throw new CSAzureDevOpsException("Failed to start test run", e);
        }
    }
    
    /**
     * Start test run with default settings
     */
    public String startTestRun() {
        return startTestRun(null, null, null);
    }
    
    /**
     * Publish single test result
     */
    public void publishTestResult(com.testforge.cs.reporting.CSTestResult testResult) {
        if (!enabled) {
            logger.debug("Azure DevOps integration disabled - skipping result publication");
            return;
        }
        
        if (!testRunManager.hasActiveTestRun()) {
            logger.debug("No active test run - creating new test run");
            startTestRun();
        }
        
        try {
            testRunManager.addTestResult(testResult);
            logger.debug("Published test result: {}", testResult.getTestName());
        } catch (Exception e) {
            logger.error("Failed to publish test result: {}", testResult.getTestName(), e);
        }
    }
    
    /**
     * Publish test results from report manager
     */
    public void publishTestResults() {
        if (!enabled) {
            logger.debug("Azure DevOps integration disabled - skipping result publication");
            return;
        }
        
        if (!testRunManager.hasActiveTestRun()) {
            logger.debug("No active test run - creating new test run");
            startTestRun();
        }
        
        try {
            // Get test results from report manager
            CSReportManager reportManager = CSReportManager.getInstance();
            java.util.Collection<com.testforge.cs.reporting.CSTestResult> frameworkResultsCollection = 
                reportManager.getAllTestResults();
            List<com.testforge.cs.reporting.CSTestResult> frameworkResults = 
                new java.util.ArrayList<>(frameworkResultsCollection);
            
            if (frameworkResults.isEmpty()) {
                logger.info("No test results to publish to Azure DevOps");
                return;
            }
            
            // Use enhanced test run manager for batch upload with evidence
            testRunManager.addTestResults(frameworkResults);
            
            logger.info("Published {} test results to Azure DevOps", frameworkResults.size());
            
        } catch (Exception e) {
            logger.error("Failed to publish test results to Azure DevOps", e);
            throw new CSAzureDevOpsException("Failed to publish test results", e);
        }
    }
    
    /**
     * Complete test run
     */
    public void completeTestRun() {
        if (!enabled) {
            logger.debug("Azure DevOps integration disabled - skipping test run completion");
            return;
        }
        
        if (!testRunManager.hasActiveTestRun()) {
            logger.debug("No active test run - skipping test run completion");
            return;
        }
        
        try {
            testRunManager.completeTestRun();
            logger.info("Completed Azure DevOps test run");
        } catch (Exception e) {
            logger.error("Failed to complete Azure DevOps test run", e);
            throw new CSAzureDevOpsException("Failed to complete test run", e);
        }
    }
    
    /**
     * Create bug from failed test
     */
    public String createBugFromFailure(com.testforge.cs.reporting.CSTestResult failedTest) {
        if (!enabled) {
            logger.debug("Azure DevOps integration disabled - skipping bug creation");
            return null;
        }
        
        if (!config.isCreateBugsOnFailure()) {
            logger.debug("Bug creation disabled - skipping");
            return null;
        }
        
        try {
            String title = config.formatBugTitle(failedTest.getTestName(), failedTest.getErrorMessage());
            
            // Build bug description
            StringBuilder description = new StringBuilder();
            description.append("<div><strong>Test Details:</strong></div>");
            description.append("<ul>");
            description.append("<li><strong>Test Class:</strong> ").append(failedTest.getClassName()).append("</li>");
            description.append("<li><strong>Test Method:</strong> ").append(failedTest.getMethodName()).append("</li>");
            description.append("<li><strong>Execution Time:</strong> ").append(failedTest.getStartTime()).append("</li>");
            description.append("<li><strong>Duration:</strong> ").append(failedTest.getDuration()).append(" ms</li>");
            description.append("</ul>");
            
            if (failedTest.getErrorMessage() != null) {
                description.append("<div><strong>Error Message:</strong></div>");
                description.append("<pre>").append(failedTest.getErrorMessage()).append("</pre>");
            }
            
            if (failedTest.getStackTrace() != null) {
                description.append("<div><strong>Stack Trace:</strong></div>");
                description.append("<pre>").append(failedTest.getStackTrace()).append("</pre>");
            }
            
            // Create patch document for bug creation
            List<Map<String, Object>> patchDocument = new java.util.ArrayList<>();
            
            addPatchOperation(patchDocument, "add", "/fields/System.Title", title);
            addPatchOperation(patchDocument, "add", "/fields/System.Description", description.toString());
            addPatchOperation(patchDocument, "add", "/fields/Microsoft.VSTS.TCM.ReproSteps", description.toString());
            
            // Add configured bug template fields
            if (config.getBugTemplate() != null) {
                CSADOConfiguration.BugTemplate template = config.getBugTemplate();
                
                if (template.getAssignedTo() != null && !template.getAssignedTo().isEmpty()) {
                    addPatchOperation(patchDocument, "add", "/fields/System.AssignedTo", template.getAssignedTo());
                }
                
                if (template.getAreaPath() != null && !template.getAreaPath().isEmpty()) {
                    addPatchOperation(patchDocument, "add", "/fields/System.AreaPath", template.getAreaPath());
                }
                
                if (template.getIterationPath() != null && !template.getIterationPath().isEmpty()) {
                    addPatchOperation(patchDocument, "add", "/fields/System.IterationPath", template.getIterationPath());
                }
                
                addPatchOperation(patchDocument, "add", "/fields/Microsoft.VSTS.Common.Priority", template.getPriority());
                addPatchOperation(patchDocument, "add", "/fields/Microsoft.VSTS.Common.Severity", template.getSeverity());
                
                if (template.getTags() != null && template.getTags().length > 0) {
                    addPatchOperation(patchDocument, "add", "/fields/System.Tags", String.join("; ", template.getTags()));
                }
            }
            
            // Create work item
            String url = config.buildUrl(config.getEndpoints().getWorkItems(), null) + "/$Bug";
            
            CSEnhancedADOClient.ADORequestOptions options = 
                new CSEnhancedADOClient.ADORequestOptions("POST", url);
            options.headers = new HashMap<>();
            options.headers.put("Content-Type", "application/json-patch+json");
            options.body = patchDocument;
            
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.request(options, Map.class);
            
            String bugId = response.data.get("id").toString();
            logger.info("Created bug {} for failed test: {}", bugId, failedTest.getTestName());
            
            return bugId;
            
        } catch (Exception e) {
            logger.error("Failed to create bug for failed test: {}", failedTest.getTestName(), e);
            return null;
        }
    }
    
    private void addPatchOperation(List<Map<String, Object>> patchDocument, 
                                  String op, String path, Object value) {
        Map<String, Object> operation = new HashMap<>();
        operation.put("op", op);
        operation.put("path", path);
        operation.put("value", value);
        patchDocument.add(operation);
    }
    
    /**
     * Get test plans
     */
    public List<CSTestPlan> getTestPlans() {
        if (!enabled) {
            return java.util.Collections.emptyList();
        }
        
        try {
            String url = config.buildUrl(config.getEndpoints().getTestPlans(), null);
            CSEnhancedADOClient.ADOListResponse<CSTestPlan> response = 
                client.getList(url, CSTestPlan.class);
            return response.value;
        } catch (Exception e) {
            logger.error("Failed to get test plans", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Get builds
     */
    public List<CSBuild> getBuilds(int count) {
        if (!enabled) {
            return java.util.Collections.emptyList();
        }
        
        try {
            String url = config.buildUrl(config.getEndpoints().getBuilds(), null);
            url = url + "&$top=" + count;
            CSEnhancedADOClient.ADOListResponse<CSBuild> response = 
                client.getList(url, CSBuild.class);
            return response.value;
        } catch (Exception e) {
            logger.error("Failed to get builds", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Check if Azure DevOps integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get current test run ID
     */
    public String getCurrentTestRunId() {
        if (testRunManager != null && testRunManager.getCurrentTestRun() != null) {
            return testRunManager.getCurrentTestRun().id;
        }
        return null;
    }
    
    /**
     * Get enhanced ADO client
     */
    public CSEnhancedADOClient getClient() {
        return client;
    }
    
    /**
     * Get test run manager
     */
    public CSTestRunManager getTestRunManager() {
        return testRunManager;
    }
    
    /**
     * Get test suite manager
     */
    public CSTestSuiteManager getTestSuiteManager() {
        return testSuiteManager;
    }
    
    /**
     * Full workflow: start run, publish results, complete run
     */
    public void executeFullWorkflow(String runName, String buildId, String planId) {
        if (!enabled) {
            logger.info("Azure DevOps integration disabled - skipping workflow");
            return;
        }
        
        try {
            logger.info("Starting Azure DevOps full workflow...");
            
            // Start test run
            String runId = startTestRun(runName, buildId, planId);
            
            if (runId != null) {
                // Publish results
                publishTestResults();
                
                // Complete run
                completeTestRun();
                
                logger.info("Azure DevOps workflow completed successfully");
            }
            
        } catch (Exception e) {
            logger.error("Azure DevOps workflow failed", e);
            
            // Try to abort the run on error
            if (testRunManager != null && testRunManager.hasActiveTestRun()) {
                try {
                    testRunManager.abortTestRun("Workflow failed: " + e.getMessage());
                } catch (Exception abortEx) {
                    logger.error("Failed to abort test run after workflow error", abortEx);
                }
            }
            
            throw new CSAzureDevOpsException("Azure DevOps workflow failed", e);
        }
    }
    
    /**
     * Execute workflow with default settings
     */
    public void executeFullWorkflow() {
        executeFullWorkflow(null, null, null);
    }
}