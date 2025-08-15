package com.testforge.cs.reporting;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.environment.CSEnvironmentCollector;
import com.testforge.cs.exceptions.CSReportingException;
import com.testforge.cs.utils.CSFileUtils;
import com.testforge.cs.utils.CSJsonUtils;
import com.testforge.cs.driver.CSWebDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages test report generation and data collection
 * Singleton implementation for thread-safe report management
 */
public class CSReportManager {
    private static final Logger logger = LoggerFactory.getLogger(CSReportManager.class);
    private static final CSReportManager instance = new CSReportManager();
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    private final Map<String, CSTestResult> testResults = new ConcurrentHashMap<>();
    private final Map<String, List<CSTestResult>> suiteResults = new ConcurrentHashMap<>();
    private final Map<String, Object> reportMetadata = new ConcurrentHashMap<>();
    private final AtomicInteger passedTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    private final AtomicInteger skippedTests = new AtomicInteger(0);
    
    // Thread-local storage for current step context
    private static final ThreadLocal<CSStepReport> currentStep = new ThreadLocal<>();
    private static final ThreadLocal<CSStepReport> lastCompletedStep = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTestId = new ThreadLocal<>();
    
    // private String reportName; // Not currently used
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reportDirectory;
    
    private CSReportManager() {
        // Private constructor for singleton
    }
    
    public static CSReportManager getInstance() {
        return instance;
    }
    
    /**
     * Initialize report for test suite
     */
    public void initializeReport(String suiteName) {
        logger.info("Initializing report for suite: {}", suiteName);
        
        // this.reportName = suiteName; // Store for later use if needed
        this.startTime = LocalDateTime.now();
        this.reportDirectory = config.getProperty("cs.report.directory", "target/test-reports");
        
        // Create report directory
        File reportDir = new File(reportDirectory);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        
        // Initialize metadata
        reportMetadata.put("suiteName", suiteName);
        reportMetadata.put("startTime", startTime.toString());
        reportMetadata.put("environment", config.getProperty("environment.name", "qa"));
        reportMetadata.put("browser", config.getProperty("browser.name", "chrome"));
        reportMetadata.put("executionMode", config.getProperty("cs.execution.mode", "sequential"));
        reportMetadata.put("operatingSystem", System.getProperty("os.name"));
        reportMetadata.put("javaVersion", System.getProperty("java.version"));
        
        // Add step packages information from suite parameters
        String stepPackages = System.getProperty("suite.cs.step.packages");
        if (stepPackages != null && !stepPackages.isEmpty()) {
            reportMetadata.put("cs.step.packages", stepPackages);
        }
    }
    
    /**
     * Add test result
     */
    public void addTestResult(CSTestResult result) {
        logger.debug("Adding test result: {}", result.getTestName());
        
        testResults.put(result.getTestId(), result);
        
        // Update suite results
        String suiteName = result.getClassName();
        suiteResults.computeIfAbsent(suiteName, k -> new ArrayList<>()).add(result);
        
        // Update counters
        switch (result.getStatus()) {
            case PASSED:
                passedTests.incrementAndGet();
                break;
            case FAILED:
                failedTests.incrementAndGet();
                break;
            case SKIPPED:
                skippedTests.incrementAndGet();
                break;
            case PENDING:
            case RETRIED:
            case BROKEN:
            case RUNNING:
                // Handle other statuses as needed
                break;
        }
    }
    
    /**
     * Generate HTML report
     */
    public void generateReport() {
        try {
            logger.info("Generating test report");
            
            endTime = LocalDateTime.now();
            reportMetadata.put("endTime", endTime.toString());
            reportMetadata.put("duration", calculateDuration());
            
            // Generate report data
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("metadata", reportMetadata);
            reportData.put("summary", generateSummary());
            reportData.put("suites", generateSuiteData());
            reportData.put("testResults", new ArrayList<>(testResults.values()));
            reportData.put("charts", generateChartData());
            
            // Generate HTML report
            String generatedReportPath = generateHtmlReport(reportData);
            String reportPath = null;
            
            if (generatedReportPath != null) {
                // The V5 generator already creates everything in the test-run folder
                // No need to create redundant files in cs-reports root
                logger.info("Report generated successfully: {}", generatedReportPath);
            } else {
                logger.error("Failed to generate HTML report - path is null");
            }
            
        } catch (Exception e) {
            throw new CSReportingException("Failed to generate report", e);
        }
    }
    
    /**
     * Generate summary data
     */
    private Map<String, Object> generateSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int totalTests = testResults.size();
        summary.put("totalTests", totalTests);
        summary.put("passedTests", passedTests.get());
        summary.put("failedTests", failedTests.get());
        summary.put("skippedTests", skippedTests.get());
        summary.put("passRate", totalTests > 0 ? (passedTests.get() * 100.0 / totalTests) : 0);
        summary.put("totalDuration", calculateDuration());
        
        return summary;
    }
    
    /**
     * Generate suite data
     */
    private List<Map<String, Object>> generateSuiteData() {
        List<Map<String, Object>> suiteData = new ArrayList<>();
        
        for (Map.Entry<String, List<CSTestResult>> entry : suiteResults.entrySet()) {
            Map<String, Object> suite = new HashMap<>();
            suite.put("name", entry.getKey());
            
            List<CSTestResult> results = entry.getValue();
            suite.put("totalTests", results.size());
            suite.put("passedTests", results.stream().filter(CSTestResult::isPassed).count());
            suite.put("failedTests", results.stream().filter(CSTestResult::isFailed).count());
            suite.put("skippedTests", results.stream().filter(CSTestResult::isSkipped).count());
            
            long suiteDuration = results.stream()
                .mapToLong(CSTestResult::getDuration)
                .sum();
            suite.put("duration", suiteDuration);
            
            suiteData.add(suite);
        }
        
        return suiteData;
    }
    
    /**
     * Generate chart data
     */
    private Map<String, Object> generateChartData() {
        Map<String, Object> charts = new HashMap<>();
        
        // Status distribution chart
        Map<String, Integer> statusDistribution = new HashMap<>();
        statusDistribution.put("Passed", passedTests.get());
        statusDistribution.put("Failed", failedTests.get());
        statusDistribution.put("Skipped", skippedTests.get());
        charts.put("statusDistribution", statusDistribution);
        
        // Execution time by suite
        Map<String, Long> executionTimeBySuite = suiteResults.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .mapToLong(CSTestResult::getDuration)
                    .sum()
            ));
        charts.put("executionTimeBySuite", executionTimeBySuite);
        
        // Test count by suite
        Map<String, Integer> testCountBySuite = suiteResults.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
        charts.put("testCountBySuite", testCountBySuite);
        
        return charts;
    }
    
    /**
     * Generate HTML report using the V5 generator
     * @return The path to the generated report
     */
    private String generateHtmlReport(Map<String, Object> reportData) {
        // Create CSReportData from the map
        CSReportData csReportData = new CSReportData();
        csReportData.setReportName("CS TestForge Report");
        csReportData.setStartTime(startTime);
        csReportData.setEndTime(endTime);
        csReportData.setDuration(java.time.Duration.between(startTime, endTime));
        csReportData.buildFrom(new ArrayList<>(testResults.values()));
        csReportData.setEnvironment(CSEnvironmentCollector.getInstance().collectEnvironmentInfo().toMap());
        
        // Use the comprehensive HTML report generator
        CSHtmlReportGenerator generator = new CSHtmlReportGenerator();
        String reportPath = generator.generateReport(csReportData, reportDirectory);
        
        // Return the path to the generated report
        return reportPath;
    }
    
    /**
     * Calculate total duration
     */
    private long calculateDuration() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
    
    /**
     * Get test result by ID
     */
    public CSTestResult getTestResult(String testId) {
        return testResults.get(testId);
    }
    
    /**
     * Get all test results
     */
    public Collection<CSTestResult> getAllTestResults() {
        return testResults.values();
    }
    
    /**
     * Get suite results
     */
    public Map<String, List<CSTestResult>> getSuiteResults() {
        return new HashMap<>(suiteResults);
    }
    
    /**
     * Clear all data
     */
    public void clear() {
        testResults.clear();
        suiteResults.clear();
        reportMetadata.clear();
        passedTests.set(0);
        failedTests.set(0);
        skippedTests.set(0);
    }
    
    /**
     * Add custom metadata
     */
    public void addMetadata(String key, Object value) {
        reportMetadata.put(key, value);
    }
    
    /**
     * Get report metadata
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(reportMetadata);
    }
    
    /**
     * Log a test step
     */
    public void logStep(String message, boolean passed) {
        // Implementation will be handled by test context
        logger.info("Step: {} - {}", message, passed ? "PASSED" : "FAILED");
    }
    
    /**
     * Log info message
     */
    public void logInfo(String message) {
        logger.info("Info: {}", message);
    }
    
    /**
     * Log warning message
     */
    public void logWarning(String message) {
        logger.warn("Warning: {}", message);
    }
    
    /**
     * Log error message
     */
    public void logError(String message) {
        logger.error("Error: {}", message);
    }
    
    // ===== Simple Static Logging Methods for Users =====
    
    /**
     * Log a PASS message - static method for easy access
     */
    public static void pass(String message) {
        getInstance().logStep("[PASS] " + message, true);
        logger.info("[PASS] {}", message);
        
        // Take screenshot if configured
        if (config.getBooleanProperty("cs.screenshot.on.pass", false) || 
            config.getBooleanProperty("cs.screenshot.on.all", false)) {
            captureScreenshot("pass_" + System.currentTimeMillis());
        }
    }
    
    /**
     * Log a FAIL message - static method for easy access
     */
    public static void fail(String message) {
        getInstance().logStep("[FAIL] " + message, false);
        logger.error("[FAIL] {}", message);
        
        // Take screenshot if configured (default true for failures)
        if (config.getBooleanProperty("cs.screenshot.on.failure", true) || 
            config.getBooleanProperty("cs.screenshot.on.all", false)) {
            captureScreenshot("fail_" + System.currentTimeMillis());
        }
    }
    
    /**
     * Log a WARNING message - static method for easy access
     */
    public static void warn(String message) {
        getInstance().logWarning("[WARN] " + message);
        logger.warn("[WARN] {}", message);
    }
    
    /**
     * Log an INFO message - static method for easy access
     */
    public static void info(String message) {
        getInstance().logInfo("[INFO] " + message);
        logger.info("[INFO] {}", message);
    }
    
    /**
     * Capture screenshot with automatic naming and reporting
     */
    private static void captureScreenshot(String name) {
        try {
            // Use temp directory to avoid redundancy
            String tempDir = System.getProperty("java.io.tmpdir") + "/cs-temp-screenshots";
            new File(tempDir).mkdirs();
            File screenshotFile = CSWebDriverManager.takeScreenshot(
                tempDir + "/" + name + ".png"
            );
            if (screenshotFile != null && screenshotFile.exists()) {
                logger.info("Screenshot captured: {}", screenshotFile.getAbsolutePath());
                getInstance().logInfo("Screenshot: " + name);
            }
        } catch (Exception e) {
            logger.warn("Failed to capture screenshot: {}", e.getMessage());
        }
    }
    
    /**
     * Start test suite
     */
    public void startTestSuite(String suiteName) {
        initializeReport(suiteName);
    }
    
    /**
     * End test suite
     */
    public void endTestSuite() {
        // Suite ended, ready for report generation
        logger.info("Test suite ended");
    }
    
    /**
     * Start test
     */
    public void startTest(String testName, String className) {
        logger.info("Starting test: {} in class: {}", testName, className);
    }
    
    /**
     * End test
     */
    public void endTest(TestStatus status) {
        endTest(status, null);
    }
    
    /**
     * End test with error
     */
    public void endTest(TestStatus status, Throwable error) {
        logger.info("Test ended with status: {}", status);
        if (error != null) {
            logger.error("Test error", error);
        }
    }
    
    /**
     * Get current suite
     */
    public CSTestSuite getCurrentSuite() {
        // Return current suite for testing
        return new CSTestSuite(reportMetadata.getOrDefault("suiteName", "Test Suite").toString());
    }
    
    /**
     * Attach screenshot
     */
    public String attachScreenshot(byte[] screenshotData, String screenshotName) {
        try {
            // Save screenshot and return path
            String fileName = screenshotName + "_" + System.currentTimeMillis() + ".png";
            
            // Use temp directory to avoid redundancy - these will be moved to test-run folder later
            File tempScreenshotsDir = new File(System.getProperty("java.io.tmpdir"), "cs-temp-screenshots");
            if (!tempScreenshotsDir.exists()) {
                tempScreenshotsDir.mkdirs();
            }
            
            // Save the screenshot to temp location
            File screenshotFile = new File(tempScreenshotsDir, fileName);
            Files.write(screenshotFile.toPath(), screenshotData);
            
            logger.debug("Screenshot temporarily saved: {}", screenshotFile.getAbsolutePath());
            
            // Return the absolute path for now - will be processed by report generator
            return screenshotFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Failed to save screenshot", e);
            return null;
        }
    }
    
    /**
     * Attach screenshot from file path
     */
    public void attachScreenshot(String filePath, String screenshotName) {
        try {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                logger.error("Screenshot file not found: {}", filePath);
                return;
            }
            
            // Use temp directory to avoid redundancy
            File tempScreenshotsDir = new File(System.getProperty("java.io.tmpdir"), "cs-temp-screenshots");
            if (!tempScreenshotsDir.exists()) {
                tempScreenshotsDir.mkdirs();
            }
            
            // Copy the screenshot to temp location
            String fileName = screenshotName + "_" + System.currentTimeMillis() + ".png";
            File destFile = new File(tempScreenshotsDir, fileName);
            Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Add screenshot to current test result using absolute path
            String relativePath = destFile.getAbsolutePath();
            String testId = currentTestId.get();
            if (testId != null) {
                CSTestResult testResult = testResults.get(testId);
                if (testResult != null) {
                    testResult.addScreenshot(relativePath, screenshotName);
                    logger.debug("Added screenshot to test result: {} -> {}", testId, relativePath);
                }
            } else {
                logger.warn("No current test context set, screenshot not associated with any test");
            }
            
            // Log as info
            logInfo("Screenshot: " + screenshotName + " - " + relativePath);
            
            logger.info("Screenshot attached: {} -> {}", filePath, destFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to attach screenshot from path: " + filePath, e);
        }
    }
    
    /**
     * Add custom data
     */
    public void addCustomData(String key, Object value) {
        addMetadata("cs.element." + key, value);
    }
    
    /**
     * Get report data
     */
    public Map<String, Object> getReportData() {
        Map<String, Object> data = new HashMap<>();
        data.put("metadata", getMetadata());
        data.put("summary", generateSummary());
        data.put("suites", generateSuiteData());
        data.put("totalSuites", suiteResults.size());
        data.put("totalTests", 0); // Will be calculated
        data.put("passedTests", passedTests.get());
        data.put("failedTests", failedTests.get());
        data.put("skippedTests", skippedTests.get());
        data.put("customData", reportMetadata);
        return data;
    }
    
    /**
     * Add performance metric
     */
    public void addPerformanceMetric(String metricName, long value) {
        logger.info("Performance metric: {} = {}ms", metricName, value);
    }
    
    /**
     * Add timeline event
     */
    public void addTimelineEvent(String eventName, LocalDateTime start, LocalDateTime end, String threadName) {
        logger.info("Timeline event: {} on thread {} from {} to {}", eventName, threadName, start, end);
    }
    
    /**
     * Get CSReportData instance
     */
    public CSReportData getCSReportData() {
        CSReportData csReportData = new CSReportData();
        csReportData.setReportName("CS TestForge Report");
        csReportData.setStartTime(startTime);
        csReportData.setEndTime(endTime != null ? endTime : LocalDateTime.now());
        csReportData.setDuration(java.time.Duration.between(startTime, csReportData.getEndTime()));
        csReportData.buildFrom(new ArrayList<>(testResults.values()));
        csReportData.setEnvironment(CSEnvironmentCollector.getInstance().collectEnvironmentInfo().toMap());
        return csReportData;
    }
    
    // ===== Step-Level Reporting Methods =====
    
    /**
     * Start a new step
     */
    public static void startStep(String stepType, String stepText) {
        CSStepReport step = new CSStepReport(stepType, stepText);
        currentStep.set(step);
        logger.info("Step started: {} {}", stepType, stepText);
    }
    
    /**
     * End the current step
     */
    public static void endStep() {
        CSStepReport step = currentStep.get();
        if (step != null) {
            step.complete();
            
            // Store the step temporarily before clearing
            lastCompletedStep.set(step);
            
            // Add step to current test result
            String testId = currentTestId.get();
            if (testId != null) {
                CSTestResult testResult = getInstance().testResults.get(testId);
                if (testResult != null) {
                    // Get the last executed step from CSScenarioRunner and enhance it with actions
                    if (!testResult.getExecutedSteps().isEmpty()) {
                        Map<String, Object> lastStep = testResult.getExecutedSteps().get(
                            testResult.getExecutedSteps().size() - 1);
                        
                        // Add the actions to the step
                        lastStep.put("actions", step.getActions().stream()
                            .map(CSStepAction::toMap)
                            .collect(Collectors.toList()));
                    }
                }
            }
            
            logger.info("Step completed: {} - {} ({}ms)", 
                step.getStepText(), step.getStatus(), step.getDuration());
            currentStep.remove();
        }
    }
    
    /**
     * Add an action to the current step
     */
    public static void addAction(String actionType, String description) {
        CSStepReport step = currentStep.get();
        if (step != null) {
            CSStepAction action = new CSStepAction(actionType, description);
            step.addAction(action);
            logger.debug("Action: {} - {}", actionType, description);
        }
    }
    
    /**
     * Add an action with target to the current step
     */
    public static void addAction(String actionType, String description, String target) {
        CSStepReport step = currentStep.get();
        if (step != null) {
            CSStepAction action = new CSStepAction(actionType, description, target);
            step.addAction(action);
            logger.debug("Action: {} - {} on {}", actionType, description, target);
        }
    }
    
    /**
     * Add an action with target and value to the current step
     */
    public static void addAction(String actionType, String description, String target, String value) {
        CSStepReport step = currentStep.get();
        if (step != null) {
            CSStepAction action = new CSStepAction(actionType, description, target, value);
            step.addAction(action);
            logger.debug("Action: {} - {} on {} with value '{}'", actionType, description, target, value);
        }
    }
    
    /**
     * Mark current action as failed
     */
    public static void failAction(String error) {
        CSStepReport step = currentStep.get();
        if (step != null && !step.getActions().isEmpty()) {
            CSStepAction lastAction = step.getActions().get(step.getActions().size() - 1);
            lastAction.setError(error);
            logger.error("Action failed: {}", error);
        }
    }
    
    /**
     * Set current test context for step reporting
     */
    public static void setCurrentTestContext(String testId) {
        currentTestId.set(testId);
    }
    
    /**
     * Clear current test context
     */
    public static void clearCurrentTestContext() {
        currentTestId.remove();
        currentStep.remove();
        lastCompletedStep.remove();
    }
    
    /**
     * Get actions from the last completed step
     */
    public static List<Map<String, Object>> getLastStepActions() {
        CSStepReport step = lastCompletedStep.get();
        if (step != null && step.getActions() != null && !step.getActions().isEmpty()) {
            return step.getActions().stream()
                .map(CSStepAction::toMap)
                .collect(Collectors.toList());
        }
        return null;
    }
}