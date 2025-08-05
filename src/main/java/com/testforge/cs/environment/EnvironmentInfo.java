package com.testforge.cs.environment;

import com.testforge.cs.environment.EnvironmentInfoClasses.*;
import com.testforge.cs.environment.SystemInfo;
import com.testforge.cs.environment.JavaInfo;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete environment information container
 */
public class EnvironmentInfo {
    private String environmentFingerprint;
    private LocalDateTime collectionTimestamp;
    private SystemInfo systemInfo;
    private JavaInfo javaInfo;
    private HardwareInfo hardwareInfo;
    private PerformanceInfo performanceInfo;
    private NetworkInfo networkInfo;
    private SecurityInfo securityInfo;
    private ApplicationInfo applicationInfo;
    private BrowserInfo browserInfo;
    private SeleniumInfo seleniumInfo;
    private TestFrameworkInfo testFrameworkInfo;
    private DatabaseInfo databaseInfo;
    private Map<String, Object> customInfo;
    
    public EnvironmentInfo() {
        this.collectionTimestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getEnvironmentFingerprint() { return environmentFingerprint; }
    public void setEnvironmentFingerprint(String environmentFingerprint) { this.environmentFingerprint = environmentFingerprint; }
    
    public LocalDateTime getCollectionTimestamp() { return collectionTimestamp; }
    public void setCollectionTimestamp(LocalDateTime collectionTimestamp) { this.collectionTimestamp = collectionTimestamp; }
    
    public SystemInfo getSystemInfo() { return systemInfo; }
    public void setSystemInfo(SystemInfo systemInfo) { this.systemInfo = systemInfo; }
    
    public JavaInfo getJavaInfo() { return javaInfo; }
    public void setJavaInfo(JavaInfo javaInfo) { this.javaInfo = javaInfo; }
    
    public HardwareInfo getHardwareInfo() { return hardwareInfo; }
    public void setHardwareInfo(HardwareInfo hardwareInfo) { this.hardwareInfo = hardwareInfo; }
    
    public PerformanceInfo getPerformanceInfo() { return performanceInfo; }
    public void setPerformanceInfo(PerformanceInfo performanceInfo) { this.performanceInfo = performanceInfo; }
    
    public NetworkInfo getNetworkInfo() { return networkInfo; }
    public void setNetworkInfo(NetworkInfo networkInfo) { this.networkInfo = networkInfo; }
    
    public SecurityInfo getSecurityInfo() { return securityInfo; }
    public void setSecurityInfo(SecurityInfo securityInfo) { this.securityInfo = securityInfo; }
    
    public ApplicationInfo getApplicationInfo() { return applicationInfo; }
    public void setApplicationInfo(ApplicationInfo applicationInfo) { this.applicationInfo = applicationInfo; }
    
    public BrowserInfo getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(BrowserInfo browserInfo) { this.browserInfo = browserInfo; }
    
    public SeleniumInfo getSeleniumInfo() { return seleniumInfo; }
    public void setSeleniumInfo(SeleniumInfo seleniumInfo) { this.seleniumInfo = seleniumInfo; }
    
    public TestFrameworkInfo getTestFrameworkInfo() { return testFrameworkInfo; }
    public void setTestFrameworkInfo(TestFrameworkInfo testFrameworkInfo) { this.testFrameworkInfo = testFrameworkInfo; }
    
    public DatabaseInfo getDatabaseInfo() { return databaseInfo; }
    public void setDatabaseInfo(DatabaseInfo databaseInfo) { this.databaseInfo = databaseInfo; }
    
    public Map<String, Object> getCustomInfo() { return customInfo; }
    public void setCustomInfo(Map<String, Object> customInfo) { this.customInfo = customInfo; }
    
    @Override
    public String toString() {
        return String.format("EnvironmentInfo{fingerprint='%s', timestamp=%s}", 
            environmentFingerprint, collectionTimestamp);
    }
    
    /**
     * Convert to Map for reporting purposes
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        
        if (systemInfo != null) {
            map.put("os.name", systemInfo.getOsName());
            map.put("os.version", systemInfo.getOsVersion());
            map.put("os.arch", systemInfo.getOsArch());
        }
        
        if (javaInfo != null) {
            map.put("java.version", javaInfo.getJavaVersion());
            map.put("java.vendor", javaInfo.getJavaVendor());
            map.put("java.home", javaInfo.getJavaHome());
        }
        
        if (hardwareInfo != null) {
            map.put("cpu.cores", String.valueOf(hardwareInfo.getCpuCores()));
            map.put("memory.total", String.valueOf(hardwareInfo.getTotalMemory()));
            map.put("memory.free", String.valueOf(hardwareInfo.getFreeMemory()));
        }
        
        if (applicationInfo != null) {
            map.put("app.name", applicationInfo.getFrameworkName());
            map.put("app.version", applicationInfo.getFrameworkVersion());
        }
        
        if (testFrameworkInfo != null) {
            map.put("testng.version", testFrameworkInfo.getTestNgVersion());
            map.put("junit.version", testFrameworkInfo.getJunitVersion());
        }
        
        return map;
    }
}