package com.testforge.cs.events.events;

import com.testforge.cs.events.CSEvent;
import com.testforge.cs.reporting.CSTestResult;

/**
 * Test lifecycle events
 */
public class CSTestEvent extends CSEvent {
    
    public static final String TEST_STARTED = "test.started";
    public static final String TEST_PASSED = "test.passed";
    public static final String TEST_FAILED = "test.failed";
    public static final String TEST_SKIPPED = "test.skipped";
    public static final String TEST_RETRY = "test.retry";
    
    private final CSTestResult testResult;
    private final String testClass;
    private final String testMethod;
    private final Exception failure;
    
    public CSTestEvent(String eventType, CSTestResult testResult, String testClass, String testMethod) {
        this(eventType, testResult, testClass, testMethod, null);
    }
    
    public CSTestEvent(String eventType, CSTestResult testResult, String testClass, String testMethod, Exception failure) {
        super(eventType, testClass + "." + testMethod);
        this.testResult = testResult;
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.failure = failure;
        
        // Add test-specific context
        if (testResult != null) {
            addContext("test.id", testResult.getTestId());
            addContext("test.name", testResult.getTestName());
            addContext("test.duration", testResult.getDuration());
            addContext("test.status", testResult.getStatus());
            addContext("test.tags", testResult.getTags());
            addContext("test.description", testResult.getDescription());
        }
        
        addContext("test.class", testClass);
        addContext("test.method", testMethod);
        
        if (failure != null) {
            addContext("failure.message", failure.getMessage());
            addContext("failure.type", failure.getClass().getSimpleName());
        }
    }
    
    public CSTestResult getTestResult() {
        return testResult;
    }
    
    public String getTestClass() {
        return testClass;
    }
    
    public String getTestMethod() {
        return testMethod;
    }
    
    public Exception getFailure() {
        return failure;
    }
    
    @Override
    public EventSeverity getSeverity() {
        switch (getEventType()) {
            case TEST_FAILED:
                return EventSeverity.ERROR;
            case TEST_SKIPPED:
                return EventSeverity.WARN;
            case TEST_RETRY:
                return EventSeverity.WARN;
            case TEST_STARTED:
            case TEST_PASSED:
            default:
                return EventSeverity.INFO;
        }
    }
    
    @Override
    public EventCategory getCategory() {
        return EventCategory.TEST_LIFECYCLE;
    }
    
    // Factory methods
    public static CSTestEvent started(CSTestResult testResult, String testClass, String testMethod) {
        return new CSTestEvent(TEST_STARTED, testResult, testClass, testMethod);
    }
    
    public static CSTestEvent passed(CSTestResult testResult, String testClass, String testMethod) {
        return new CSTestEvent(TEST_PASSED, testResult, testClass, testMethod);
    }
    
    public static CSTestEvent failed(CSTestResult testResult, String testClass, String testMethod, Exception failure) {
        return new CSTestEvent(TEST_FAILED, testResult, testClass, testMethod, failure);
    }
    
    public static CSTestEvent skipped(CSTestResult testResult, String testClass, String testMethod) {
        return new CSTestEvent(TEST_SKIPPED, testResult, testClass, testMethod);
    }
    
    public static CSTestEvent retry(CSTestResult testResult, String testClass, String testMethod, Exception failure) {
        CSTestEvent event = new CSTestEvent(TEST_RETRY, testResult, testClass, testMethod, failure);
        event.addContext("retry.attempt", testResult != null ? testResult.getRetryCount() : 0);
        return event;
    }
}