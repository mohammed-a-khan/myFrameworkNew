package com.testforge.cs.repository;

import java.util.List;
import java.util.Map;

/**
 * Test data set definition for repository management
 */
public class CSTestDataSet {
    private String name;
    private String description;
    private String version;
    private List<Map<String, Object>> data;
    private Map<String, String> metadata;
    private List<String> tags;
    private String dataSource;
    private String format;
    
    // Default constructor for JSON deserialization
    public CSTestDataSet() {}
    
    public CSTestDataSet(String name, String description, List<Map<String, Object>> data) {
        this.name = name;
        this.description = description;
        this.data = data;
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
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<Map<String, Object>> getData() {
        return data;
    }
    
    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Get data row by index
     */
    public Map<String, Object> getDataRow(int index) {
        if (data == null || index < 0 || index >= data.size()) {
            return null;
        }
        return data.get(index);
    }
    
    /**
     * Get data row count
     */
    public int getDataRowCount() {
        return data != null ? data.size() : 0;
    }
    
    /**
     * Check if has specific tag
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    /**
     * Get metadata value
     */
    public String getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    @Override
    public String toString() {
        return String.format("CSTestDataSet{name='%s', rows=%d, version='%s'}", 
            name, getDataRowCount(), version);
    }
}