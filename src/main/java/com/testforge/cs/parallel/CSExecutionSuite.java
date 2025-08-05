package com.testforge.cs.parallel;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Execution suite tracking for parallel execution
 */
public class CSExecutionSuite {
    private final String suiteId;
    private final CSTestSuite testSuite;
    private final LocalDateTime startTime;
    private final AtomicInteger completedTests;
    private final AtomicInteger failedTests;
    
    public CSExecutionSuite(String suiteId, CSTestSuite testSuite) {
        this.suiteId = suiteId;
        this.testSuite = testSuite;
        this.startTime = LocalDateTime.now();
        this.completedTests = new AtomicInteger(0);
        this.failedTests = new AtomicInteger(0);
    }
    
    public String getSuiteId() {
        return suiteId;
    }
    
    public CSTestSuite getTestSuite() {
        return testSuite;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void incrementCompletedTests() {
        completedTests.incrementAndGet();
    }
    
    public void incrementFailedTests() {
        failedTests.incrementAndGet();
    }
    
    public int getCompletedTests() {
        return completedTests.get();
    }
    
    public int getFailedTests() {
        return failedTests.get();
    }
    
    public int getTotalTests() {
        return testSuite.getTestMethods().size();
    }
    
    public double getProgress() {
        return (double) completedTests.get() / getTotalTests();
    }
}