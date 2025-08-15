package com.testforge.cs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Property mapper for CS TestForge Framework
 * Maps only the two exception properties that don't use cs. prefix:
 * - environment.name
 * - browser.name
 * All other properties should use cs.* prefix directly
 */
public class CSPropertyMapper {
    private static final Logger logger = LoggerFactory.getLogger(CSPropertyMapper.class);
    private static final Map<String, String> PROPERTY_MAPPING = new HashMap<>();
    
    static {
        initializePropertyMappings();
    }
    
    /**
     * Initialize property mappings - only for the two exception properties
     */
    private static void initializePropertyMappings() {
        // Only these two properties are allowed without cs. prefix
        // They map to cs.* prefixed versions internally
        PROPERTY_MAPPING.put("environment.name", "cs.environment.name");
        PROPERTY_MAPPING.put("browser.name", "cs.browser.name");
        
        // No other mappings needed - all other properties must use cs.* prefix directly
    }
    
    /**
     * Get the standardized property name for a given property
     * @param property The property name
     * @return The standardized property name
     */
    public static String getStandardizedProperty(String property) {
        if (property == null) {
            return null;
        }
        
        // If already starts with cs., return as is
        if (property.startsWith("cs.")) {
            return property;
        }
        
        // Check if it's one of the two exception properties
        String mappedProperty = PROPERTY_MAPPING.get(property);
        if (mappedProperty != null) {
            logger.debug("Mapped exception property '{}' to '{}'", property, mappedProperty);
            return mappedProperty;
        }
        
        // For any other property without cs. prefix, log a warning
        if (!property.equals("environment.name") && !property.equals("browser.name")) {
            logger.warn("Property '{}' should use 'cs.' prefix. Only 'environment.name' and 'browser.name' are allowed without prefix.", property);
        }
        
        // Return original property
        return property;
    }
    
    /**
     * Apply property mappings to a Properties object
     * @param properties The properties to process
     * @return A new Properties object with mapped property names
     */
    public static Properties applyMappings(Properties properties) {
        Properties mappedProperties = new Properties();
        
        // First, copy all existing properties
        mappedProperties.putAll(properties);
        
        // Then apply mappings for the two exception properties only
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String mappedKey = getStandardizedProperty(key);
            
            if (!key.equals(mappedKey)) {
                // If the new property doesn't exist, add it
                if (!mappedProperties.containsKey(mappedKey)) {
                    mappedProperties.setProperty(mappedKey, entry.getValue().toString());
                    logger.trace("Added mapped property: {} -> {}", key, mappedKey);
                }
            }
        }
        
        return mappedProperties;
    }
    
    /**
     * Check if a property is one of the allowed exceptions
     * @param property The property to check
     * @return true if the property is allowed without cs. prefix
     */
    public static boolean isAllowedException(String property) {
        return "environment.name".equals(property) || "browser.name".equals(property);
    }
    
    /**
     * Log a warning for properties that should use cs. prefix
     * @param property The property name
     */
    public static void logInvalidPropertyWarning(String property) {
        if (property != null && !property.startsWith("cs.") && !isAllowedException(property)) {
            logger.warn("Property '{}' must use 'cs.' prefix. Only 'environment.name' and 'browser.name' are exceptions.", property);
        }
    }
}