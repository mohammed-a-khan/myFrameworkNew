package com.testforge.cs.database;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSDataException;
import com.testforge.cs.utils.CSFileUtils;
import com.testforge.cs.utils.CSStringUtils;
import com.testforge.cs.utils.CSJsonUtils;
import com.testforge.cs.utils.CSDbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Production-ready SQL query manager for centralized query management
 * Supports query templates, parameterization, caching, and performance monitoring
 */
public class CSQueryManager {
    private static final Logger logger = LoggerFactory.getLogger(CSQueryManager.class);
    
    private static volatile CSQueryManager instance;
    private static final Object instanceLock = new Object();
    
    // Query storage and caching
    private final Map<String, CSQueryDefinition> queryDefinitions = new ConcurrentHashMap<>();
    private final Map<String, PreparedStatement> preparedStatementCache = new ConcurrentHashMap<>();
    private final Map<String, QueryExecutionStats> executionStats = new ConcurrentHashMap<>();
    
    // Configuration
    private CSConfigManager config;
    private String queryFilesDirectory;
    private boolean cacheEnabled;
    private int maxCacheSize;
    private long cacheExpirationMs;
    private boolean performanceMonitoringEnabled;
    
    // Query parameter pattern for replacement
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile(":(\\w+)");
    
    /**
     * Get singleton instance
     */
    public static CSQueryManager getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSQueryManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton
     */
    private CSQueryManager() {
        initialize();
    }
    
    /**
     * Initialize query manager
     */
    private void initialize() {
        try {
            config = CSConfigManager.getInstance();
            
            // Load configuration
            queryFilesDirectory = config.getProperty("cs.queries.directory", "src/main/resources/queries");
            cacheEnabled = Boolean.parseBoolean(config.getProperty("cs.queries.cache.enabled", "true"));
            maxCacheSize = Integer.parseInt(config.getProperty("cs.queries.cache.max.size", "100"));
            cacheExpirationMs = Long.parseLong(config.getProperty("cs.queries.cache.expiration.ms", "300000")); // 5 minutes
            performanceMonitoringEnabled = Boolean.parseBoolean(config.getProperty("cs.queries.performance.monitoring", "true"));
            
            // Load query definitions
            loadQueryDefinitions();
            
            logger.info("CSQueryManager initialized - Queries: {}, Cache: {}, Monitoring: {}", 
                queryDefinitions.size(), cacheEnabled, performanceMonitoringEnabled);
            
        } catch (Exception e) {
            logger.error("Failed to initialize CSQueryManager", e);
            throw new CSDataException("Failed to initialize query manager", e);
        }
    }
    
    /**
     * Load query definitions from files
     */
    private void loadQueryDefinitions() {
        Path queryDir = Paths.get(queryFilesDirectory);
        
        if (!Files.exists(queryDir)) {
            logger.warn("Query directory does not exist: {}", queryFilesDirectory);
            return;
        }
        
        try {
            // Load .sql files
            try (Stream<Path> paths = Files.walk(queryDir)) {
                paths.filter(path -> path.toString().endsWith(".sql"))
                     .forEach(this::loadQueryFile);
            }
            
            // Load .json query definitions
            try (Stream<Path> paths = Files.walk(queryDir)) {
                paths.filter(path -> path.toString().endsWith(".json"))
                     .forEach(this::loadQueryDefinitionFile);
            }
            
        } catch (IOException e) {
            logger.error("Error loading query definitions from: {}", queryFilesDirectory, e);
        }
    }
    
    /**
     * Load individual SQL file
     */
    private void loadQueryFile(Path sqlFile) {
        try {
            String content = Files.readString(sqlFile);
            String fileName = sqlFile.getFileName().toString();
            String queryName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // Parse multiple queries from single file if separated by semicolon
            String[] queries = content.split(";\\s*--\\s*QUERY:\\s*");
            
            if (queries.length == 1) {
                // Single query file
                CSQueryDefinition definition = new CSQueryDefinition(queryName, content.trim());
                definition.setFilePath(sqlFile.toString());
                queryDefinitions.put(queryName, definition);
                
            } else {
                // Multiple queries in file
                for (int i = 1; i < queries.length; i++) {
                    String[] parts = queries[i].split("\\n", 2);
                    if (parts.length >= 2) {
                        String specificQueryName = parts[0].trim();
                        String queryContent = parts[1].trim();
                        
                        CSQueryDefinition definition = new CSQueryDefinition(specificQueryName, queryContent);
                        definition.setFilePath(sqlFile.toString());
                        queryDefinitions.put(specificQueryName, definition);
                    }
                }
            }
            
            logger.debug("Loaded queries from file: {}", sqlFile);
            
        } catch (IOException e) {
            logger.error("Failed to load query file: {}", sqlFile, e);
        }
    }
    
    /**
     * Load query definition from JSON file
     */
    private void loadQueryDefinitionFile(Path jsonFile) {
        try {
            String content = Files.readString(jsonFile);
            Map<String, Object> definitions = CSJsonUtils.jsonToMap(content);
            
            for (Map.Entry<String, Object> entry : definitions.entrySet()) {
                String queryName = entry.getKey();
                
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> queryDef = (Map<String, Object>) entry.getValue();
                    
                    String sql = (String) queryDef.get("sql");
                    String description = (String) queryDef.get("description");
                    String category = (String) queryDef.get("category");
                    
                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) queryDef.get("tags");
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) queryDef.get("parameters");
                    
                    CSQueryDefinition definition = new CSQueryDefinition(queryName, sql);
                    definition.setDescription(description);
                    definition.setCategory(category);
                    definition.setTags(tags);
                    definition.setParameterDefinitions(parameters);
                    definition.setFilePath(jsonFile.toString());
                    
                    queryDefinitions.put(queryName, definition);
                }
            }
            
            logger.debug("Loaded query definitions from JSON file: {}", jsonFile);
            
        } catch (Exception e) {
            logger.error("Failed to load query definition file: {}", jsonFile, e);
        }
    }
    
    /**
     * Get query by key
     */
    public String getQuery(String queryKey) {
        CSQueryDefinition definition = queryDefinitions.get(queryKey);
        if (definition == null) {
            throw new CSDataException("Query not found: " + queryKey);
        }
        return definition.getSql();
    }
    
    /**
     * Execute query and return results
     */
    public List<Map<String, Object>> executeQuery(String queryName, Map<String, Object> parameters) {
        CSQueryDefinition definition = getQueryDefinition(queryName);
        if (definition == null) {
            throw new CSDataException("Query not found: " + queryName);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Prepare SQL with parameters
            String sql = prepareSQL(definition.getSql(), parameters);
            
            // Execute query using default database
            List<Map<String, Object>> results = CSDbUtils.executeQuery(sql, convertParametersToObjects(parameters));
            
            // Record execution stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, results.size(), true);
            }
            
            logger.debug("Executed query '{}' - {} rows returned in {}ms", 
                queryName, results.size(), System.currentTimeMillis() - startTime);
            
            return results;
            
        } catch (Exception e) {
            // Record failure stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, 0, false);
            }
            
            logger.error("Failed to execute query: {}", queryName, e);
            throw new CSDataException("Failed to execute query: " + queryName, e);
        }
    }
    
    /**
     * Execute query and return single result
     */
    public Map<String, Object> executeQuerySingle(String queryName, Map<String, Object> parameters) {
        List<Map<String, Object>> results = executeQuery(queryName, parameters);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Execute query and return specific column values
     */
    public <T> List<T> executeQueryColumn(String queryName, String columnName, Class<T> columnType, Map<String, Object> parameters) {
        List<Map<String, Object>> results = executeQuery(queryName, parameters);
        List<T> columnValues = new ArrayList<>();
        
        for (Map<String, Object> row : results) {
            Object value = row.get(columnName);
            if (value != null && columnType.isInstance(value)) {
                columnValues.add(columnType.cast(value));
            } else if (value != null) {
                // Try to convert value
                T convertedValue = convertValue(value, columnType);
                if (convertedValue != null) {
                    columnValues.add(convertedValue);
                }
            }
        }
        
        return columnValues;
    }
    
    /**
     * Execute update/insert/delete query
     */
    public int executeUpdate(String queryName, Map<String, Object> parameters) {
        CSQueryDefinition definition = getQueryDefinition(queryName);
        if (definition == null) {
            throw new CSDataException("Query not found: " + queryName);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Prepare SQL with parameters
            String sql = prepareSQL(definition.getSql(), parameters);
            
            // Execute update using default database
            int rowsAffected = CSDbUtils.executeUpdate(sql, convertParametersToObjects(parameters));
            
            // Record execution stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, rowsAffected, true);
            }
            
            logger.debug("Executed update query '{}' - {} rows affected in {}ms", 
                queryName, rowsAffected, System.currentTimeMillis() - startTime);
            
            return rowsAffected;
            
        } catch (Exception e) {
            // Record failure stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, 0, false);
            }
            
            logger.error("Failed to execute update query: {}", queryName, e);
            throw new CSDataException("Failed to execute update query: " + queryName, e);
        }
    }
    
    /**
     * Execute batch operations
     */
    public int[] executeBatch(String queryName, List<Map<String, Object>> batchParameters) {
        CSQueryDefinition definition = getQueryDefinition(queryName);
        if (definition == null) {
            throw new CSDataException("Query not found: " + queryName);
        }
        
        if (batchParameters == null || batchParameters.isEmpty()) {
            return new int[0];
        }
        
        long startTime = System.currentTimeMillis();
        
        // Use CSDbUtils for batch execution (simplified approach)
        String sql = definition.getSql();
        List<String> parameterNames = extractNamedParameters(sql);
        
        try {
            int totalAffected = 0;
            int[] results = new int[batchParameters.size()];
            
            for (int i = 0; i < batchParameters.size(); i++) {
                Map<String, Object> paramMap = batchParameters.get(i);
                Object[] params = new Object[parameterNames.size()];
                for (int j = 0; j < parameterNames.size(); j++) {
                    params[j] = paramMap.get(parameterNames.get(j));
                }
                
                try {
                    int affected = CSDbUtils.executeUpdate(sql, params);
                    results[i] = affected;
                    totalAffected += affected;
                } catch (Exception e) {
                    results[i] = -1; // Error indicator
                    logger.error("Batch execution failed for row {}", i, e);
                }
            }
                
            // Record execution stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, totalAffected, true);
            }
            
            logger.debug("Executed batch query '{}' - {} batches, {} total rows affected in {}ms", 
                queryName, results.length, totalAffected, System.currentTimeMillis() - startTime);
            
            return results;
            
        } catch (Exception e) {
            // Record failure stats
            if (performanceMonitoringEnabled) {
                recordExecutionStats(queryName, System.currentTimeMillis() - startTime, 0, false);
            }
            
            logger.error("Failed to execute batch query: {}", queryName, e);
            throw new CSDataException("Failed to execute batch query: " + queryName, e);
        }
    }
    
    /**
     * Get query definition
     */
    public CSQueryDefinition getQueryDefinition(String queryName) {
        return queryDefinitions.get(queryName);
    }
    
    /**
     * Get all query names
     */
    public Set<String> getQueryNames() {
        return new HashSet<>(queryDefinitions.keySet());
    }
    
    /**
     * Get queries by category
     */
    public List<CSQueryDefinition> getQueriesByCategory(String category) {
        return queryDefinitions.values().stream()
            .filter(def -> category.equals(def.getCategory()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get queries by tag
     */
    public List<CSQueryDefinition> getQueriesByTag(String tag) {
        return queryDefinitions.values().stream()
            .filter(def -> def.getTags() != null && def.getTags().contains(tag))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Add or update query definition
     */
    public void addQueryDefinition(CSQueryDefinition definition) {
        if (definition == null || definition.getName() == null || definition.getSql() == null) {
            throw new IllegalArgumentException("Invalid query definition");
        }
        
        queryDefinitions.put(definition.getName(), definition);
        logger.debug("Added/updated query definition: {}", definition.getName());
    }
    
    /**
     * Remove query definition
     */
    public void removeQueryDefinition(String queryName) {
        queryDefinitions.remove(queryName);
        preparedStatementCache.remove(queryName);
        executionStats.remove(queryName);
        logger.debug("Removed query definition: {}", queryName);
    }
    
    /**
     * Reload all query definitions
     */
    public void reloadQueryDefinitions() {
        queryDefinitions.clear();
        preparedStatementCache.clear();
        loadQueryDefinitions();
        logger.info("Reloaded all query definitions - {} queries loaded", queryDefinitions.size());
    }
    
    /**
     * Get execution statistics
     */
    public QueryExecutionStats getExecutionStats(String queryName) {
        return executionStats.get(queryName);
    }
    
    /**
     * Get all execution statistics
     */
    public Map<String, QueryExecutionStats> getAllExecutionStats() {
        return new HashMap<>(executionStats);
    }
    
    /**
     * Clear execution statistics
     */
    public void clearExecutionStats() {
        executionStats.clear();
        logger.debug("Cleared all execution statistics");
    }
    
    /**
     * Prepare SQL by replacing parameters
     */
    private String prepareSQL(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        String preparedSQL = sql;
        
        // Replace ${parameter} style parameters
        Matcher matcher = PARAMETER_PATTERN.matcher(preparedSQL);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = parameters.get(paramName);
            if (value != null) {
                preparedSQL = preparedSQL.replace("${" + paramName + "}", String.valueOf(value));
            }
        }
        
        return preparedSQL;
    }
    
    /**
     * Extract named parameters from SQL
     */
    private List<String> extractNamedParameters(String sql) {
        List<String> parameters = new ArrayList<>();
        Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(sql);
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!parameters.contains(paramName)) {
                parameters.add(paramName);
            }
        }
        
        return parameters;
    }
    
    /**
     * Convert parameter map to Object array for CSDbUtils
     */
    private Object[] convertParametersToObjects(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return new Object[0];
        }
        return parameters.values().toArray();
    }
    
    /**
     * Convert value to target type
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> targetType) {
        try {
            if (targetType == String.class) {
                return (T) String.valueOf(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(value.toString());
            } else if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(value.toString());
            } else if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(value.toString());
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return (T) Boolean.valueOf(value.toString());
            }
        } catch (Exception e) {
            logger.warn("Failed to convert value {} to type {}", value, targetType.getSimpleName());
        }
        
        return null;
    }
    
    /**
     * Record execution statistics
     */
    private void recordExecutionStats(String queryName, long executionTimeMs, int rowsAffected, boolean success) {
        executionStats.compute(queryName, (key, stats) -> {
            if (stats == null) {
                stats = new QueryExecutionStats(queryName);
            }
            
            stats.recordExecution(executionTimeMs, rowsAffected, success);
            return stats;
        });
    }
    
    /**
     * Query execution statistics
     */
    public static class QueryExecutionStats {
        private final String queryName;
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private long totalExecutionTimeMs;
        private long minExecutionTimeMs = Long.MAX_VALUE;
        private long maxExecutionTimeMs = 0;
        private long totalRowsAffected;
        private long lastExecutionTime;
        
        public QueryExecutionStats(String queryName) {
            this.queryName = queryName;
        }
        
        public synchronized void recordExecution(long executionTimeMs, int rowsAffected, boolean success) {
            totalExecutions++;
            
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }
            
            totalExecutionTimeMs += executionTimeMs;
            minExecutionTimeMs = Math.min(minExecutionTimeMs, executionTimeMs);
            maxExecutionTimeMs = Math.max(maxExecutionTimeMs, executionTimeMs);
            totalRowsAffected += rowsAffected;
            lastExecutionTime = System.currentTimeMillis();
        }
        
        // Getters
        public String getQueryName() { return queryName; }
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public double getSuccessRate() { 
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0; 
        }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
        public long getMinExecutionTimeMs() { return minExecutionTimeMs == Long.MAX_VALUE ? 0 : minExecutionTimeMs; }
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        public double getAverageExecutionTimeMs() { 
            return totalExecutions > 0 ? (double) totalExecutionTimeMs / totalExecutions : 0; 
        }
        public long getTotalRowsAffected() { return totalRowsAffected; }
        public double getAverageRowsAffected() { 
            return totalExecutions > 0 ? (double) totalRowsAffected / totalExecutions : 0; 
        }
        public long getLastExecutionTime() { return lastExecutionTime; }
        
        @Override
        public String toString() {
            return String.format("QueryStats{name='%s', executions=%d, success=%.1f%%, avgTime=%.1fms}", 
                queryName, totalExecutions, getSuccessRate(), getAverageExecutionTimeMs());
        }
    }
}