package com.testforge.cs.parallel;

import com.testforge.cs.reporting.CSTestResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Execution result for parallel test suite execution
 */
public class CSExecutionResult {
    private final String suiteId;
    private final String suiteName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<CSTestResult> testResults;
    private final List<Exception> exceptions;
    private final long durationMs;
    
    public CSExecutionResult(String suiteId, String suiteName, LocalDateTime startTime, 
                           LocalDateTime endTime, List<CSTestResult> testResults, 
                           List<Exception> exceptions) {
        this.suiteId = suiteId;
        this.suiteName = suiteName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.testResults = testResults;
        this.exceptions = exceptions;
        this.durationMs = Duration.between(startTime, endTime).toMillis();
    }
    
    public String getSuiteId() {
        return suiteId;
    }
    
    public String getSuiteName() {
        return suiteName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public List<CSTestResult> getTestResults() {
        return testResults;
    }
    
    public List<Exception> getExceptions() {
        return exceptions;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public int getTotalTests() {
        return testResults.size();
    }
    
    public int getPassedTests() {
        return (int) testResults.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.PASSED)
            .count();
    }
    
    public int getFailedTests() {
        return (int) testResults.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.FAILED)
            .count();
    }
    
    public int getSkippedTests() {
        return (int) testResults.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.SKIPPED)
            .count();
    }
    
    public double getSuccessRate() {
        return getTotalTests() > 0 ? (double) getPassedTests() / getTotalTests() : 0.0;
    }
    
    public boolean hasFailures() {
        return getFailedTests() > 0 || !exceptions.isEmpty();
    }
}