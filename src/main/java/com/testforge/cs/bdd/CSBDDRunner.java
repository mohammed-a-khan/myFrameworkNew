package com.testforge.cs.bdd;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Framework-provided BDD Runner for executing Gherkin feature files
 * Can be configured via TestNG XML suite files
 */
public class CSBDDRunner extends CSBaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(CSBDDRunner.class);
    
    private String featuresPath;
    private String tagsToRun;
    private String tagsToExclude;
    private String stepDefPackages;
    private List<String> featureFiles = new ArrayList<>();
    private CSFeatureParser featureParser;
    private CSScenarioRunner scenarioRunner;
    
    @BeforeClass
    @Parameters({"featuresPath", "tags", "excludeTags", "stepDefPackages"})
    public void setupBDDRunner(
            @org.testng.annotations.Optional("features") String featuresPath,
            @org.testng.annotations.Optional("") String tags,
            @org.testng.annotations.Optional("") String excludeTags,
            @org.testng.annotations.Optional("") String stepDefPackages) {
        
        this.featuresPath = featuresPath;
        this.tagsToRun = tags;
        this.tagsToExclude = excludeTags;
        this.stepDefPackages = stepDefPackages;
        this.featureParser = new CSFeatureParser();
        this.scenarioRunner = new CSScenarioRunner();
        
        // Driver will be initialized later in @BeforeMethod
        // Skip driver setup in @BeforeClass
        
        // Register step definition classes
        registerStepDefinitions();
        
        logger.info("BDD Runner initialized - Features: {}, Tags: {}, Exclude: {}", 
            featuresPath, tags, excludeTags);
        
        // Discover feature files
        discoverFeatureFiles();
    }
    
    /**
     * Register step definition classes
     * Override this method to register your step definitions
     */
    protected void registerStepDefinitions() {
        // Dynamic package scanning for step definitions
        try {
            // Use parameter if provided, otherwise fall back to config property
            String packagesToScan = (this.stepDefPackages != null && !this.stepDefPackages.trim().isEmpty()) 
                ? this.stepDefPackages 
                : config.getProperty("cs.bdd.stepdefs.packages", "com.orangehrm.stepdefs");
            logger.info("Scanning for step definitions in packages: {}", packagesToScan);
            
            CSStepRegistry registry = CSStepRegistry.getInstance();
            
            // Split comma-separated packages and scan each one
            String[] packages = packagesToScan.split(",");
            for (String packageName : packages) {
                String trimmedPackage = packageName.trim();
                if (!trimmedPackage.isEmpty()) {
                    logger.info("Scanning package: {}", trimmedPackage);
                    registry.scanPackage(trimmedPackage);
                }
            }
            
            // Log the registered steps count
            int totalSteps = registry.getAllSteps().values().stream()
                .mapToInt(List::size)
                .sum();
            logger.info("Total step definitions registered: {}", totalSteps);
            
        } catch (Exception e) {
            logger.error("Failed to register step definitions", e);
        }
    }
    
    /**
     * Discover all feature files in the specified path
     */
    private void discoverFeatureFiles() {
        try {
            featureFiles.clear();
            
            // Support comma-separated paths
            String[] paths = featuresPath.split(",");
            
            for (String pathStr : paths) {
                String trimmedPath = pathStr.trim();
                if (trimmedPath.isEmpty()) continue;
                
                Path path = Paths.get(trimmedPath);
                
                // Check if it's a specific feature file
                if (trimmedPath.endsWith(".feature")) {
                    if (Files.exists(path)) {
                        featureFiles.add(path.toString());
                        logger.info("Added feature file: {}", path);
                    } else {
                        logger.warn("Feature file not found: {}", path);
                    }
                } else {
                    // It's a directory, scan for feature files
                    if (!Files.exists(path)) {
                        logger.warn("Features directory does not exist: {}", trimmedPath);
                        continue;
                    }
                    
                    List<String> foundFiles = Files.walk(path)
                        .filter(p -> p.toString().endsWith(".feature"))
                        .map(Path::toString)
                        .collect(Collectors.toList());
                    
                    featureFiles.addAll(foundFiles);
                    logger.info("Discovered {} feature files in: {}", foundFiles.size(), trimmedPath);
                }
            }
            
            logger.info("Total feature files discovered: {}", featureFiles.size());
            
            if (featureFiles.isEmpty()) {
                throw new RuntimeException("No feature files found in paths: " + featuresPath);
            }
            
        } catch (Exception e) {
            logger.error("Failed to discover feature files", e);
            throw new RuntimeException("Failed to discover feature files", e);
        }
    }
    
    /**
     * Data provider for feature files
     */
    @DataProvider(name = "featureFiles", parallel = false)
    public Object[][] getFeatureFiles() {
        List<Object[]> testData = new ArrayList<>();
        
        for (String featureFile : featureFiles) {
            try {
                CSFeatureFile feature = featureParser.parseFeatureFile(featureFile);
                
                // Check if feature should be included based on tags
                boolean includeFeature = shouldIncludeFeature(feature);
                
                if (includeFeature) {
                    // If feature is included, run all its scenarios
                    logger.info("Feature {} is included, has {} scenarios", feature.getName(), feature.getScenarios().size());
                    for (CSFeatureFile.Scenario scenario : feature.getScenarios()) {
                        logger.info("Adding scenario to test data: {} with {} steps", 
                            scenario.getName(), scenario.getSteps().size());
                        testData.add(new Object[]{featureFile, feature, scenario});
                    }
                } else {
                    // Otherwise, filter scenarios by tags
                    logger.info("Feature {} is not included at feature level, checking scenarios individually", feature.getName());
                    List<CSFeatureFile.Scenario> scenarios = filterScenariosByTags(feature.getScenarios());
                    logger.info("Found {} matching scenarios out of {}", scenarios.size(), feature.getScenarios().size());
                    
                    for (CSFeatureFile.Scenario scenario : scenarios) {
                        logger.debug("Adding filtered scenario to test data: {} with {} steps", 
                            scenario.getName(), scenario.getSteps().size());
                        // Debug: Log first few steps
                        if (scenario.getSteps() != null && !scenario.getSteps().isEmpty()) {
                            for (int i = 0; i < Math.min(3, scenario.getSteps().size()); i++) {
                                CSFeatureFile.Step step = scenario.getSteps().get(i);
                                logger.info("  Step {}: {} {}", i+1, step.getKeyword(), step.getText());
                            }
                        }
                        testData.add(new Object[]{featureFile, feature, scenario});
                    }
                }
                
            } catch (Exception e) {
                logger.error("Failed to parse feature file: {}", featureFile, e);
            }
        }
        
        return testData.toArray(new Object[0][]);
    }
    
    @Override
    @BeforeMethod(alwaysRun = true)
    public void setupTest(Method method, Object[] params, ITestContext context) {
        // Call parent setup first
        super.setupTest(method, params, context);
        
        // Now set up the driver for step definitions
        if (driver != null) {
            CSDriver csDriver = new CSDriver(driver);
            CSStepDefinitions.setDriver(csDriver);
            CSWebDriverManager.setDriver(driver);
        }
    }
    
    /**
     * Test method that executes each scenario
     */
    @Test(dataProvider = "featureFiles", description = "Execute BDD Scenario")
    public void executeBDDScenario(String featureFile, CSFeatureFile feature, CSFeatureFile.Scenario scenario) {
        String scenarioName = feature.getName() + " - " + scenario.getName();
        logger.info("Executing scenario: {} (Tags: {}) - First step: {} {}", 
            scenarioName, scenario.getTags(), 
            scenario.getSteps().isEmpty() ? "NO STEPS" : scenario.getSteps().get(0).getKeyword(),
            scenario.getSteps().isEmpty() ? "" : scenario.getSteps().get(0).getText());
        
        // Create test result
        CSTestResult testResult = new CSTestResult();
        testResult.setTestId(UUID.randomUUID().toString());
        testResult.setTestName(scenarioName);
        testResult.setDescription(scenario.getDescription() != null ? scenario.getDescription() : "");
        testResult.setTags(scenario.getTags());
        testResult.setClassName(this.getClass().getName());
        testResult.setMethodName("executeBDDScenario");
        testResult.setStartTime(LocalDateTime.now());
        testResult.setEnvironment(config.getProperty("env.current", "qa"));
        testResult.setBrowser(config.getProperty("browser.default", "chrome"));
        
        // Set proper suite name and feature file
        testResult.setSuiteName("Simple Sequential Test Suite");
        testResult.setFeatureFile(new File(featureFile).getName());
        
        try {
            // Execute scenario
            scenarioRunner.runScenarioFromFile(feature, scenario);
            
            // Get executed steps from scenario context
            Map<String, Object> scenarioContext = scenarioRunner.getScenarioContext();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> executedSteps = (List<Map<String, Object>>) scenarioContext.get("executed_steps");
            logger.info("Retrieved executed steps from context: {} steps", executedSteps != null ? executedSteps.size() : "null");
            if (executedSteps != null && !executedSteps.isEmpty()) {
                logger.info("Setting {} executed steps to test result", executedSteps.size());
                testResult.setExecutedSteps(executedSteps);
            } else {
                logger.warn("No executed steps found in scenario context for: {}", scenarioName);
            }
            
            testResult.setStatus(CSTestResult.Status.PASSED);
            reportManager.logInfo("Scenario passed: " + scenarioName);
            
        } catch (Exception e) {
            testResult.setStatus(CSTestResult.Status.FAILED);
            testResult.setErrorMessage(e.getMessage());
            testResult.setStackTrace(getStackTrace(e));
            testResult.setEndTime(LocalDateTime.now());
            testResult.setDuration(testResult.calculateDuration());
            
            // Get executed steps even on failure
            Map<String, Object> scenarioContext = scenarioRunner.getScenarioContext();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> executedSteps = (List<Map<String, Object>>) scenarioContext.get("executed_steps");
            logger.info("Retrieved executed steps from context on failure: {} steps", executedSteps != null ? executedSteps.size() : "null");
            if (executedSteps != null && !executedSteps.isEmpty()) {
                logger.info("Setting {} executed steps to test result on failure", executedSteps.size());
                testResult.setExecutedSteps(executedSteps);
            } else {
                logger.warn("No executed steps found in scenario context on failure for: {}", scenarioName);
            }
            
            // Try to capture screenshot on failure
            try {
                if (CSWebDriverManager.getDriver() != null) {
                    byte[] screenshot = CSScreenshotUtils.captureScreenshot(CSWebDriverManager.getDriver());
                    if (screenshot != null && screenshot.length > 0) {
                        String screenshotName = "failure_executeBDDScenario_" + System.currentTimeMillis();
                        String path = reportManager.attachScreenshot(screenshot, screenshotName);
                        if (path != null) {
                            testResult.setScreenshotPath(path);
                            logger.info("Screenshot captured for failed scenario: {}", path);
                        }
                    }
                }
            } catch (Exception screenshotError) {
                logger.warn("Failed to capture screenshot: {}", screenshotError.getMessage());
            }
            
            reportManager.logError("Scenario failed: " + scenarioName + " - " + e.getMessage());
            throw new RuntimeException("Scenario failed: " + scenarioName, e);
            
        } finally {
            // Set end time if not already set
            if (testResult.getEndTime() == null) {
                testResult.setEndTime(LocalDateTime.now());
                testResult.setDuration(testResult.calculateDuration());
            }
            // Add test result to report
            CSReportManager.getInstance().addTestResult(testResult);
        }
    }
    
    /**
     * Check if feature should be included based on feature-level tags
     */
    private boolean shouldIncludeFeature(CSFeatureFile feature) {
        // If no tags specified, include the feature
        if ((tagsToRun == null || tagsToRun.trim().isEmpty()) && 
            (tagsToExclude == null || tagsToExclude.trim().isEmpty())) {
            return true;
        }
        
        Set<String> includeTags = tagsToRun != null && !tagsToRun.trim().isEmpty() 
            ? new HashSet<>(Arrays.asList(tagsToRun.split(",")))
            : new HashSet<>();
        Set<String> excludeTags = tagsToExclude != null && !tagsToExclude.trim().isEmpty() 
            ? new HashSet<>(Arrays.asList(tagsToExclude.split(",")))
            : new HashSet<>();
        
        List<String> featureTags = feature.getTags();
        
        // Check exclude tags first
        for (String tag : featureTags) {
            if (excludeTags.contains(tag)) {
                return false;
            }
        }
        
        // If no include tags specified, feature is included (unless excluded)
        if (includeTags.isEmpty()) {
            return true;
        }
        
        // Check include tags
        for (String tag : featureTags) {
            if (includeTags.contains(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Filter scenarios by tags
     */
    private List<CSFeatureFile.Scenario> filterScenariosByTags(List<CSFeatureFile.Scenario> scenarios) {
        // If no tags specified, include all scenarios
        if ((tagsToRun == null || tagsToRun.trim().isEmpty()) && 
            (tagsToExclude == null || tagsToExclude.trim().isEmpty())) {
            logger.info("No tag filters specified, including all {} scenarios", scenarios.size());
            return scenarios;
        }
        
        Set<String> includeTags = tagsToRun != null && !tagsToRun.trim().isEmpty() 
            ? new HashSet<>(Arrays.asList(tagsToRun.split(",")))
            : new HashSet<>();
        Set<String> excludeTags = tagsToExclude != null && !tagsToExclude.trim().isEmpty() 
            ? new HashSet<>(Arrays.asList(tagsToExclude.split(",")))
            : new HashSet<>();
        
        logger.info("Filtering scenarios with include tags: {} and exclude tags: {}", includeTags, excludeTags);
        
        return scenarios.stream()
            .filter(scenario -> {
                List<String> scenarioTags = scenario.getTags();
                logger.debug("Scenario '{}' has tags: {}", scenario.getName(), scenarioTags);
                
                // Check exclude tags first
                for (String tag : scenarioTags) {
                    if (excludeTags.contains(tag)) {
                        return false;
                    }
                }
                
                // Check include tags
                if (!includeTags.isEmpty()) {
                    for (String tag : scenarioTags) {
                        if (includeTags.contains(tag)) {
                            return true;
                        }
                    }
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Alternative runner for specific feature file
     */
    @Test(enabled = false)
    @Parameters({"featureFile"})
    public void runSpecificFeature(@org.testng.annotations.Optional("") String featureFile) {
        if (featureFile.isEmpty()) {
            return;
        }
        
        logger.info("Running specific feature: {}", featureFile);
        scenarioRunner.runFeatureFile(featureFile);
    }
    
    /**
     * Alternative runner for all features with specific tag
     */
    @Test(enabled = false)
    @Parameters({"tag"})
    public void runFeaturesWithTag(@org.testng.annotations.Optional("") String tag) {
        if (tag.isEmpty()) {
            return;
        }
        
        logger.info("Running features with tag: {}", tag);
        
        for (String featureFile : featureFiles) {
            try {
                CSFeatureFile feature = featureParser.parseFeatureFile(featureFile);
                
                for (CSFeatureFile.Scenario scenario : feature.getScenarios()) {
                    if (scenario.getTags().contains(tag)) {
                        scenarioRunner.runScenarioFromFile(feature, scenario);
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error running feature: {}", featureFile, e);
            }
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        // Include cause if present
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(getStackTrace(cause));
        }
        
        return sb.toString();
    }
}