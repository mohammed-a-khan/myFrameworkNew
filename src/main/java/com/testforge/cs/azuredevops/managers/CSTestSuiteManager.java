package com.testforge.cs.azuredevops.managers;

import com.testforge.cs.azuredevops.client.CSEnhancedADOClient;
import com.testforge.cs.azuredevops.config.CSADOConfiguration;
import com.testforge.cs.azuredevops.models.CSTestPoint;
import com.testforge.cs.azuredevops.models.CSTestSuite;
import com.testforge.cs.exceptions.CSAzureDevOpsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * Manages test suites and test points in Azure DevOps
 * Based on Playwright framework's TestSuiteManager
 */
public class CSTestSuiteManager {
    private static final Logger logger = LoggerFactory.getLogger(CSTestSuiteManager.class);
    private static CSTestSuiteManager instance;
    
    private final CSEnhancedADOClient client;
    private final CSADOConfiguration config;
    
    // Caches
    private final Map<Integer, CSTestSuite> testSuiteCache = new ConcurrentHashMap<>();
    private final Map<String, CSTestPoint> testPointCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> testCaseToPointMapping = new ConcurrentHashMap<>();
    
    private CSTestSuiteManager() {
        this.client = CSEnhancedADOClient.getInstance();
        this.config = CSADOConfiguration.getInstance();
    }
    
    public static synchronized CSTestSuiteManager getInstance() {
        if (instance == null) {
            instance = new CSTestSuiteManager();
        }
        return instance;
    }
    
    /**
     * Get test suite by ID
     */
    public CSTestSuite getTestSuite(int planId, int suiteId) {
        // Check cache first
        if (testSuiteCache.containsKey(suiteId)) {
            return testSuiteCache.get(suiteId);
        }
        
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestSuites(),
                Map.of("planId", planId)
            ) + "/" + suiteId;
            
            CSEnhancedADOClient.ADOResponse<CSTestSuite> response = 
                client.get(url, CSTestSuite.class);
            
            CSTestSuite suite = response.data;
            testSuiteCache.put(suiteId, suite);
            
            logger.info("Retrieved test suite: {} (ID: {})", suite.getName(), suite.getId());
            return suite;
            
        } catch (Exception e) {
            logger.error("Failed to get test suite: {}", suiteId, e);
            throw new CSAzureDevOpsException("Failed to get test suite", e);
        }
    }
    
    /**
     * Get all test suites in a plan
     */
    public List<CSTestSuite> getTestSuites(int planId) {
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestSuites(),
                Map.of("planId", planId)
            );
            
            CSEnhancedADOClient.ADOListResponse<CSTestSuite> response = 
                client.getList(url, CSTestSuite.class);
            
            List<CSTestSuite> suites = response.value;
            
            // Cache the suites
            for (CSTestSuite suite : suites) {
                testSuiteCache.put(suite.getId(), suite);
            }
            
            logger.info("Retrieved {} test suites from plan: {}", suites.size(), planId);
            return suites;
            
        } catch (Exception e) {
            logger.error("Failed to get test suites for plan: {}", planId, e);
            throw new CSAzureDevOpsException("Failed to get test suites", e);
        }
    }
    
    /**
     * Get test points in a suite
     */
    public List<CSTestPoint> getTestPoints(int planId, int suiteId) {
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestPoints(),
                Map.of("planId", planId, "suiteId", suiteId)
            );
            
            // Get all test points with pagination
            List<CSTestPoint> points = client.getAll(url, CSTestPoint.class, 200);
            
            // Cache the test points
            for (CSTestPoint point : points) {
                String cacheKey = planId + "_" + suiteId + "_" + point.getId();
                testPointCache.put(cacheKey, point);
                
                // Map test case ID to test point ID
                if (point.getTestCase() != null) {
                    testCaseToPointMapping.put(
                        point.getTestCase().getId(), 
                        point.getId()
                    );
                }
            }
            
            logger.info("Retrieved {} test points from suite: {}", points.size(), suiteId);
            return points;
            
        } catch (Exception e) {
            logger.error("Failed to get test points for suite: {}", suiteId, e);
            throw new CSAzureDevOpsException("Failed to get test points", e);
        }
    }
    
    /**
     * Get test point by test case name
     */
    public CSTestPoint getTestPointByTestName(int planId, int suiteId, String testName) {
        // First try to get from cache
        String searchKey = testName.toLowerCase();
        
        for (CSTestPoint point : testPointCache.values()) {
            if (point.getTestCase() != null && 
                point.getTestCase().getName() != null &&
                point.getTestCase().getName().toLowerCase().contains(searchKey)) {
                return point;
            }
        }
        
        // If not in cache, fetch from API
        List<CSTestPoint> points = getTestPoints(planId, suiteId);
        
        for (CSTestPoint point : points) {
            if (point.getTestCase() != null && 
                point.getTestCase().getName() != null &&
                point.getTestCase().getName().toLowerCase().contains(searchKey)) {
                return point;
            }
        }
        
        logger.warn("No test point found for test name: {}", testName);
        return null;
    }
    
    /**
     * Map test method to test point
     */
    public Integer mapTestToPoint(String className, String methodName, int planId, int suiteId) {
        // First check if the method name contains a test case ID pattern
        // Pattern: testADOCase419_ValidLogin -> extract 419
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:testADOCase|testCase|ADO|TC)(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(methodName);
        
        if (matcher.find()) {
            String testCaseId = matcher.group(1);
            logger.info("Found test case ID in method name: {} -> {}", methodName, testCaseId);
            
            // Try to find test point for this test case ID
            for (CSTestPoint point : testPointCache.values()) {
                if (point.getTestCase() != null && 
                    point.getTestCase().getId() != null &&
                    point.getTestCase().getId().equals(testCaseId)) {
                    logger.info("Mapped test {}.{} to test point {} via test case ID {}", 
                        className, methodName, point.getId(), testCaseId);
                    return point.getId();
                }
            }
        }
        
        // Fallback to name-based search
        String fullName = className + "." + methodName;
        String simpleName = methodName;
        
        // Try to find by full name first
        CSTestPoint point = getTestPointByTestName(planId, suiteId, fullName);
        
        if (point == null) {
            // Try by method name only
            point = getTestPointByTestName(planId, suiteId, simpleName);
        }
        
        if (point != null) {
            logger.debug("Mapped test {}.{} to test point: {}", 
                className, methodName, point.getId());
            return point.getId();
        }
        
        logger.debug("No test point mapping found for {}.{}", className, methodName);
        return null;
    }
    
    /**
     * Create test suite
     */
    public CSTestSuite createTestSuite(int planId, String name, int parentSuiteId) {
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestSuites(),
                Map.of("planId", planId)
            );
            
            Map<String, Object> suiteData = new HashMap<>();
            suiteData.put("name", name);
            suiteData.put("suiteType", "StaticTestSuite");
            
            if (parentSuiteId > 0) {
                Map<String, Object> parentSuite = new HashMap<>();
                parentSuite.put("id", parentSuiteId);
                suiteData.put("parentSuite", parentSuite);
            }
            
            CSEnhancedADOClient.ADOResponse<CSTestSuite> response = 
                client.post(url, suiteData, CSTestSuite.class);
            
            CSTestSuite suite = response.data;
            testSuiteCache.put(suite.getId(), suite);
            
            logger.info("Created test suite: {} (ID: {})", suite.getName(), suite.getId());
            return suite;
            
        } catch (Exception e) {
            logger.error("Failed to create test suite: {}", name, e);
            throw new CSAzureDevOpsException("Failed to create test suite", e);
        }
    }
    
    /**
     * Update test suite
     */
    public CSTestSuite updateTestSuite(int planId, int suiteId, Map<String, Object> updates) {
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestSuites(),
                Map.of("planId", planId)
            ) + "/" + suiteId;
            
            CSEnhancedADOClient.ADOResponse<CSTestSuite> response = 
                client.patch(url, updates, CSTestSuite.class);
            
            CSTestSuite suite = response.data;
            testSuiteCache.put(suite.getId(), suite);
            
            logger.info("Updated test suite: {} (ID: {})", suite.getName(), suite.getId());
            return suite;
            
        } catch (Exception e) {
            logger.error("Failed to update test suite: {}", suiteId, e);
            throw new CSAzureDevOpsException("Failed to update test suite", e);
        }
    }
    
    /**
     * Add test cases to suite
     */
    public List<CSTestPoint> addTestCasesToSuite(int planId, int suiteId, List<Integer> testCaseIds) {
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestPoints(),
                Map.of("planId", planId, "suiteId", suiteId)
            );
            
            List<Map<String, Object>> testCases = new ArrayList<>();
            for (Integer testCaseId : testCaseIds) {
                Map<String, Object> testCase = new HashMap<>();
                testCase.put("id", testCaseId);
                testCases.add(testCase);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("testCases", testCases);
            
            CSEnhancedADOClient.ADOListResponse<CSTestPoint> response = 
                client.post(url, requestBody, Map.class).data != null ?
                client.getList(url, CSTestPoint.class) : new CSEnhancedADOClient.ADOListResponse<>();
            
            List<CSTestPoint> points = response.value;
            
            // Cache the new test points
            for (CSTestPoint point : points) {
                String cacheKey = planId + "_" + suiteId + "_" + point.getId();
                testPointCache.put(cacheKey, point);
            }
            
            logger.info("Added {} test cases to suite: {}", testCaseIds.size(), suiteId);
            return points;
            
        } catch (Exception e) {
            logger.error("Failed to add test cases to suite: {}", suiteId, e);
            throw new CSAzureDevOpsException("Failed to add test cases to suite", e);
        }
    }
    
    /**
     * Get test point by ID
     */
    public CSTestPoint getTestPoint(int planId, int suiteId, int pointId) {
        // Check cache first
        String cacheKey = planId + "_" + suiteId + "_" + pointId;
        if (testPointCache.containsKey(cacheKey)) {
            return testPointCache.get(cacheKey);
        }
        
        try {
            String url = config.buildUrl(
                config.getEndpoints().getTestPoints(),
                Map.of("planId", planId, "suiteId", suiteId)
            ) + "/" + pointId;
            
            CSEnhancedADOClient.ADOResponse<CSTestPoint> response = 
                client.get(url, CSTestPoint.class);
            
            CSTestPoint point = response.data;
            testPointCache.put(cacheKey, point);
            
            return point;
            
        } catch (Exception e) {
            logger.error("Failed to get test point: {}", pointId, e);
            throw new CSAzureDevOpsException("Failed to get test point", e);
        }
    }
    
    /**
     * Clear caches
     */
    public void clearCaches() {
        testSuiteCache.clear();
        testPointCache.clear();
        testCaseToPointMapping.clear();
        logger.info("Cleared test suite manager caches");
    }
    
    /**
     * Get test point for test case ID
     */
    public Integer getTestPointForTestCase(String testCaseId) {
        return testCaseToPointMapping.get(testCaseId);
    }
    
    /**
     * Initialize test points for configured suite
     */
    public void initializeTestPoints() {
        if (!config.isEnabled()) {
            return;
        }
        
        String planId = config.getTestPlanId();
        String suiteId = config.getTestSuiteId();
        
        if (planId != null && suiteId != null) {
            try {
                logger.info("Initializing test points for plan: {}, suite: {}", planId, suiteId);
                List<CSTestPoint> points = getTestPoints(
                    Integer.parseInt(planId), 
                    Integer.parseInt(suiteId)
                );
                logger.info("Loaded {} test points", points.size());
            } catch (Exception e) {
                logger.warn("Failed to initialize test points", e);
            }
        }
    }
    
    /**
     * Find test point by test case ID
     */
    public Integer findTestPointByTestCase(Integer testCaseId, String planId, String suiteId) {
        if (testCaseId == null || planId == null || suiteId == null) {
            return null;
        }
        
        String cacheKey = planId + "-" + suiteId;
        // Get test points from suite cache
        List<CSTestPoint> testPoints = null;
        CSTestSuite suite = testSuiteCache.get(Integer.parseInt(suiteId));
        if (suite != null && suite.getTestPoints() != null) {
            testPoints = suite.getTestPoints();
        }
        
        if (testPoints == null) {
            // Try to load test points if not in cache
            try {
                testPoints = getTestPoints(
                    Integer.parseInt(planId), 
                    Integer.parseInt(suiteId)
                );
            } catch (Exception e) {
                logger.error("Failed to load test points for finding test case {}", testCaseId, e);
                return null;
            }
        }
        
        // Find test point with matching test case ID
        for (CSTestPoint testPoint : testPoints) {
            if (testPoint.getTestCase() != null && 
                testCaseId.equals(testPoint.getTestCase().getId())) {
                logger.debug("Found test point {} for test case {}", testPoint.getId(), testCaseId);
                return testPoint.getId();
            }
        }
        
        logger.debug("No test point found for test case {} in plan {} suite {}", 
            testCaseId, planId, suiteId);
        return null;
    }
    
    /**
     * Check if test suite manager is initialized
     */
    public boolean isInitialized() {
        return !testSuiteCache.isEmpty() || !testPointCache.isEmpty();
    }
    
    /**
     * Update test point outcome in test plan
     * This updates the test point's last result to reflect in the test plan view
     */
    public void updateTestPointOutcome(String planId, String suiteId, Integer testCaseId, 
                                       String outcome, String runId, String resultId) {
        if (testCaseId == null || planId == null || suiteId == null) {
            logger.warn("Cannot update test point - missing required IDs");
            return;
        }
        
        Integer testPointId = null;
        
        try {
            // Find the test point for this test case
            testPointId = findTestPointByTestCase(testCaseId, planId, suiteId);
            
            if (testPointId == null) {
                logger.warn("No test point found for test case {} in plan {} suite {}", 
                    testCaseId, planId, suiteId);
                return;
            }
            
            // The correct API endpoint for updating test points outcomes
            // Uses the Update Test Points API which accepts an array of test point updates
            String url = config.buildUrl(
                "/test/plans/" + planId + "/suites/" + suiteId + "/points",
                null
            );
            
            // Create update payload - Azure DevOps expects an array of test point updates
            List<Map<String, Object>> testPointUpdates = new ArrayList<>();
            Map<String, Object> pointUpdate = new HashMap<>();
            
            // Specify the test point ID to update
            pointUpdate.put("id", testPointId);
            
            // Create the results object with outcome
            Map<String, Object> results = new HashMap<>();
            results.put("outcome", outcome);
            
            // Add run and result references if provided
            if (runId != null && resultId != null) {
                Map<String, Object> lastTestRun = new HashMap<>();
                lastTestRun.put("id", Integer.parseInt(runId));
                results.put("lastTestRun", lastTestRun);
                
                Map<String, Object> lastResult = new HashMap<>();
                lastResult.put("id", Integer.parseInt(resultId));
                results.put("lastResult", lastResult);
            }
            
            results.put("state", 2); // 2 = Completed state in ADO
            results.put("lastUpdatedDate", LocalDateTime.now().toString());
            results.put("lastUpdatedBy", "CS TestForge Framework");
            
            // Add results to the point update
            pointUpdate.put("results", results);
            
            // Add to the array
            testPointUpdates.add(pointUpdate);
            
            // PATCH the test points (array format)
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.patch(url, testPointUpdates, Map.class);
            
            if (response.data != null) {
                logger.info("Successfully updated test point {} for test case {} with outcome: {}", 
                    testPointId, testCaseId, outcome);
                
                // Also try to update via the test results API to ensure the outcome is reflected
                updateTestCaseOutcomeViaResults(planId, suiteId, testPointId, testCaseId, outcome, runId, resultId);
            } else {
                logger.warn("Test point update response was null for test point {}", testPointId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update test point outcome for test case {}", testCaseId, e);
            // Try alternate approach
            updateTestCaseOutcomeViaResults(planId, suiteId, testPointId, testCaseId, outcome, runId, resultId);
        }
    }
    
    /**
     * Alternative method to update test case outcome using test results API
     */
    private void updateTestCaseOutcomeViaResults(String planId, String suiteId, Integer testPointId, 
                                                 Integer testCaseId, String outcome, String runId, String resultId) {
        if (runId == null || resultId == null) {
            return;
        }
        
        try {
            // Update the test result to ensure it has the correct associations
            String url = config.buildUrl(
                "/test/runs/" + runId + "/results/" + resultId,
                null
            );
            
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("outcome", outcome);
            updateData.put("state", "Completed");
            
            // Add test point association
            if (testPointId != null) {
                Map<String, Object> testPoint = new HashMap<>();
                testPoint.put("id", testPointId);
                updateData.put("testPoint", testPoint);
            }
            
            // Add test case association
            Map<String, Object> testCase = new HashMap<>();
            testCase.put("id", testCaseId);
            updateData.put("testCase", testCase);
            
            // Add test plan association
            Map<String, Object> testPlan = new HashMap<>();
            testPlan.put("id", Integer.parseInt(planId));
            updateData.put("testPlan", testPlan);
            
            @SuppressWarnings("unchecked")
            CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
                (CSEnhancedADOClient.ADOResponse<Map<String, Object>>) (CSEnhancedADOClient.ADOResponse<?>) 
                client.patch(url, updateData, Map.class);
            
            if (response.data != null) {
                logger.debug("Updated test result {} with test case associations", resultId);
            }
            
        } catch (Exception e) {
            logger.debug("Could not update test result associations: {}", e.getMessage());
        }
    }
}