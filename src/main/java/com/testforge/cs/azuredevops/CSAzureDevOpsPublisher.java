package com.testforge.cs.azuredevops;

import com.testforge.cs.exceptions.CSAzureDevOpsException;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.config.CSConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Publisher for sending test results to Azure DevOps
 * Integrates with CS framework reporting system
 */
public class CSAzureDevOpsPublisher {
    private static final Logger logger = LoggerFactory.getLogger(CSAzureDevOpsPublisher.class);
    private static CSAzureDevOpsPublisher instance;
    
    private CSAzureDevOpsClient client;
    private boolean enabled;
    private String currentTestRunId;
    
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
     * Initialize Azure DevOps integration
     */
    private void initialize() {
        try {
            CSConfigManager config = CSConfigManager.getInstance();
            
            String organization = config.getProperty("azure.devops.organization");
            String project = config.getProperty("azure.devops.project");
            String token = config.getProperty("azure.devops.token");
            
            enabled = Boolean.parseBoolean(config.getProperty("azure.devops.enabled", "false"));
            
            if (enabled && organization != null && project != null && token != null) {
                this.client = new CSAzureDevOpsClient(organization, project, token);
                
                // Test connection
                if (client.testConnection()) {
                    logger.info("Azure DevOps integration initialized successfully");
                } else {
                    logger.warn("Azure DevOps connection test failed - disabling integration");
                    enabled = false;
                }
            } else {
                logger.info("Azure DevOps integration disabled or not configured");
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
            if (runName == null) {
                runName = "CS TestForge Framework Run - " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            CSTestRun testRun = client.createTestRun(runName, buildId, planId);
            currentTestRunId = testRun.getId();
            
            logger.info("Started Azure DevOps test run: {} (ID: {})", testRun.getName(), currentTestRunId);
            logger.info("Test run URL: {}", testRun.getWebAccessUrl());
            
            return currentTestRunId;
            
        } catch (Exception e) {
            logger.error("Failed to start Azure DevOps test run", e);
            throw new CSAzureDevOpsException("Failed to start test run", e);
        }
    }
    
    /**
     * Publish test results
     */
    public void publishTestResults() {
        if (!enabled || currentTestRunId == null) {
            logger.debug("Azure DevOps integration disabled or no active test run - skipping result publication");
            return;
        }
        
        try {
            // Get test results from report manager
            CSReportManager reportManager = CSReportManager.getInstance();
            java.util.Collection<com.testforge.cs.reporting.CSTestResult> frameworkResultsCollection = reportManager.getAllTestResults();
            List<com.testforge.cs.reporting.CSTestResult> frameworkResults = new java.util.ArrayList<>(frameworkResultsCollection);
            
            if (frameworkResults.isEmpty()) {
                logger.info("No test results to publish to Azure DevOps");
                return;
            }
            
            // Convert to Azure DevOps format
            List<CSTestResult> azureResults = frameworkResults.stream()
                .map(CSTestResult::fromFrameworkResult)
                .collect(Collectors.toList());
            
            // Convert to API format
            List<java.util.Map<String, Object>> apiResults = azureResults.stream()
                .map(CSTestResult::toApiFormat)
                .collect(Collectors.toList());
            
            // Send to Azure DevOps
            client.addTestResults(currentTestRunId, apiResults);
            
            logger.info("Published {} test results to Azure DevOps run: {}", 
                azureResults.size(), currentTestRunId);
            
        } catch (Exception e) {
            logger.error("Failed to publish test results to Azure DevOps", e);
            throw new CSAzureDevOpsException("Failed to publish test results", e);
        }
    }
    
    /**
     * Complete test run
     */
    public void completeTestRun() {
        if (!enabled || currentTestRunId == null) {
            logger.debug("Azure DevOps integration disabled or no active test run - skipping test run completion");
            return;
        }
        
        try {
            client.completeTestRun(currentTestRunId);
            logger.info("Completed Azure DevOps test run: {}", currentTestRunId);
            
            // Get final test run info
            CSTestRun completedRun = client.getTestRun(currentTestRunId);
            logger.info("Final test run URL: {}", completedRun.getWebAccessUrl());
            
            currentTestRunId = null;
            
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
        
        try {
            String title = String.format("Test Failure: %s", failedTest.getTestName());
            
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
            
            java.util.Map<String, Object> fields = new java.util.HashMap<>();
            fields.put("System.Priority", 2); // High priority
            fields.put("System.Severity", "2 - High");
            fields.put("Microsoft.VSTS.TCM.ReproSteps", description.toString());
            fields.put("System.Tags", "automated-test;cs-testforge-framework");
            
            String bugId = client.createWorkItem(title, description.toString(), "Bug", fields);
            
            logger.info("Created bug {} for failed test: {}", bugId, failedTest.getTestName());
            
            return bugId;
            
        } catch (Exception e) {
            logger.error("Failed to create bug for failed test: {}", failedTest.getTestName(), e);
            throw new CSAzureDevOpsException("Failed to create bug", e);
        }
    }
    
    /**
     * Get available test plans
     */
    public List<CSTestPlan> getTestPlans() {
        if (!enabled) {
            return java.util.Collections.emptyList();
        }
        
        try {
            return client.getTestPlans();
        } catch (Exception e) {
            logger.error("Failed to get test plans", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Get available builds
     */
    public List<CSBuild> getBuilds(int count) {
        if (!enabled) {
            return java.util.Collections.emptyList();
        }
        
        try {
            return client.getBuilds(count);
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
        return currentTestRunId;
    }
    
    /**
     * Get Azure DevOps client
     */
    public CSAzureDevOpsClient getClient() {
        return client;
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
            
            // Try to complete the run even if publishing failed
            if (currentTestRunId != null) {
                try {
                    completeTestRun();
                } catch (Exception completeEx) {
                    logger.error("Failed to complete test run after workflow error", completeEx);
                }
            }
            
            throw new CSAzureDevOpsException("Azure DevOps workflow failed", e);
        }
    }
}