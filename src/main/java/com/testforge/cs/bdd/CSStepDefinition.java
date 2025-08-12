package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSDataRow;
import com.testforge.cs.security.CSEncryptionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        GIVEN, WHEN, THEN, AND, BUT, ANY
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
        // If pattern already contains regex symbols, use it as-is (legacy support)
        if (pattern.contains("^") || pattern.contains("$") || pattern.contains("\\")) {
            return Pattern.compile(pattern);
        }
        
        // Convert placeholder-based pattern to regex
        String regex = pattern;
        
        // First, escape special regex characters (except our placeholders)
        regex = regex.replaceAll("([\\[\\]().*+?])", "\\\\$1");
        
        // Replace placeholders with regex groups
        // Support both quoted and unquoted parameters
        regex = regex
            .replaceAll("\\{string\\}", "\"([^\"]*)\"")      // Match quoted strings
            .replaceAll("\\{int\\}", "(\\d+)")                // Match integers
            .replaceAll("\\{float\\}", "(\\d+\\.\\d+)")       // Match floats
            .replaceAll("\\{number\\}", "(\\d+(?:\\.\\d+)?)")// Match int or float
            .replaceAll("\\{word\\}", "(\\w+)")               // Match single words
            .replaceAll("\\{(\\w+)\\}", "(?:\"([^\"]*)\"|([^\\s]+))"); // Named parameters - match quoted or unquoted
        
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
        
        // Count actual non-null groups (for named parameters with alternation)
        List<Object> extractedParams = new ArrayList<>();
        Class<?>[] paramTypes = method.getParameterTypes();
        int paramIndex = 0;
        
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String value = matcher.group(i);
            if (value != null) {
                // Skip if this is part of an alternation group and we already have a value
                if (i > 1 && matcher.group(i-1) != null && 
                    originalPattern.contains("{") && !originalPattern.contains("^")) {
                    continue;
                }
                
                if (paramIndex < paramTypes.length) {
                    extractedParams.add(convertParameter(value, paramTypes[paramIndex]));
                } else {
                    extractedParams.add(value);
                }
                paramIndex++;
            }
        }
        
        return extractedParams.toArray();
    }
    
    /**
     * Extract parameters with context support for data row injection
     */
    public Object[] extractParametersWithContext(String stepText, Map<String, Object> context) {
        var matcher = pattern.matcher(stepText);
        if (!matcher.matches()) {
            return new Object[0];
        }
        
        Parameter[] methodParams = method.getParameters();
        List<Object> extractedParams = new ArrayList<>();
        int textParamIndex = 0;
        
        // Process each method parameter
        for (int i = 0; i < methodParams.length; i++) {
            Parameter param = methodParams[i];
            
            // Check if this parameter should receive the data row
            if (param.isAnnotationPresent(CSDataRow.class)) {
                CSDataRow dataRowAnnotation = param.getAnnotation(CSDataRow.class);
                Object dataRow = context.get("dataRow");
                
                if (dataRow instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> fullDataRow = (Map<String, String>) dataRow;
                    
                    // Filter metadata if requested
                    if (!dataRowAnnotation.includeMetadata()) {
                        Map<String, String> filteredData = new HashMap<>();
                        for (Map.Entry<String, String> entry : fullDataRow.entrySet()) {
                            String key = entry.getKey();
                            // Skip metadata fields
                            if (!key.equals("dataSourceType") && 
                                !key.equals("dataSourceFile") && 
                                !key.equals("browser") && 
                                !key.equals("environment")) {
                                filteredData.put(key, entry.getValue());
                            }
                        }
                        extractedParams.add(filteredData);
                    } else {
                        extractedParams.add(fullDataRow);
                    }
                } else {
                    // No data row available, pass empty map
                    extractedParams.add(new HashMap<String, String>());
                }
            } else {
                // Regular parameter from step text
                String value = null;
                
                // Get value from regex groups
                for (int j = textParamIndex + 1; j <= matcher.groupCount(); j++) {
                    value = matcher.group(j);
                    if (value != null) {
                        textParamIndex = j;
                        break;
                    }
                }
                
                if (value != null) {
                    extractedParams.add(convertParameter(value, param.getType()));
                }
            }
        }
        
        return extractedParams.toArray();
    }
    
    /**
     * Convert string parameter to the appropriate type
     * Automatically decrypts encrypted values (wrapped in ENC())
     */
    private Object convertParameter(String value, Class<?> type) {
        // First check if the value is encrypted and decrypt it
        if (CSEncryptionUtils.isEncrypted(value)) {
            value = CSEncryptionUtils.decrypt(value);
        }
        
        // Then convert to the appropriate type
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