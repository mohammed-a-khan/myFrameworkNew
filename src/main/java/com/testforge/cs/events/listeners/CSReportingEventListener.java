package com.testforge.cs.events.listeners;

import com.testforge.cs.events.CSEvent;
import com.testforge.cs.events.CSEventListener;
import com.testforge.cs.events.events.CSTestEvent;
import com.testforge.cs.events.events.CSWebDriverEvent;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.context.CSThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Event listener that integrates with reporting system
 * Automatically captures events for report generation
 */
public class CSReportingEventListener implements CSEventListener {
    private static final Logger logger = LoggerFactory.getLogger(CSReportingEventListener.class);
    
    private final CSReportManager reportManager;
    
    public CSReportingEventListener() {
        this.reportManager = CSReportManager.getInstance();
    }
    
    @Override
    public void handleEvent(CSEvent event) {
        try {
            // Handle different event types
            switch (event.getCategory()) {
                case TEST_LIFECYCLE:
                    handleTestEvent(event);
                    break;
                case WEB_DRIVER:
                    handleWebDriverEvent(event);
                    break;
                case API_TESTING:
                    handleApiEvent(event);
                    break;
                case PERFORMANCE:
                    handlePerformanceEvent(event);
                    break;
                default:
                    handleGenericEvent(event);
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Error handling event for reporting: {}", event.getEventId(), e);
        }
    }
    
    /**
     * Handle test lifecycle events
     */
    private void handleTestEvent(CSEvent event) {
        if (event instanceof CSTestEvent) {
            CSTestEvent testEvent = (CSTestEvent) event;
            
            if (testEvent.getTestResult() != null) {
                switch (event.getEventType()) {
                    case CSTestEvent.TEST_STARTED:
                        testEvent.getTestResult().addStep("Test started: " + testEvent.getTestMethod());
                        break;
                        
                    case CSTestEvent.TEST_PASSED:
                        testEvent.getTestResult().addStep("Test passed: " + testEvent.getTestMethod());
                        break;
                        
                    case CSTestEvent.TEST_FAILED:
                        String failureMessage = testEvent.getFailure() != null ? 
                            testEvent.getFailure().getMessage() : "Test failed";
                        testEvent.getTestResult().addStep("Test failed: " + failureMessage);
                        break;
                        
                    case CSTestEvent.TEST_SKIPPED:
                        testEvent.getTestResult().addStep("Test skipped: " + testEvent.getTestMethod());
                        break;
                        
                    case CSTestEvent.TEST_RETRY:
                        int retryCount = (int) event.getContext().getOrDefault("retry.attempt", 0);
                        testEvent.getTestResult().addStep("Test retry attempt " + retryCount + ": " + testEvent.getTestMethod());
                        break;
                }
            }
        }
    }
    
    /**
     * Handle WebDriver events
     */
    private void handleWebDriverEvent(CSEvent event) {
        if (event instanceof CSWebDriverEvent) {
            CSWebDriverEvent driverEvent = (CSWebDriverEvent) event;
            com.testforge.cs.reporting.CSTestResult currentTest = getCurrentTestResult();
            
            if (currentTest != null) {
                switch (event.getEventType()) {
                    case CSWebDriverEvent.NAVIGATION:
                        currentTest.addStep("Navigated to: " + driverEvent.getUrl() + 
                            " (" + driverEvent.getExecutionTime() + "ms)");
                        break;
                        
                    case CSWebDriverEvent.ELEMENT_CLICK:
                        currentTest.addStep("Clicked element: " + driverEvent.getLocator() +
                            " (" + driverEvent.getExecutionTime() + "ms)");
                        break;
                        
                    case CSWebDriverEvent.ELEMENT_SEND_KEYS:
                        currentTest.addStep("Entered text in: " + driverEvent.getLocator() +
                            " (" + driverEvent.getExecutionTime() + "ms)");
                        break;
                        
                    case CSWebDriverEvent.SCREENSHOT_TAKEN:
                        currentTest.addStep("Screenshot taken: " + driverEvent.getValue());
                        break;
                        
                    case CSWebDriverEvent.ELEMENT_NOT_FOUND:
                    case CSWebDriverEvent.ELEMENT_TIMEOUT:
                        currentTest.addStep("Element not found: " + driverEvent.getLocator() +
                            " (timeout after " + driverEvent.getExecutionTime() + "ms)");
                        break;
                }
            }
        }
    }
    
    /**
     * Handle API testing events
     */
    private void handleApiEvent(CSEvent event) {
        com.testforge.cs.reporting.CSTestResult currentTest = getCurrentTestResult();
        if (currentTest != null) {
            Object url = event.getContext().get("request.url");
            Object method = event.getContext().get("request.method");
            Object statusCode = event.getContext().get("response.status");
            Object responseTime = event.getContext().get("response.time");
            
            StringBuilder stepMessage = new StringBuilder();
            stepMessage.append("API ").append(event.getEventType()).append(": ");
            
            if (method != null && url != null) {
                stepMessage.append(method).append(" ").append(url);
            }
            
            if (statusCode != null) {
                stepMessage.append(" -> ").append(statusCode);
            }
            
            if (responseTime != null) {
                stepMessage.append(" (").append(responseTime).append("ms)");
            }
            
            currentTest.addStep(stepMessage.toString());
        }
    }
    
    /**
     * Handle performance events
     */
    private void handlePerformanceEvent(CSEvent event) {
        com.testforge.cs.reporting.CSTestResult currentTest = getCurrentTestResult();
        if (currentTest != null) {
            Object metric = event.getContext().get("performance.metric");
            Object value = event.getContext().get("performance.value");
            Object threshold = event.getContext().get("performance.threshold");
            
            StringBuilder stepMessage = new StringBuilder();
            stepMessage.append("Performance ").append(event.getEventType()).append(": ");
            
            if (metric != null && value != null) {
                stepMessage.append(metric).append(" = ").append(value);
                
                if (threshold != null) {
                    stepMessage.append(" (threshold: ").append(threshold).append(")");
                }
            }
            
            currentTest.addStep(stepMessage.toString());
        }
    }
    
    /**
     * Handle generic events
     */
    private void handleGenericEvent(CSEvent event) {
        // For high-severity events, always add to report
        if (event.getSeverity().isHigherThan(CSEvent.EventSeverity.WARN)) {
            com.testforge.cs.reporting.CSTestResult currentTest = getCurrentTestResult();
            if (currentTest != null) {
                currentTest.addStep("[" + event.getSeverity() + "] " + event.getEventType() + 
                    " from " + event.getSource());
            }
        }
    }
    
    /**
     * Get current test result from thread context
     */
    private com.testforge.cs.reporting.CSTestResult getCurrentTestResult() {
        return CSThreadContext.getCurrentTestResult();
    }
    
    @Override
    public Set<String> getSupportedEventTypes() {
        return Set.of("*"); // Handle all event types
    }
    
    @Override
    public Set<CSEvent.EventCategory> getSupportedCategories() {
        return EnumSet.allOf(CSEvent.EventCategory.class);
    }
    
    @Override
    public CSEvent.EventSeverity getMinimumSeverity() {
        return CSEvent.EventSeverity.DEBUG;
    }
    
    @Override
    public String getListenerName() {
        return "ReportingEventListener";
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for reporting
    }
    
    @Override
    public void initialize() {
        logger.info("Initialized reporting event listener");
    }
    
    @Override
    public void cleanup() {
        logger.info("Reporting event listener cleanup complete");
    }
}