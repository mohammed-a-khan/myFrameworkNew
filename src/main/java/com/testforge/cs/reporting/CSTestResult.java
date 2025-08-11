package com.testforge.cs.reporting;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Represents a test result with all execution details
 */
public class CSTestResult {
    public enum Status {
        PENDING, RUNNING, PASSED, FAILED, BROKEN, SKIPPED, RETRIED
    }
    
    private String testId;
    private String testName;
    private String className;
    private String methodName;
    private String description;
    private Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration; // in milliseconds
    private String errorMessage;
    private String stackTrace;
    private List<String> steps;
    private Map<String, String> attachments;
    private Map<String, Object> metadata;
    private int retryCount;
    private String browser;
    private String environment;
    private List<String> tags;
    private Map<String, Object> testData;
    private String suiteName;
    private String featureFile;
    private List<Map<String, Object>> executedSteps;
    private String screenshotPath;
    private List<Screenshot> screenshots;
    private String threadName;
    private String scenarioName;
    
    public CSTestResult() {
        this.steps = new ArrayList<>();
        this.attachments = new LinkedHashMap<>();
        this.metadata = new HashMap<>();
        this.tags = new ArrayList<>();
        this.testData = new HashMap<>();
        this.status = Status.PENDING;
        this.retryCount = 0;
        this.executedSteps = new ArrayList<>();
        this.screenshots = new ArrayList<>();
        this.threadName = Thread.currentThread().getName();
    }
    
    // Getters and Setters
    public String getTestId() {
        return testId;
    }
    
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    public String getTestName() {
        return testName;
    }
    
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
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
    
    public List<String> getSteps() {
        return steps;
    }
    
    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
    
    public Map<String, String> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getTestData() {
        return testData;
    }
    
    public void setTestData(Map<String, Object> testData) {
        this.testData = testData;
    }
    
    public String getSuiteName() {
        return suiteName;
    }
    
    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }
    
    public List<Map<String, Object>> getExecutedSteps() {
        return executedSteps;
    }
    
    public void setExecutedSteps(List<Map<String, Object>> executedSteps) {
        this.executedSteps = executedSteps;
    }
    
    public String getScreenshotPath() {
        return screenshotPath;
    }
    
    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }
    
    public String getFeatureFile() {
        return featureFile;
    }
    
    public void setFeatureFile(String featureFile) {
        this.featureFile = featureFile;
    }
    
    // Helper methods
    public void addStep(String step) {
        this.steps.add(String.format("[%s] %s", LocalDateTime.now(), step));
    }
    
    public void addAttachment(String name, String path) {
        this.attachments.put(name, path);
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }
    
    public long calculateDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
    
    public boolean isPassed() {
        return status == Status.PASSED;
    }
    
    public boolean isFailed() {
        return status == Status.FAILED;
    }
    
    public boolean isSkipped() {
        return status == Status.SKIPPED;
    }
    
    public boolean isRunning() {
        return status == Status.RUNNING;
    }
    
    public String getFullName() {
        return className + "." + methodName;
    }
    
    public List<Screenshot> getScreenshots() {
        return screenshots;
    }
    
    public void setScreenshots(List<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }
    
    public void addScreenshot(String path, String name) {
        this.screenshots.add(new Screenshot(path, name, System.currentTimeMillis()));
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    
    public String getScenarioName() {
        return scenarioName;
    }
    
    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }
    
    @Override
    public String toString() {
        return String.format("CSTestResult{id='%s', name='%s', status=%s, duration=%dms}",
                testId, testName, status, duration);
    }
    
    /**
     * Inner class to represent a screenshot
     */
    public static class Screenshot {
        private String path;
        private final String name;
        private final long timestamp;
        
        public Screenshot(String path, String name, long timestamp) {
            this.path = path;
            this.name = name;
            this.timestamp = timestamp;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}