package com.testforge.cs.azuredevops;

/**
 * Represents an Azure DevOps build
 */
public class CSBuild {
    private final String id;
    private final String buildNumber;
    private final String status;
    private final String result;
    
    public CSBuild(String id, String buildNumber, String status, String result) {
        this.id = id;
        this.buildNumber = buildNumber;
        this.status = status;
        this.result = result;
    }
    
    public String getId() {
        return id;
    }
    
    public String getBuildNumber() {
        return buildNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getResult() {
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Build{id='%s', buildNumber='%s', status='%s', result='%s'}", 
            id, buildNumber, status, result);
    }
}