package com.testforge.cs.reporting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a test suite with its results
 */
public class CSTestSuite {
    private final String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<CSTestResult> tests = new ArrayList<>();
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    
    public CSTestSuite(String name) {
        this.name = name;
        this.startTime = LocalDateTime.now();
    }
    
    public String getName() {
        return name;
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
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
    
    public List<CSTestResult> getTests() {
        return new ArrayList<>(tests);
    }
    
    public void addTest(CSTestResult test) {
        tests.add(test);
    }
    
    public int getTotalTests() {
        return tests.size();
    }
    
    public int getPassedTests() {
        return (int) tests.stream().filter(CSTestResult::isPassed).count();
    }
    
    public int getFailedTests() {
        return (int) tests.stream().filter(CSTestResult::isFailed).count();
    }
    
    public int getSkippedTests() {
        return (int) tests.stream().filter(CSTestResult::isSkipped).count();
    }
    
    public double getPassRate() {
        return tests.isEmpty() ? 0 : (getPassedTests() * 100.0 / tests.size());
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}