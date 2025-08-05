package com.testforge.cs.repository;

import java.util.List;
import java.util.Map;

/**
 * Page object definition for repository management
 */
public class CSPageObjectDefinition {
    private String name;
    private String description;
    private String url;
    private String title;
    private List<CSElementDefinition> elements;
    private Map<String, String> properties;
    private List<String> tags;
    private String version;
    private String author;
    private String lastModified;
    
    // Default constructor for JSON deserialization
    public CSPageObjectDefinition() {}
    
    public CSPageObjectDefinition(String name, String description, List<CSElementDefinition> elements) {
        this.name = name;
        this.description = description;
        this.elements = elements;
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
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<CSElementDefinition> getElements() {
        return elements;
    }
    
    public void setElements(List<CSElementDefinition> elements) {
        this.elements = elements;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * Find element by name
     */
    public CSElementDefinition findElement(String elementName) {
        if (elements == null) {
            return null;
        }
        
        return elements.stream()
            .filter(element -> element.getName().equals(elementName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if element exists
     */
    public boolean hasElement(String elementName) {
        return findElement(elementName) != null;
    }
    
    @Override
    public String toString() {
        return String.format("CSPageObjectDefinition{name='%s', elements=%d, url='%s'}", 
            name, elements != null ? elements.size() : 0, url);
    }
}