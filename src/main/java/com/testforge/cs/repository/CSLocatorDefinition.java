package com.testforge.cs.repository;

import java.util.Map;

/**
 * Locator definition for element identification
 */
public class CSLocatorDefinition {
    private String type;
    private String value;
    private int priority;
    private Map<String, String> parameters;
    private String description;
    private boolean dynamic;
    
    // Default constructor for JSON deserialization
    public CSLocatorDefinition() {}
    
    public CSLocatorDefinition(String type, String value) {
        this.type = type;
        this.value = value;
        this.priority = 1;
        this.dynamic = false;
    }
    
    public CSLocatorDefinition(String type, String value, int priority) {
        this.type = type;
        this.value = value;
        this.priority = priority;
        this.dynamic = false;
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isDynamic() {
        return dynamic;
    }
    
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }
    
    /**
     * Get parameter value
     */
    public String getParameter(String paramName) {
        return parameters != null ? parameters.get(paramName) : null;
    }
    
    /**
     * Check if locator is valid
     */
    public boolean isValid() {
        return type != null && !type.trim().isEmpty() && 
               value != null && !value.trim().isEmpty();
    }
    
    /**
     * Get locator as Selenium By
     */
    public org.openqa.selenium.By toSeleniumBy() {
        if (!isValid()) {
            throw new IllegalStateException("Invalid locator: " + this);
        }
        
        switch (type.toLowerCase()) {
            case "id":
                return org.openqa.selenium.By.id(value);
            case "name":
                return org.openqa.selenium.By.name(value);
            case "classname":
            case "class":
                return org.openqa.selenium.By.className(value);
            case "tagname":
            case "tag":
                return org.openqa.selenium.By.tagName(value);
            case "linktext":
                return org.openqa.selenium.By.linkText(value);
            case "partiallinktext":
                return org.openqa.selenium.By.partialLinkText(value);
            case "css":
            case "cssselector":
                return org.openqa.selenium.By.cssSelector(value);
            case "xpath":
                return org.openqa.selenium.By.xpath(value);
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + type);
        }
    }
    
    @Override
    public String toString() {
        return String.format("CSLocatorDefinition{type='%s', value='%s', priority=%d, dynamic=%s}", 
            type, value, priority, dynamic);
    }
}