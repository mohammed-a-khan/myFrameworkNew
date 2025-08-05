package com.testforge.cs.repository;

import java.util.List;
import java.util.Map;

/**
 * Element definition for repository management
 */
public class CSElementDefinition {
    private String name;
    private String description;
    private List<CSLocatorDefinition> locators;
    private String elementType;
    private Map<String, String> attributes;
    private List<String> tags;
    private String waitCondition;
    private int timeoutSeconds;
    private boolean optional;
    private String validation;
    
    // Default constructor for JSON deserialization
    public CSElementDefinition() {}
    
    public CSElementDefinition(String name, String description, List<CSLocatorDefinition> locators) {
        this.name = name;
        this.description = description;
        this.locators = locators;
        this.timeoutSeconds = 10; // Default timeout
        this.optional = false;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<CSLocatorDefinition> getLocators() {
        return locators;
    }
    
    public void setLocators(List<CSLocatorDefinition> locators) {
        this.locators = locators;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getWaitCondition() {
        return waitCondition;
    }
    
    public void setWaitCondition(String waitCondition) {
        this.waitCondition = waitCondition;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    public String getValidation() {
        return validation;
    }
    
    public void setValidation(String validation) {
        this.validation = validation;
    }
    
    /**
     * Get primary locator (first in the list)
     */
    public CSLocatorDefinition getPrimaryLocator() {
        return locators != null && !locators.isEmpty() ? locators.get(0) : null;
    }
    
    /**
     * Get locator by type
     */
    public CSLocatorDefinition getLocatorByType(String type) {
        if (locators == null) {
            return null;
        }
        
        return locators.stream()
            .filter(locator -> locator.getType().equalsIgnoreCase(type))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if element has specific tag
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    /**
     * Get attribute value
     */
    public String getAttributeValue(String attributeName) {
        return attributes != null ? attributes.get(attributeName) : null;
    }
    
    @Override
    public String toString() {
        return String.format("CSElementDefinition{name='%s', type='%s', locators=%d, optional=%s}", 
            name, elementType, locators != null ? locators.size() : 0, optional);
    }
}