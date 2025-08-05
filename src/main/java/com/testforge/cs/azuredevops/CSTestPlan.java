package com.testforge.cs.azuredevops;

/**
 * Represents an Azure DevOps test plan
 */
public class CSTestPlan {
    private final String id;
    private final String name;
    private final String state;
    
    public CSTestPlan(String id, String name, String state) {
        this.id = id;
        this.name = name;
        this.state = state;
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
    
    @Override
    public String toString() {
        return String.format("TestPlan{id='%s', name='%s', state='%s'}", id, name, state);
    }
}