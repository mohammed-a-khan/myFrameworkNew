package com.testforge.cs.visualization;

import com.testforge.cs.context.CSThreadContext;
import com.testforge.cs.listeners.CSTestListener;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.visualization.CSTimelineVisualization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration helper for automatic timeline event capture
 * Hooks into the test framework to automatically record timeline events
 */
public class CSTimelineIntegration {
    private static final Logger logger = LoggerFactory.getLogger(CSTimelineIntegration.class);
    
    private static volatile CSTimelineIntegration instance;
    private static final Object instanceLock = new Object();
    
    private final CSTimelineVisualization timeline;
    private final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> suiteStartTimes = new ConcurrentHashMap<>();
    
    /**
     * Get singleton instance
     */
    public static CSTimelineIntegration getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSTimelineIntegration();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private CSTimelineIntegration() {
        this.timeline = CSTimelineVisualization.getInstance();
    }
    
    /**
     * Called when test suite starts
     */
    public void onSuiteStart(String suiteName) {
        long startTime = System.currentTimeMillis();
        suiteStartTimes.put(suiteName, startTime);
        timeline.recordSuitePhase(suiteName, "START", startTime, startTime);
        logger.debug("Timeline: Suite started - {}", suiteName);
    }
    
    /**
     * Called when test suite finishes
     */
    public void onSuiteFinish(String suiteName) {
        long endTime = System.currentTimeMillis();
        Long startTime = suiteStartTimes.get(suiteName);
        
        if (startTime != null) {
            timeline.recordSuitePhase(suiteName, "FINISH", startTime, endTime);
            suiteStartTimes.remove(suiteName);
            logger.debug("Timeline: Suite finished - {} ({}ms)", suiteName, endTime - startTime);
        }
    }
    
    /**
     * Called when test starts
     */
    public void onTestStart(ITestResult result) {
        String testKey = getTestKey(result);
        long startTime = System.currentTimeMillis();
        
        testStartTimes.put(testKey, startTime);
        
        timeline.recordTestStart(
            result.getMethod().getMethodName(),
            result.getTestClass().getName(),
            result.getMethod().getMethodName()
        );
        
        logger.debug("Timeline: Test started - {}", testKey);
    }
    
    /**
     * Called when test finishes
     */
    public void onTestFinish(ITestResult result) {
        String testKey = getTestKey(result);
        long endTime = System.currentTimeMillis();
        Long startTime = testStartTimes.remove(testKey);
        
        if (startTime != null) {
            long duration = endTime - startTime;
            boolean passed = result.getStatus() == ITestResult.SUCCESS;
            
            timeline.recordTestEnd(
                result.getMethod().getMethodName(),
                result.getTestClass().getName(),
                result.getMethod().getMethodName(),
                passed,
                duration
            );
            
            logger.debug("Timeline: Test finished - {} ({}) - {}ms", 
                testKey, passed ? "PASSED" : "FAILED", duration);
        }
    }
    
    /**
     * Record WebDriver action with automatic timing
     */
    public void recordWebDriverAction(String action, String element, Runnable actionCode) {
        long startTime = System.currentTimeMillis();
        
        try {
            actionCode.run();
            long duration = System.currentTimeMillis() - startTime;
            timeline.recordWebDriverAction(action, element, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            timeline.recordWebDriverAction(action + " (FAILED)", element, duration);
            throw e;
        }
    }
    
    /**
     * Record API call with automatic timing
     */
    public <T> T recordApiCall(String method, String url, java.util.function.Supplier<T> apiCall) {
        long startTime = System.currentTimeMillis();
        int responseCode = 200; // Default
        
        try {
            T result = apiCall.get();
            long duration = System.currentTimeMillis() - startTime;
            
            // Try to extract response code if result has it
            if (result != null) {
                try {
                    Method getStatusMethod = result.getClass().getMethod("statusCode");
                    if (getStatusMethod != null) {
                        responseCode = (Integer) getStatusMethod.invoke(result);
                    }
                } catch (Exception ignored) {
                    // Ignore if we can't get status code
                }
            }
            
            timeline.recordApiCall(method, url, responseCode, duration);
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            timeline.recordApiCall(method, url, 500, duration); // Assume 500 error
            throw e;
        }
    }
    
    /**
     * Record database operation with automatic timing
     */
    public <T> T recordDatabaseOperation(String operation, String query, java.util.function.Supplier<T> dbOperation) {
        long startTime = System.currentTimeMillis();
        int rowsAffected = 0;
        
        try {
            T result = dbOperation.get();
            long duration = System.currentTimeMillis() - startTime;
            
            // Try to extract rows affected
            if (result instanceof Number) {
                rowsAffected = ((Number) result).intValue();
            } else if (result instanceof java.util.Collection) {
                rowsAffected = ((java.util.Collection<?>) result).size();
            }
            
            timeline.recordDatabaseOperation(operation, query, duration, rowsAffected);
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            timeline.recordDatabaseOperation(operation + " (FAILED)", query, duration, 0);
            throw e;
        }
    }
    
    /**
     * Record custom event with context information
     */
    public void recordCustomEvent(String eventName, String category) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        // Add context information
        try {
            var context = CSThreadContext.getCurrentContext();
            if (context != null) {
                if (context.getCurrentTestMethod() != null) {
                    metadata.put("testMethod", context.getCurrentTestMethod());
                }
                if (context.getCurrentTestClass() != null) {
                    metadata.put("testClass", context.getCurrentTestClass());
                }
                // Browser type would be available through WebDriver context
                metadata.put("threadId", Thread.currentThread().getId());
                metadata.put("threadName", Thread.currentThread().getName());
            }
        } catch (Exception e) {
            // Ignore context errors
        }
        
        timeline.recordCustomEvent(eventName, category, metadata);
    }
    
    /**
     * Generate timeline report
     */
    public String generateTimelineReport() {
        return timeline.generateTimeline();
    }
    
    /**
     * Generate timeline report with custom filename
     */
    public String generateTimelineReport(String filename) {
        return timeline.generateTimeline(filename);
    }
    
    /**
     * Get timeline statistics
     */
    public CSTimelineVisualization.TimelineStatistics getStatistics() {
        return timeline.getStatistics();
    }
    
    /**
     * Clear timeline data
     */
    public void clearTimeline() {
        timeline.clearTimeline();
        testStartTimes.clear();
        suiteStartTimes.clear();
    }
    
    /**
     * Generate test key for tracking
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName() + 
               "_" + Thread.currentThread().getId();
    }
    
    /**
     * Integration with CSTestListener
     */
    public static class TimelineTestListener extends CSTestListener {
        private final CSTimelineIntegration integration = CSTimelineIntegration.getInstance();
        
        @Override
        public void onStart(org.testng.ITestContext context) {
            super.onStart(context);
            integration.onSuiteStart(context.getName());
        }
        
        @Override
        public void onFinish(org.testng.ITestContext context) {
            super.onFinish(context);
            integration.onSuiteFinish(context.getName());
        }
        
        @Override
        public void onTestStart(ITestResult result) {
            super.onTestStart(result);
            integration.onTestStart(result);
        }
        
        @Override
        public void onTestSuccess(ITestResult result) {
            super.onTestSuccess(result);
            integration.onTestFinish(result);
        }
        
        @Override
        public void onTestFailure(ITestResult result) {
            super.onTestFailure(result);
            integration.onTestFinish(result);
        }
        
        @Override
        public void onTestSkipped(ITestResult result) {
            super.onTestSkipped(result);
            integration.onTestFinish(result);
        }
    }
}