package com.testforge.cs.database;

import com.testforge.cs.exceptions.CSDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced query template manager with conditional logic and dynamic SQL generation
 */
public class CSQueryTemplateManager {
    private static final Logger logger = LoggerFactory.getLogger(CSQueryTemplateManager.class);
    
    private static volatile CSQueryTemplateManager instance;
    private static final Object instanceLock = new Object();
    
    // Template cache
    private final Map<String, QueryTemplate> templateCache = new ConcurrentHashMap<>();
    
    // Template patterns
    private static final Pattern IF_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);
    private static final Pattern UNLESS_PATTERN = Pattern.compile("\\{\\{#unless\\s+([^}]+)\\}\\}(.*?)\\{\\{/unless\\}\\}", Pattern.DOTALL);
    private static final Pattern EACH_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Get singleton instance
     */
    public static CSQueryTemplateManager getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSQueryTemplateManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private CSQueryTemplateManager() {
        initializeCommonTemplates();
    }
    
    /**
     * Initialize common query templates
     */
    private void initializeCommonTemplates() {
        // Dynamic WHERE clause template
        addTemplate("dynamicWhere", 
            "{{#if conditions}}WHERE {{#each conditions}}{{column}} {{operator}} {{value}}{{#unless @last}} AND {{/unless}}{{/each}}{{/if}}");
        
        // Dynamic ORDER BY template
        addTemplate("dynamicOrderBy",
            "{{#if sortFields}}ORDER BY {{#each sortFields}}{{field}} {{direction}}{{#unless @last}}, {{/unless}}{{/each}}{{/if}}");
        
        // Pagination template
        addTemplate("pagination",
            "{{#if pageSize}}LIMIT {{pageSize}}{{#if offset}} OFFSET {{offset}}{{/if}}{{/if}}");
        
        // Dynamic SELECT fields template
        addTemplate("selectFields",
            "SELECT {{#if fields}}{{#each fields}}{{.}}{{#unless @last}}, {{/unless}}{{/each}}{{#unless fields}}*{{/unless}}{{/if}}");
        
        // Dynamic JOIN template
        addTemplate("dynamicJoins",
            "{{#each joins}}{{type}} JOIN {{table}} ON {{condition}}{{/each}}");
        
        logger.info("Initialized {} common query templates", templateCache.size());
    }
    
    /**
     * Add query template
     */
    public void addTemplate(String name, String template) {
        templateCache.put(name, new QueryTemplate(name, template));
        logger.debug("Added query template: {}", name);
    }
    
    /**
     * Get query template
     */
    public QueryTemplate getTemplate(String name) {
        return templateCache.get(name);
    }
    
    /**
     * Process template with data
     */
    public String processTemplate(String templateName, Map<String, Object> data) {
        QueryTemplate template = getTemplate(templateName);
        if (template == null) {
            throw new CSDataException("Template not found: " + templateName);
        }
        
        return processTemplateContent(template.getContent(), data);
    }
    
    /**
     * Process template content with data
     */
    private String processTemplateContent(String templateContent, Map<String, Object> data) {
        if (templateContent == null || templateContent.trim().isEmpty()) {
            return "";
        }
        
        String processed = templateContent;
        
        // Process conditional blocks (if/unless)
        processed = processConditionals(processed, data);
        
        // Process loops (each)
        processed = processLoops(processed, data);
        
        // Process variables
        processed = processVariables(processed, data);
        
        return processed.trim();
    }
    
    /**
     * Build dynamic query using templates
     */
    public String buildDynamicQuery(String baseQuery, Map<String, Object> templateData) {
        StringBuilder queryBuilder = new StringBuilder(baseQuery);
        
        // Replace template placeholders with processed templates
        for (Map.Entry<String, Object> entry : templateData.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            
            if (queryBuilder.toString().contains(placeholder)) {
                String replacement = "";
                
                // Check if it's a template reference
                if (entry.getValue() instanceof String && templateCache.containsKey((String) entry.getValue())) {
                    replacement = processTemplate((String) entry.getValue(), templateData);
                } else if (entry.getValue() instanceof Map) {
                    // Process inline template
                    @SuppressWarnings("unchecked")
                    Map<String, Object> templateMap = (Map<String, Object>) entry.getValue();
                    String templateContent = (String) templateMap.get("template");
                    if (templateContent != null) {
                        replacement = processTemplateContent(templateContent, templateData);
                    }
                } else {
                    replacement = String.valueOf(entry.getValue());
                }
                
                queryBuilder = new StringBuilder(queryBuilder.toString().replace(placeholder, replacement));
            }
        }
        
        return queryBuilder.toString();
    }
    
    /**
     * Process conditional blocks ({{#if}} and {{#unless}})
     */
    private String processConditionals(String content, Map<String, Object> data) {
        String processed = content;
        
        // Process #if blocks
        Matcher ifMatcher = IF_PATTERN.matcher(processed);
        while (ifMatcher.find()) {
            String condition = ifMatcher.group(1).trim();
            String block = ifMatcher.group(2);
            
            boolean conditionResult = evaluateCondition(condition, data);
            String replacement = conditionResult ? block : "";
            
            processed = processed.replace(ifMatcher.group(0), replacement);
            ifMatcher = IF_PATTERN.matcher(processed);
        }
        
        // Process #unless blocks
        Matcher unlessMatcher = UNLESS_PATTERN.matcher(processed);
        while (unlessMatcher.find()) {
            String condition = unlessMatcher.group(1).trim();
            String block = unlessMatcher.group(2);
            
            boolean conditionResult = evaluateCondition(condition, data);
            String replacement = !conditionResult ? block : "";
            
            processed = processed.replace(unlessMatcher.group(0), replacement);
            unlessMatcher = UNLESS_PATTERN.matcher(processed);
        }
        
        return processed;
    }
    
    /**
     * Process loop blocks ({{#each}})
     */
    private String processLoops(String content, Map<String, Object> data) {
        String processed = content;
        
        Matcher eachMatcher = EACH_PATTERN.matcher(processed);
        while (eachMatcher.find()) {
            String arrayName = eachMatcher.group(1).trim();
            String blockTemplate = eachMatcher.group(2);
            
            Object arrayValue = getNestedValue(arrayName, data);
            StringBuilder loopResult = new StringBuilder();
            
            if (arrayValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) arrayValue;
                
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    Map<String, Object> itemData = new HashMap<>(data);
                    
                    // Add loop context variables
                    itemData.put("@index", i);
                    itemData.put("@first", i == 0);
                    itemData.put("@last", i == list.size() - 1);
                    itemData.put("@length", list.size());
                    
                    // Add item data
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        itemData.putAll(itemMap);
                    } else {
                        itemData.put(".", item);
                    }
                    
                    String processedBlock = processTemplateContent(blockTemplate, itemData);
                    loopResult.append(processedBlock);
                }
            }
            
            processed = processed.replace(eachMatcher.group(0), loopResult.toString());
            eachMatcher = EACH_PATTERN.matcher(processed);
        }
        
        return processed;
    }
    
    /**
     * Process variables ({{variable}})
     */
    private String processVariables(String content, Map<String, Object> data) {
        String processed = content;
        
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(processed);
        while (variableMatcher.find()) {
            String variable = variableMatcher.group(1).trim();
            
            // Skip if it's a block helper
            if (variable.startsWith("#") || variable.startsWith("/")) {
                continue;
            }
            
            Object value = getNestedValue(variable, data);
            String replacement = value != null ? String.valueOf(value) : "";
            
            processed = processed.replace(variableMatcher.group(0), replacement);
        }
        
        return processed;
    }
    
    /**
     * Evaluate condition
     */
    private boolean evaluateCondition(String condition, Map<String, Object> data) {
        condition = condition.trim();
        
        // Handle negation
        if (condition.startsWith("!")) {
            return !evaluateCondition(condition.substring(1).trim(), data);
        }
        
        // Handle comparison operators
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            Object left = getNestedValue(parts[0].trim(), data);
            Object right = parseValue(parts[1].trim(), data);
            return Objects.equals(left, right);
        }
        
        if (condition.contains("!=")) {
            String[] parts = condition.split("!=", 2);
            Object left = getNestedValue(parts[0].trim(), data);
            Object right = parseValue(parts[1].trim(), data);
            return !Objects.equals(left, right);
        }
        
        // Simple truthiness check
        Object value = getNestedValue(condition, data);
        
        if (value == null) {
            return false;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value instanceof String) {
            return !((String) value).trim().isEmpty();
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        
        return true;
    }
    
    /**
     * Get nested value from data map using dot notation
     */
    private Object getNestedValue(String path, Map<String, Object> data) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Parse value (string, number, or variable reference)
     */
    private Object parseValue(String valueStr, Map<String, Object> data) {
        valueStr = valueStr.trim();
        
        // String literal
        if ((valueStr.startsWith("'") && valueStr.endsWith("'")) ||
            (valueStr.startsWith("\"") && valueStr.endsWith("\""))) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        
        // Number literal
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            // Not a number, treat as variable
        }
        
        // Boolean literal
        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }
        
        // Variable reference
        return getNestedValue(valueStr, data);
    }
    
    /**
     * Query template class
     */
    public static class QueryTemplate {
        private final String name;
        private final String content;
        private final long createdTime;
        
        public QueryTemplate(String name, String content) {
            this.name = name;
            this.content = content;
            this.createdTime = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public String getContent() { return content; }
        public long getCreatedTime() { return createdTime; }
        
        @Override
        public String toString() {
            return String.format("QueryTemplate{name='%s', length=%d}", name, content.length());
        }
    }
    
    /**
     * Get all template names
     */
    public Set<String> getTemplateNames() {
        return new HashSet<>(templateCache.keySet());
    }
    
    /**
     * Remove template
     */
    public void removeTemplate(String name) {
        templateCache.remove(name);
        logger.debug("Removed query template: {}", name);
    }
    
    /**
     * Clear all templates
     */
    public void clearTemplates() {
        templateCache.clear();
        logger.debug("Cleared all query templates");
    }
}