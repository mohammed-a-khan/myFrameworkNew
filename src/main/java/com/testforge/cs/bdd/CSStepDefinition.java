package com.testforge.cs.bdd;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Represents a step definition with pattern matching and method binding
 */
public class CSStepDefinition {
    private final Pattern pattern;
    private final Method method;
    private final Object instance;
    private final StepType stepType;
    private final String originalPattern;
    
    public enum StepType {
        GIVEN, WHEN, THEN, AND, BUT
    }
    
    public CSStepDefinition(String pattern, Method method, Object instance, StepType stepType) {
        this.originalPattern = pattern;
        this.pattern = compilePattern(pattern);
        this.method = method;
        this.instance = instance;
        this.stepType = stepType;
    }
    
    /**
     * Compile the step pattern with parameter placeholders
     */
    private Pattern compilePattern(String pattern) {
        // Replace parameter placeholders with regex groups
        String regex = pattern
            .replaceAll("\\{string\\}", "\"([^\"]*)\"") // Match quoted strings
            .replaceAll("\\{int\\}", "(\\d+)")           // Match integers
            .replaceAll("\\{float\\}", "(\\d+\\.\\d+)")  // Match floats
            .replaceAll("\\{word\\}", "(\\w+)")           // Match single words
            .replaceAll("\\{.*?\\}", "(.*?)");           // Match any other placeholders
        
        return Pattern.compile("^" + regex + "$");
    }
    
    /**
     * Check if the step text matches this definition
     */
    public boolean matches(String stepText) {
        return pattern.matcher(stepText).matches();
    }
    
    /**
     * Extract parameters from step text
     */
    public Object[] extractParameters(String stepText) {
        var matcher = pattern.matcher(stepText);
        if (!matcher.matches()) {
            return new Object[0];
        }
        
        Object[] params = new Object[matcher.groupCount()];
        Class<?>[] paramTypes = method.getParameterTypes();
        
        for (int i = 0; i < matcher.groupCount(); i++) {
            String value = matcher.group(i + 1);
            if (i < paramTypes.length) {
                params[i] = convertParameter(value, paramTypes[i]);
            } else {
                params[i] = value;
            }
        }
        
        return params;
    }
    
    /**
     * Convert string parameter to the appropriate type
     */
    private Object convertParameter(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        } else if (type == Float.class || type == float.class) {
            return Float.parseFloat(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }
    
    /**
     * Execute the step with given parameters
     */
    public void execute(Object[] parameters) throws Exception {
        method.setAccessible(true);
        method.invoke(instance, parameters);
    }
    
    // Getters
    public Pattern getPattern() {
        return pattern;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Object getInstance() {
        return instance;
    }
    
    public StepType getStepType() {
        return stepType;
    }
    
    public String getOriginalPattern() {
        return originalPattern;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s -> %s.%s", 
            stepType, originalPattern, 
            instance.getClass().getSimpleName(), 
            method.getName());
    }
}