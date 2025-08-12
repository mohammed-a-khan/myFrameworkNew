package com.testforge.cs.azuredevops.models;

import java.util.Date;
import java.util.List;

/**
 * Test Suite model for Azure DevOps
 */
public class CSTestSuite {
    private int id;
    private String name;
    private String url;
    private CSTestPlan plan;
    private CSTestSuite parentSuite;
    private String suiteType;
    private String state;
    private Date lastUpdatedDate;
    private String lastUpdatedBy;
    private int testCaseCount;
    private boolean hasChildren;
    private boolean isDeleted;
    private String revision;
    private List<CSTestPoint> testPoints;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public CSTestPlan getPlan() {
        return plan;
    }
    
    public void setPlan(CSTestPlan plan) {
        this.plan = plan;
    }
    
    public CSTestSuite getParentSuite() {
        return parentSuite;
    }
    
    public void setParentSuite(CSTestSuite parentSuite) {
        this.parentSuite = parentSuite;
    }
    
    public String getSuiteType() {
        return suiteType;
    }
    
    public void setSuiteType(String suiteType) {
        this.suiteType = suiteType;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }
    
    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
    
    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }
    
    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
    
    public int getTestCaseCount() {
        return testCaseCount;
    }
    
    public void setTestCaseCount(int testCaseCount) {
        this.testCaseCount = testCaseCount;
    }
    
    public boolean isHasChildren() {
        return hasChildren;
    }
    
    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public String getRevision() {
        return revision;
    }
    
    public void setRevision(String revision) {
        this.revision = revision;
    }
    
    public List<CSTestPoint> getTestPoints() {
        return testPoints;
    }
    
    public void setTestPoints(List<CSTestPoint> testPoints) {
        this.testPoints = testPoints;
    }
    
    // Nested class for test plan reference
    public static class CSTestPlan {
        private int id;
        private String name;
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}