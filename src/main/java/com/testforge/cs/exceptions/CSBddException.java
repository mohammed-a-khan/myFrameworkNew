package com.testforge.cs.exceptions;

/**
 * Exception for BDD related errors
 */
public class CSBddException extends CSFrameworkException {
    
    private String feature;
    private String scenario;
    private String step;
    
    public CSBddException(String message) {
        super("BDD_ERROR", "BDD", message);
    }
    
    public CSBddException(String message, Throwable cause) {
        super("BDD_ERROR", "BDD", message, cause);
    }
    
    public CSBddException(String feature, String scenario, String step, String message) {
        super("BDD_ERROR", "BDD", message);
        this.feature = feature;
        this.scenario = scenario;
        this.step = step;
    }
    
    public CSBddException(String feature, String scenario, String step, String message, Throwable cause) {
        super("BDD_ERROR", "BDD", message, cause);
        this.feature = feature;
        this.scenario = scenario;
        this.step = step;
    }
    
    public String getFeature() {
        return feature;
    }
    
    public String getScenario() {
        return scenario;
    }
    
    public String getStep() {
        return step;
    }
}