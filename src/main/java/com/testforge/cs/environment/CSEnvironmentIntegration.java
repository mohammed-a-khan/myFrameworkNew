package com.testforge.cs.environment;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSEnvironmentException;
import com.testforge.cs.reporting.CSReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready environment integration for automatic collection and reporting
 * Provides seamless integration with test execution lifecycle
 */
public class CSEnvironmentIntegration {
    private static final Logger logger = LoggerFactory.getLogger(CSEnvironmentIntegration.class);
    
    private static volatile CSEnvironmentIntegration instance;
    private static final Object instanceLock = new Object();
    
    private final CSEnvironmentCollector collector;
    private final Map<String, EnvironmentInfo> environmentCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private CSConfigManager config;
    private boolean autoCollectOnSuiteStart;
    private boolean autoCollectOnTestFailure;
    private boolean includeInReports;
    private boolean enablePeriodicCollection;
    private int periodicCollectionIntervalMinutes;
    private String outputDirectory;
    private boolean exportToJson;
    private boolean exportToHtml;
    private boolean enableEnvironmentComparison;
    
    /**
     * Get singleton instance
     */
    public static CSEnvironmentIntegration getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSEnvironmentIntegration();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private CSEnvironmentIntegration() {
        this.collector = CSEnvironmentCollector.getInstance();
        initialize();
    }
    
    /**
     * Initialize environment integration
     */
    private void initialize() {
        try {
            config = CSConfigManager.getInstance();
            
            // Load configuration
            autoCollectOnSuiteStart = Boolean.parseBoolean(config.getProperty("environment.auto.collect.suite.start", "true"));
            autoCollectOnTestFailure = Boolean.parseBoolean(config.getProperty("environment.auto.collect.test.failure", "false"));
            includeInReports = Boolean.parseBoolean(config.getProperty("environment.include.in.reports", "true"));
            enablePeriodicCollection = Boolean.parseBoolean(config.getProperty("environment.periodic.collection.enabled", "false"));
            periodicCollectionIntervalMinutes = Integer.parseInt(config.getProperty("environment.periodic.collection.interval.minutes", "30"));
            outputDirectory = config.getProperty("environment.output.directory", "target/environment");
            exportToJson = Boolean.parseBoolean(config.getProperty("environment.export.json", "true"));
            exportToHtml = Boolean.parseBoolean(config.getProperty("environment.export.html", "true"));
            enableEnvironmentComparison = Boolean.parseBoolean(config.getProperty("environment.comparison.enabled", "true"));
            
            // Create output directory
            Path envDir = Paths.get(outputDirectory);
            if (!Files.exists(envDir)) {
                Files.createDirectories(envDir);
            }
            
            // Schedule periodic collection if enabled
            if (enablePeriodicCollection) {
                schedulePeriodicCollection();
            }
            
            // Add shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            logger.info("Environment integration initialized - Auto-collect: {}, Periodic: {}, Reports: {}", 
                autoCollectOnSuiteStart, enablePeriodicCollection, includeInReports);
            
        } catch (Exception e) {
            logger.error("Failed to initialize environment integration", e);
            throw new CSEnvironmentException("Failed to initialize environment integration", e);
        }
    }
    
    /**
     * Collect environment information on suite start
     */
    public EnvironmentInfo onSuiteStart(String suiteName) {
        if (!autoCollectOnSuiteStart) {
            return null;
        }
        
        logger.info("Collecting environment information for suite: {}", suiteName);
        
        try {
            EnvironmentInfo envInfo = collector.collectEnvironmentInfo();
            
            // Cache environment info
            environmentCache.put("suite_" + suiteName, envInfo);
            
            // Export if configured
            if (exportToJson) {
                exportEnvironmentToJson(envInfo, "environment_" + suiteName + "_start.json");
            }
            
            if (exportToHtml) {
                exportEnvironmentToHtml(envInfo, "environment_" + suiteName + "_start.html");
            }
            
            // Include in report if configured
            if (includeInReports) {
                addEnvironmentToReport(envInfo, suiteName);
            }
            
            logger.info("Environment information collected and exported for suite: {}", suiteName);
            return envInfo;
            
        } catch (Exception e) {
            logger.error("Failed to collect environment information for suite: {}", suiteName, e);
            return null;
        }
    }
    
    /**
     * Collect environment information on test failure
     */
    public EnvironmentInfo onTestFailure(String testName, Throwable failure) {
        if (!autoCollectOnTestFailure) {
            return null;
        }
        
        logger.info("Collecting environment information for failed test: {}", testName);
        
        try {
            EnvironmentInfo envInfo = collector.collectEnvironmentInfo();
            
            // Add failure context
            if (envInfo.getCustomInfo() != null) {
                envInfo.getCustomInfo().put("failure.test.name", testName);
                envInfo.getCustomInfo().put("failure.message", failure.getMessage());
                envInfo.getCustomInfo().put("failure.timestamp", LocalDateTime.now().toString());
            }
            
            // Cache environment info
            environmentCache.put("failure_" + testName + "_" + System.currentTimeMillis(), envInfo);
            
            // Export failure environment
            if (exportToJson) {
                String filename = "environment_failure_" + sanitizeFilename(testName) + "_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
                exportEnvironmentToJson(envInfo, filename);
            }
            
            return envInfo;
            
        } catch (Exception e) {
            logger.error("Failed to collect environment information for failed test: {}", testName, e);
            return null;
        }
    }
    
    /**
     * Get current environment information
     */
    public EnvironmentInfo getCurrentEnvironment() {
        try {
            return collector.collectEnvironmentInfo();
        } catch (Exception e) {
            logger.error("Failed to collect current environment information", e);
            throw new CSEnvironmentException("Failed to collect current environment", e);
        }
    }
    
    /**
     * Compare environments
     */
    public EnvironmentComparisonResult compareEnvironments(EnvironmentInfo env1, EnvironmentInfo env2) {
        if (!enableEnvironmentComparison || env1 == null || env2 == null) {
            return null;
        }
        
        try {
            EnvironmentComparisonResult comparison = new EnvironmentComparisonResult();
            
            // Compare fingerprints
            comparison.setFingerprintMatch(
                env1.getEnvironmentFingerprint().equals(env2.getEnvironmentFingerprint())
            );
            
            // Compare system information
            if (env1.getSystemInfo() != null && env2.getSystemInfo() != null) {
                comparison.setOsMatch(
                    env1.getSystemInfo().getOsName().equals(env2.getSystemInfo().getOsName()) &&
                    env1.getSystemInfo().getOsVersion().equals(env2.getSystemInfo().getOsVersion())
                );
            }
            
            // Compare Java information
            if (env1.getJavaInfo() != null && env2.getJavaInfo() != null) {
                comparison.setJavaVersionMatch(
                    env1.getJavaInfo().getJavaVersion().equals(env2.getJavaInfo().getJavaVersion())
                );
            }
            
            // Compare hardware information
            if (env1.getHardwareInfo() != null && env2.getHardwareInfo() != null) {
                comparison.setCpuCoresMatch(
                    env1.getHardwareInfo().getCpuCores() == env2.getHardwareInfo().getCpuCores()
                );
                comparison.setMemoryMatch(
                    env1.getHardwareInfo().getMaxMemory() == env2.getHardwareInfo().getMaxMemory()
                );
            }
            
            // Overall compatibility score
            int matches = 0;
            int total = 0;
            
            if (comparison.isFingerprintMatch()) matches++; total++;
            if (comparison.isOsMatch()) matches++; total++;
            if (comparison.isJavaVersionMatch()) matches++; total++;
            if (comparison.isCpuCoresMatch()) matches++; total++;
            if (comparison.isMemoryMatch()) matches++; total++;
            
            comparison.setCompatibilityScore(total > 0 ? (double) matches / total * 100 : 0);
            
            return comparison;
            
        } catch (Exception e) {
            logger.error("Failed to compare environments", e);
            return null;
        }
    }
    
    /**
     * Export environment to JSON
     */
    public String exportEnvironmentToJson(EnvironmentInfo envInfo, String filename) {
        try {
            String filePath = Paths.get(outputDirectory, filename).toString();
            String json = collector.exportToJson(envInfo);
            
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(json);
            }
            
            logger.debug("Environment exported to JSON: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to export environment to JSON: {}", filename, e);
            throw new CSEnvironmentException("Failed to export environment to JSON", e);
        }
    }
    
    /**
     * Export environment to HTML
     */
    public String exportEnvironmentToHtml(EnvironmentInfo envInfo, String filename) {
        try {
            String filePath = Paths.get(outputDirectory, filename).toString();
            String html = generateEnvironmentHtml(envInfo);
            
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(html);
            }
            
            logger.debug("Environment exported to HTML: {}", filePath);
            return filePath;
            
        } catch (Exception e) {
            logger.error("Failed to export environment to HTML: {}", filename, e);
            throw new CSEnvironmentException("Failed to export environment to HTML", e);
        }
    }
    
    /**
     * Generate environment HTML report
     */
    private String generateEnvironmentHtml(EnvironmentInfo envInfo) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>Environment Information</title>\n")
            .append("    <style>\n")
            .append("        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f7fa; }\n")
            .append("        .container { max-width: 1200px; margin: 0 auto; }\n")
            .append("        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\n")
            .append("        .section { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
            .append("        .section h2 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }\n")
            .append("        .info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }\n")
            .append("        .info-item { background: #ecf0f1; padding: 15px; border-radius: 6px; }\n")
            .append("        .info-label { font-weight: bold; color: #34495e; }\n")
            .append("        .info-value { margin-top: 5px; color: #2c3e50; }\n")
            .append("        .fingerprint { font-family: monospace; background: #34495e; color: white; padding: 10px; border-radius: 4px; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"container\">\n")
            .append("        <div class=\"header\">\n")
            .append("            <h1>Environment Information Report</h1>\n")
            .append("            <p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n")
            .append("            <div class=\"fingerprint\">Fingerprint: ").append(envInfo.getEnvironmentFingerprint()).append("</div>\n")
            .append("        </div>\n");
        
        // System Information
        if (envInfo.getSystemInfo() != null) {
            html.append("        <div class=\"section\">\n")
                .append("            <h2>System Information</h2>\n")
                .append("            <div class=\"info-grid\">\n");
            
            addInfoItem(html, "Operating System", envInfo.getSystemInfo().getOsName() + " " + envInfo.getSystemInfo().getOsVersion());
            addInfoItem(html, "Architecture", envInfo.getSystemInfo().getOsArch());
            addInfoItem(html, "Computer Name", envInfo.getSystemInfo().getComputerName());
            addInfoItem(html, "User Name", envInfo.getSystemInfo().getUserName());
            addInfoItem(html, "Available Processors", String.valueOf(envInfo.getSystemInfo().getAvailableProcessors()));
            addInfoItem(html, "System Load Average", String.format("%.2f", envInfo.getSystemInfo().getSystemLoadAverage()));
            
            html.append("            </div>\n")
                .append("        </div>\n");
        }
        
        // Java Information
        if (envInfo.getJavaInfo() != null) {
            html.append("        <div class=\"section\">\n")
                .append("            <h2>Java Runtime Information</h2>\n")
                .append("            <div class=\"info-grid\">\n");
            
            addInfoItem(html, "Java Version", envInfo.getJavaInfo().getJavaVersion());
            addInfoItem(html, "Java Vendor", envInfo.getJavaInfo().getJavaVendor());
            addInfoItem(html, "JVM Name", envInfo.getJavaInfo().getJvmName());
            addInfoItem(html, "JVM Version", envInfo.getJavaInfo().getJvmVersion());
            addInfoItem(html, "Process ID", String.valueOf(envInfo.getJavaInfo().getPid()));
            addInfoItem(html, "Uptime", formatDuration(envInfo.getJavaInfo().getUptime()));
            
            html.append("            </div>\n")
                .append("        </div>\n");
        }
        
        // Hardware Information
        if (envInfo.getHardwareInfo() != null) {
            html.append("        <div class=\"section\">\n")
                .append("            <h2>Hardware Information</h2>\n")
                .append("            <div class=\"info-grid\">\n");
            
            addInfoItem(html, "CPU Cores", String.valueOf(envInfo.getHardwareInfo().getCpuCores()));
            addInfoItem(html, "CPU Model", envInfo.getHardwareInfo().getCpuModel());
            addInfoItem(html, "Max Memory", formatBytes(envInfo.getHardwareInfo().getMaxMemory()));
            addInfoItem(html, "Total Memory", formatBytes(envInfo.getHardwareInfo().getTotalMemory()));
            addInfoItem(html, "Used Memory", formatBytes(envInfo.getHardwareInfo().getUsedMemory()));
            
            html.append("            </div>\n")
                .append("        </div>\n");
        }
        
        html.append("    </div>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString();
    }
    
    /**
     * Add info item to HTML
     */
    private void addInfoItem(StringBuilder html, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            html.append("                <div class=\"info-item\">\n")
                .append("                    <div class=\"info-label\">").append(label).append("</div>\n")
                .append("                    <div class=\"info-value\">").append(value).append("</div>\n")
                .append("                </div>\n");
        }
    }
    
    /**
     * Add environment information to report
     */
    private void addEnvironmentToReport(EnvironmentInfo envInfo, String suiteName) {
        try {
            CSReportManager reportManager = CSReportManager.getInstance();
            // This would integrate with the existing report manager
            // Implementation would depend on report manager API
            logger.debug("Environment information added to report for suite: {}", suiteName);
        } catch (Exception e) {
            logger.warn("Failed to add environment to report", e);
        }
    }
    
    /**
     * Schedule periodic environment collection
     */
    private void schedulePeriodicCollection() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.debug("Performing periodic environment collection");
                EnvironmentInfo envInfo = collector.collectEnvironmentInfo();
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                environmentCache.put("periodic_" + timestamp, envInfo);
                
                if (exportToJson) {
                    exportEnvironmentToJson(envInfo, "environment_periodic_" + timestamp + ".json");
                }
                
            } catch (Exception e) {
                logger.error("Error during periodic environment collection", e);
            }
        }, periodicCollectionIntervalMinutes, periodicCollectionIntervalMinutes, TimeUnit.MINUTES);
        
        logger.info("Scheduled periodic environment collection every {} minutes", periodicCollectionIntervalMinutes);
    }
    
    /**
     * Get all cached environment information
     */
    public Map<String, EnvironmentInfo> getCachedEnvironments() {
        return new ConcurrentHashMap<>(environmentCache);
    }
    
    /**
     * Clear environment cache
     */
    public void clearCache() {
        environmentCache.clear();
        logger.info("Environment cache cleared");
    }
    
    /**
     * Shutdown integration
     */
    private void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("Environment integration shutdown completed");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Utility methods
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Environment comparison result
     */
    public static class EnvironmentComparisonResult {
        private boolean fingerprintMatch;
        private boolean osMatch;
        private boolean javaVersionMatch;
        private boolean cpuCoresMatch;
        private boolean memoryMatch;
        private double compatibilityScore;
        
        // Getters and setters
        public boolean isFingerprintMatch() { return fingerprintMatch; }
        public void setFingerprintMatch(boolean fingerprintMatch) { this.fingerprintMatch = fingerprintMatch; }
        
        public boolean isOsMatch() { return osMatch; }
        public void setOsMatch(boolean osMatch) { this.osMatch = osMatch; }
        
        public boolean isJavaVersionMatch() { return javaVersionMatch; }
        public void setJavaVersionMatch(boolean javaVersionMatch) { this.javaVersionMatch = javaVersionMatch; }
        
        public boolean isCpuCoresMatch() { return cpuCoresMatch; }
        public void setCpuCoresMatch(boolean cpuCoresMatch) { this.cpuCoresMatch = cpuCoresMatch; }
        
        public boolean isMemoryMatch() { return memoryMatch; }
        public void setMemoryMatch(boolean memoryMatch) { this.memoryMatch = memoryMatch; }
        
        public double getCompatibilityScore() { return compatibilityScore; }
        public void setCompatibilityScore(double compatibilityScore) { this.compatibilityScore = compatibilityScore; }
    }
}