package com.testforge.cs.azuredevops.models;

import java.util.Date;
import java.util.Map;

/**
 * Test Point model for Azure DevOps
 */
public class CSTestPoint {
    private int id;
    private String url;
    private CSTestCase testCase;
    private CSAssignedTo assignedTo;
    private boolean automated;
    private CSConfiguration configuration;
    private CSTestRun lastTestRun;
    private CSTestResult lastResult;
    private String outcome;
    private String state;
    private String lastResultState;
    private CSLastResultDetails lastResultDetails;
    private String lastRunBuildNumber;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public CSTestCase getTestCase() {
        return testCase;
    }
    
    public void setTestCase(CSTestCase testCase) {
        this.testCase = testCase;
    }
    
    public CSAssignedTo getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(CSAssignedTo assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public boolean isAutomated() {
        return automated;
    }
    
    public void setAutomated(boolean automated) {
        this.automated = automated;
    }
    
    public CSConfiguration getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(CSConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public CSTestRun getLastTestRun() {
        return lastTestRun;
    }
    
    public void setLastTestRun(CSTestRun lastTestRun) {
        this.lastTestRun = lastTestRun;
    }
    
    public CSTestResult getLastResult() {
        return lastResult;
    }
    
    public void setLastResult(CSTestResult lastResult) {
        this.lastResult = lastResult;
    }
    
    public String getOutcome() {
        return outcome;
    }
    
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getLastResultState() {
        return lastResultState;
    }
    
    public void setLastResultState(String lastResultState) {
        this.lastResultState = lastResultState;
    }
    
    public CSLastResultDetails getLastResultDetails() {
        return lastResultDetails;
    }
    
    public void setLastResultDetails(CSLastResultDetails lastResultDetails) {
        this.lastResultDetails = lastResultDetails;
    }
    
    public String getLastRunBuildNumber() {
        return lastRunBuildNumber;
    }
    
    public void setLastRunBuildNumber(String lastRunBuildNumber) {
        this.lastRunBuildNumber = lastRunBuildNumber;
    }
    
    // Nested classes
    
    public static class CSTestCase {
        private String id;
        private String name;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class CSAssignedTo {
        private String id;
        private String displayName;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
    
    public static class CSConfiguration {
        private String id;
        private String name;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class CSTestRun {
        private String id;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
    }
    
    public static class CSTestResult {
        private String id;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
    }
    
    public static class CSLastResultDetails {
        private Long duration;
        private Date dateCompleted;
        private CSAssignedTo runBy;
        
        public Long getDuration() {
            return duration;
        }
        
        public void setDuration(Long duration) {
            this.duration = duration;
        }
        
        public Date getDateCompleted() {
            return dateCompleted;
        }
        
        public void setDateCompleted(Date dateCompleted) {
            this.dateCompleted = dateCompleted;
        }
        
        public CSAssignedTo getRunBy() {
            return runBy;
        }
        
        public void setRunBy(CSAssignedTo runBy) {
            this.runBy = runBy;
        }
    }
}