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
    @SuppressWarnings("unused")
    private String currentFeatureFile;
    @SuppressWarnings("unused")
    private CSFeatureFile currentFeature;
    
    public CSScenarioRunner() {
        this.stepRegistry = CSStepRegistry.getInstance();
        this.scenarioContext = new ConcurrentHashMap<>();
        instance.set(this);
        
        // Initialize ThreadLocal registry for this thread
        String threadName = Thread.currentThread().getName();
        logger.debug("Initializing CSScenarioRunner for thread: {}", threadName);
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
     * Get the current test result for step reporting context
     */
    public CSTestResult getCurrentTestResult() {
        return currentTestResult;
    }
    
    /**
     * Get the scenario context for accessing current step result
     */
    public Map<String, Object> getScenarioContext() {
        return scenarioContext;
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
        if (feature != null) {
            tags.addAll(Arrays.asList(feature.tags()));
        }
        tags.addAll(Arrays.asList(scenario.tags()));
        currentTestResult.setTags(tags);
        
        try {
            // Clear scenario context
            scenarioContext.clear();
            
            // Execute scenario method
            method.setAccessible(true);
            method.invoke(instance);
            
            // Check if any steps soft-failed
            List<Map<String, Object>> executedSteps = (List<Map<String, Object>>) scenarioContext.get("executed_steps");
            boolean hasFailedSteps = false;
            if (executedSteps != null) {
                hasFailedSteps = executedSteps.stream()
                    .anyMatch(step -> {
                        String status = (String) step.get("status");
                        Boolean softFailed = (Boolean) step.get("softFailed");
                        return "failed".equals(status) || (softFailed != null && softFailed);
                    });
            }
            
            if (hasFailedSteps) {
                currentTestResult.setStatus(CSTestResult.Status.FAILED);
                currentTestResult.setErrorMessage("Scenario contains soft-failed steps");
                logger.info("Scenario marked as failed due to soft-failed steps");
            } else {
                currentTestResult.setStatus(CSTestResult.Status.PASSED);
            }
            
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
        String threadName = Thread.currentThread().getName();
        logger.debug("[{}] Executing step: {} {}", threadName, stepType, stepText);
        
        if (currentTestResult != null) {
            currentTestResult.addStep(String.format("%s %s", stepType, stepText));
        }
        
        try {
            // Use original registry - ThreadLocal page objects ensure thread safety
            if (!scenarioContext.isEmpty() && scenarioContext.containsKey("dataRow")) {
                logger.debug("[{}] Executing step with context", threadName);
                stepRegistry.executeStep(stepText, stepType, scenarioContext);
            } else {
                logger.debug("[{}] Executing step without context", threadName);
                stepRegistry.executeStep(stepText, stepType);
            }
        } catch (Exception e) {
            logger.error("[{}] Step failed: {} {}", threadName, stepType, stepText, e);
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
                    
                    // If step hard failed (not soft fail), stop executing remaining steps
                    Boolean isSoftFailed = (Boolean) stepResult.get("softFailed");
                    if ("failed".equals(stepResult.get("status")) && (isSoftFailed == null || !isSoftFailed)) {
                        String errorMsg = (String) stepResult.get("error");
                        scenarioFailure = new CSBddException("Step failed: " + step.getKeyword() + " " + step.getText() + 
                                                            (errorMsg != null ? " - " + errorMsg : ""));
                        logger.error("Step hard failed, stopping scenario execution");
                        break;
                    } else if (isSoftFailed != null && isSoftFailed) {
                        logger.info("Step soft failed, continuing execution");
                    }
                }
            }
            
            // If scenario failed, throw the exception now
            if (scenarioFailure != null) {
                throw scenarioFailure;
            }
            
            // Check if any steps soft-failed
            boolean hasFailedSteps = executedSteps.stream()
                .anyMatch(step -> {
                    String status = (String) step.get("status");
                    Boolean softFailed = (Boolean) step.get("softFailed");
                    return "failed".equals(status) || (softFailed != null && softFailed);
                });
            
            if (hasFailedSteps) {
                logger.info("Scenario has soft-failed steps: {}", scenario.getName());
                // Store this in context so CSBDDRunner can detect it
                scenarioContext.put("has_soft_failed_steps", true);
            } else {
                logger.info("Scenario passed: {}", scenario.getName());
            }
            
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
        @SuppressWarnings("unchecked")
        Map<String, String> dataRow = (Map<String, String>) scenarioContext.get("dataRow");
        if (dataRow != null && !dataRow.isEmpty()) {
            // Replace placeholders like <username> with actual values
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                stepText = stepText.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        stepResult.put("text", stepText);
        
        // Store the current step result in scenario context so CSReportManager can access it
        scenarioContext.put("current_step_result", stepResult);
        
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
            
            // Check if step was marked as soft-failed by CSReportManager.fail()
            Boolean softFailed = (Boolean) stepResult.get("softFailed");
            String displayStatus = (String) stepResult.get("displayStatus");
            
            if (softFailed != null && softFailed) {
                // This is a soft fail - show as failed in HTML but don't stop execution
                stepResult.put("status", displayStatus != null ? displayStatus : "failed");
                logger.info("Step marked as soft-failed, will continue execution");
            } else {
                // Normal status handling
                String stepStatus = (String) stepResult.get("status");
                if (!"failed".equals(stepStatus)) {
                    stepResult.put("status", "passed");
                }
            }
            stepResult.put("duration", System.currentTimeMillis() - startTime);
            
            // End step reporting and get actions
            CSReportManager.endStep();
            
            // Get the actions from the completed step AND from the step result itself
            List<Map<String, Object>> actions = CSReportManager.getLastStepActions();
            
            // Also check if actions were added directly to step result (fallback mechanism)
            List<Map<String, Object>> existingActions = (List<Map<String, Object>>) stepResult.get("actions");
            
            // Merge actions from both sources (CSStepReport first, then direct actions)
            List<Map<String, Object>> allActions = new ArrayList<>();
            if (actions != null) {
                allActions.addAll(actions);  // Add navigation actions first
                logger.info("Found {} actions from CSStepReport", actions.size());
            }
            if (existingActions != null) {
                allActions.addAll(existingActions);  // Add FAIL action after
                logger.info("Found {} actions already in step result", existingActions.size());
            }
            
            if (!allActions.isEmpty()) {
                stepResult.put("actions", allActions);
                logger.info("Total {} actions added to step result", allActions.size());
                
                // Also check if any action is a FAIL - this ensures step is marked as failed visually
                boolean hasFailAction = allActions.stream()
                    .anyMatch(action -> "FAIL".equals(action.get("type")));
                
                if (hasFailAction && !"failed".equals(stepResult.get("status"))) {
                    // Mark for display but don't trigger execution stop
                    stepResult.put("status", "failed");
                    stepResult.put("softFailed", true);
                    logger.info("Step marked as failed due to FAIL action (soft fail)");
                }
            }
            
        } catch (Exception e) {
            stepResult.put("status", "failed");
            stepResult.put("error", e.getMessage());
            stepResult.put("stackTrace", getStackTrace(e));
            stepResult.put("duration", System.currentTimeMillis() - startTime);
            
            // If this is an AssertionError from CSReportManager.fail(), extract the custom message
            if (e.getMessage() != null && e.getMessage().startsWith("Step failed: ")) {
                String customMessage = e.getMessage().substring("Step failed: ".length());
                stepResult.put("failureMessage", customMessage);
                logger.info("Step failed with custom message: {}", customMessage);
            }
            
            // Also check if the cause is an AssertionError
            Throwable cause = e.getCause();
            if (cause instanceof AssertionError && cause.getMessage() != null && cause.getMessage().startsWith("Step failed: ")) {
                String customMessage = cause.getMessage().substring("Step failed: ".length());
                stepResult.put("failureMessage", customMessage);
                logger.info("Step failed with custom message from cause: {}", customMessage);
            }
            
            // End step reporting with failure
            CSReportManager.endStep();
            
            // CRITICAL: For actual failures (not soft fails), ALWAYS capture a fresh screenshot
            // This ensures we get the current page state, not a reused soft fail screenshot
            try {
                // Get driver from WebDriverManager if available
                if (CSWebDriverManager.getDriver() != null) {
                    // Wait briefly to ensure page is stable before screenshot
                    Thread.sleep(500);
                    
                    // Log current page info for debugging
                    try {
                        String currentUrl = CSWebDriverManager.getDriver().getCurrentUrl();
                        String pageTitle = CSWebDriverManager.getDriver().getTitle();
                        logger.info("Capturing ACTUAL HARD FAILURE screenshot - URL: {}, Title: {}", currentUrl, pageTitle);
                    } catch (Exception pageEx) {
                        logger.warn("Could not get page details for actual failure screenshot: {}", pageEx.getMessage());
                    }
                    
                    // ALWAYS capture a fresh screenshot for actual failures
                    byte[] freshScreenshot = CSScreenshotUtils.captureScreenshot(CSWebDriverManager.getDriver());
                    if (freshScreenshot != null && freshScreenshot.length > 0) {
                        // Convert to base64 for embedding in report
                        String base64Screenshot = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(freshScreenshot);
                        
                        // Use unique naming to differentiate from soft fail screenshots
                        String screenshotName = "HARD_FAILURE_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis();
                        
                        // Override any existing screenshot with this fresh one
                        stepResult.put("screenshot", base64Screenshot);
                        stepResult.put("screenshotBase64", base64Screenshot);
                        stepResult.put("screenshotName", screenshotName);
                        stepResult.put("hardFailureScreenshot", true); // Mark this as a hard failure screenshot
                        
                        // Also attach to report manager
                        String path = CSReportManager.getInstance().attachScreenshot(freshScreenshot, screenshotName);
                        if (path != null) {
                            scenarioContext.put("last_hard_failure_screenshot", path);
                        }
                        
                        logger.info("HARD FAILURE screenshot captured and attached: {} (size: {} bytes)", screenshotName, freshScreenshot.length);
                    }
                }
            } catch (Exception screenshotError) {
                logger.error("Failed to capture hard failure screenshot: {}", screenshotError.getMessage(), screenshotError);
            }
            
            // Don't throw immediately - let the step be recorded first
        }
        
        // Clean up the current step result from context for next step
        scenarioContext.remove("current_step_result");
        
        return stepResult;
    }
    
    /**
     * Execute a step from feature file
     */
    @SuppressWarnings("unused")
    private void executeFileStep(CSFeatureFile.Step step) {
        // Get the step text and replace placeholders if data row exists
        String stepText = step.getText();
        @SuppressWarnings("unchecked")
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