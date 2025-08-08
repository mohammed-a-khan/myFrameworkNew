package com.testforge.cs.reporting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an action performed within a step
 */
public class CSStepAction {
    private String actionType;
    private String description;
    private String target;
    private String value;
    private boolean passed;
    private String error;
    private LocalDateTime timestamp;
    private long duration;
    private String screenshot;
    private Map<String, Object> metadata;
    
    public CSStepAction(String actionType, String description) {
        this.actionType = actionType;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.passed = true;
        this.metadata = new HashMap<>();
    }
    
    public CSStepAction(String actionType, String description, String target) {
        this(actionType, description);
        this.target = target;
    }
    
    public CSStepAction(String actionType, String description, String target, String value) {
        this(actionType, description, target);
        this.value = value;
    }
    
    // Getters and Setters
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
        this.passed = false;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public String getScreenshot() {
        return screenshot;
    }
    
    public void setScreenshot(String screenshot) {
        this.screenshot = screenshot;
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
        map.put("actionType", actionType);
        map.put("description", description);
        map.put("target", target);
        map.put("value", value);
        map.put("passed", passed);
        map.put("error", error);
        map.put("timestamp", timestamp != null ? timestamp.toString() : null);
        map.put("duration", duration);
        map.put("screenshot", screenshot);
        map.put("metadata", metadata);
        return map;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s %s %s - %s", 
            actionType, 
            description, 
            target != null ? "on " + target : "", 
            value != null ? "with value '" + value + "'" : "",
            passed ? "PASSED" : "FAILED: " + error);
    }
}