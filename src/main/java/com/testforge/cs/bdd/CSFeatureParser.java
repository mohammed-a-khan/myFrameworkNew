package com.testforge.cs.bdd;

import com.testforge.cs.exceptions.CSBddException;
import com.testforge.cs.utils.CSFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Gherkin feature files
 */
public class CSFeatureParser {
    private static final Logger logger = LoggerFactory.getLogger(CSFeatureParser.class);
    
    private static final Pattern FEATURE_PATTERN = Pattern.compile("^\\s*Feature:\\s*(.+)$");
    private static final Pattern SCENARIO_PATTERN = Pattern.compile("^\\s*Scenario:\\s*(.+)$");
    private static final Pattern SCENARIO_OUTLINE_PATTERN = Pattern.compile("^\\s*Scenario Outline:\\s*(.+)$");
    private static final Pattern BACKGROUND_PATTERN = Pattern.compile("^\\s*Background:\\s*(.*)$");
    private static final Pattern EXAMPLES_PATTERN = Pattern.compile("^\\s*Examples:\\s*(.*)$");
    private static final Pattern STEP_PATTERN = Pattern.compile("^\\s*(Given|When|Then|And|But)\\s+(.+)$");
    private static final Pattern TAG_PATTERN = Pattern.compile("(@[\\w-]+)");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*#.*$");
    private static final Pattern DOC_STRING_PATTERN = Pattern.compile("^\\s*\"\"\"\\s*$");
    
    private final CSDataSourceProcessor dataSourceProcessor;
    
    public CSFeatureParser() {
        this.dataSourceProcessor = new CSDataSourceProcessor();
    }
    
    /**
     * Parse a feature file
     */
    public CSFeatureFile parseFeatureFile(String filePath) {
        try {
            String content = CSFileUtils.readFileAsString(filePath);
            return parseFeatureContent(content, filePath);
        } catch (Exception e) {
            throw new CSBddException("Failed to parse feature file: " + filePath, e);
        }
    }
    
    /**
     * Parse feature content
     */
    public CSFeatureFile parseFeatureContent(String content, String sourcePath) {
        CSFeatureFile feature = new CSFeatureFile();
        feature.setSourcePath(sourcePath);
        
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            int lineNumber = 0;
            
            ParserState state = ParserState.INITIAL;
            CSFeatureFile.Scenario currentScenario = null;
            CSFeatureFile.Background background = null;
            CSFeatureFile.Step currentStep = null;
            List<String> currentTags = new ArrayList<>();
            StringBuilder description = new StringBuilder();
            StringBuilder docString = new StringBuilder();
            List<List<String>> dataTable = new ArrayList<>();
            List<Map<String, String>> examples = new ArrayList<>();
            List<String> exampleHeaders = null;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines and comments
                if (line.trim().isEmpty() || COMMENT_PATTERN.matcher(line).matches()) {
                    continue;
                }
                
                // Check for tags
                Matcher tagMatcher = TAG_PATTERN.matcher(line);
                if (tagMatcher.find()) {
                    currentTags.clear();
                    // Reset the matcher to find all tags
                    tagMatcher = TAG_PATTERN.matcher(line);
                    while (tagMatcher.find()) {
                        currentTags.add(tagMatcher.group(1));
                    }
                    continue;
                }
                
                // Check for feature
                Matcher featureMatcher = FEATURE_PATTERN.matcher(line);
                if (featureMatcher.matches()) {
                    feature.setName(featureMatcher.group(1).trim());
                    feature.setTags(new ArrayList<>(currentTags));
                    currentTags.clear();
                    state = ParserState.FEATURE;
                    description.setLength(0);
                    continue;
                }
                
                // Check for background
                Matcher backgroundMatcher = BACKGROUND_PATTERN.matcher(line);
                if (backgroundMatcher.matches()) {
                    background = new CSFeatureFile.Background();
                    background.setName(backgroundMatcher.group(1).trim());
                    feature.setBackground(background);
                    state = ParserState.BACKGROUND;
                    continue;
                }
                
                // Check for scenario
                Matcher scenarioMatcher = SCENARIO_PATTERN.matcher(line);
                if (scenarioMatcher.matches()) {
                    // Save any pending step before switching scenarios
                    if (currentStep != null) {
                        addStepToContext(currentStep, state, background, currentScenario);
                        logger.debug("Added step to {}: {} {}", 
                            currentScenario != null ? currentScenario.getName() : "background",
                            currentStep.getKeyword(), currentStep.getText());
                        currentStep = null;
                    }
                    
                    if (currentScenario != null) {
                        // Process previous scenario
                        processAndAddScenario(feature, currentScenario, examples, dataSourceProcessor);
                        examples.clear();
                    }
                    currentScenario = new CSFeatureFile.Scenario();
                    currentScenario.setName(scenarioMatcher.group(1).trim());
                    // Inherit feature tags and add scenario-specific tags
                    List<String> allTags = new ArrayList<>(feature.getTags());
                    allTags.addAll(currentTags);
                    currentScenario.setTags(allTags);
                    currentTags.clear();
                    state = ParserState.SCENARIO;
                    description.setLength(0);
                    continue;
                }
                
                // Check for scenario outline
                Matcher outlineMatcher = SCENARIO_OUTLINE_PATTERN.matcher(line);
                if (outlineMatcher.matches()) {
                    // Save any pending step before switching scenarios
                    if (currentStep != null) {
                        addStepToContext(currentStep, state, background, currentScenario);
                        logger.debug("Added step to {}: {} {}", 
                            currentScenario != null ? currentScenario.getName() : "background",
                            currentStep.getKeyword(), currentStep.getText());
                        currentStep = null;
                    }
                    
                    if (currentScenario != null) {
                        // Process previous scenario
                        processAndAddScenario(feature, currentScenario, examples, dataSourceProcessor);
                        examples.clear();
                    }
                    currentScenario = new CSFeatureFile.Scenario();
                    currentScenario.setName(outlineMatcher.group(1).trim());
                    // Inherit feature tags and add scenario-specific tags
                    List<String> allTags = new ArrayList<>(feature.getTags());
                    allTags.addAll(currentTags);
                    currentScenario.setTags(allTags);
                    currentScenario.setOutline(true);
                    currentTags.clear();
                    state = ParserState.SCENARIO_OUTLINE;
                    description.setLength(0);
                    continue;
                }
                
                // Check for examples
                Matcher examplesMatcher = EXAMPLES_PATTERN.matcher(line);
                if (examplesMatcher.matches()) {
                    // Save any pending step before switching to examples
                    if (currentStep != null) {
                        if (!dataTable.isEmpty()) {
                            currentStep.setDataTable(dataTable);
                            dataTable.clear();
                        }
                        addStepToContext(currentStep, state, background, currentScenario);
                        logger.debug("Added step to {}: {} {}", 
                            currentScenario != null ? currentScenario.getName() : "background",
                            currentStep.getKeyword(), currentStep.getText());
                        currentStep = null;
                    }
                    
                    state = ParserState.EXAMPLES;
                    examples.clear();
                    exampleHeaders = null;
                    
                    // Check if Examples line contains JSON configuration
                    String examplesText = examplesMatcher.group(1).trim();
                    if (examplesText.startsWith("{") && examplesText.endsWith("}")) {
                        // This is a JSON configuration for external data source
                        if (currentScenario != null) {
                            currentScenario.setExamplesConfig(examplesText);
                        }
                    }
                    continue;
                }
                
                // Check for steps
                Matcher stepMatcher = STEP_PATTERN.matcher(line);
                if (stepMatcher.matches()) {
                    // Save previous step if any
                    if (currentStep != null) {
                        addStepToContext(currentStep, state, background, currentScenario);
                        logger.debug("Added step to {}: {} {}", 
                            currentScenario != null ? currentScenario.getName() : "background",
                            currentStep.getKeyword(), currentStep.getText());
                    }
                    
                    currentStep = new CSFeatureFile.Step();
                    currentStep.setKeyword(stepMatcher.group(1));
                    currentStep.setText(stepMatcher.group(2).trim());
                    currentStep.setLineNumber(lineNumber);
                    dataTable.clear();
                    docString.setLength(0);
                    continue;
                }
                
                // Check for doc strings
                if (DOC_STRING_PATTERN.matcher(line).matches()) {
                    if (state == ParserState.DOC_STRING) {
                        // End of doc string
                        if (currentStep != null) {
                            currentStep.setDocString(docString.toString());
                        }
                        state = getPreviousState(background, currentScenario);
                    } else {
                        // Start of doc string
                        state = ParserState.DOC_STRING;
                        docString.setLength(0);
                    }
                    continue;
                }
                
                // Handle state-specific content
                switch (state) {
                    case INITIAL:
                        // Ignore content before feature declaration
                        break;
                        
                    case FEATURE:
                        if (description.length() > 0) description.append("\n");
                        description.append(line.trim());
                        break;
                        
                    case SCENARIO:
                    case SCENARIO_OUTLINE:
                        if (currentStep == null && currentScenario != null) {
                            if (currentScenario.getDescription() == null) {
                                currentScenario.setDescription("");
                            }
                            currentScenario.setDescription(currentScenario.getDescription() + line.trim() + "\n");
                        } else if (line.trim().startsWith("|")) {
                            // Data table
                            dataTable.add(parseTableRow(line));
                        }
                        break;
                        
                    case BACKGROUND:
                        if (currentStep == null && line.trim().startsWith("|")) {
                            // Data table
                            dataTable.add(parseTableRow(line));
                        }
                        break;
                        
                    case DOC_STRING:
                        if (docString.length() > 0) docString.append("\n");
                        docString.append(line);
                        break;
                        
                    case EXAMPLES:
                        if (line.trim().startsWith("|")) {
                            List<String> row = parseTableRow(line);
                            if (exampleHeaders == null) {
                                exampleHeaders = row;
                            } else {
                                Map<String, String> example = new HashMap<>();
                                for (int i = 0; i < Math.min(exampleHeaders.size(), row.size()); i++) {
                                    example.put(exampleHeaders.get(i), row.get(i));
                                }
                                examples.add(example);
                            }
                        }
                        break;
                }
            }
            
            // Save last items
            if (currentStep != null) {
                if (!dataTable.isEmpty()) {
                    currentStep.setDataTable(dataTable);
                }
                addStepToContext(currentStep, state, background, currentScenario);
            }
            
            if (currentScenario != null) {
                processAndAddScenario(feature, currentScenario, examples, dataSourceProcessor);
            }
            
            if (feature.getDescription() == null && description.length() > 0) {
                feature.setDescription(description.toString());
            }
            
        } catch (IOException e) {
            throw new CSBddException("Failed to parse feature content", e);
        }
        
        logger.info("Parsed feature '{}' with {} scenarios", feature.getName(), feature.getScenarios().size());
        
        // Log details of each scenario for debugging
        for (int i = 0; i < feature.getScenarios().size(); i++) {
            CSFeatureFile.Scenario scenario = feature.getScenarios().get(i);
            logger.info("  Scenario #{}: {} - Data: {}", i + 1, scenario.getName(), scenario.getDataRow());
        }
        
        return feature;
    }
    
    /**
     * Process and add scenario to feature
     */
    private void processAndAddScenario(CSFeatureFile feature, CSFeatureFile.Scenario scenario, 
                                       List<Map<String, String>> examples, 
                                       CSDataSourceProcessor dataSourceProcessor) {
        if (scenario.isOutline()) {
            // Check if scenario has external data source configuration in Examples
            if (scenario.getExamplesConfig() != null) {
                logger.info("Processing external data source for scenario: {}", scenario.getName());
                // Process external data source from Examples configuration
                List<Map<String, String>> externalData = dataSourceProcessor.processExamplesConfig(scenario.getExamplesConfig());
                logger.info("Found {} data rows from external source for scenario: {}", externalData.size(), scenario.getName());
                for (Map<String, String> dataRow : externalData) {
                    logger.info("Expanding scenario '{}' with data row: {}", scenario.getName(), dataRow);
                    CSFeatureFile.Scenario expandedScenario = expandScenarioOutline(scenario, dataRow);
                    expandedScenario.setExamplesConfig(scenario.getExamplesConfig());
                    logger.info("Adding expanded scenario '{}' to feature", expandedScenario.getName());
                    feature.addScenario(expandedScenario);
                }
            } else if (!examples.isEmpty()) {
                // Traditional Examples table
                for (Map<String, String> example : examples) {
                    CSFeatureFile.Scenario expandedScenario = expandScenarioOutline(scenario, example);
                    feature.addScenario(expandedScenario);
                }
            } else {
                // No examples provided, add as regular scenario
                feature.addScenario(scenario);
            }
        } else {
            // Check if scenario has @CSDataSource tag
            List<Map<String, String>> externalData = dataSourceProcessor.processDataSource(scenario.getTags());
            if (!externalData.isEmpty()) {
                // Expand scenario with external data
                for (Map<String, String> dataRow : externalData) {
                    CSFeatureFile.Scenario expandedScenario = expandScenarioWithData(scenario, dataRow);
                    feature.addScenario(expandedScenario);
                }
            } else {
                feature.addScenario(scenario);
            }
        }
    }
    
    /**
     * Parse table row
     */
    private List<String> parseTableRow(String line) {
        List<String> cells = new ArrayList<>();
        String trimmed = line.trim();
        
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            String content = trimmed.substring(1, trimmed.length() - 1);
            String[] parts = content.split("\\|");
            
            for (String part : parts) {
                cells.add(part.trim());
            }
        }
        
        return cells;
    }
    
    /**
     * Add step to appropriate context
     */
    private void addStepToContext(CSFeatureFile.Step step, ParserState state, 
                                 CSFeatureFile.Background background, 
                                 CSFeatureFile.Scenario scenario) {
        switch (state) {
            case BACKGROUND:
                if (background != null) {
                    background.addStep(step);
                }
                break;
            case SCENARIO:
            case SCENARIO_OUTLINE:
                if (scenario != null) {
                    scenario.addStep(step);
                }
                break;
            case INITIAL:
            case FEATURE:
            case EXAMPLES:
            case DOC_STRING:
                // Steps are not added in these states
                break;
        }
    }
    
    /**
     * Get previous state
     */
    private ParserState getPreviousState(CSFeatureFile.Background background, 
                                       CSFeatureFile.Scenario scenario) {
        if (scenario != null) {
            return scenario.isOutline() ? ParserState.SCENARIO_OUTLINE : ParserState.SCENARIO;
        } else if (background != null) {
            return ParserState.BACKGROUND;
        }
        return ParserState.FEATURE;
    }
    
    /**
     * Expand scenario with external data source
     */
    private CSFeatureFile.Scenario expandScenarioWithData(CSFeatureFile.Scenario original,
                                                         Map<String, String> dataRow) {
        CSFeatureFile.Scenario expanded = new CSFeatureFile.Scenario();
        
        // Keep original name without data values
        expanded.setName(original.getName());
        expanded.setDescription(original.getDescription());
        expanded.setTags(new ArrayList<>(original.getTags()));
        expanded.setOutline(false);
        expanded.setDataRow(new HashMap<>(dataRow));
        
        // Expand steps with data
        for (CSFeatureFile.Step originalStep : original.getSteps()) {
            CSFeatureFile.Step expandedStep = new CSFeatureFile.Step();
            expandedStep.setKeyword(originalStep.getKeyword());
            expandedStep.setText(dataSourceProcessor.replacePlaceholders(originalStep.getText(), dataRow));
            expandedStep.setLineNumber(originalStep.getLineNumber());
            
            // Expand data table if present
            if (originalStep.getDataTable() != null) {
                List<List<String>> expandedTable = new ArrayList<>();
                for (List<String> row : originalStep.getDataTable()) {
                    List<String> expandedRow = new ArrayList<>();
                    for (String cell : row) {
                        expandedRow.add(dataSourceProcessor.replacePlaceholders(cell, dataRow));
                    }
                    expandedTable.add(expandedRow);
                }
                expandedStep.setDataTable(expandedTable);
            }
            
            // Expand doc string if present
            if (originalStep.getDocString() != null) {
                expandedStep.setDocString(dataSourceProcessor.replacePlaceholders(originalStep.getDocString(), dataRow));
            }
            
            expanded.addStep(expandedStep);
        }
        
        return expanded;
    }
    
    /**
     * Expand scenario outline with example data
     */
    private CSFeatureFile.Scenario expandScenarioOutline(CSFeatureFile.Scenario outline, 
                                                        Map<String, String> example) {
        CSFeatureFile.Scenario expanded = new CSFeatureFile.Scenario();
        
        // Expand placeholders in scenario name
        String expandedName = outline.getName();
        for (Map.Entry<String, String> entry : example.entrySet()) {
            String placeholder = "<" + entry.getKey() + ">";
            String value = entry.getValue();
            expandedName = expandedName.replace(placeholder, value);
        }
        expanded.setName(expandedName);
        
        // Also expand description if it has placeholders
        String expandedDescription = outline.getDescription() != null ? outline.getDescription() : "";
        for (Map.Entry<String, String> entry : example.entrySet()) {
            String placeholder = "<" + entry.getKey() + ">";
            String value = entry.getValue();
            expandedDescription = expandedDescription.replace(placeholder, value);
        }
        expanded.setDescription(expandedDescription);
        
        expanded.setTags(new ArrayList<>(outline.getTags()));
        expanded.setOutline(false);
        expanded.setDataRow(new HashMap<>(example));  // Set a copy of the data row
        
        // Expand steps
        for (CSFeatureFile.Step step : outline.getSteps()) {
            CSFeatureFile.Step expandedStep = new CSFeatureFile.Step();
            expandedStep.setKeyword(step.getKeyword());
            expandedStep.setLineNumber(step.getLineNumber());
            
            // Replace placeholders in step text
            String expandedText = step.getText();
            logger.debug("Expanding step text: {} with data: {}", expandedText, example);
            for (Map.Entry<String, String> entry : example.entrySet()) {
                String placeholder = "<" + entry.getKey() + ">";
                String value = entry.getValue();
                logger.debug("Replacing {} with {}", placeholder, value);
                expandedText = expandedText.replace(placeholder, value);
            }
            logger.debug("Expanded step text: {}", expandedText);
            expandedStep.setText(expandedText);
            
            // Copy data table and doc string if any
            if (step.getDataTable() != null) {
                List<List<String>> expandedTable = new ArrayList<>();
                for (List<String> row : step.getDataTable()) {
                    List<String> expandedRow = new ArrayList<>();
                    for (String cell : row) {
                        String expandedCell = cell;
                        for (Map.Entry<String, String> entry : example.entrySet()) {
                            expandedCell = expandedCell.replace("<" + entry.getKey() + ">", entry.getValue());
                        }
                        expandedRow.add(expandedCell);
                    }
                    expandedTable.add(expandedRow);
                }
                expandedStep.setDataTable(expandedTable);
            }
            
            if (step.getDocString() != null) {
                String expandedDocString = step.getDocString();
                for (Map.Entry<String, String> entry : example.entrySet()) {
                    expandedDocString = expandedDocString.replace("<" + entry.getKey() + ">", entry.getValue());
                }
                expandedStep.setDocString(expandedDocString);
            }
            
            expanded.addStep(expandedStep);
        }
        
        return expanded;
    }
    
    /**
     * Parser states
     */
    private enum ParserState {
        INITIAL,
        FEATURE,
        BACKGROUND,
        SCENARIO,
        SCENARIO_OUTLINE,
        EXAMPLES,
        DOC_STRING
    }
}