package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.annotations.CSScenario;
import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.exceptions.CSBddException;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runner for BDD scenarios
 * Executes feature files and scenarios with step definitions
 */
public class CSScenarioRunner {
    private static final Logger logger = LoggerFactory.getLogger(CSScenarioRunner.class);
    private static final ThreadLocal<CSScenarioRunner> instance = new ThreadLocal<>();
    private final CSStepRegistry stepRegistry;
    private final Map<String, Object> scenarioContext;
    private CSTestResult currentTestResult;
    private String currentFeatureFile;
    private CSFeatureFile currentFeature;
    
    public CSScenarioRunner() {
        this.stepRegistry = CSStepRegistry.getInstance();
        this.scenarioContext = new ConcurrentHashMap<>();
        instance.set(this);
    }
    
    /**
     * Get the current instance for this thread
     */
    public static CSScenarioRunner getCurrentInstance() {
        return instance.get();
    }
    
    /**
     * Get the current instance of CSScenarioRunner
     */
    public static CSScenarioRunner getInstance() {
        return instance.get();
    }
    
    /**
     * Static method to run a scenario
     */
    public static void runScenario(Object testInstance, String methodName) {
        try {
            Method method = testInstance.getClass().getMethod(methodName);
            CSFeature feature = testInstance.getClass().getAnnotation(CSFeature.class);
            new CSScenarioRunner().runScenario(method, testInstance, feature);
        } catch (NoSuchMethodException e) {
            throw new CSBddException("Method not found: " + methodName, e);
        }
    }
    
    /**
     * Run a feature class
     */
    public void runFeature(Class<?> featureClass) {
        CSFeature feature = featureClass.getAnnotation(CSFeature.class);
        if (feature == null) {
            throw new CSBddException("Class " + featureClass.getName() + " is not annotated with @CSFeature");
        }
        
        logger.info("Running feature: {}", feature.name());
        
        // Create instance
        Object instance;
        try {
            instance = featureClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CSBddException("Failed to instantiate feature class: " + featureClass.getName(), e);
        }
        
        // Run background if present
        runBackground(featureClass, instance);
        
        // Run all scenarios
        for (Method method : featureClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CSScenario.class)) {
                runScenario(method, instance, feature);
            }
        }
    }
    
    /**
     * Run background steps
     */
    private void runBackground(Class<?> featureClass, Object instance) {
        try {
            Method backgroundMethod = featureClass.getDeclaredMethod("background");
            if (backgroundMethod != null) {
                logger.info("Running background");
                backgroundMethod.setAccessible(true);
                backgroundMethod.invoke(instance);
            }
        } catch (NoSuchMethodException e) {
            // No background method, that's fine
        } catch (Exception e) {
            logger.error("Failed to run background", e);
            throw new CSBddException("Background execution failed", e);
        }
    }
    
    /**
     * Run a scenario
     */
    private void runScenario(Method method, Object instance, CSFeature feature) {
        CSScenario scenario = method.getAnnotation(CSScenario.class);
        logger.info("Running scenario: {}", scenario.name());
        
        // Create test result
        currentTestResult = new CSTestResult();
        currentTestResult.setTestId(UUID.randomUUID().toString());
        currentTestResult.setTestName(scenario.name());
        currentTestResult.setClassName(instance.getClass().getName());
        currentTestResult.setMethodName(method.getName());
        currentTestResult.setDescription(scenario.description());
        currentTestResult.setStartTime(LocalDateTime.now());
        currentTestResult.setStatus(CSTestResult.Status.RUNNING);
        
        // Set proper suite name and feature name
        // Suite name will be set by the test runner
        if (feature != null) {
            currentTestResult.setFeatureFile(feature.name());
        }
        
        // Add tags
        List<String> tags = new ArrayList<>();
        tags.addAll(Arrays.asList(feature.tags()));
        tags.addAll(Arrays.asList(scenario.tags()));
        currentTestResult.setTags(tags);
        
        try {
            // Clear scenario context
            scenarioContext.clear();
            
            // Execute scenario method
            method.setAccessible(true);
            method.invoke(instance);
            
            // If we get here, scenario passed
            currentTestResult.setStatus(CSTestResult.Status.PASSED);
            
        } catch (Exception e) {
            logger.error("Scenario failed: {}", scenario.name(), e);
            currentTestResult.setStatus(CSTestResult.Status.FAILED);
            currentTestResult.setErrorMessage(e.getMessage());
            currentTestResult.setStackTrace(getStackTrace(e));
        } finally {
            currentTestResult.setEndTime(LocalDateTime.now());
            currentTestResult.setDuration(currentTestResult.calculateDuration());
            
            // Add to report
            CSReportManager.getInstance().addTestResult(currentTestResult);
        }
    }
    
    /**
     * Run a step from scenario code
     */
    public void runStep(String stepText, CSStepDefinition.StepType stepType) {
        logger.debug("Executing step: {} {}", stepType, stepText);
        
        if (currentTestResult != null) {
            currentTestResult.addStep(String.format("%s %s", stepType, stepText));
        }
        
        try {
            // Use context-aware execution if we have scenario context
            if (!scenarioContext.isEmpty() && scenarioContext.containsKey("dataRow")) {
                stepRegistry.executeStep(stepText, stepType, scenarioContext);
            } else {
                stepRegistry.executeStep(stepText, stepType);
            }
        } catch (Exception e) {
            logger.error("Step failed: {} {}", stepType, stepText, e);
            throw new CSBddException("Step execution failed: " + stepText, e);
        }
    }
    
    /**
     * Run feature file (Gherkin format)
     */
    public void runFeatureFile(String featureFile) {
        logger.info("Running feature file: {}", featureFile);
        
        // Parse feature file
        CSFeatureParser parser = new CSFeatureParser();
        CSFeatureFile feature = parser.parseFeatureFile(featureFile);
        
        // Run each scenario with feature file context
        for (CSFeatureFile.Scenario scenario : feature.getScenarios()) {
            runScenarioFromFile(featureFile, feature, scenario);
        }
    }
    
    /**
     * Run scenario from feature file with feature context
     */
    public void runScenarioFromFile(String featureFile, CSFeatureFile feature, CSFeatureFile.Scenario scenario) {
        logger.info("Running scenario: {} from feature: {}", scenario.getName(), feature.getName());
        
        // Store current feature context
        this.currentFeatureFile = featureFile;
        this.currentFeature = feature;
        
        try {
            // Clear scenario context for complete isolation
            scenarioContext.clear();
            
            // Add feature file context to prevent cross-contamination
            scenarioContext.put("feature_file", featureFile);
            scenarioContext.put("feature_name", feature.getName());
            
            // Store data row in context if present - with feature isolation
            if (scenario.getDataRow() != null && !scenario.getDataRow().isEmpty()) {
                // Create a defensive copy to prevent data contamination
                Map<String, String> isolatedDataRow = new HashMap<>(scenario.getDataRow());
                scenarioContext.put("dataRow", isolatedDataRow);
                logger.info("Using data row for feature '{}': {}", feature.getName(), isolatedDataRow);
            }
            
            // Store scenario details in context for step reporting
            scenarioContext.put("current_scenario", scenario);
            scenarioContext.put("current_feature", feature);
            List<Map<String, Object>> executedSteps = new ArrayList<>();
            scenarioContext.put("executed_steps", executedSteps);
            logger.info("Initialized executed_steps list in scenario context");
            
            // Run background steps if any
            if (feature.getBackground() != null) {
                for (CSFeatureFile.Step step : feature.getBackground().getSteps()) {
                    Map<String, Object> stepResult = executeFileStepWithResult(step);
                    executedSteps.add(stepResult);
                    
                    // If background step failed, stop execution
                    if ("failed".equals(stepResult.get("status"))) {
                        String errorMsg = (String) stepResult.get("error");
                        Exception failure = new CSBddException("Background step failed: " + step.getKeyword() + " " + step.getText() + 
                                               (errorMsg != null ? " - " + errorMsg : ""));
                        scenarioContext.put("scenario_failure", failure);
                        break; // Stop executing more background steps
                    }
                }
            }
            
            // Check if background steps failed
            Exception scenarioFailure = (Exception) scenarioContext.get("scenario_failure");
            
            // Run scenario steps only if background didn't fail
            if (scenarioFailure == null) {
                logger.info("Scenario {} has {} steps", scenario.getName(), scenario.getSteps().size());
                
                for (CSFeatureFile.Step step : scenario.getSteps()) {
                    logger.info("About to execute step: {} {}", step.getKeyword(), step.getText());
                    Map<String, Object> stepResult = executeFileStepWithResult(step);
                    executedSteps.add(stepResult);
                    logger.info("Added step result to executedSteps. Total steps: {}", executedSteps.size());
                    
                    // If step failed, stop executing remaining steps but keep the failure recorded
                    if ("failed".equals(stepResult.get("status"))) {
                        String errorMsg = (String) stepResult.get("error");
                        scenarioFailure = new CSBddException("Step failed: " + step.getKeyword() + " " + step.getText() + 
                                                            (errorMsg != null ? " - " + errorMsg : ""));
                        logger.error("Step failed, stopping scenario execution");
                        break;
                    }
                }
            }
            
            // If scenario failed, throw the exception now
            if (scenarioFailure != null) {
                throw scenarioFailure;
            }
            
            // If we get here, scenario passed
            logger.info("Scenario passed: {}", scenario.getName());
            
        } catch (Exception e) {
            logger.error("Scenario failed: {}", scenario.getName(), e);
            // Wrap in RuntimeException if not already
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new CSBddException("Scenario execution failed", e);
            }
        }
    }
    
    /**
     * Execute a step from feature file and return result
     */
    private Map<String, Object> executeFileStepWithResult(CSFeatureFile.Step step) {
        Map<String, Object> stepResult = new HashMap<>();
        stepResult.put("keyword", step.getKeyword());
        
        // Get the step text and replace placeholders if data row exists
        String stepText = step.getText();
        Map<String, String> dataRow = (Map<String, String>) scenarioContext.get("dataRow");
        if (dataRow != null && !dataRow.isEmpty()) {
            // Replace placeholders like <username> with actual values
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                stepText = stepText.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        stepResult.put("text", stepText);
        
        long startTime = System.currentTimeMillis();
        
        // Start step-level reporting
        CSReportManager.startStep(step.getKeyword(), stepText);
        
        try {
            CSStepDefinition.StepType stepType = CSStepDefinition.StepType.valueOf(step.getKeyword().toUpperCase());
            runStep(stepText, stepType);
            
            // Handle data table if present
            if (step.getDataTable() != null && !step.getDataTable().isEmpty()) {
                scenarioContext.put("lastDataTable", step.getDataTable());
                stepResult.put("dataTable", step.getDataTable());
            }
            
            // Handle doc string if present
            if (step.getDocString() != null) {
                scenarioContext.put("lastDocString", step.getDocString());
                stepResult.put("docString", step.getDocString());
            }
            
            stepResult.put("status", "passed");
            stepResult.put("duration", System.currentTimeMillis() - startTime);
            
            // End step reporting and get actions
            CSReportManager.endStep();
            
            // Get the actions from the completed step
            List<Map<String, Object>> actions = CSReportManager.getLastStepActions();
            if (actions != null && !actions.isEmpty()) {
                stepResult.put("actions", actions);
            }
            
        } catch (Exception e) {
            stepResult.put("status", "failed");
            stepResult.put("error", e.getMessage());
            stepResult.put("stackTrace", getStackTrace(e));
            stepResult.put("duration", System.currentTimeMillis() - startTime);
            
            // End step reporting with failure
            CSReportManager.endStep();
            
            // Try to capture screenshot on failure
            try {
                // Get driver from WebDriverManager if available
                if (CSWebDriverManager.getDriver() != null) {
                    byte[] screenshot = CSScreenshotUtils.captureScreenshot(CSWebDriverManager.getDriver());
                    if (screenshot != null && screenshot.length > 0) {
                        String screenshotName = "step_failure_" + System.currentTimeMillis() + ".png";
                        String path = CSReportManager.getInstance().attachScreenshot(screenshot, screenshotName);
                        if (path != null) {
                            stepResult.put("screenshot", path);
                            scenarioContext.put("last_screenshot", path);
                        }
                    }
                }
            } catch (Exception screenshotError) {
                logger.warn("Failed to capture screenshot: {}", screenshotError.getMessage());
            }
            
            // Don't throw immediately - let the step be recorded first
        }
        
        return stepResult;
    }
    
    /**
     * Execute a step from feature file
     */
    private void executeFileStep(CSFeatureFile.Step step) {
        // Get the step text and replace placeholders if data row exists
        String stepText = step.getText();
        Map<String, String> dataRow = (Map<String, String>) scenarioContext.get("dataRow");
        if (dataRow != null && !dataRow.isEmpty()) {
            // Replace placeholders like <username> with actual values
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                stepText = stepText.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        
        CSStepDefinition.StepType stepType = CSStepDefinition.StepType.valueOf(step.getKeyword().toUpperCase());
        runStep(stepText, stepType);
        
        // Handle data table if present
        if (step.getDataTable() != null && !step.getDataTable().isEmpty()) {
            scenarioContext.put("lastDataTable", step.getDataTable());
        }
        
        // Handle doc string if present
        if (step.getDocString() != null) {
            scenarioContext.put("lastDocString", step.getDocString());
        }
    }
    
    /**
     * Get scenario context
     */
    public Map<String, Object> getScenarioContext() {
        return scenarioContext;
    }
    
    /**
     * Store value in scenario context
     */
    public void storeInContext(String key, Object value) {
        scenarioContext.put(key, value);
    }
    
    /**
     * Get value from scenario context
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromContext(String key) {
        return (T) scenarioContext.get(key);
    }
    
    /**
     * Run scenario from feature file (deprecated - use version with feature file parameter)
     * @deprecated Use {@link #runScenarioFromFile(String, CSFeatureFile, CSFeatureFile.Scenario)} instead
     */
    @Deprecated
    public void runScenarioFromFile(CSFeatureFile feature, CSFeatureFile.Scenario scenario) {
        // Delegate to new method with empty feature file path for backward compatibility
        runScenarioFromFile("", feature, scenario);
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        StringBuilder sb = new StringBuilder();
        sb.append(cause.toString()).append("\n");
        for (StackTraceElement element : cause.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}