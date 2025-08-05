package com.testforge.cs.reporting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhances report data with real execution information
 */
public class CSReportDataEnhancer {
    
    private static final String HISTORY_DIR = "cs-reports/history";
    private static final Map<String, Object> CACHED_ENV_DATA = new ConcurrentHashMap<>();
    
    /**
     * Get real branch name from Git or environment
     */
    public static String getBranchName() {
        // Try environment variables first
        String branch = System.getenv("GIT_BRANCH");
        if (branch == null) branch = System.getenv("BRANCH_NAME");
        if (branch == null) branch = System.getenv("CI_COMMIT_REF_NAME");
        
        // Try to get from git command
        if (branch == null) {
            try {
                Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
                process.waitFor();
                if (process.exitValue() == 0) {
                    branch = new String(process.getInputStream().readAllBytes()).trim();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return branch != null ? branch : "local";
    }
    
    /**
     * Get real build number from environment
     */
    public static String getBuildNumber() {
        String build = System.getenv("BUILD_NUMBER");
        if (build == null) build = System.getenv("BUILD_ID");
        if (build == null) build = System.getenv("CI_PIPELINE_ID");
        if (build == null) build = System.getenv("GITHUB_RUN_NUMBER");
        
        if (build == null) {
            // Generate local build number based on timestamp
            build = "local-" + System.currentTimeMillis() / 1000;
        }
        
        return build;
    }
    
    /**
     * Get real commit hash
     */
    public static String getCommitHash() {
        String commit = System.getenv("GIT_COMMIT");
        if (commit == null) commit = System.getenv("CI_COMMIT_SHA");
        if (commit == null) commit = System.getenv("GITHUB_SHA");
        
        if (commit == null) {
            try {
                Process process = Runtime.getRuntime().exec("git rev-parse HEAD");
                process.waitFor();
                if (process.exitValue() == 0) {
                    commit = new String(process.getInputStream().readAllBytes()).trim();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return commit != null ? commit.substring(0, Math.min(commit.length(), 8)) : "unknown";
    }
    
    /**
     * Get environment details
     */
    public static Map<String, String> getEnvironmentDetails() {
        Map<String, String> env = new HashMap<>();
        
        // System details
        env.put("os.name", System.getProperty("os.name", "Unknown"));
        env.put("os.version", System.getProperty("os.version", "Unknown"));
        env.put("os.arch", System.getProperty("os.arch", "Unknown"));
        env.put("java.version", System.getProperty("java.version", "Unknown"));
        env.put("java.vendor", System.getProperty("java.vendor", "Unknown"));
        env.put("java.home", System.getProperty("java.home", "Unknown"));
        
        // Runtime details
        env.put("user.name", System.getProperty("user.name", "Unknown"));
        env.put("user.timezone", System.getProperty("user.timezone", "Unknown"));
        env.put("file.encoding", System.getProperty("file.encoding", "Unknown"));
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        env.put("memory.max", maxMemory + " MB");
        env.put("memory.total", totalMemory + " MB");
        env.put("memory.free", freeMemory + " MB");
        env.put("processors", String.valueOf(runtime.availableProcessors()));
        
        // Test framework details
        env.put("testng.version", getTestNGVersion());
        env.put("selenium.version", getSeleniumVersion());
        env.put("cucumber.version", getCucumberVersion());
        
        // CI/CD Environment
        if (System.getenv("CI") != null) {
            env.put("ci.platform", detectCIPlatform());
            env.put("ci.build.url", System.getenv("BUILD_URL") != null ? System.getenv("BUILD_URL") : "N/A");
        }
        
        return env;
    }
    
    /**
     * Calculate test reliability (flaky test detection)
     */
    public static double calculateTestReliability(String testName, List<HistoricalTestResult> history) {
        if (history.isEmpty()) return 100.0;
        
        // Look at last 10 runs
        List<HistoricalTestResult> recentRuns = history.stream()
            .filter(h -> h.getTestName().equals(testName))
            .sorted((a, b) -> b.getExecutionTime().compareTo(a.getExecutionTime()))
            .limit(10)
            .collect(Collectors.toList());
        
        if (recentRuns.size() < 3) return 100.0; // Not enough data
        
        // Count status changes
        int statusChanges = 0;
        for (int i = 1; i < recentRuns.size(); i++) {
            if (!recentRuns.get(i).getStatus().equals(recentRuns.get(i-1).getStatus())) {
                statusChanges++;
            }
        }
        
        // More status changes = less reliable
        double reliability = 100.0 - (statusChanges * 10.0);
        return Math.max(0, Math.min(100, reliability));
    }
    
    /**
     * Load historical test results
     */
    public static List<HistoricalTestResult> loadHistoricalResults() {
        List<HistoricalTestResult> history = new ArrayList<>();
        
        try {
            Path historyPath = Paths.get(HISTORY_DIR);
            if (Files.exists(historyPath)) {
                Files.walk(historyPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> {
                        try {
                            // Parse historical result file
                            String content = Files.readString(file);
                            // Add parsing logic here
                        } catch (IOException e) {
                            // Ignore individual file errors
                        }
                    });
            }
        } catch (IOException e) {
            // Return empty history if can't read
        }
        
        return history;
    }
    
    /**
     * Calculate trend compared to previous run
     */
    public static TrendData calculateTrend(CSReportData currentData, List<HistoricalTestResult> history) {
        TrendData trend = new TrendData();
        
        // Find previous run
        Optional<ExecutionSummary> previousRun = findPreviousRun(history);
        
        if (previousRun.isPresent()) {
            ExecutionSummary prev = previousRun.get();
            
            // Test count trend
            int currentTotal = currentData.getTotalTests();
            int prevTotal = prev.getTotalTests();
            trend.setTestCountTrend(calculatePercentageChange(prevTotal, currentTotal));
            
            // Pass rate trend
            double currentPassRate = currentData.getPassRate();
            double prevPassRate = prev.getPassRate();
            trend.setPassRateTrend(currentPassRate - prevPassRate);
            
            // Duration trend
            long currentDuration = currentData.getDuration().toMillis();
            long prevDuration = prev.getDuration();
            trend.setDurationTrend(calculatePercentageChange(prevDuration, currentDuration));
        }
        
        return trend;
    }
    
    private static double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) return 0;
        return ((newValue - oldValue) / oldValue) * 100;
    }
    
    private static String getTestNGVersion() {
        try {
            return org.testng.TestNG.class.getPackage().getImplementationVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private static String getSeleniumVersion() {
        try {
            return org.openqa.selenium.WebDriver.class.getPackage().getImplementationVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private static String getCucumberVersion() {
        // Would need to check actual Cucumber classes
        return "7.x";
    }
    
    private static String detectCIPlatform() {
        if (System.getenv("JENKINS_URL") != null) return "Jenkins";
        if (System.getenv("GITHUB_ACTIONS") != null) return "GitHub Actions";
        if (System.getenv("GITLAB_CI") != null) return "GitLab CI";
        if (System.getenv("CIRCLECI") != null) return "CircleCI";
        if (System.getenv("TRAVIS") != null) return "Travis CI";
        return "Unknown";
    }
    
    private static Optional<ExecutionSummary> findPreviousRun(List<HistoricalTestResult> history) {
        // Implementation to find previous run summary
        return Optional.empty();
    }
    
    // Helper classes
    public static class HistoricalTestResult {
        private String testName;
        private String status;
        private LocalDateTime executionTime;
        private long duration;
        
        // Getters and setters
        public String getTestName() { return testName; }
        public String getStatus() { return status; }
        public LocalDateTime getExecutionTime() { return executionTime; }
    }
    
    public static class ExecutionSummary {
        private int totalTests;
        private double passRate;
        private long duration;
        
        // Getters
        public int getTotalTests() { return totalTests; }
        public double getPassRate() { return passRate; }
        public long getDuration() { return duration; }
    }
    
    public static class TrendData {
        private double testCountTrend;
        private double passRateTrend;
        private double durationTrend;
        
        // Getters and setters
        public void setTestCountTrend(double trend) { this.testCountTrend = trend; }
        public void setPassRateTrend(double trend) { this.passRateTrend = trend; }
        public void setDurationTrend(double trend) { this.durationTrend = trend; }
        
        public double getTestCountTrend() { return testCountTrend; }
        public double getPassRateTrend() { return passRateTrend; }
        public double getDurationTrend() { return durationTrend; }
    }
}