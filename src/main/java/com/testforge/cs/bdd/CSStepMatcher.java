package com.testforge.cs.bdd;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches step descriptions to step definitions
 * Supports parameter placeholders like {string}, {int}, {float}, etc.
 */
public class CSStepMatcher {
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{(\\w+)\\}");
    
    /**
     * Convert step description to regex pattern
     */
    public static Pattern descriptionToPattern(String description) {
        String regex = description;
        
        // Escape special regex characters except for parameter placeholders
        regex = regex.replaceAll("([\\^\\$\\.\\|\\?\\*\\+\\(\\)\\[\\]\\\\])", "\\\\$1");
        
        // Replace parameter placeholders with regex groups
        regex = regex.replaceAll("\\{string\\}", "\"([^\"]+)\"");
        regex = regex.replaceAll("\\{int\\}", "(\\d+)");
        regex = regex.replaceAll("\\{float\\}", "(\\d+\\.\\d+)");
        regex = regex.replaceAll("\\{number\\}", "(\\d+(?:\\.\\d+)?)");
        regex = regex.replaceAll("\\{word\\}", "(\\w+)");
        regex = regex.replaceAll("\\{.*?\\}", "(.+)");
        
        return Pattern.compile("^" + regex + "$");
    }
    
    /**
     * Check if step text matches description
     */
    public static boolean matches(String stepText, String description) {
        Pattern pattern = descriptionToPattern(description);
        return pattern.matcher(stepText).matches();
    }
    
    /**
     * Extract parameters from step text
     */
    public static Object[] extractParameters(String stepText, String description, Class<?>[] paramTypes) {
        Pattern pattern = descriptionToPattern(description);
        Matcher matcher = pattern.matcher(stepText);
        
        if (!matcher.matches()) {
            return new Object[0];
        }
        
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < matcher.groupCount(); i++) {
            String value = matcher.group(i + 1);
            
            if (i < paramTypes.length) {
                params.add(convertParameter(value, paramTypes[i]));
            } else {
                params.add(value);
            }
        }
        
        return params.toArray();
    }
    
    /**
     * Convert string parameter to appropriate type
     */
    private static Object convertParameter(String value, Class<?> type) {
        try {
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
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type.getSimpleName());
        }
        
        return value;
    }
    
    /**
     * Get parameter count from description
     */
    public static int getParameterCount(String description) {
        Matcher matcher = PARAMETER_PATTERN.matcher(description);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}