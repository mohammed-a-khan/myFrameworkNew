package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSDataSource;
import com.testforge.cs.database.CSDatabaseManager;
import com.testforge.cs.database.CSDatabase;
import com.testforge.cs.database.CSQueryManager;
import com.testforge.cs.exceptions.CSBddException;
import com.testforge.cs.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Properties;

/**
 * Processes @CSDataSource annotations in BDD feature files
 * Handles loading data from Excel, CSV, JSON, and Database sources
 */
public class CSDataSourceProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CSDataSourceProcessor.class);
    private static final Pattern DATA_SOURCE_PATTERN = Pattern.compile("@CSDataSource\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*([^,;]+)");
    
    /**
     * Process data source annotation from scenario tags
     */
    public List<Map<String, String>> processDataSource(List<String> tags) {
        String dataSourceTag = findDataSourceTag(tags);
        if (dataSourceTag == null) {
            return Collections.emptyList();
        }
        
        Map<String, String> params = parseDataSourceParams(dataSourceTag);
        return loadDataFromSource(params);
    }
    
    /**
     * Process Examples configuration (JSON format)
     */
    public List<Map<String, String>> processExamplesConfig(String examplesConfig) {
        try {
            logger.debug("Processing Examples configuration: {}", examplesConfig);
            // Parse JSON configuration
            Map<String, String> params = parseJsonConfig(examplesConfig);
            logger.debug("Parsed params: {}", params);
            List<Map<String, String>> data = loadDataFromSource(params);
            logger.info("Loaded {} data rows from Examples configuration", data.size());
            return data;
        } catch (Exception e) {
            logger.error("Failed to process Examples configuration: {}", examplesConfig, e);
            throw new CSBddException("Invalid Examples configuration: " + examplesConfig, e);
        }
    }
    
    /**
     * Find @CSDataSource tag from list of tags
     */
    private String findDataSourceTag(List<String> tags) {
        for (String tag : tags) {
            if (tag.contains("@CSDataSource")) {
                return tag;
            }
        }
        return null;
    }
    
    /**
     * Parse data source parameters from annotation string
     */
    private Map<String, String> parseDataSourceParams(String dataSourceTag) {
        Map<String, String> params = new HashMap<>();
        
        Matcher matcher = DATA_SOURCE_PATTERN.matcher(dataSourceTag);
        if (!matcher.find()) {
            throw new CSBddException("Invalid @CSDataSource format: " + dataSourceTag);
        }
        
        String paramString = matcher.group(1);
        
        // Parse individual parameters
        Matcher paramMatcher = PARAM_PATTERN.matcher(paramString);
        while (paramMatcher.find()) {
            String key = paramMatcher.group(1).trim();
            String value = paramMatcher.group(2).trim();
            
            // Remove quotes if present
            if ((value.startsWith("\"") && value.endsWith("\"")) || 
                (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            
            params.put(key, value);
        }
        
        // Handle aliases
        if (params.containsKey("location") && !params.containsKey("source")) {
            params.put("source", params.get("location"));
        }
        if (params.containsKey("source") && !params.containsKey("path")) {
            params.put("path", params.get("source"));
        }
        
        return params;
    }
    
    /**
     * Parse JSON configuration from Examples
     */
    private Map<String, String> parseJsonConfig(String jsonConfig) {
        Map<String, String> params = new HashMap<>();
        
        // Parse JSON manually to avoid dependency issues
        // Remove outer braces and split by comma
        String content = jsonConfig.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }
        
        // Split by comma, but not within quotes
        List<String> pairs = splitJsonPairs(content);
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                params.put(key, value);
            }
        }
        
        // Handle aliases
        if (params.containsKey("location") && !params.containsKey("source")) {
            params.put("source", params.get("location"));
        }
        // Don't overwrite path if it already exists (it might be a JSON path expression)
        // The 'path' parameter in JSON Examples config is used for JSON path expressions like $.testData[*]
        
        return params;
    }
    
    /**
     * Split JSON pairs considering quotes
     */
    private List<String> splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int braceLevel = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i-1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') braceLevel++;
                else if (c == '}') braceLevel--;
                else if (c == ',' && braceLevel == 0) {
                    pairs.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            pairs.add(current.toString().trim());
        }
        
        return pairs;
    }
    
    /**
     * Load data from the specified source
     */
    private List<Map<String, String>> loadDataFromSource(Map<String, String> params) {
        String type = params.getOrDefault("type", determineTypeFromSource(params));
        
        logger.info("Loading data from {} source: {}", type, params);
        
        switch (type.toLowerCase()) {
            case "excel":
                return loadExcelData(params);
            case "csv":
                return loadCsvData(params);
            case "json":
                return loadJsonData(params);
            case "database":
                return loadDatabaseData(params);
            case "properties":
                return loadPropertiesData(params);
            default:
                throw new CSBddException("Unsupported data source type: " + type);
        }
    }
    
    /**
     * Determine type from source file extension if not specified
     */
    private String determineTypeFromSource(Map<String, String> params) {
        String source = params.getOrDefault("path", params.get("source"));
        if (source == null) {
            return "database"; // Default to database if no file path
        }
        
        if (source.endsWith(".xlsx") || source.endsWith(".xls")) {
            return "excel";
        } else if (source.endsWith(".csv")) {
            return "csv";
        } else if (source.endsWith(".json")) {
            return "json";
        }
        
        return "unknown";
    }
    
    /**
     * Load data from Excel file
     */
    private List<Map<String, String>> loadExcelData(Map<String, String> params) {
        String path = params.getOrDefault("source", params.get("path"));
        String sheet = params.get("sheet");
        String key = params.getOrDefault("key", params.get("keyField"));
        String filter = params.get("filter");
        
        if (path == null) {
            throw new CSBddException("Excel data source requires 'source' or 'path' parameter");
        }
        
        // Resolve path relative to project root if not absolute
        if (!path.startsWith("/")) {
            path = path;
        }
        
        List<Map<String, String>> data;
        
        // Try CSV fallback first since Excel reading might not be implemented
        String csvPath = path.replace(".xlsx", ".csv").replace(".xls", ".csv");
        java.io.File csvFile = new java.io.File(csvPath);
        java.io.File xlsxFile = new java.io.File(path);
        
        if (csvFile.exists()) {
            logger.info("Using CSV file as fallback for Excel: {}", csvPath);
            data = CSCsvUtils.readCsv(csvPath, true);
        } else if (xlsxFile.exists()) {
            try {
                if (sheet != null) {
                    data = CSExcelUtils.readExcel(path, sheet, true);
                } else {
                    data = CSExcelUtils.readExcel(path);
                }
            } catch (Exception e) {
                logger.warn("Failed to read Excel file, trying CSV fallback", e);
                data = new ArrayList<>();
            }
        } else {
            logger.warn("Neither Excel file {} nor CSV fallback {} found", path, csvPath);
            data = new ArrayList<>();
        }
        
        // Apply key filter if specified
        if (key != null && params.containsKey("keyValues")) {
            String keyValues = params.get("keyValues");
            Set<String> allowedValues = new HashSet<>(Arrays.asList(keyValues.split(",")));
            data = data.stream()
                .filter(row -> allowedValues.contains(row.get(key)))
                .collect(Collectors.toList());
        }
        
        // Apply additional filters
        if (filter != null) {
            data = applyFilters(data, filter);
        }
        
        return data;
    }
    
    /**
     * Load data from CSV file
     */
    private List<Map<String, String>> loadCsvData(Map<String, String> params) {
        String path = params.getOrDefault("source", params.get("path"));
        String key = params.getOrDefault("key", params.get("keyField"));
        String filter = params.get("filter");
        boolean hasHeader = Boolean.parseBoolean(params.getOrDefault("hasHeader", "true"));
        
        if (path == null) {
            throw new CSBddException("CSV data source requires 'source' or 'path' parameter");
        }
        
        // Don't prefix path - let it be relative to project root
        
        List<Map<String, String>> data = CSCsvUtils.readCsv(path, hasHeader);
        logger.info("CSV file {} loaded {} data rows", path, data.size());
        
        // Apply key filter if specified
        if (key != null && params.containsKey("keyValues")) {
            String keyValues = params.get("keyValues");
            Set<String> allowedValues = new HashSet<>(Arrays.asList(keyValues.split(",")));
            data = data.stream()
                .filter(row -> allowedValues.contains(row.get(key)))
                .collect(Collectors.toList());
        }
        
        // Apply additional filters
        if (filter != null) {
            data = applyFilters(data, filter);
        }
        
        return data;
    }
    
    /**
     * Load data from JSON file
     */
    private List<Map<String, String>> loadJsonData(Map<String, String> params) {
        String sourcePath = params.getOrDefault("source", params.get("path"));
        String jsonPath = params.get("path"); // JSON path expression like $.testData[*]
        String key = params.getOrDefault("key", params.get("keyField"));
        String filter = params.get("filter");
        
        if (sourcePath == null) {
            throw new CSBddException("JSON data source requires 'source' parameter");
        }
        
        // Don't prefix path - let it be relative to project root
        
        String jsonContent = CSFileUtils.readTextFile(sourcePath);
        List<Map<String, Object>> jsonData;
        
        // Check if we have a JSON path expression
        if (jsonPath != null && jsonPath.startsWith("$")) {
            logger.debug("Processing JSON with path expression: {}", jsonPath);
            // Extract data using JSON path
            try {
                // Parse the entire JSON
                Map<String, Object> rootObject = CSJsonUtils.jsonToMap(jsonContent);
                logger.debug("Root JSON object keys: {}", rootObject.keySet());
                
                // Extract the array from the path (simplified for $.testData[*])
                if (jsonPath.equals("$.testData[*]") && rootObject.containsKey("testData")) {
                    Object testDataObj = rootObject.get("testData");
                    if (testDataObj instanceof List) {
                        jsonData = new ArrayList<>();
                        for (Object item : (List<?>) testDataObj) {
                            if (item instanceof Map) {
                                jsonData.add((Map<String, Object>) item);
                            }
                        }
                        logger.debug("Extracted {} records from JSON path {}", jsonData.size(), jsonPath);
                    } else {
                        throw new CSBddException("JSON path " + jsonPath + " does not point to an array");
                    }
                } else {
                    // For other paths, fall back to trying to parse as array
                    throw new CSBddException("Unsupported JSON path: " + jsonPath);
                }
            } catch (Exception e) {
                logger.error("Failed to extract data using JSON path: {}", jsonPath, e);
                throw new CSBddException("Failed to process JSON with path: " + jsonPath, e);
            }
        } else {
            // No JSON path, try to parse as array at root level
            try {
                jsonData = CSJsonUtils.jsonToListOfMaps(jsonContent);
            } catch (Exception e) {
                // If not array, try as single object
                Map<String, Object> singleObject = CSJsonUtils.jsonToMap(jsonContent);
                jsonData = Collections.singletonList(singleObject);
            }
        }
        
        // Convert to Map<String, String>
        List<Map<String, String>> data = new ArrayList<>();
        for (Map<String, Object> jsonRow : jsonData) {
            Map<String, String> stringRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : jsonRow.entrySet()) {
                stringRow.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            data.add(stringRow);
        }
        
        // Apply key filter if specified
        if (key != null && params.containsKey("keyValues")) {
            String keyValues = params.get("keyValues");
            Set<String> allowedValues = new HashSet<>(Arrays.asList(keyValues.split(",")));
            data = data.stream()
                .filter(row -> allowedValues.contains(row.get(key)))
                .collect(Collectors.toList());
        }
        
        // Apply additional filters
        if (filter != null) {
            data = applyFilters(data, filter);
        }
        
        logger.debug("Returning {} data rows from JSON source", data.size());
        return data;
    }
    
    /**
     * Load data from database
     */
    private List<Map<String, String>> loadDatabaseData(Map<String, String> params) {
        String databaseName = params.getOrDefault("name", params.getOrDefault("database", "default"));
        String query = params.get("query");
        String queryKey = params.get("queryKey");
        
        if (query == null && queryKey == null) {
            throw new CSBddException("Database data source requires either 'query' or 'queryKey' parameter");
        }
        
        // Get query from query manager if using queryKey
        if (queryKey != null) {
            query = CSQueryManager.getInstance().getQuery(queryKey);
        }
        
        // Parse query parameters
        Map<String, Object> queryParams = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("param.")) {
                String paramName = entry.getKey().substring(6);
                queryParams.put(paramName, entry.getValue());
            }
        }
        
        // Execute query - convert Map params to array for CSDbUtils
        List<Map<String, Object>> results;
        if (!queryParams.isEmpty()) {
            // If query has named parameters, we need to replace them with ? and create ordered params
            // For now, use CSDatabase directly
            CSDatabaseManager dbManager = CSDatabaseManager.getInstance();
            CSDatabase database = dbManager.getDatabase(databaseName);
            results = database.query(query, queryParams);
        } else {
            // For queries without parameters, use CSDbUtils
            results = CSDbUtils.executeQuery(databaseName, query);
        }
        
        // Convert to Map<String, String>
        List<Map<String, String>> data = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Map<String, String> stringRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                stringRow.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
            data.add(stringRow);
        }
        
        return data;
    }
    
    /**
     * Load data from properties file
     */
    private List<Map<String, String>> loadPropertiesData(Map<String, String> params) {
        String path = params.getOrDefault("source", params.get("path"));
        String key = params.getOrDefault("key", params.get("keyField"));
        
        if (path == null) {
            throw new CSBddException("Properties data source requires 'source' or 'path' parameter");
        }
        
        // Don't prefix path - let it be relative to project root
        
        List<Map<String, String>> data = new ArrayList<>();
        Properties props = new Properties();
        
        // Load properties file
        try (java.io.InputStream is = new java.io.FileInputStream(path)) {
            props.load(is);
        } catch (java.io.IOException e) {
            throw new CSBddException("Failed to load properties file: " + path, e);
        }
        
        // Group properties by key prefix if specified
        if (key != null) {
            Set<String> processedKeys = new HashSet<>();
            
            for (String propKey : props.stringPropertyNames()) {
                if (propKey.startsWith(key + "=") || propKey.equals(key)) {
                    String keyValue = props.getProperty(propKey);
                    
                    // Find all properties with same key value prefix
                    Map<String, String> row = new HashMap<>();
                    String prefix = key + "=" + keyValue + ".";
                    
                    for (String pk : props.stringPropertyNames()) {
                        if (pk.startsWith(prefix)) {
                            String fieldName = pk.substring(prefix.length());
                            row.put(fieldName, props.getProperty(pk));
                            processedKeys.add(pk);
                        } else if (pk.equals(key)) {
                            // Add the key itself
                            row.put(key, keyValue);
                        }
                    }
                    
                    // Also check for properties without the key prefix
                    for (String pk : props.stringPropertyNames()) {
                        if (!processedKeys.contains(pk) && !pk.contains("=")) {
                            row.put(pk, props.getProperty(pk));
                        }
                    }
                    
                    if (!row.isEmpty()) {
                        data.add(row);
                    }
                }
            }
        } else {
            // If no key specified, treat entire properties file as one data row
            Map<String, String> row = new HashMap<>();
            for (String propKey : props.stringPropertyNames()) {
                row.put(propKey, props.getProperty(propKey));
            }
            data.add(row);
        }
        
        return data;
    }
    
    /**
     * Apply filters to data
     * Format: "Field1=Value1;Field2=Value2" or "Field1:Value1;Field2:Value2"
     */
    private List<Map<String, String>> applyFilters(List<Map<String, String>> data, String filterString) {
        if (filterString == null || filterString.trim().isEmpty()) {
            return data;
        }
        
        // Parse filters
        Map<String, String> filters = new HashMap<>();
        String[] filterPairs = filterString.split(";");
        
        for (String filterPair : filterPairs) {
            String[] parts = filterPair.split("[:=]");
            if (parts.length == 2) {
                filters.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        // Apply filters
        return data.stream()
            .filter(row -> {
                for (Map.Entry<String, String> filter : filters.entrySet()) {
                    String fieldValue = row.get(filter.getKey());
                    if (fieldValue == null || !fieldValue.equals(filter.getValue())) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Replace placeholders in step text with data values
     */
    public String replacePlaceholders(String stepText, Map<String, String> data) {
        String result = stepText;
        
        // Replace <ColumnName> placeholders
        Pattern placeholderPattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = placeholderPattern.matcher(stepText);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = data.getOrDefault(placeholder, "<" + placeholder + ">");
            result = result.replace("<" + placeholder + ">", value);
        }
        
        return result;
    }
}