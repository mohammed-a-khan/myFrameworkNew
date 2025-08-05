package com.testforge.cs.reporting;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.environment.CSEnvironmentCollector;
import com.testforge.cs.exceptions.CSReportingException;
import com.testforge.cs.utils.CSFileUtils;
import com.testforge.cs.utils.CSJsonUtils;
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
    
    private String reportName;
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
        
        this.reportName = suiteName;
        this.startTime = LocalDateTime.now();
        this.reportDirectory = config.getProperty("report.directory", "target/test-reports");
        
        // Create report directory
        File reportDir = new File(reportDirectory);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        
        // Initialize metadata
        reportMetadata.put("suiteName", suiteName);
        reportMetadata.put("startTime", startTime.toString());
        reportMetadata.put("environment", config.getProperty("env.current", "qa"));
        reportMetadata.put("browser", config.getProperty("browser.default", "chrome"));
        reportMetadata.put("executionMode", config.getProperty("execution.mode", "sequential"));
        reportMetadata.put("operatingSystem", System.getProperty("os.name"));
        reportMetadata.put("javaVersion", System.getProperty("java.version"));
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
                // The V5 generator already creates the report file, so we don't need to write it again
                // Just create a summary report file that points to the generated report
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(startTime);
                String reportFileName = String.format("test-report_%s.html", timestamp);
                reportPath = reportDirectory + File.separator + reportFileName;
                
                // Create a simple redirect to the actual report
                String redirectContent = String.format(
                    "<html><head><meta http-equiv=\"refresh\" content=\"0;url=%s\"></head><body>Redirecting to report...</body></html>",
                    generatedReportPath.replace(reportDirectory + File.separator, "")
                );
                CSFileUtils.writeStringToFile(reportPath, redirectContent);
                
                // Write JSON data to cs-reports temporarily (will be moved by CSReportGeneratorV3)
                String jsonPath = "cs-reports" + File.separator + "report-data.json";
                CSJsonUtils.writeJsonFile(jsonPath, reportData, true);
                
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
            
            // Create screenshots directory in report directory
            File screenshotsDir = new File(reportDirectory, "screenshots");
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs();
            }
            
            // Save the screenshot
            File screenshotFile = new File(screenshotsDir, fileName);
            Files.write(screenshotFile.toPath(), screenshotData);
            
            logger.info("Screenshot saved: {}", screenshotFile.getAbsolutePath());
            
            // Return relative path for HTML report
            return "screenshots/" + fileName;
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
            
            // Create screenshots directory in report directory
            File screenshotsDir = new File(reportDirectory, "screenshots");
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs();
            }
            
            // Copy the screenshot to report directory
            String fileName = screenshotName + "_" + System.currentTimeMillis() + ".png";
            File destFile = new File(screenshotsDir, fileName);
            Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Add screenshot to current test result
            String relativePath = "screenshots/" + fileName;
            String currentTestId = reportMetadata.get("currentTestId") != null ? 
                reportMetadata.get("currentTestId").toString() : null;
            if (currentTestId != null) {
                CSTestResult testResult = testResults.get(currentTestId);
                if (testResult != null) {
                    testResult.addScreenshot(relativePath, screenshotName);
                }
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
        addMetadata("custom." + key, value);
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
}