package com.testforge.cs.bdd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed Gherkin feature file
 */
public class CSFeatureFile {
    
    private String name;
    private String description;
    private List<String> tags;
    private Background background;
    private List<Scenario> scenarios;
    private String sourcePath;
    
    public CSFeatureFile() {
        this.tags = new ArrayList<>();
        this.scenarios = new ArrayList<>();
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
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Background getBackground() {
        return background;
    }
    
    public void setBackground(Background background) {
        this.background = background;
    }
    
    public List<Scenario> getScenarios() {
        return scenarios;
    }
    
    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }
    
    public void addScenario(Scenario scenario) {
        this.scenarios.add(scenario);
    }
    
    public String getSourcePath() {
        return sourcePath;
    }
    
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
    
    /**
     * Represents a Background section
     */
    public static class Background {
        private String name;
        private List<Step> steps;
        
        public Background() {
            this.steps = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<Step> getSteps() {
            return steps;
        }
        
        public void setSteps(List<Step> steps) {
            this.steps = steps;
        }
        
        public void addStep(Step step) {
            this.steps.add(step);
        }
    }
    
    /**
     * Represents a Scenario or Scenario Outline
     */
    public static class Scenario {
        private String name;
        private String description;
        private List<String> tags;
        private List<Step> steps;
        private Examples examples;
        private boolean isOutline;
        private Map<String, String> dataRow;
        private String examplesConfig;
        
        public Scenario() {
            this.tags = new ArrayList<>();
            this.steps = new ArrayList<>();
            this.isOutline = false;
        }
        
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
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public List<Step> getSteps() {
            return steps;
        }
        
        public void setSteps(List<Step> steps) {
            this.steps = steps;
        }
        
        public void addStep(Step step) {
            this.steps.add(step);
        }
        
        public Examples getExamples() {
            return examples;
        }
        
        public void setExamples(Examples examples) {
            this.examples = examples;
        }
        
        public boolean isOutline() {
            return isOutline;
        }
        
        public void setOutline(boolean outline) {
            isOutline = outline;
        }
        
        public Map<String, String> getDataRow() {
            return dataRow;
        }
        
        public void setDataRow(Map<String, String> dataRow) {
            this.dataRow = dataRow;
        }
        
        public String getExamplesConfig() {
            return examplesConfig;
        }
        
        public void setExamplesConfig(String examplesConfig) {
            this.examplesConfig = examplesConfig;
        }
    }
    
    /**
     * Represents a Step
     */
    public static class Step {
        private String keyword;
        private String text;
        private List<List<String>> dataTable;
        private String docString;
        private int lineNumber;
        
        public Step() {
            this.dataTable = new ArrayList<>();
        }
        
        public String getKeyword() {
            return keyword;
        }
        
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public List<List<String>> getDataTable() {
            return dataTable;
        }
        
        public void setDataTable(List<List<String>> dataTable) {
            this.dataTable = dataTable;
        }
        
        public String getDocString() {
            return docString;
        }
        
        public void setDocString(String docString) {
            this.docString = docString;
        }
        
        public boolean hasDataTable() {
            return dataTable != null && !dataTable.isEmpty();
        }
        
        public boolean hasDocString() {
            return docString != null && !docString.isEmpty();
        }
        
        public int getLineNumber() {
            return lineNumber;
        }
        
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }
    
    /**
     * Represents Examples section for Scenario Outline
     */
    public static class Examples {
        private String name;
        private List<String> headers;
        private List<Map<String, String>> rows;
        private Map<String, String> metadata; // For external data source
        
        public Examples() {
            this.headers = new ArrayList<>();
            this.rows = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(List<String> headers) {
            this.headers = headers;
        }
        
        public List<Map<String, String>> getRows() {
            return rows;
        }
        
        public void setRows(List<Map<String, String>> rows) {
            this.rows = rows;
        }
        
        public Map<String, String> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
        
        public boolean isExternalDataSource() {
            return metadata != null && !metadata.isEmpty();
        }
    }
}