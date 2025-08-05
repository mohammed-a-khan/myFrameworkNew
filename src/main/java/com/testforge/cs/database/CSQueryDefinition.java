package com.testforge.cs.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Query definition class for centralized query management
 */
public class CSQueryDefinition {
    private String name;
    private String sql;
    private String description;
    private String category;
    private List<String> tags;
    private Map<String, Object> parameterDefinitions;
    private String filePath;
    private LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;
    private String author;
    private String version;
    private QueryType queryType;
    private boolean cacheable;
    private long cacheExpirationMs;
    
    /**
     * Query types
     */
    public enum QueryType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        DDL,
        PROCEDURE,
        FUNCTION
    }
    
    /**
     * Default constructor
     */
    public CSQueryDefinition() {
        this.createdTime = LocalDateTime.now();
        this.lastModifiedTime = LocalDateTime.now();
        this.cacheable = true;
        this.cacheExpirationMs = 300000; // 5 minutes default
    }
    
    /**
     * Constructor with name and SQL
     */
    public CSQueryDefinition(String name, String sql) {
        this();
        this.name = name;
        this.sql = sql;
        this.queryType = determineQueryType(sql);
    }
    
    /**
     * Constructor with all basic fields
     */
    public CSQueryDefinition(String name, String sql, String description, String category) {
        this(name, sql);
        this.description = description;
        this.category = category;
    }
    
    /**
     * Determine query type from SQL
     */
    private QueryType determineQueryType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return QueryType.SELECT;
        }
        
        String upperSQL = sql.trim().toUpperCase();
        
        if (upperSQL.startsWith("SELECT")) {
            return QueryType.SELECT;
        } else if (upperSQL.startsWith("INSERT")) {
            return QueryType.INSERT;
        } else if (upperSQL.startsWith("UPDATE")) {
            return QueryType.UPDATE;
        } else if (upperSQL.startsWith("DELETE")) {
            return QueryType.DELETE;
        } else if (upperSQL.startsWith("CREATE") || upperSQL.startsWith("ALTER") || 
                   upperSQL.startsWith("DROP") || upperSQL.startsWith("TRUNCATE")) {
            return QueryType.DDL;
        } else if (upperSQL.startsWith("CALL") || upperSQL.startsWith("EXEC")) {
            return QueryType.PROCEDURE;
        } else {
            return QueryType.SELECT; // Default
        }
    }
    
    // Getters and setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        updateLastModified();
    }
    
    public String getSql() {
        return sql;
    }
    
    public void setSql(String sql) {
        this.sql = sql;
        this.queryType = determineQueryType(sql);
        updateLastModified();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        updateLastModified();
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
        updateLastModified();
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
        updateLastModified();
    }
    
    public Map<String, Object> getParameterDefinitions() {
        return parameterDefinitions;
    }
    
    public void setParameterDefinitions(Map<String, Object> parameterDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
        updateLastModified();
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
        updateLastModified();
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
        updateLastModified();
    }
    
    public QueryType getQueryType() {
        return queryType;
    }
    
    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
        updateLastModified();
    }
    
    public boolean isCacheable() {
        return cacheable;
    }
    
    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        updateLastModified();
    }
    
    public long getCacheExpirationMs() {
        return cacheExpirationMs;
    }
    
    public void setCacheExpirationMs(long cacheExpirationMs) {
        this.cacheExpirationMs = cacheExpirationMs;
        updateLastModified();
    }
    
    /**
     * Update last modified time
     */
    private void updateLastModified() {
        this.lastModifiedTime = LocalDateTime.now();
    }
    
    /**
     * Check if query has specific tag
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    /**
     * Get parameter definition for specific parameter
     */
    public Object getParameterDefinition(String parameterName) {
        return parameterDefinitions != null ? parameterDefinitions.get(parameterName) : null;
    }
    
    /**
     * Check if query is a SELECT query
     */
    public boolean isSelectQuery() {
        return queryType == QueryType.SELECT;
    }
    
    /**
     * Check if query is a modification query (INSERT/UPDATE/DELETE)
     */
    public boolean isModificationQuery() {
        return queryType == QueryType.INSERT || 
               queryType == QueryType.UPDATE || 
               queryType == QueryType.DELETE;
    }
    
    /**
     * Check if query is a DDL query
     */
    public boolean isDDLQuery() {
        return queryType == QueryType.DDL;
    }
    
    /**
     * Validate query definition
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               sql != null && !sql.trim().isEmpty();
    }
    
    /**
     * Get formatted SQL for display
     */
    public String getFormattedSql() {
        if (sql == null) {
            return "";
        }
        
        // Basic SQL formatting
        return sql.replaceAll("\\s+", " ")
                 .replaceAll(",\\s*", ",\n    ")
                 .replaceAll("\\bFROM\\b", "\nFROM")
                 .replaceAll("\\bWHERE\\b", "\nWHERE")
                 .replaceAll("\\bJOIN\\b", "\nJOIN")
                 .replaceAll("\\bLEFT JOIN\\b", "\nLEFT JOIN")
                 .replaceAll("\\bRIGHT JOIN\\b", "\nRIGHT JOIN")
                 .replaceAll("\\bINNER JOIN\\b", "\nINNER JOIN")
                 .replaceAll("\\bORDER BY\\b", "\nORDER BY")
                 .replaceAll("\\bGROUP BY\\b", "\nGROUP BY")
                 .replaceAll("\\bHAVING\\b", "\nHAVING")
                 .trim();
    }
    
    /**
     * Clone query definition
     */
    public CSQueryDefinition clone() {
        CSQueryDefinition cloned = new CSQueryDefinition();
        cloned.name = this.name;
        cloned.sql = this.sql;
        cloned.description = this.description;
        cloned.category = this.category;
        cloned.tags = this.tags != null ? java.util.List.copyOf(this.tags) : null;
        cloned.parameterDefinitions = this.parameterDefinitions != null ? 
            new java.util.HashMap<>(this.parameterDefinitions) : null;
        cloned.filePath = this.filePath;
        cloned.createdTime = this.createdTime;
        cloned.lastModifiedTime = this.lastModifiedTime;
        cloned.author = this.author;
        cloned.version = this.version;
        cloned.queryType = this.queryType;
        cloned.cacheable = this.cacheable;
        cloned.cacheExpirationMs = this.cacheExpirationMs;
        
        return cloned;
    }
    
    @Override
    public String toString() {
        return String.format("CSQueryDefinition{name='%s', type=%s, category='%s', cacheable=%s}", 
            name, queryType, category, cacheable);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CSQueryDefinition other = (CSQueryDefinition) obj;
        return java.util.Objects.equals(name, other.name) &&
               java.util.Objects.equals(sql, other.sql);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, sql);
    }
}