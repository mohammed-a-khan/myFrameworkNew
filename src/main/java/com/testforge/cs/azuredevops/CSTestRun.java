package com.testforge.cs.azuredevops;

/**
 * Represents an Azure DevOps test run
 */
public class CSTestRun {
    private final String id;
    private final String name;
    private final String state;
    private final String webAccessUrl;
    
    public CSTestRun(String id, String name, String state, String webAccessUrl) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.webAccessUrl = webAccessUrl;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getState() {
        return state;
    }
    
    public String getWebAccessUrl() {
        return webAccessUrl;
    }
    
    @Override
    public String toString() {
        return String.format("TestRun{id='%s', name='%s', state='%s'}", id, name, state);
    }
}