package com.testforge.cs.reporting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a BDD step with all its actions
 */
public class CSStepReport {
    private String stepType; // GIVEN, WHEN, THEN, AND, BUT
    private String stepText;
    private String status; // PASSED, FAILED, SKIPPED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;
    private List<CSStepAction> actions;
    private String error;
    private String stackTrace;
    private Map<String, Object> metadata;
    
    public CSStepReport(String stepType, String stepText) {
        this.stepType = stepType;
        this.stepText = stepText;
        this.status = "RUNNING";
        this.startTime = LocalDateTime.now();
        this.actions = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public void addAction(CSStepAction action) {
        this.actions.add(action);
    }
    
    public void complete() {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.Duration.between(startTime, endTime).toMillis();
        
        // Determine status based on actions
        boolean hasFailure = actions.stream().anyMatch(action -> !action.isPassed());
        this.status = hasFailure ? "FAILED" : "PASSED";
    }
    
    public void fail(String error, String stackTrace) {
        this.status = "FAILED";
        this.error = error;
        this.stackTrace = stackTrace;
        complete();
    }
    
    public void skip() {
        this.status = "SKIPPED";
        complete();
    }
    
    // Getters and Setters
    public String getStepType() {
        return stepType;
    }
    
    public void setStepType(String stepType) {
        this.stepType = stepType;
    }
    
    public String getStepText() {
        return stepText;
    }
    
    public void setStepText(String stepText) {
        this.stepText = stepText;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
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
    
    public List<CSStepAction> getActions() {
        return actions;
    }
    
    public void setActions(List<CSStepAction> actions) {
        this.actions = actions;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Convert to Map for JSON serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("stepType", stepType);
        map.put("stepText", stepText);
        map.put("status", status);
        map.put("startTime", startTime != null ? startTime.toString() : null);
        map.put("endTime", endTime != null ? endTime.toString() : null);
        map.put("duration", duration);
        map.put("error", error);
        map.put("stackTrace", stackTrace);
        map.put("metadata", metadata);
        
        // Convert actions to maps
        List<Map<String, Object>> actionMaps = new ArrayList<>();
        for (CSStepAction action : actions) {
            actionMaps.add(action.toMap());
        }
        map.put("actions", actionMaps);
        
        return map;
    }
}