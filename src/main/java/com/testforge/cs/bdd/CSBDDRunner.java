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
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
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
    
    // Track test executions across threads
    private static final java.util.concurrent.atomic.AtomicInteger testCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final Map<String, Integer> threadTestCount = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Long, Boolean> threadHasMoreTests = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track feature file isolation
    private static final Map<String, Set<String>> featureScenarioMap = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track iteration numbers for each scenario outline
    private static final Map<String, java.util.concurrent.atomic.AtomicInteger> scenarioIterationMap = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track which threads have finished their tests
    private static final java.util.concurrent.atomic.AtomicInteger completedTests = new java.util.concurrent.atomic.AtomicInteger(0);
    private static volatile int totalExpectedTests = 0; // Will be set dynamically
    
    @BeforeClass
    @Parameters({"featuresPath", "tags", "excludeTags", "stepDefPackages"})
    public void setupBDDRunner(
            @org.testng.annotations.Optional("features") String featuresPath,
            @org.testng.annotations.Optional("") String tags,
            @org.testng.annotations.Optional("") String excludeTags,
            @org.testng.annotations.Optional("") String stepDefPackages) {
        
        logger.info("@BeforeClass - Setting up BDD Runner on thread: {}", 
            Thread.currentThread().getName());
        
        this.featuresPath = featuresPath;
        this.tagsToRun = tags;
        this.tagsToExclude = excludeTags;
        this.stepDefPackages = stepDefPackages;
        this.featureParser = new CSFeatureParser();
        this.scenarioRunner = new CSScenarioRunner();
        
        // Reset counters
        testCounter.set(0);
        completedTests.set(0);
        threadTestCount.clear();
        scenarioIterationMap.clear();
        
        // Register step definition classes
        registerStepDefinitions();
        
        logger.info("BDD Runner initialized - Features: {}, Tags: {}, Exclude: {}", 
            featuresPath, tags, excludeTags);
        
        // Discover feature files
        discoverFeatureFiles();
    }
    
    @AfterMethod(alwaysRun = true)
    @Override
    public void teardownTest(Method method, ITestResult result) {
        String threadName = Thread.currentThread().getName();
        logger.info("[{}] @AfterMethod for test: {}", threadName, method.getName());
        
        // Call parent teardown
        super.teardownTest(method, result);
        
        // Don't close browsers here - let them be reused by thread pool
        // Browsers will be closed in @AfterClass
    }
    
    @AfterClass(alwaysRun = true)
    public void teardownBDDRunner() {
        String threadName = Thread.currentThread().getName();
        logger.info("BDD Runner @AfterClass for thread: {}", threadName);
        
        // Clean up ALL drivers in the pool, not just current thread's
        // This is important because @AfterClass is only called once, not once per thread
        logger.info("Cleaning up all browsers in the driver pool");
        CSWebDriverManager.quitAllDrivers();
        
        logger.info("Test execution summary for this class instance:");
        logger.info("  Total tests executed: {}", testCounter.get());
        logger.info("  Thread distribution: {}", threadTestCount);
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
     * Uses parallel=true to enable parallel capability
     * The suite's parallel attribute controls actual execution:
     * - parallel="none" → Sequential (TestNG should respect this)
     * - parallel="methods" → Parallel execution
     * 
     * Note: There's a TestNG quirk where DataProvider parallel=true can override suite parallel="none"
     * To force sequential: Set suite parallel="none" AND set -Dtestng.data-provider-thread-count=1
     */
    @DataProvider(name = "featureFiles", parallel = true)
    public Object[][] getFeatureFiles(ITestContext context) {
        System.out.println("\n>>> DataProvider getFeatureFiles() called <<<");
        List<Object[]> testData = new ArrayList<>();
        
        // Get parallel mode from suite configuration
        String parallelMode = context.getSuite().getParallel();
        boolean isParallel = parallelMode != null && !parallelMode.equals("none") && !parallelMode.equals("false");
        
        // IMPORTANT: Control thread count based on suite configuration
        if (!isParallel) {
            // Force sequential execution
            System.setProperty("testng.data-provider-thread-count", "1");
            context.getSuite().getXmlSuite().setDataProviderThreadCount(1);
            logger.info("Suite has parallel='{}' - Forcing sequential execution by setting data-provider-thread-count=1", parallelMode);
        } else {
            // For parallel mode, respect the thread-count from suite
            int suiteThreadCount = context.getSuite().getXmlSuite().getThreadCount();
            if (suiteThreadCount > 0) {
                System.setProperty("testng.data-provider-thread-count", String.valueOf(suiteThreadCount));
                context.getSuite().getXmlSuite().setDataProviderThreadCount(suiteThreadCount);
                logger.info("Suite has thread-count={}, setting data-provider-thread-count to match", suiteThreadCount);
            }
        }
        
        // Implement hierarchical thread count determination
        // Priority: 1. thread-count from XML, 2. data-provider-thread-count from XML, 3. application.properties
        int dataProviderThreadCount = -1;
        String threadCountSource = "unknown";
        
        // Get XmlSuite for accessing attributes
        org.testng.xml.XmlSuite xmlSuite = context.getSuite().getXmlSuite();
        
        if (xmlSuite != null) {
            int xmlThreadCount = xmlSuite.getThreadCount();
            int xmlDataProviderThreadCount = xmlSuite.getDataProviderThreadCount();
            
            // Check if data-provider-thread-count is explicitly set (not default value of 10)
            boolean hasExplicitDataProviderThreadCount = xmlDataProviderThreadCount != 10;
            
            // Check if thread-count is explicitly set (not default value of 1)
            boolean hasExplicitThreadCount = xmlThreadCount > 1;
            
            // Priority logic based on user requirement:
            // 1. If thread-count is explicitly set, use it as the default for data-provider-thread-count
            // 2. However, if data-provider-thread-count is ALSO explicitly set, override with that (more specific)
            // 3. Otherwise fall back to application.properties
            
            if (hasExplicitThreadCount) {
                // thread-count is explicitly set, use it as default
                dataProviderThreadCount = xmlThreadCount;
                threadCountSource = "thread-count attribute from suite XML";
                logger.info("Using thread-count={} from suite XML as default for data-provider-thread-count", xmlThreadCount);
                
                // But check if data-provider-thread-count overrides it
                if (hasExplicitDataProviderThreadCount) {
                    dataProviderThreadCount = xmlDataProviderThreadCount;
                    threadCountSource = "data-provider-thread-count attribute from suite XML (overrides thread-count)";
                    logger.info("Note: Both thread-count={} and data-provider-thread-count={} are specified. " +
                               "Using data-provider-thread-count={} as it explicitly overrides the thread-count default.", 
                               xmlThreadCount, xmlDataProviderThreadCount, xmlDataProviderThreadCount);
                }
            } else if (hasExplicitDataProviderThreadCount) {
                // Only data-provider-thread-count is explicitly set
                dataProviderThreadCount = xmlDataProviderThreadCount;
                threadCountSource = "data-provider-thread-count attribute from suite XML";
                logger.info("Using data-provider-thread-count={} from suite XML", xmlDataProviderThreadCount);
            }
        }
        
        // Third priority: Fall back to application.properties
        if (dataProviderThreadCount <= 0 || dataProviderThreadCount == 10) {
            String propertyThreadCount = config.getProperty("thread.count", "1");
            try {
                dataProviderThreadCount = Integer.parseInt(propertyThreadCount);
                threadCountSource = "thread.count from application.properties";
                logger.info("No thread count specified in suite XML, using thread.count={} from application.properties", dataProviderThreadCount);
            } catch (NumberFormatException e) {
                logger.warn("Invalid thread.count in application.properties: {}, using default of 1", propertyThreadCount);
                dataProviderThreadCount = 1;
                threadCountSource = "default fallback";
            }
        }
        
        // Allow system property override if needed
        String systemThreadCount = System.getProperty("testng.data-provider-thread-count");
        if (systemThreadCount != null) {
            try {
                int overrideCount = Integer.parseInt(systemThreadCount);
                logger.info("System property testng.data-provider-thread-count detected: {} (overrides {})", 
                    overrideCount, threadCountSource);
                // Note: We can't actually change TestNG's thread pool size here, but we log it for awareness
            } catch (NumberFormatException e) {
                logger.warn("Invalid system property testng.data-provider-thread-count: {}", systemThreadCount);
            }
        }
        
        logger.info("DataProvider method called on thread: {} (parallel mode: {})", 
                Thread.currentThread().getName(), parallelMode);
        
        // Log intelligent parallel handling
        if (isParallel) {
            logger.info("Parallel execution detected (mode: {}). DataProvider will run tests concurrently.", parallelMode);
            logger.info("TestNG is using data-provider-thread-count={} (source: {})", dataProviderThreadCount, threadCountSource);
            logger.info("Thread count hierarchy: thread-count (XML) -> data-provider-thread-count (XML) -> application.properties");
        } else {
            logger.info("Sequential execution mode. DataProvider will run tests one at a time.");
            logger.info("Parallel mode is set to: {}", parallelMode != null ? parallelMode : "not specified (defaulting to none)");
        }
        
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
        
        logger.info("DataProvider created {} total test scenarios", testData.size());
        
        // Log scenario details for debugging
        Map<String, Integer> scenarioCount = new HashMap<>();
        for (int i = 0; i < testData.size(); i++) {
            Object[] test = testData.get(i);
            CSFeatureFile.Scenario scenario = (CSFeatureFile.Scenario) test[2];
            String scenarioName = scenario.getName();
            scenarioCount.merge(scenarioName, 1, Integer::sum);
            logger.info("Test #{}: {} - Data: {}", i + 1, scenario.getName(), scenario.getDataRow());
        }
        
        // Log summary
        logger.info("=== SCENARIO SUMMARY ===");
        scenarioCount.forEach((name, count) -> 
            logger.info("  {}: {} instances", name, count));
        
        // Log thread pool information
        logger.info("DataProvider returning {} test cases, parallel={}", testData.size(), true);
        logger.info("Expected to run on {} threads (source: {})", dataProviderThreadCount, threadCountSource);
        
        // Set the total expected tests for cleanup tracking
        totalExpectedTests = testData.size();
        logger.info("Total expected tests set to: {}", totalExpectedTests);
        
        // Print to console for debugging
        System.out.println("\n========== DATAPROVIDER SUMMARY ==========");
        System.out.println("Total test scenarios created: " + testData.size());
        scenarioCount.forEach((name, count) -> 
            System.out.println("  " + name + ": " + count + " instances"));
        System.out.println("==========================================\n");
        
        return testData.toArray(new Object[0][]);
    }
    
    @Override
    @BeforeMethod(alwaysRun = true)
    public void setupTest(Method method, Object[] params, ITestContext context) {
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        logger.info("[{}] @BeforeMethod for: {}", threadName, method.getName());
        
        // Call parent setup first - this creates the driver
        super.setupTest(method, params, context);
        
        logger.info("[{}] After parent setup, driver = {}", threadName, driver);
        
        // IMPORTANT: Each thread needs its own driver instance properly set
        // The parent setupTest creates a new driver, now we need to ensure it's properly
        // registered in all the thread-local storages
        if (driver != null) {
            // Create CSDriver wrapper for this specific thread
            CSDriver csDriver = new CSDriver(driver);
            
            // Set in CSStepDefinitions ThreadLocal - this is critical for step execution
            CSStepDefinitions.setDriver(csDriver);
            
            // Also ensure it's in CSWebDriverManager's ThreadLocal
            CSWebDriverManager.setDriver(driver);
            
            // Log to verify the driver is properly set for this thread
            logger.info("[{}] Thread ID {} - Driver properly initialized and set in all ThreadLocals", threadName, threadId);
            logger.info("[{}] CSWebDriverManager.getDriver() = {}", threadName, CSWebDriverManager.getDriver());
            logger.info("[{}] Driver hashCode = {}", threadName, driver.hashCode());
        } else {
            logger.error("[{}] CRITICAL: Driver is null after parent setup! Thread ID: {}", threadName, threadId);
            throw new RuntimeException("Failed to initialize driver for thread: " + threadName);
        }
    }
    
    /**
     * Test method that executes each scenario
     */
    @Test(dataProvider = "featureFiles", description = "Execute BDD Scenario")
    public void executeBDDScenario(String featureFile, CSFeatureFile feature, CSFeatureFile.Scenario scenario) {
        // Log thread and isolation information
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        logger.info("[{}] Thread ID {} Starting test execution for scenario: {} with data: {}", 
            threadName, threadId, scenario.getName(), scenario.getDataRow());
        logger.info("[ISOLATION] Feature file: {}, Feature name: {}, Scenario: {}", 
            featureFile, feature.getName(), scenario.getName());
        
        // Track thread distribution
        int testNumber = testCounter.incrementAndGet();
        threadTestCount.merge(threadName, 1, Integer::sum);
        
        logger.info("Test execution #{} - Thread: {} (total tests on this thread: {}), Scenario: {}, Data Source: {}", 
            testNumber, threadName, threadTestCount.get(threadName), scenario.getName(), 
            scenario.getExamplesConfig() != null ? scenario.getExamplesConfig() : "Unknown");
        
        // Log thread pool status
        if (testNumber == 1 || testNumber == 4 || testNumber == 7 || testNumber == 10 || testNumber == 12) {
            logger.info("Thread distribution after {} tests: {}", testNumber, threadTestCount);
        }
        
        // Verify driver is available from multiple sources
        logger.info("[{}] Test #{} - Verifying driver availability", threadName, testNumber);
        logger.info("[{}] - Instance driver field: {}", threadName, driver != null ? "AVAILABLE" : "NULL");
        logger.info("[{}] - CSWebDriverManager.getDriver(): {}", threadName, CSWebDriverManager.getDriver() != null ? "AVAILABLE" : "NULL");
        
        // Critical check - ensure driver is properly initialized for this thread
        if (driver == null) {
            logger.error("[{}] CRITICAL ERROR: Instance driver is null at start of test execution!", threadName);
            throw new RuntimeException("Driver not initialized for thread: " + threadName);
        }
        
        if (CSWebDriverManager.getDriver() == null) {
            logger.error("[{}] CRITICAL ERROR: CSWebDriverManager.getDriver() is null!", threadName);
            logger.warn("[{}] Attempting to re-set driver in CSWebDriverManager", threadName);
            CSWebDriverManager.setDriver(driver);
            
            if (CSWebDriverManager.getDriver() == null) {
                throw new RuntimeException("Failed to set driver in CSWebDriverManager for thread: " + threadName);
            }
        }
        
        // Re-ensure CSStepDefinitions has the driver for this thread
        CSDriver csDriver = new CSDriver(driver);
        CSStepDefinitions.setDriver(csDriver);
        logger.info("[{}] Test #{} - Driver re-verified and set in all contexts", threadName, testNumber);
        
        // Track scenario execution per feature file
        featureScenarioMap.computeIfAbsent(featureFile, k -> new HashSet<>()).add(scenario.getName());
        
        // Create a new scenario runner for each test to ensure thread safety and feature isolation
        CSScenarioRunner threadSafeScenarioRunner = new CSScenarioRunner();
        
        // Build proper scenario name with iteration number for data-driven tests
        String scenarioDisplayName = scenario.getName();
        String baseScenarioName = scenario.getName(); // Keep original name for tracking
        
        // For data-driven scenarios (Scenario Outlines with Examples), add iteration number
        if (scenario.getDataRow() != null && !scenario.getDataRow().isEmpty()) {
            // Get or create iteration counter for this scenario
            java.util.concurrent.atomic.AtomicInteger iterationCounter = 
                scenarioIterationMap.computeIfAbsent(baseScenarioName, 
                    k -> new java.util.concurrent.atomic.AtomicInteger(0));
            
            // Increment and get the iteration number
            int iterationNumber = iterationCounter.incrementAndGet();
            
            // Format: ScenarioName_Iteration<number>
            scenarioDisplayName = baseScenarioName + "_Iteration" + iterationNumber;
            
            logger.info("Data-driven scenario: {} (Iteration {})", baseScenarioName, iterationNumber);
            logger.info("Data row for iteration {}: {}", iterationNumber, scenario.getDataRow());
        }
        
        // Ensure we have a valid scenario name
        if (scenarioDisplayName == null || scenarioDisplayName.trim().isEmpty()) {
            scenarioDisplayName = "Unnamed Scenario";
            logger.warn("Scenario has no name, using default: {}", scenarioDisplayName);
        }
        
        // Ensure we have a valid feature name
        String featureName = feature.getName();
        if (featureName == null || featureName.trim().isEmpty()) {
            featureName = "Unnamed Feature";
            logger.warn("Feature has no name, using default: {}", featureName);
        }
        
        String scenarioName = featureName + " - " + scenarioDisplayName;
        
        // Log feature file isolation info
        logger.info("[ISOLATION] Executing scenario '{}' from feature file: {}", scenarioDisplayName, featureFile);
        logger.info("[ISOLATION] Feature '{}' has executed scenarios: {}", 
            featureName, featureScenarioMap.get(featureFile));
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
        testResult.setEnvironment(config.getProperty("environment.name", "qa"));
        testResult.setBrowser(config.getProperty("browser.name", "chrome"));
        testResult.setThreadName(Thread.currentThread().getName());
        testResult.setScenarioName(scenarioDisplayName);
        
        // Set current test context for reporting
        CSReportManager.setCurrentTestContext(testResult.getTestId());
        
        // Set test data information if available
        if (scenario.getDataRow() != null && !scenario.getDataRow().isEmpty()) {
            logger.info("[{}] Scenario has data row: {}", threadName, scenario.getDataRow());
            Map<String, Object> testData = new HashMap<>();
            testData.putAll(scenario.getDataRow());
            
            // Determine data source type
            String dataSource = "Unknown";
            String sourceFile = "";
            
            // Check if it's from JSON configuration in Examples
            if (scenario.getExamplesConfig() != null) {
                try {
                    // Parse JSON configuration to extract source information
                    String config = scenario.getExamplesConfig();
                    if (config.contains("type")) {
                        if (config.contains("csv")) {
                            dataSource = "CSV";
                            // Extract source file path
                            int sourceStart = config.indexOf("source") + 9;
                            int sourceEnd = config.indexOf("\"", sourceStart);
                            if (sourceStart > 8 && sourceEnd > sourceStart) {
                                sourceFile = config.substring(sourceStart, sourceEnd);
                            }
                        } else if (config.contains("excel")) {
                            dataSource = "Excel";
                        } else if (config.contains("json")) {
                            dataSource = "JSON";
                        } else if (config.contains("database")) {
                            dataSource = "Database";
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse data source config: {}", e.getMessage());
                }
            } else {
                // Check tags for @CSDataSource
                for (String tag : scenario.getTags()) {
                    if (tag.contains("csv")) {
                        dataSource = "CSV";
                    } else if (tag.contains("excel")) {
                        dataSource = "Excel";
                    } else if (tag.contains("json")) {
                        dataSource = "JSON";
                    } else if (tag.contains("database")) {
                        dataSource = "Database";
                    }
                }
            }
            
            testData.put("dataSourceType", dataSource);
            testData.put("dataSourceFile", sourceFile);
            testResult.setTestData(testData);
        }
        
        // Set proper suite name and feature file
        testResult.setSuiteName("Simple Sequential Test Suite");
        testResult.setFeatureFile(new File(featureFile).getName());
        
        try {
            // Create a deep copy of the scenario to avoid thread and feature interference
            CSFeatureFile.Scenario scenarioCopy = createScenarioCopy(scenario);
            
            // Ensure complete isolation by clearing any shared state
            ensureScenarioIsolation(threadName, featureFile, feature.getName(), scenario.getName());
            
            // Log driver instance and scenario data
            logger.info("[{}] Driver instance: {}", threadName, driver);
            logger.info("[{}] CSWebDriverManager driver: {}", threadName, CSWebDriverManager.getDriver());
            logger.info("[{}] Scenario copy data row: {}", threadName, scenarioCopy.getDataRow());
            
            // Execute scenario with the copy and feature file context
            threadSafeScenarioRunner.runScenarioFromFile(featureFile, feature, scenarioCopy);
            
            // Get executed steps from scenario context
            Map<String, Object> scenarioContext = threadSafeScenarioRunner.getScenarioContext();
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
            Map<String, Object> scenarioContext = threadSafeScenarioRunner.getScenarioContext();
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
            
            // Clear test context
            CSReportManager.clearCurrentTestContext();
            
            // Track test completion
            int completed = completedTests.incrementAndGet();
            logger.info("[{}] Test completed. Total completed: {}/{}", threadName, completed, totalExpectedTests);
            
            // Note: Final cleanup will be handled by @AfterSuite and shutdown hook
            // This ensures all browsers are properly closed even if tests fail
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
     * Create a deep copy of a scenario to ensure thread safety
     */
    private CSFeatureFile.Scenario createScenarioCopy(CSFeatureFile.Scenario original) {
        CSFeatureFile.Scenario copy = new CSFeatureFile.Scenario();
        copy.setName(original.getName());
        copy.setDescription(original.getDescription());
        copy.setTags(new ArrayList<>(original.getTags()));
        copy.setOutline(original.isOutline());
        copy.setExamplesConfig(original.getExamplesConfig());
        
        // Deep copy the data row
        if (original.getDataRow() != null) {
            Map<String, String> dataRowCopy = new HashMap<>();
            dataRowCopy.putAll(original.getDataRow());
            copy.setDataRow(dataRowCopy);
        }
        
        // Deep copy steps
        List<CSFeatureFile.Step> stepsCopy = new ArrayList<>();
        for (CSFeatureFile.Step step : original.getSteps()) {
            CSFeatureFile.Step stepCopy = new CSFeatureFile.Step();
            stepCopy.setKeyword(step.getKeyword());
            stepCopy.setText(step.getText());
            stepCopy.setLineNumber(step.getLineNumber());
            
            // Copy data table if present
            if (step.getDataTable() != null) {
                List<List<String>> tableCopy = new ArrayList<>();
                for (List<String> row : step.getDataTable()) {
                    tableCopy.add(new ArrayList<>(row));
                }
                stepCopy.setDataTable(tableCopy);
            }
            
            // Copy doc string if present
            if (step.getDocString() != null) {
                stepCopy.setDocString(step.getDocString());
            }
            
            stepsCopy.add(stepCopy);
        }
        copy.setSteps(stepsCopy);
        
        return copy;
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
                        scenarioRunner.runScenarioFromFile(featureFile, feature, scenario);
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error running feature: {}", featureFile, e);
            }
        }
    }
    
    /**
     * Ensure complete isolation between scenarios
     */
    private void ensureScenarioIsolation(String threadName, String featureFile, String featureName, String scenarioName) {
        logger.debug("[{}] Ensuring isolation for scenario '{}' from feature '{}' ({})", 
            threadName, scenarioName, featureName, featureFile);
        
        // Clear any thread-local state that might leak between scenarios
        // This is especially important when running multiple feature files
        CSStepDefinitions.clearThreadLocalState();
        
        // Log current isolation state
        logger.debug("[ISOLATION] Current feature scenario map: {}", featureScenarioMap);
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