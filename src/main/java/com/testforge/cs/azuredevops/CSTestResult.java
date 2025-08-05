package com.testforge.cs.azuredevops;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an Azure DevOps test result
 */
public class CSTestResult {
    private String testCaseTitle;
    private String automatedTestName;
    private String automatedTestStorage;
    private String automatedTestType;
    private String outcome;
    private String state;
    private String errorMessage;
    private String stackTrace;
    private LocalDateTime startedDate;
    private LocalDateTime completedDate;
    private long durationInMs;
    private String computerName;
    private String testRunner;
    private List<String> attachments;
    private Map<String, Object> customFields;
    
    public CSTestResult() {
        this.attachments = new ArrayList<>();
        this.state = "Completed";
        this.testRunner = "CS TestForge Framework";
        this.automatedTestType = "UnitTest";
    }
    
    /**
     * Create test result from framework test result
     */
    public static CSTestResult fromFrameworkResult(com.testforge.cs.reporting.CSTestResult frameworkResult) {
        CSTestResult result = new CSTestResult();
        
        result.setTestCaseTitle(frameworkResult.getTestName());
        result.setAutomatedTestName(frameworkResult.getClassName() + "." + frameworkResult.getMethodName());
        result.setAutomatedTestStorage(frameworkResult.getClassName());
        
        // Map framework status to Azure DevOps outcome
        switch (frameworkResult.getStatus()) {
            case PASSED:
                result.setOutcome("Passed");
                break;
            case FAILED:
                result.setOutcome("Failed");
                result.setErrorMessage(frameworkResult.getErrorMessage());
                result.setStackTrace(frameworkResult.getStackTrace());
                break;
            case SKIPPED:
                result.setOutcome("NotExecuted");
                break;
            default:
                result.setOutcome("None");
        }
        
        result.setStartedDate(frameworkResult.getStartTime());
        result.setCompletedDate(frameworkResult.getEndTime());
        result.setDurationInMs(frameworkResult.getDuration());
        
        try {
            result.setComputerName(java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            result.setComputerName("Unknown");
        }
        
        return result;
    }
    
    // Getters and setters
    public String getTestCaseTitle() {
        return testCaseTitle;
    }
    
    public void setTestCaseTitle(String testCaseTitle) {
        this.testCaseTitle = testCaseTitle;
    }
    
    public String getAutomatedTestName() {
        return automatedTestName;
    }
    
    public void setAutomatedTestName(String automatedTestName) {
        this.automatedTestName = automatedTestName;
    }
    
    public String getAutomatedTestStorage() {
        return automatedTestStorage;
    }
    
    public void setAutomatedTestStorage(String automatedTestStorage) {
        this.automatedTestStorage = automatedTestStorage;
    }
    
    public String getAutomatedTestType() {
        return automatedTestType;
    }
    
    public void setAutomatedTestType(String automatedTestType) {
        this.automatedTestType = automatedTestType;
    }
    
    public String getOutcome() {
        return outcome;
    }
    
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public LocalDateTime getStartedDate() {
        return startedDate;
    }
    
    public void setStartedDate(LocalDateTime startedDate) {
        this.startedDate = startedDate;
    }
    
    public LocalDateTime getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }
    
    public long getDurationInMs() {
        return durationInMs;
    }
    
    public void setDurationInMs(long durationInMs) {
        this.durationInMs = durationInMs;
    }
    
    public String getComputerName() {
        return computerName;
    }
    
    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }
    
    public String getTestRunner() {
        return testRunner;
    }
    
    public void setTestRunner(String testRunner) {
        this.testRunner = testRunner;
    }
    
    public List<String> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
    
    public void addAttachment(String attachment) {
        this.attachments.add(attachment);
    }
    
    public Map<String, Object> getCustomFields() {
        return customFields;
    }
    
    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
    
    /**
     * Convert to Azure DevOps API format
     */
    public Map<String, Object> toApiFormat() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        result.put("testCaseTitle", testCaseTitle);
        result.put("automatedTestName", automatedTestName);
        result.put("automatedTestStorage", automatedTestStorage);
        result.put("automatedTestType", automatedTestType);
        result.put("outcome", outcome);
        result.put("state", state);
        result.put("computerName", computerName);
        result.put("testRunner", testRunner);
        result.put("durationInMs", durationInMs);
        
        if (errorMessage != null) {
            result.put("errorMessage", errorMessage);
        }
        
        if (stackTrace != null) {
            result.put("stackTrace", stackTrace);
        }
        
        if (startedDate != null) {
            result.put("startedDate", startedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        }
        
        if (completedDate != null) {
            result.put("completedDate", completedDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        }
        
        if (customFields != null) {
            result.putAll(customFields);
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("CSTestResult{testCaseTitle='%s', outcome='%s', duration=%dms}", 
            testCaseTitle, outcome, durationInMs);
    }
}