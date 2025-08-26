package com.testforge.cs.reporting;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.environment.CSEnvironmentCollector;
import com.testforge.cs.exceptions.CSReportingException;
import com.testforge.cs.utils.CSFileUtils;
import com.testforge.cs.utils.CSJsonUtils;
import com.testforge.cs.utils.CSImageUtils;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.bdd.CSScenarioRunner;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Base64;
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
        
        // Store suite name in metadata
        reportMetadata.put("suiteName", suiteName);
        
        // Create report directory
        File reportDir = new File(reportDirectory);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        
        // Initialize metadata with defaults (will be overridden by setExecutionContext if called)
        reportMetadata.put("suiteName", suiteName);
        reportMetadata.put("startTime", startTime.toString());
        reportMetadata.put("environment", config.getProperty("environment.name", "qa"));
        reportMetadata.put("browser", config.getProperty("browser.name", "chrome"));
        reportMetadata.put("executionMode", "sequential"); // Default, will be updated by setExecutionContext
        reportMetadata.put("parallelMode", "none"); // Default
        reportMetadata.put("threadCount", "1"); // Default
        reportMetadata.put("operatingSystem", System.getProperty("os.name"));
        reportMetadata.put("javaVersion", System.getProperty("java.version"));
        
        // Add step packages information from suite parameters
        String stepPackages = System.getProperty("suite.cs.step.packages");
        if (stepPackages != null && !stepPackages.isEmpty()) {
            reportMetadata.put("cs.step.packages", stepPackages);
        }
    }
    
    /**
     * Set the actual execution context from TestNG suite
     */
    public void setExecutionContext(String suiteName, String parallelMode, int threadCount, 
                                   String browser, String environment) {
        logger.info("Setting execution context - Suite: {}, Parallel: {}, Threads: {}, Browser: {}, Env: {}", 
                   suiteName, parallelMode, threadCount, browser, environment);
        
        // Update suite name if provided
        if (suiteName != null && !suiteName.trim().isEmpty()) {
            reportMetadata.put("suiteName", suiteName);
        }
        
        // Determine execution mode based on parallel settings
        boolean isParallel = parallelMode != null && !parallelMode.equals("none") && !parallelMode.equals("false");
        reportMetadata.put("executionMode", isParallel ? "parallel" : "sequential");
        reportMetadata.put("parallelMode", parallelMode != null ? parallelMode : "none");
        reportMetadata.put("threadCount", String.valueOf(threadCount));
        reportMetadata.put("parallelExecution", isParallel ? "Yes" : "No");
        
        // Update browser if provided
        if (browser != null && !browser.trim().isEmpty()) {
            reportMetadata.put("browser", browser);
        }
        
        // Update environment if provided
        if (environment != null && !environment.trim().isEmpty()) {
            reportMetadata.put("environment", environment);
        }
        
        // Capture execution command (Java command line)
        String executionCommand = getExecutionCommand();
        reportMetadata.put("executionCommand", executionCommand);
    }
    
    /**
     * Get the execution command used to run the tests
     */
    private String getExecutionCommand() {
        try {
            // Check if we're running through Maven (surefire)
            String mainClass = System.getProperty("sun.java.command", "");
            if (mainClass.contains("surefire")) {
                // We're running through Maven, build a clean Maven command
                StringBuilder cmd = new StringBuilder("mvn test");
                
                // Add suite file if specified
                String suiteFile = System.getProperty("surefire.suiteXmlFiles");
                if (suiteFile != null && !suiteFile.isEmpty()) {
                    cmd.append(" -Dsurefire.suiteXmlFiles=").append(suiteFile);
                }
                
                // Add test class if specified
                String testClass = System.getProperty("test");
                if (testClass != null && !testClass.isEmpty()) {
                    cmd.append(" -Dtest=").append(testClass);
                }
                
                // Add browser
                String browser = System.getProperty("browser.name");
                if (browser != null && !browser.isEmpty()) {
                    cmd.append(" -Dbrowser.name=").append(browser);
                }
                
                // Add environment
                String environment = System.getProperty("environment.name");
                if (environment != null && !environment.isEmpty()) {
                    cmd.append(" -Denvironment.name=").append(environment);
                }
                
                // Add thread count if specified
                String threadCount = System.getProperty("cs.test.thread.count");
                if (threadCount != null && !threadCount.isEmpty() && !"1".equals(threadCount)) {
                    cmd.append(" -Dcs.test.thread.count=").append(threadCount);
                }
                
                return cmd.toString();
            } else {
                // Not running through Maven, try to build a reasonable command
                StringBuilder cmd = new StringBuilder("java");
                
                // Add key JVM arguments (skip verbose ones)
                List<String> jvmArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
                for (String arg : jvmArgs) {
                    if (arg.startsWith("-D") && !arg.contains("surefire") && !arg.contains("basedir")) {
                        cmd.append(" ").append(arg);
                    }
                }
                
                // Add main class if it's not a surefire booter
                if (!mainClass.contains("surefirebooter")) {
                    cmd.append(" ").append(mainClass);
                }
                
                return cmd.toString();
            }
        } catch (Exception e) {
            logger.warn("Could not determine execution command: {}", e.getMessage());
            return "mvn test"; // Default fallback
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
        
        // Get environment info and add dynamic execution details
        Map<String, String> envMap = CSEnvironmentCollector.getInstance().collectEnvironmentInfo().toMap();
        
        // Add/override with actual execution values from reportMetadata
        envMap.put("browser", reportMetadata.getOrDefault("browser", 
            System.getProperty("browser.name", CSConfigManager.getInstance().getProperty("browser.name", "chrome"))).toString());
        envMap.put("environment", reportMetadata.getOrDefault("environment",
            System.getProperty("environment.name", CSConfigManager.getInstance().getProperty("environment.name", "qa"))).toString());
        envMap.put("suiteName", reportMetadata.getOrDefault("suiteName", "Test Suite").toString());
        envMap.put("executionMode", reportMetadata.getOrDefault("executionMode", "sequential").toString());
        envMap.put("parallelMode", reportMetadata.getOrDefault("parallelMode", "none").toString());
        envMap.put("threadCount", reportMetadata.getOrDefault("threadCount", "1").toString());
        envMap.put("parallelExecution", reportMetadata.getOrDefault("parallelExecution", "No").toString());
        envMap.put("executionCommand", reportMetadata.getOrDefault("executionCommand", "mvn test").toString());
        
        csReportData.setEnvironment(envMap);
        
        // Set execution mode separately
        String executionMode = reportMetadata.getOrDefault("executionMode", "sequential").toString();
        csReportData.setExecutionMode(executionMode);
        
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
        
        // Add to current step as an action
        addAction("PASS", message);
        
        // Take screenshot if configured
        if (config.getBooleanProperty("cs.screenshot.on.pass", false) || 
            config.getBooleanProperty("cs.screenshot.on.all", false)) {
            captureScreenshot("pass_" + System.currentTimeMillis());
        }
    }
    
    /**
     * Log a FAIL message - static method for easy access
     * This is a SOFT FAIL - marks step as failed visually but continues execution
     */
    public static void fail(String message) {
        logger.error("[FAIL] {}", message);
        getInstance().logError("[FAIL] " + message); // Add this like info() does
        
        // Take screenshot first and get path
        String screenshotPath = null;
        // Check configuration before taking screenshot for soft fail
        String captureScreenshotStr = CSConfigManager.getInstance().getProperty("cs.soft.fail.capture.screenshot", "true");
        boolean captureScreenshot = Boolean.parseBoolean(captureScreenshotStr);
        if (captureScreenshot) {
            screenshotPath = captureScreenshotAndGetPath("SOFT_FAIL_" + System.currentTimeMillis() + "_Thread_" + Thread.currentThread().getId());
        }
        
        // CRITICAL: Directly mark step as failed in scenario context FIRST
        // This ensures the step is failed even if addAction() doesn't work
        boolean stepMarkedFailed = false;
        try {
            CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
            if (runner != null) {
                Map<String, Object> scenarioContext = runner.getScenarioContext();
                if (scenarioContext != null) {
                    Map<String, Object> currentStepResult = (Map<String, Object>) scenarioContext.get("current_step_result");
                    if (currentStepResult != null) {
                        // Mark as soft-failed
                        currentStepResult.put("softFailed", true);
                        currentStepResult.put("status", "failed"); // FORCE status to failed
                        currentStepResult.put("error", message); // Need this for error detection
                        currentStepResult.put("failureMessage", message); // Keep for compatibility
                        
                        // Ensure actions list exists and add FAIL action
                        List<Map<String, Object>> actions = (List<Map<String, Object>>) currentStepResult.get("actions");
                        if (actions == null) {
                            actions = new ArrayList<>();
                            currentStepResult.put("actions", actions);
                        }
                        
                        // Create comprehensive FAIL action with all fields including screenshot
                        Map<String, Object> failAction = new HashMap<>();
                        failAction.put("type", "FAIL");
                        failAction.put("actionType", "FAIL");
                        failAction.put("description", message);
                        failAction.put("status", "failed");
                        failAction.put("passed", false);
                        failAction.put("error", message);
                        failAction.put("timestamp", LocalDateTime.now().toString());
                        failAction.put("userInitiated", true); // Mark as user action
                        
                        // Add screenshot path if available
                        if (screenshotPath != null) {
                            failAction.put("screenshot", screenshotPath);
                        } else {
                            failAction.put("screenshot", "null");
                        }
                        
                        actions.add(failAction);
                        
                        stepMarkedFailed = true;
                        logger.info("Step forcefully marked as FAILED with message: {}", message);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error marking step as failed: {}", e.getMessage(), e);
        }
        
        // Only add through normal flow if we couldn't add to scenario context
        if (!stepMarkedFailed) {
            try {
                addAction("FAIL", message);
            } catch (Exception e) {
                logger.debug("Could not add action through normal flow: {}", e.getMessage());
            }
        }
        
        // Log warning if we couldn't mark the step as failed, but DON'T throw exception
        // This is SOFT FAIL - we continue execution
        if (!stepMarkedFailed) {
            logger.warn("Could not mark step as failed through normal means, but continuing execution (soft fail)");
        }
    }
    
    /**
     * Log a WARNING message - static method for easy access
     */
    public static void warn(String message) {
        getInstance().logWarning("[WARN] " + message);
        logger.warn("[WARN] {}", message);
        
        // Add to current step as an action
        addAction("WARN", message);
    }
    
    /**
     * Log an INFO message - static method for easy access
     */
    public static void info(String message) {
        getInstance().logInfo("[INFO] " + message);
        logger.info("[INFO] {}", message);
        
        // Add to current step as an action
        addAction("INFO", message);
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
                
                // Add screenshot to current step result for display in report
                try {
                    CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
                    if (runner != null) {
                        Map<String, Object> scenarioContext = runner.getScenarioContext();
                        if (scenarioContext != null) {
                            Map<String, Object> currentStepResult = (Map<String, Object>) scenarioContext.get("current_step_result");
                            if (currentStepResult != null) {
                                currentStepResult.put("screenshot", screenshotFile.getAbsolutePath());
                                logger.info("Screenshot attached to step result");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Could not attach screenshot to step: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to capture screenshot: {}", e.getMessage());
        }
    }
    
    /**
     * Capture screenshot and return the path or base64 data
     */
    private static String captureScreenshotAndGetPath(String name) {
        logger.info("Attempting to capture screenshot: {}", name);
        
        try {
            // Check if WebDriver is available first
            WebDriver driver = null;
            try {
                driver = CSWebDriverManager.getDriver();
                if (driver == null) {
                    logger.warn("No active WebDriver session available for screenshot capture");
                    return null;
                }
                logger.info("WebDriver available: {}", driver.getClass().getSimpleName());
                
                // Log current URL and page title for debugging
                try {
                    String currentUrl = driver.getCurrentUrl();
                    String pageTitle = driver.getTitle();
                    logger.info("Screenshot capture - Current URL: {}, Page Title: {}", currentUrl, pageTitle);
                    
                    // Check if page is loaded properly
                    if (currentUrl == null || currentUrl.equals("data:,") || currentUrl.startsWith("chrome://")) {
                        logger.warn("WebDriver appears to be on a blank page: {}", currentUrl);
                    }
                } catch (Exception e) {
                    logger.warn("Could not get page details: {}", e.getMessage());
                }
                
                // Wait a moment to ensure page is ready for screenshot
                try {
                    Thread.sleep(1000); // Increased wait to ensure page is ready
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
            } catch (Exception e) {
                logger.warn("Could not get WebDriver for screenshot: {}", e.getMessage());
                return null;
            }
            
            // Use temp directory to avoid redundancy
            String tempDir = System.getProperty("java.io.tmpdir") + "/cs-temp-screenshots";
            new File(tempDir).mkdirs();
            String screenshotPath = tempDir + "/" + name + ".png";
            
            logger.info("Taking screenshot to: {}", screenshotPath);
            File screenshotFile = CSWebDriverManager.takeScreenshot(screenshotPath);
            
            if (screenshotFile != null && screenshotFile.exists()) {
                long fileSize = screenshotFile.length();
                logger.info("Screenshot captured successfully: {} (size: {} bytes)", screenshotFile.getAbsolutePath(), fileSize);
                getInstance().logInfo("Screenshot: " + name + " (" + fileSize + " bytes)");
                
                // Convert screenshot to optimized base64 for embedding in HTML
                String base64Screenshot = null;
                try {
                    // Check if compression is enabled (default: true)
                    boolean enableCompression = config.getBooleanProperty("cs.screenshot.compression.enabled", true);
                    logger.info("Screenshot compression enabled: {}", enableCompression);
                    
                    if (enableCompression) {
                        // Use compressed/optimized version for better performance
                        int maxWidth = config.getIntegerProperty("cs.screenshot.max.width", 800);
                        float quality = config.getFloatProperty("cs.screenshot.quality", 0.7f);
                        
                        logger.info("Compressing screenshot: maxWidth={}, quality={}", maxWidth, quality);
                        base64Screenshot = CSImageUtils.compressImageToBase64(screenshotFile.getAbsolutePath(), maxWidth, quality);
                        
                        if (base64Screenshot != null) {
                            logger.info("Screenshot optimized for web display (size: {})", CSImageUtils.getBase64Size(base64Screenshot));
                        } else {
                            logger.warn("Compression failed, falling back to original");
                        }
                    }
                    
                    // Fallback to original approach if compression disabled or failed
                    if (base64Screenshot == null) {
                        logger.info("Using uncompressed screenshot conversion");
                        byte[] fileContent = Files.readAllBytes(screenshotFile.toPath());
                        base64Screenshot = "data:image/png;base64," + Base64.getEncoder().encodeToString(fileContent);
                        logger.info("Screenshot converted to base64 (uncompressed, size: {})", CSImageUtils.getBase64Size(base64Screenshot));
                    }
                } catch (Exception e) {
                    logger.error("Could not convert screenshot to base64: {}", e.getMessage(), e);
                    base64Screenshot = screenshotFile.getAbsolutePath();
                }
                
                // Add screenshot to current step result for display in report
                try {
                    CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
                    if (runner != null) {
                        Map<String, Object> scenarioContext = runner.getScenarioContext();
                        if (scenarioContext != null) {
                            Map<String, Object> currentStepResult = (Map<String, Object>) scenarioContext.get("current_step_result");
                            if (currentStepResult != null) {
                                // Store both file path and base64 for different uses
                                currentStepResult.put("screenshot", screenshotFile.getAbsolutePath());
                                currentStepResult.put("screenshotBase64", base64Screenshot);
                                logger.info("Screenshot attached to step result: {}", screenshotFile.getName());
                            }
                            
                            // CRITICAL: Also set test-level screenshot for Failure Analysis display
                            // This ensures the screenshot appears in the Failure Analysis modal
                            String testId = currentTestId.get();
                            if (testId != null) {
                                CSTestResult testResult = getInstance().testResults.get(testId);
                                if (testResult != null) {
                                    testResult.setScreenshotPath(base64Screenshot != null ? base64Screenshot : screenshotFile.getAbsolutePath());
                                    logger.info("Screenshot also attached to test result for Failure Analysis: {}", testId);
                                }
                            } else {
                                logger.warn("No current test ID available for screenshot attachment");
                            }
                        } else {
                            logger.warn("No scenario context available for screenshot attachment");
                        }
                    } else {
                        logger.warn("No CSScenarioRunner available for screenshot attachment");
                    }
                } catch (Exception e) {
                    logger.error("Could not attach screenshot to step: {}", e.getMessage(), e);
                }
                
                logger.info("Screenshot processing completed successfully");
                return base64Screenshot != null ? base64Screenshot : screenshotFile.getAbsolutePath();
            } else {
                logger.error("Screenshot file was not created or does not exist: {}", screenshotPath);
            }
        } catch (Exception e) {
            logger.error("Failed to capture screenshot '{}': {}", name, e.getMessage(), e);
        }
        
        logger.warn("Screenshot capture failed for: {}", name);
        return null;
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
        logger.info("Step started in thread {}: {} {}", Thread.currentThread().getName(), stepType, stepText);
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
            logger.info("Added action to CSStepReport: {} - {}", actionType, description);
            return;
        } else {
            logger.warn("No current step in ThreadLocal to add action: {} - {}", actionType, description);
        }
        
        // Simple fallback: try scenario runner context only
        try {
            CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
            if (runner != null) {
                Map<String, Object> scenarioContext = runner.getScenarioContext();
                if (scenarioContext != null) {
                    Map<String, Object> currentStepResult = (Map<String, Object>) scenarioContext.get("current_step_result");
                    if (currentStepResult != null) {
                        List<Map<String, Object>> actions = (List<Map<String, Object>>) currentStepResult.get("actions");
                        if (actions == null) {
                            actions = new ArrayList<>();
                            currentStepResult.put("actions", actions);
                        }
                        
                        Map<String, Object> actionMap = new HashMap<>();
                        actionMap.put("type", actionType);
                        actionMap.put("description", description);
                        actionMap.put("timestamp", LocalDateTime.now().toString());
                        actionMap.put("status", actionType.equals("FAIL") ? "failed" : "passed");
                        actions.add(actionMap);
                        
                        logger.debug("Action added: {} - {}", actionType, description);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not add action: {}", e.getMessage());
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
        if (step != null) {
            logger.info("Found last completed step with {} actions", 
                step.getActions() != null ? step.getActions().size() : 0);
            if (step.getActions() != null && !step.getActions().isEmpty()) {
                List<Map<String, Object>> actionMaps = step.getActions().stream()
                    .map(CSStepAction::toMap)
                    .collect(Collectors.toList());
                logger.info("Returning {} action maps", actionMaps.size());
                return actionMaps;
            }
        } else {
            logger.warn("No last completed step found in ThreadLocal");
        }
        return null;
    }
}