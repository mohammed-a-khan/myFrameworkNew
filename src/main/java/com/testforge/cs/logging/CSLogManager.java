package com.testforge.cs.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Manages logging configuration and log file storage for test runs
 * Provides functionality to capture and store logs alongside test reports
 */
public class CSLogManager {
    private static final Logger logger = LoggerFactory.getLogger(CSLogManager.class);
    private static String currentTestRunDirectory;
    private static String originalLogFile = getLogFilePath();
    private static boolean logCaptureEnabled = false;
    
    /**
     * Get the log file path, trying multiple possible locations
     */
    private static String getLogFilePath() {
        // Try multiple possible log file locations
        String[] possiblePaths = {
            "target/test.log",                    // Default location
            "./target/test.log",                  // Explicit relative path
            System.getProperty("user.dir") + "/target/test.log",  // Absolute path from working directory
            "test.log",                           // Fallback in current directory
            "target/logs/cs-framework.log"        // Alternative location from config
        };
        
        for (String path : possiblePaths) {
            File logFile = new File(path);
            if (logFile.exists() && logFile.canRead()) {
                logger.debug("Found log file at: {}", logFile.getAbsolutePath());
                return path;
            }
        }
        
        // If no existing log file found, return the default path
        logger.debug("No existing log file found, using default: target/test.log");
        return "target/test.log";
    }
    
    /**
     * Initialize log capture for a specific test run directory
     * This should be called when a test run starts
     */
    public static void initializeLogCapture(String testRunDirectory) {
        try {
            currentTestRunDirectory = testRunDirectory;
            logCaptureEnabled = true;
            logger.info("Log capture initialized for test run: {}", testRunDirectory);
            logger.info("Current working directory: {}", System.getProperty("user.dir"));
            logger.info("Log file path resolved to: {}", originalLogFile);
            
            // Check if log file exists at initialization
            File logFile = new File(originalLogFile);
            if (logFile.exists()) {
                logger.info("Log file found at initialization: {} (size: {} bytes)", 
                    logFile.getAbsolutePath(), logFile.length());
            } else {
                logger.warn("Log file not found at initialization: {}", logFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to initialize log capture: {}", e.getMessage());
            logCaptureEnabled = false;
        }
    }
    
    /**
     * Finalize log capture and copy logs to the test run directory
     * This should be called when report generation is complete
     */
    public static void finalizeLogCapture() {
        if (!logCaptureEnabled || currentTestRunDirectory == null) {
            return;
        }
        
        try {
            // Copy the log file to test run directory
            copyLogToTestRun();
            
            logger.info("Log capture finalized for test run: {}", currentTestRunDirectory);
        } catch (Exception e) {
            logger.warn("Failed to finalize log capture: {}", e.getMessage());
        } finally {
            logCaptureEnabled = false;
            currentTestRunDirectory = null;
        }
    }
    
    /**
     * Copy the current log file to the test run directory
     */
    private static void copyLogToTestRun() {
        try {
            // Refresh log file path in case it changed during execution
            String currentLogPath = getLogFilePath();
            File originalLog = new File(currentLogPath);
            
            if (!originalLog.exists()) {
                logger.warn("Original log file not found: {}. Trying alternative locations...", currentLogPath);
                
                // Try to find any log file in common locations
                String[] fallbackPaths = {
                    "target/surefire-reports/testng-results.xml.log",
                    "target/surefire-reports/*.log",
                    "*.log"
                };
                
                boolean found = false;
                for (String fallbackPath : fallbackPaths) {
                    File fallbackLog = new File(fallbackPath);
                    if (fallbackLog.exists()) {
                        originalLog = fallbackLog;
                        found = true;
                        logger.info("Using fallback log file: {}", fallbackLog.getAbsolutePath());
                        break;
                    }
                }
                
                if (!found) {
                    logger.warn("No log file found in any location. Current working directory: {}", 
                        System.getProperty("user.dir"));
                    return;
                }
            }
            
            // Create test run directory if it doesn't exist
            File testRunDir = new File(currentTestRunDirectory);
            if (!testRunDir.exists()) {
                testRunDir.mkdirs();
            }
            
            // Copy log file to test run directory
            String logFileName = "test-execution.log";
            File testRunLog = new File(testRunDir, logFileName);
            
            Path source = originalLog.toPath();
            Path target = testRunLog.toPath();
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("Log file copied to test run directory: {}", testRunLog.getAbsolutePath());
            
            // Also create a console output capture if available
            createConsoleOutputSummary(testRunDir);
            
        } catch (IOException e) {
            logger.warn("Failed to copy log file to test run directory: {}", e.getMessage());
        }
    }
    
    /**
     * Create a summary of console output for the test run
     */
    private static void createConsoleOutputSummary(File testRunDir) {
        try {
            File summaryFile = new File(testRunDir, "console-summary.txt");
            StringBuilder summary = new StringBuilder();
            
            summary.append("=== Test Execution Console Summary ===\n");
            summary.append("Generated: ").append(new java.util.Date()).append("\n");
            summary.append("Test Run Directory: ").append(testRunDir.getName()).append("\n");
            summary.append("\n");
            summary.append("Note: Full detailed logs are available in test-execution.log\n");
            summary.append("\n");
            summary.append("This summary provides key information about the test execution:\n");
            summary.append("- All console output has been captured in test-execution.log\n");
            summary.append("- Screenshots are stored in the screenshots/ folder (if not embedded)\n");
            summary.append("- HTML test report is available as cs_test_run_report.html\n");
            summary.append("- Test run data is stored in report-data.json\n");
            summary.append("\n");
            summary.append("For complete details, please refer to the test-execution.log file.\n");
            
            Files.write(summaryFile.toPath(), summary.toString().getBytes());
            
        } catch (IOException e) {
            logger.warn("Failed to create console output summary: {}", e.getMessage());
        }
    }
    
    /**
     * Check if log capture is currently enabled
     */
    public static boolean isLogCaptureEnabled() {
        return logCaptureEnabled;
    }
    
    /**
     * Get the current test run directory for logging
     */
    public static String getCurrentTestRunDirectory() {
        return currentTestRunDirectory;
    }
    
    /**
     * Debug method to help troubleshoot log file location issues
     */
    public static void debugLogConfiguration() {
        System.out.println("=== CSLogManager Debug Information ===");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Current Log File Path: " + originalLogFile);
        System.out.println("Log Capture Enabled: " + logCaptureEnabled);
        System.out.println("Test Run Directory: " + currentTestRunDirectory);
        
        // Check if log file exists
        File logFile = new File(originalLogFile);
        System.out.println("Log File Exists: " + logFile.exists());
        System.out.println("Log File Absolute Path: " + logFile.getAbsolutePath());
        System.out.println("Log File Can Read: " + logFile.canRead());
        
        // List target directory contents
        File targetDir = new File("target");
        if (targetDir.exists()) {
            System.out.println("Target Directory Contents:");
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains("log") || file.getName().endsWith(".log")) {
                        System.out.println("  - " + file.getName() + " (exists: " + file.exists() + ")");
                    }
                }
            }
        } else {
            System.out.println("Target directory does not exist");
        }
        System.out.println("===========================================");
    }
}