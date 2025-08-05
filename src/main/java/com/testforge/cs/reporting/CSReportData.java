package com.testforge.cs.reporting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data model for test execution report
 * Contains all test results and metadata for report generation
 */
public class CSReportData {
    private String reportName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    private List<CSTestResult> testResults;
    private Map<String, List<CSTestResult>> suites;
    private Map<String, String> environment;
    private String executionMode;
    private int totalCount;
    private int passedCount;
    private int failedCount;
    private int brokenCount;
    private int skippedCount;
    private double passRate;
    private int retriedCount;
    
    public CSReportData() {
        this.testResults = new ArrayList<>();
        this.suites = new HashMap<>();
        this.environment = new HashMap<>();
        this.startTime = LocalDateTime.now();
    }
    
    /**
     * Build report data from test results
     */
    public void buildFrom(List<CSTestResult> results) {
        this.testResults = new ArrayList<>(results);
        
        // Calculate counts
        this.totalCount = results.size();
        this.passedCount = (int) results.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.PASSED)
            .count();
        this.failedCount = (int) results.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.FAILED)
            .count();
        this.brokenCount = (int) results.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.BROKEN)
            .count();
        this.skippedCount = (int) results.stream()
            .filter(r -> r.getStatus() == CSTestResult.Status.SKIPPED)
            .count();
        
        // Calculate pass rate
        this.passRate = totalCount > 0 ? (passedCount * 100.0) / totalCount : 0;
        
        // Group by suite
        this.suites = results.stream()
            .collect(Collectors.groupingBy(r -> r.getSuiteName() != null ? r.getSuiteName() : "Default Suite"));
        
        // Calculate duration
        if (!results.isEmpty()) {
            this.startTime = results.stream()
                .map(CSTestResult::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            this.endTime = results.stream()
                .map(CSTestResult::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            this.duration = Duration.between(startTime, endTime);
        } else {
            this.endTime = LocalDateTime.now();
            this.duration = Duration.ZERO;
        }
    }
    
    // Getters and Setters
    public String getReportName() {
        return reportName;
    }
    
    public void setReportName(String reportName) {
        this.reportName = reportName;
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
    
    public Duration getDuration() {
        return duration;
    }
    
    public void setDuration(Duration duration) {
        this.duration = duration;
    }
    
    public List<CSTestResult> getTestResults() {
        return testResults;
    }
    
    public void setTestResults(List<CSTestResult> testResults) {
        this.testResults = testResults;
    }
    
    public Map<String, List<CSTestResult>> getSuites() {
        return suites;
    }
    
    public void setSuites(Map<String, List<CSTestResult>> suites) {
        this.suites = suites;
    }
    
    public Map<String, String> getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }
    
    public String getExecutionMode() {
        return executionMode;
    }
    
    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public int getPassedCount() {
        return passedCount;
    }
    
    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }
    
    public int getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    
    public int getBrokenCount() {
        return brokenCount;
    }
    
    public void setBrokenCount(int brokenCount) {
        this.brokenCount = brokenCount;
    }
    
    public int getSkippedCount() {
        return skippedCount;
    }
    
    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
    
    public double getPassRate() {
        return passRate;
    }
    
    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }
    
    public int getRetriedCount() {
        return retriedCount;
    }
    
    public void setRetriedCount(int retriedCount) {
        this.retriedCount = retriedCount;
    }
    
    // Additional convenience methods
    public int getTotalTests() {
        return totalCount;
    }
    
    public int getPassedTests() {
        return passedCount;
    }
    
    public int getFailedTests() {
        return failedCount;
    }
    
    public int getSkippedTests() {
        return skippedCount;
    }
    
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reportName", reportName);
        metadata.put("executionMode", executionMode);
        metadata.put("startTime", startTime);
        metadata.put("endTime", endTime);
        metadata.put("duration", duration != null ? duration.toMillis() : 0);
        metadata.put("environment", environment);
        return metadata;
    }
}