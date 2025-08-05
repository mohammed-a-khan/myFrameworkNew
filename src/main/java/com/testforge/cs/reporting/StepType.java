package com.testforge.cs.reporting;

/**
 * Enum representing different types of test steps
 */
public enum StepType {
    ACTION("Action"),
    ASSERTION("Assertion"),
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Error"),
    DEBUG("Debug"),
    SCREENSHOT("Screenshot"),
    API_CALL("API Call"),
    DATABASE("Database"),
    VALIDATION("Validation");
    
    private final String displayName;
    
    StepType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}