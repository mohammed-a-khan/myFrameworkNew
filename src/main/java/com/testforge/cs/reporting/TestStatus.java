package com.testforge.cs.reporting;

/**
 * Enum representing test execution status
 */
public enum TestStatus {
    PASSED("Passed", "✓", "#4CAF50"),
    FAILED("Failed", "✗", "#F44336"),
    SKIPPED("Skipped", "⊘", "#FF9800"),
    PENDING("Pending", "◯", "#9E9E9E"),
    BROKEN("Broken", "!", "#E91E63"),
    UNKNOWN("Unknown", "?", "#607D8B");
    
    private final String displayName;
    private final String symbol;
    private final String color;
    
    TestStatus(String displayName, String symbol, String color) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getColor() {
        return color;
    }
    
    public boolean isSuccessful() {
        return this == PASSED;
    }
    
    public boolean isFailure() {
        return this == FAILED || this == BROKEN;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}