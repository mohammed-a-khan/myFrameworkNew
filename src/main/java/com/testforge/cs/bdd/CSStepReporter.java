package com.testforge.cs.bdd;

import com.testforge.cs.reporting.CSReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reporter for BDD step execution
 */
public class CSStepReporter {
    private static final Logger logger = LoggerFactory.getLogger(CSStepReporter.class);
    private final CSReportManager reportManager = CSReportManager.getInstance();
    
    /**
     * Log a step execution
     */
    public void logStep(String stepDescription) {
        logger.info("Step: {}", stepDescription);
        reportManager.logStep(stepDescription, true);
    }
    
    /**
     * Log a validation
     */
    public void logValidation(String validation, String expected, String actual, String status) {
        logger.info("Validation: {} - Expected: {}, Actual: {}, Status: {}", 
                   validation, expected, actual, status);
        // CSReportManager doesn't have logValidation - use logStep instead
        String validationMsg = String.format("Validation: %s - Expected: %s, Actual: %s", 
                                           validation, expected, actual);
        reportManager.logStep(validationMsg, status.equals("PASS"));
    }
    
    /**
     * Log an error
     */
    public void logError(String error, Throwable throwable) {
        logger.error("Error: {}", error, throwable);
        reportManager.logError(error + ": " + throwable.getMessage());
    }
    
    /**
     * Log info
     */
    public void logInfo(String info) {
        logger.info("Info: {}", info);
        reportManager.logInfo(info);
    }
    
    /**
     * Log warning
     */
    public void logWarning(String warning) {
        logger.warn("Warning: {}", warning);
        reportManager.logWarning(warning);
    }
}