package com.testforge.cs.azuredevops.managers;

import com.testforge.cs.azuredevops.client.CSEnhancedADOClient;
import com.testforge.cs.azuredevops.config.CSADOConfiguration;
import com.testforge.cs.azuredevops.models.CSTestPoint;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import com.testforge.cs.reporting.CSTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages test runs in Azure DevOps
 * Based on Playwright framework's TestRunManager
 */
public class CSTestRunManager {
    private static final Logger logger = LoggerFactory.getLogger(CSTestRunManager.class);
    private static CSTestRunManager instance;
    
    private final CSEnhancedADOClient client;
    private final CSADOConfiguration config;
    private final CSTestSuiteManager suiteManager;
    private final CSEvidenceUploader evidenceUploader;
    
    // Current test run tracking
    private TestRun currentTestRun;
    private final Map<String, Integer> testResultMapping = new ConcurrentHashMap<>();
    
    private CSTestRunManager() {
        this.client = CSEnhancedADOClient.getInstance();
        this.config = CSADOConfiguration.getInstance();
        this.suiteManager = CSTestSuiteManager.getInstance();
        this.evidenceUploader = CSEvidenceUploader.getInstance();
    }
    
    /**
     * Get suite manager instance
     */
    public CSTestSuiteManager getSuiteManager() {
        return suiteManager;
    }
    
    public static synchronized CSTestRunManager getInstance() {
        if (instance == null) {
            instance = new CSTestRunManager();
        }
        return instance;
    }
    
    /**
     * Create test run with specific test points
     */
    public String createTestRunWithPoints(String name, String planId, String suiteId, List<Integer> testPointIds) {
        try {
            Map<String, Object> runData = new HashMap<>();
            runData.put("name", name);
            runData.put("plan", Map.of("id", planId));
            runData.put("state", "InProgress");
            runData.put("automated", true);
            
            // Add test points to the run
            if (testPointIds != null && !testPointIds.isEmpty()) {
                runData.put("pointIds", testPointIds);
                logger.info("Creating test run with {} test points: {}", testPointIds.size(), testPointIds);
            }
            
            // Add custom fields
            if (config.getCustomFields() != null) {
                runData.putAll(config.getCustomFields());
            }
            
            String url = config.buildUrl(config.getEndpoints().getTestRuns(), null);
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.post(url, runData, Map.class);
            
            TestRun testRun = new TestRun();
            testRun.id = response.data.get("id").toString();
            testRun.name = response.data.get("name").toString();
            testRun.state = response.data.get("state").toString();
            testRun.url = response.data.get("url").toString();
            testRun.webAccessUrl = response.data.get("webAccessUrl") != null ? 
                response.data.get("webAccessUrl").toString() : "";
            
            currentTestRun = testRun;
            
            logger.info("Created test run with points: {} (ID: {})", testRun.name, testRun.id);
            logger.info("Test run URL: {}", testRun.webAccessUrl);
            
            return testRun.id;
            
        } catch (Exception e) {
            logger.error("Failed to create test run with points", e);
            throw new CSAzureDevOpsException("Failed to create test run", e);
        }
    }
    
    /**
     * Create a new test run
     */
    public TestRun createTestRun(TestRunCreateRequest request) {
        try {
            logger.info("Creating test run: {}", request.name);
            
            Map<String, Object> runData = new HashMap<>();
            runData.put("name", request.name != null ? request.name : generateRunName());
            runData.put("automated", config.isAutomated());
            runData.put("isAutomated", config.isAutomated());
            runData.put("state", "InProgress");
            runData.put("startedDate", LocalDateTime.now().toString());
            
            // DON'T add plan reference during creation to avoid auto-creating test results
            // We'll add plan reference when posting individual test results
            // This prevents ADO from creating placeholder results for ALL test points in the suite
            
            // Add build reference
            if (request.buildId != null || config.getBuildId() != null) {
                Map<String, Object> build = new HashMap<>();
                build.put("id", request.buildId != null ? request.buildId : config.getBuildId());
                runData.put("build", build);
            }
            
            // Add release reference
            if (request.releaseId != null || config.getReleaseId() != null) {
                Map<String, Object> release = new HashMap<>();
                release.put("id", request.releaseId != null ? request.releaseId : config.getReleaseId());
                runData.put("release", release);
            }
            
            // Don't add test points during creation - let them be added when results are posted
            // This avoids creating empty test results for all test points in the suite
            // Only the tests that are actually executed will have results posted
            
            // Add custom fields
            if (config.getCustomFields() != null) {
                runData.putAll(config.getCustomFields());
            }
            
            String url = config.buildUrl(config.getEndpoints().getTestRuns(), null);
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.post(url, runData, Map.class);
            
            TestRun testRun = new TestRun();
            testRun.id = response.data.get("id").toString();
            testRun.name = response.data.get("name").toString();
            testRun.state = response.data.get("state").toString();
            testRun.url = response.data.get("url").toString();
            testRun.webAccessUrl = response.data.get("webAccessUrl") != null ? 
                response.data.get("webAccessUrl").toString() : "";
            
            currentTestRun = testRun;
            
            logger.info("Created test run: {} (ID: {})", testRun.name, testRun.id);
            logger.info("Test run URL: {}", testRun.webAccessUrl);
            
            return testRun;
            
        } catch (Exception e) {
            logger.error("Failed to create test run", e);
            throw new CSAzureDevOpsException("Failed to create test run", e);
        }
    }
    
    /**
     * Add test result to current run
     */
    public void addTestResult(CSTestResult testResult) {
        if (currentTestRun == null) {
            logger.warn("No active test run to add result to");
            return;
        }
        
        try {
            // Map test to test point if configured
            Integer testPointId = null;
            Integer testCaseId = null;
            Integer testPlanId = null;
            Integer testSuiteId = null;
            
            // First check if test result has ADO metadata
            if (testResult.getMetadata() != null) {
                // Extract test case ID
                if (testResult.getMetadata().containsKey("ado.testcase.id")) {
                    try {
                        testCaseId = Integer.parseInt(testResult.getMetadata().get("ado.testcase.id").toString());
                        logger.info("Found ADO test case ID in metadata: {}", testCaseId);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid ADO test case ID in metadata: {}", 
                            testResult.getMetadata().get("ado.testcase.id"));
                    }
                }
                
                // Extract test plan ID
                if (testResult.getMetadata().containsKey("ado.testplan.id")) {
                    try {
                        testPlanId = Integer.parseInt(testResult.getMetadata().get("ado.testplan.id").toString());
                        logger.debug("Found ADO test plan ID in metadata: {}", testPlanId);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid ADO test plan ID in metadata: {}", 
                            testResult.getMetadata().get("ado.testplan.id"));
                    }
                }
                
                // Extract test suite ID
                if (testResult.getMetadata().containsKey("ado.testsuite.id")) {
                    try {
                        testSuiteId = Integer.parseInt(testResult.getMetadata().get("ado.testsuite.id").toString());
                        logger.debug("Found ADO test suite ID in metadata: {}", testSuiteId);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid ADO test suite ID in metadata: {}", 
                            testResult.getMetadata().get("ado.testsuite.id"));
                    }
                }
            }
            
            // Use metadata values or fall back to config values
            String planIdStr = testPlanId != null ? testPlanId.toString() : config.getTestPlanId();
            String suiteIdStr = testSuiteId != null ? testSuiteId.toString() : config.getTestSuiteId();
            
            // Try to map to test point if we have plan and suite IDs
            if (planIdStr != null && suiteIdStr != null) {
                testPointId = suiteManager.mapTestToPoint(
                    testResult.getClassName(),
                    testResult.getMethodName(),
                    Integer.parseInt(planIdStr),
                    Integer.parseInt(suiteIdStr)
                );
                
                // If we have a test case ID but no test point, try to find it
                if (testPointId == null && testCaseId != null) {
                    testPointId = suiteManager.findTestPointByTestCase(
                        testCaseId,
                        planIdStr,
                        suiteIdStr
                    );
                }
            }
            
            // IMPORTANT: When a test run is created with specific test points,
            // Azure DevOps automatically creates placeholder test results for those points.
            // We need to UPDATE the existing result, not create a new one.
            
            // First, get existing test results from the run to find the one for this test point
            Integer existingResultId = null;
            if (testPointId != null) {
                existingResultId = findExistingTestResultForTestPoint(currentTestRun.id, testPointId);
            }
            
            // Prepare test result data
            Map<String, Object> resultData = new HashMap<>();
            
            resultData.put("outcome", mapTestStatus(testResult.getStatus().toString()));
            resultData.put("state", "Completed");
            resultData.put("startedDate", testResult.getStartTime());
            resultData.put("completedDate", testResult.getEndTime());
            resultData.put("durationInMs", testResult.getDuration());
            resultData.put("testCaseTitle", testResult.getTestName());
            resultData.put("automatedTestName", testResult.getClassName() + "." + testResult.getMethodName());
            resultData.put("automatedTestStorage", testResult.getClassName());
            
            if (testResult.getErrorMessage() != null) {
                resultData.put("errorMessage", testResult.getErrorMessage());
            }
            
            if (testResult.getStackTrace() != null) {
                resultData.put("stackTrace", testResult.getStackTrace());
            }
            
            Integer resultId = null;
            String url;
            
            // If we found an existing result, UPDATE it. Otherwise, create a new one.
            if (existingResultId != null) {
                // UPDATE existing test result
                // IMPORTANT: Azure DevOps expects /results endpoint for updates, not /results/{id}
                url = config.buildUrl(
                    config.getEndpoints().getTestResults(),
                    Map.of("runId", currentTestRun.id)
                );
                
                logger.info("Updating existing test result {} for test point {}", existingResultId, testPointId);
                
                // Add the result ID to the update payload
                resultData.put("id", existingResultId);
                
                // IMPORTANT: PATCH expects an ARRAY of test results, even for a single result
                List<Map<String, Object>> resultsArray = Arrays.asList(resultData);
                
                @SuppressWarnings("unchecked")
                CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                    (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                    client.patch(url, resultsArray, Map.class);
                
                // The PATCH response also returns an object with "value" array
                if (response.data != null && response.data.containsKey("value")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> updatedResults = (List<Map<String, Object>>) response.data.get("value");
                    
                    if (updatedResults != null && !updatedResults.isEmpty()) {
                        Map<String, Object> updatedResult = updatedResults.get(0);
                        resultId = Integer.parseInt(updatedResult.get("id").toString());
                        logger.debug("Successfully updated test result ID: {}", resultId);
                    }
                } else {
                    // Fallback if response format is different
                    resultId = existingResultId;
                }
                
            } else {
                // CREATE new test result (fallback for non-test-point scenarios)
                url = config.buildUrl(
                    config.getEndpoints().getTestResults(),
                    Map.of("runId", currentTestRun.id)
                );
                
                // For new results, we need the test point ID
                if (testPointId != null) {
                    resultData.put("testPointId", testPointId);
                }
                if (testCaseId != null) {
                    resultData.put("testCaseId", testCaseId);
                }
                
                List<Map<String, Object>> results = Arrays.asList(resultData);
                
                // Post test results - ADO returns an object with "value" array
                @SuppressWarnings("unchecked")
                CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                    (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                    client.post(url, results, Map.class);
            
                // Get the result ID from response
                if (response.data != null && response.data.containsKey("value")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> createdResults = (List<Map<String, Object>>) response.data.get("value");
                    
                    if (createdResults != null && !createdResults.isEmpty()) {
                        Map<String, Object> createdResult = createdResults.get(0);
                        resultId = (Integer) createdResult.get("id");
                    }
                }
            }
            
            if (resultId != null) {
                // Store mapping
                String testKey = testResult.getClassName() + "." + testResult.getMethodName();
                testResultMapping.put(testKey, resultId);
                
                // Upload evidence if configured
                if (config.isUploadAttachments()) {
                    uploadTestEvidence(currentTestRun.id, resultId.toString(), testResult);
                }
                
                logger.info("Updated test result for: {} (ID: {})", testResult.getTestName(), resultId);
                
                // COMMENTED OUT: Update test point outcome in the test plan
                // REASON: ADO automatically updates test point outcomes when test results are added to test runs
                // This manual update was causing duplicate "Manual" test runs to be created
                // Test points are automatically updated based on test results - no manual intervention needed
                /*
                if (testCaseId != null && planIdStr != null && suiteIdStr != null && testPointId != null) {
                    String outcome = mapTestStatus(testResult.getStatus().toString());
                    logger.debug("Updating test point outcome for test case {} to {}", testCaseId, outcome);
                    suiteManager.updateTestPointOutcome(
                        planIdStr, 
                        suiteIdStr, 
                        testCaseId, 
                        outcome, 
                        currentTestRun.id, 
                        resultId.toString()
                    );
                }
                */
            }
            
        } catch (Exception e) {
            logger.error("Failed to add test result: {}", testResult.getTestName(), e);
            // Don't throw - continue with other results
        }
    }
    
    /**
     * Add batch of test results
     */
    public void addTestResults(List<CSTestResult> testResults) {
        if (currentTestRun == null) {
            logger.warn("No active test run to add results to");
            return;
        }
        
        if (testResults.isEmpty()) {
            return;
        }
        
        try {
            List<Map<String, Object>> resultsData = new ArrayList<>();
            
            for (CSTestResult testResult : testResults) {
                Integer testCaseId = null;
                Integer testPlanId = null;
                Integer testSuiteId = null;
                Integer testPointId = null;
                
                // Extract test case ID from metadata if available
                if (testResult.getMetadata() != null) {
                    if (testResult.getMetadata().containsKey("ado.testcase.id")) {
                        try {
                            testCaseId = Integer.parseInt(testResult.getMetadata().get("ado.testcase.id").toString());
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid test case ID in metadata");
                        }
                    }
                    if (testResult.getMetadata().containsKey("ado.testplan.id")) {
                        try {
                            testPlanId = Integer.parseInt(testResult.getMetadata().get("ado.testplan.id").toString());
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid test plan ID in metadata");
                        }
                    }
                    if (testResult.getMetadata().containsKey("ado.testsuite.id")) {
                        try {
                            testSuiteId = Integer.parseInt(testResult.getMetadata().get("ado.testsuite.id").toString());
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid test suite ID in metadata");
                        }
                    }
                }
                
                // Use metadata values or fall back to config values
                String planIdStr = testPlanId != null ? testPlanId.toString() : config.getTestPlanId();
                String suiteIdStr = testSuiteId != null ? testSuiteId.toString() : config.getTestSuiteId();
                
                // Find test point for this test case
                if (testCaseId != null && planIdStr != null && suiteIdStr != null) {
                    testPointId = suiteManager.findTestPointByTestCase(
                        testCaseId,
                        planIdStr,
                        suiteIdStr
                    );
                }
                
                Map<String, Object> resultData = new HashMap<>();
                
                // When a test run is created with specific test points, Azure DevOps requires specific fields
                if (testPointId != null) {
                    // Use testPointId (capital P) as required by ADO API
                    resultData.put("testPointId", testPointId);
                }
                
                // Add test case ID directly for planned test results
                if (testCaseId != null) {
                    resultData.put("testCaseId", testCaseId);
                }
                
                resultData.put("testCaseTitle", testResult.getTestName());
                resultData.put("automatedTestName", testResult.getClassName() + "." + testResult.getMethodName());
                resultData.put("automatedTestStorage", testResult.getClassName());
                resultData.put("outcome", mapTestStatus(testResult.getStatus().toString()));
                resultData.put("state", "Completed");
                resultData.put("startedDate", testResult.getStartTime());
                resultData.put("completedDate", testResult.getEndTime());
                resultData.put("durationInMs", testResult.getDuration());
                
                if (testResult.getErrorMessage() != null) {
                    resultData.put("errorMessage", testResult.getErrorMessage());
                }
                
                if (testResult.getStackTrace() != null) {
                    resultData.put("stackTrace", testResult.getStackTrace());
                }
                
                resultsData.add(resultData);
            }
            
            // Add all results in batch
            String url = config.buildUrl(
                config.getEndpoints().getTestResults(),
                Map.of("runId", currentTestRun.id)
            );
            
            // Post test results - ADO returns an object with "value" array
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.post(url, resultsData, Map.class);
            
            // Process response and upload evidence
            if (response.data != null && response.data.containsKey("value")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> createdResults = (List<Map<String, Object>>) response.data.get("value");
                
                if (createdResults != null) {
                    for (int i = 0; i < createdResults.size() && i < testResults.size(); i++) {
                        Map<String, Object> createdResult = createdResults.get(i);
                        Integer resultId = (Integer) createdResult.get("id");
                        CSTestResult testResult = testResults.get(i);
                        
                        // Store mapping
                        String testKey = testResult.getClassName() + "." + testResult.getMethodName();
                        testResultMapping.put(testKey, resultId);
                        
                        // Upload evidence asynchronously
                        if (config.isUploadAttachments()) {
                            uploadTestEvidence(currentTestRun.id, resultId.toString(), testResult);
                        }
                        
                        // Update test point outcome in test plan
                        Integer testCaseId = null;
                        Integer testPlanId = null;
                        Integer testSuiteId = null;
                        
                        // Extract from metadata
                        if (testResult.getMetadata() != null) {
                            if (testResult.getMetadata().containsKey("ado.testcase.id")) {
                                try {
                                    testCaseId = Integer.parseInt(testResult.getMetadata().get("ado.testcase.id").toString());
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                            if (testResult.getMetadata().containsKey("ado.testplan.id")) {
                                try {
                                    testPlanId = Integer.parseInt(testResult.getMetadata().get("ado.testplan.id").toString());
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                            if (testResult.getMetadata().containsKey("ado.testsuite.id")) {
                                try {
                                    testSuiteId = Integer.parseInt(testResult.getMetadata().get("ado.testsuite.id").toString());
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                        }
                        
                        // Use metadata values or fall back to config values
                        String planIdStr = testPlanId != null ? testPlanId.toString() : config.getTestPlanId();
                        String suiteIdStr = testSuiteId != null ? testSuiteId.toString() : config.getTestSuiteId();
                        
                        // COMMENTED OUT: Update test point outcome (BATCH METHOD)
                        // REASON: ADO automatically updates test point outcomes when test results are added to test runs
                        // This manual update was causing duplicate "Manual" test runs to be created
                        // Test points are automatically updated based on test results - no manual intervention needed
                        /*
                        if (testCaseId != null && planIdStr != null && suiteIdStr != null) {
                            String outcome = mapTestStatus(testResult.getStatus().toString());
                            suiteManager.updateTestPointOutcome(
                                planIdStr, 
                                suiteIdStr, 
                                testCaseId, 
                                outcome, 
                                currentTestRun.id, 
                                resultId.toString()
                            );
                            logger.debug("Updated test point outcome for test case {} to {}", testCaseId, outcome);
                        }
                        */
                    }
                }
            }
            
            logger.info("Added {} test results to run: {}", testResults.size(), currentTestRun.id);
            
        } catch (Exception e) {
            logger.error("Failed to add batch test results", e);
        }
    }
    
    /**
     * Complete test run
     */
    public void completeTestRun() {
        if (currentTestRun == null) {
            logger.warn("No active test run to complete");
            return;
        }
        
        try {
            // Upload complete test report folder as zip before completing
            if (config.isUploadAttachments()) {
                logger.info("Uploading complete test report folder to test run...");
                CompletableFuture<String> reportUpload = 
                    evidenceUploader.uploadTestReportFolder(currentTestRun.id);
                
                // Wait for report upload to complete (with timeout)
                try {
                    String attachmentId = reportUpload.get(60, java.util.concurrent.TimeUnit.SECONDS);
                    if (attachmentId != null) {
                        logger.info("Successfully uploaded test report folder as attachment: {}", attachmentId);
                    }
                } catch (java.util.concurrent.TimeoutException te) {
                    logger.warn("Test report folder upload timed out - continuing with test run completion");
                }
            }
            
            // Wait for other evidence uploads to complete
            evidenceUploader.waitForUploads(30, java.util.concurrent.TimeUnit.SECONDS);
            
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("state", "Completed");
            updateData.put("completedDate", LocalDateTime.now().toString());
            updateData.put("comment", "Test run completed by CS TestForge Framework");
            
            // Build the correct URL for updating test run
            String url = config.buildUrl(
                config.getEndpoints().getTestRuns() + "/" + currentTestRun.id,
                null
            );
            
            client.patch(url, updateData, Map.class);
            
            logger.info("Completed test run: {} (ID: {})", currentTestRun.name, currentTestRun.id);
            logger.info("View results at: {}", currentTestRun.webAccessUrl);
            logger.info("Test point outcomes have been updated in the test plan");
            
            currentTestRun = null;
            testResultMapping.clear();
            
        } catch (Exception e) {
            logger.error("Failed to complete test run", e);
            throw new CSAzureDevOpsException("Failed to complete test run", e);
        }
    }
    
    /**
     * Abort test run
     */
    public void abortTestRun(String reason) {
        if (currentTestRun == null) {
            return;
        }
        
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("state", "Aborted");
            updateData.put("completedDate", LocalDateTime.now().toString());
            updateData.put("comment", "Test run aborted: " + reason);
            
            // Build the correct URL for updating test run
            String url = config.buildUrl(
                config.getEndpoints().getTestRuns() + "/" + currentTestRun.id,
                null
            );
            
            client.patch(url, updateData, Map.class);
            
            logger.info("Aborted test run: {} (ID: {})", currentTestRun.name, currentTestRun.id);
            
        } catch (Exception e) {
            logger.error("Failed to abort test run", e);
        } finally {
            currentTestRun = null;
            testResultMapping.clear();
        }
    }
    
    /**
     * Upload test evidence
     */
    private void uploadTestEvidence(String runId, String resultId, CSTestResult testResult) {
        try {
            // Upload evidence asynchronously
            evidenceUploader.uploadTestEvidence(runId, resultId, testResult.getTestName());
            
            // Create bug if test failed and configured
            if (config.isCreateBugsOnFailure() && 
                (testResult.getStatus() == CSTestResult.Status.FAILED || 
                 testResult.getStatus() == CSTestResult.Status.BROKEN)) {
                createBugForFailedTest(testResult);
            }
            
        } catch (Exception e) {
            logger.error("Failed to upload evidence for test: {}", testResult.getTestName(), e);
        }
    }
    
    /**
     * Create bug for failed test
     */
    private void createBugForFailedTest(CSTestResult testResult) {
        try {
            String title = config.formatBugTitle(testResult.getTestName(), testResult.getErrorMessage());
            
            List<Map<String, Object>> patchDocument = new ArrayList<>();
            
            // Add title
            addPatchOperation(patchDocument, "add", "/fields/System.Title", title);
            
            // Add description
            String description = formatBugDescription(testResult);
            addPatchOperation(patchDocument, "add", "/fields/System.Description", description);
            
            // Add repro steps
            addPatchOperation(patchDocument, "add", "/fields/Microsoft.VSTS.TCM.ReproSteps", description);
            
            // Add configured fields
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
            logger.info("Created bug {} for failed test: {}", bugId, testResult.getTestName());
            
        } catch (Exception e) {
            logger.error("Failed to create bug for test: {}", testResult.getTestName(), e);
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
    
    private String formatBugDescription(CSTestResult testResult) {
        StringBuilder description = new StringBuilder();
        description.append("<div><strong>Test Details:</strong></div>");
        description.append("<ul>");
        description.append("<li><strong>Test Class:</strong> ").append(testResult.getClassName()).append("</li>");
        description.append("<li><strong>Test Method:</strong> ").append(testResult.getMethodName()).append("</li>");
        description.append("<li><strong>Execution Time:</strong> ").append(testResult.getStartTime()).append("</li>");
        description.append("<li><strong>Duration:</strong> ").append(testResult.getDuration()).append(" ms</li>");
        description.append("</ul>");
        
        if (testResult.getErrorMessage() != null) {
            description.append("<div><strong>Error Message:</strong></div>");
            description.append("<pre>").append(testResult.getErrorMessage()).append("</pre>");
        }
        
        if (testResult.getStackTrace() != null) {
            description.append("<div><strong>Stack Trace:</strong></div>");
            description.append("<pre>").append(testResult.getStackTrace()).append("</pre>");
        }
        
        return description.toString();
    }
    
    /**
     * Find existing test result for a test point in the current run
     */
    private Integer findExistingTestResultForTestPoint(String runId, Integer testPointId) {
        try {
            // Get all test results from the current run
            String url = config.buildUrl(
                config.getEndpoints().getTestResults(),
                Map.of("runId", runId)
            );
            
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.get(url, Map.class);
            
            if (response.data != null && response.data.containsKey("value")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.data.get("value");
                
                // Find the result that matches our test point
                for (Map<String, Object> result : results) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> testPoint = (Map<String, Object>) result.get("testPoint");
                    if (testPoint != null) {
                        Object pointId = testPoint.get("id");
                        if (pointId != null && pointId.toString().equals(testPointId.toString())) {
                            Object resultId = result.get("id");
                            if (resultId != null) {
                                logger.debug("Found existing test result {} for test point {}", resultId, testPointId);
                                return Integer.parseInt(resultId.toString());
                            }
                        }
                    }
                }
            }
            
            logger.debug("No existing test result found for test point {}", testPointId);
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to find existing test result for test point {}", testPointId, e);
            return null;
        }
    }
    
    /**
     * Map test status to Azure DevOps outcome
     */
    private String mapTestStatus(String status) {
        if (status == null) {
            return "NotExecuted";
        }
        
        switch (status.toUpperCase()) {
            case "PASS":
            case "SUCCESS":
            case "PASSED":
                return "Passed";
            case "FAIL":
            case "FAILURE":
            case "FAILED":
                return "Failed";
            case "SKIP":
            case "SKIPPED":
            case "IGNORED":
                return "NotExecuted";
            case "BLOCKED":
                return "Blocked";
            default:
                return "NotApplicable";
        }
    }
    
    /**
     * Generate run name
     */
    private String generateRunName() {
        String runName = config.getRunName();
        if (runName == null || runName.isEmpty()) {
            runName = "CS TestForge Automated Run - " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return runName;
    }
    
    /**
     * Get current test run
     */
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }
    
    /**
     * Check if test run is active
     */
    public boolean hasActiveTestRun() {
        return currentTestRun != null;
    }
    
    // Inner classes
    
    public static class TestRun {
        public String id;
        public String name;
        public String state;
        public String url;
        public String webAccessUrl;
        public Date startedDate;
        public Date completedDate;
        public int totalTests;
        public int passedTests;
        public int failedTests;
    }
    
    public static class TestRunCreateRequest {
        public String name;
        public Integer planId;
        public Integer suiteId;
        public String buildId;
        public String releaseId;
        public String environment;
        
        public TestRunCreateRequest() {}
        
        public TestRunCreateRequest(String name) {
            this.name = name;
        }
    }
    
}