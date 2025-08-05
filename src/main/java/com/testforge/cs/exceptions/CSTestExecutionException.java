package com.testforge.cs.exceptions;

/**
 * Exception for test execution related errors
 */
public class CSTestExecutionException extends CSFrameworkException {
    
    private String testName;
    private String testClass;
    private String phase; // SETUP, EXECUTION, TEARDOWN
    
    public CSTestExecutionException(String message) {
        super("TEST_EXECUTION_ERROR", "TestExecution", message);
    }
    
    public CSTestExecutionException(String message, Throwable cause) {
        super("TEST_EXECUTION_ERROR", "TestExecution", message, cause);
    }
    
    public CSTestExecutionException(String testName, String testClass, String phase, String message) {
        super("TEST_EXECUTION_ERROR", "TestExecution", message);
        this.testName = testName;
        this.testClass = testClass;
        this.phase = phase;
    }
    
    public CSTestExecutionException(String testName, String testClass, String phase, String message, Throwable cause) {
        super("TEST_EXECUTION_ERROR", "TestExecution", message, cause);
        this.testName = testName;
        this.testClass = testClass;
        this.phase = phase;
    }
    
    public String getTestName() {
        return testName;
    }
    
    public String getTestClass() {
        return testClass;
    }
    
    public String getPhase() {
        return phase;
    }
}