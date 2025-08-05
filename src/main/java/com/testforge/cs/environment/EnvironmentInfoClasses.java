package com.testforge.cs.environment;

import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;

/**
 * Consolidated environment information classes for space efficiency
 */
public class EnvironmentInfoClasses {
    
    /**
     * Hardware information container
     */
    public static class HardwareInfo {
        private int cpuCores;
        private String cpuModel;
        private String cpuSpeed;
        private long maxMemory;
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        private Map<String, DiskInfo> diskInfo;
        private Map<String, Object> graphicsInfo;
        
        // Getters and setters
        public int getCpuCores() { return cpuCores; }
        public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
        
        public String getCpuModel() { return cpuModel; }
        public void setCpuModel(String cpuModel) { this.cpuModel = cpuModel; }
        
        public String getCpuSpeed() { return cpuSpeed; }
        public void setCpuSpeed(String cpuSpeed) { this.cpuSpeed = cpuSpeed; }
        
        public long getMaxMemory() { return maxMemory; }
        public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }
        
        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
        
        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }
        
        public long getUsedMemory() { return usedMemory; }
        public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
        
        public Map<String, DiskInfo> getDiskInfo() { return diskInfo; }
        public void setDiskInfo(Map<String, DiskInfo> diskInfo) { this.diskInfo = diskInfo; }
        
        public Map<String, Object> getGraphicsInfo() { return graphicsInfo; }
        public void setGraphicsInfo(Map<String, Object> graphicsInfo) { this.graphicsInfo = graphicsInfo; }
    }
    
    /**
     * Disk information container
     */
    public static class DiskInfo {
        private String path;
        private long totalSpace;
        private long freeSpace;
        private long usableSpace;
        private long usedSpace;
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public long getTotalSpace() { return totalSpace; }
        public void setTotalSpace(long totalSpace) { this.totalSpace = totalSpace; }
        
        public long getFreeSpace() { return freeSpace; }
        public void setFreeSpace(long freeSpace) { this.freeSpace = freeSpace; }
        
        public long getUsableSpace() { return usableSpace; }
        public void setUsableSpace(long usableSpace) { this.usableSpace = usableSpace; }
        
        public long getUsedSpace() { return usedSpace; }
        public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }
    }
    
    /**
     * Performance information container
     */
    public static class PerformanceInfo {
        private long heapMemoryUsed;
        private long heapMemoryMax;
        private long heapMemoryCommitted;
        private long nonHeapMemoryUsed;
        private long nonHeapMemoryMax;
        private long nonHeapMemoryCommitted;
        private int pendingFinalizationCount;
        private int threadCount;
        private int peakThreadCount;
        private int daemonThreadCount;
        private long totalStartedThreadCount;
        private Map<String, GCInfo> gcInfo;
        private Map<String, MemoryPoolInfo> memoryPoolInfo;
        private int loadedClassCount;
        private long totalLoadedClassCount;
        private long unloadedClassCount;
        
        // Getters and setters
        public long getHeapMemoryUsed() { return heapMemoryUsed; }
        public void setHeapMemoryUsed(long heapMemoryUsed) { this.heapMemoryUsed = heapMemoryUsed; }
        
        public long getHeapMemoryMax() { return heapMemoryMax; }
        public void setHeapMemoryMax(long heapMemoryMax) { this.heapMemoryMax = heapMemoryMax; }
        
        public long getHeapMemoryCommitted() { return heapMemoryCommitted; }
        public void setHeapMemoryCommitted(long heapMemoryCommitted) { this.heapMemoryCommitted = heapMemoryCommitted; }
        
        public long getNonHeapMemoryUsed() { return nonHeapMemoryUsed; }
        public void setNonHeapMemoryUsed(long nonHeapMemoryUsed) { this.nonHeapMemoryUsed = nonHeapMemoryUsed; }
        
        public long getNonHeapMemoryMax() { return nonHeapMemoryMax; }
        public void setNonHeapMemoryMax(long nonHeapMemoryMax) { this.nonHeapMemoryMax = nonHeapMemoryMax; }
        
        public long getNonHeapMemoryCommitted() { return nonHeapMemoryCommitted; }
        public void setNonHeapMemoryCommitted(long nonHeapMemoryCommitted) { this.nonHeapMemoryCommitted = nonHeapMemoryCommitted; }
        
        public int getPendingFinalizationCount() { return pendingFinalizationCount; }
        public void setPendingFinalizationCount(int pendingFinalizationCount) { this.pendingFinalizationCount = pendingFinalizationCount; }
        
        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
        
        public int getPeakThreadCount() { return peakThreadCount; }
        public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }
        
        public int getDaemonThreadCount() { return daemonThreadCount; }
        public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }
        
        public long getTotalStartedThreadCount() { return totalStartedThreadCount; }
        public void setTotalStartedThreadCount(long totalStartedThreadCount) { this.totalStartedThreadCount = totalStartedThreadCount; }
        
        public Map<String, GCInfo> getGcInfo() { return gcInfo; }
        public void setGcInfo(Map<String, GCInfo> gcInfo) { this.gcInfo = gcInfo; }
        
        public Map<String, MemoryPoolInfo> getMemoryPoolInfo() { return memoryPoolInfo; }
        public void setMemoryPoolInfo(Map<String, MemoryPoolInfo> memoryPoolInfo) { this.memoryPoolInfo = memoryPoolInfo; }
        
        public int getLoadedClassCount() { return loadedClassCount; }
        public void setLoadedClassCount(int loadedClassCount) { this.loadedClassCount = loadedClassCount; }
        
        public long getTotalLoadedClassCount() { return totalLoadedClassCount; }
        public void setTotalLoadedClassCount(long totalLoadedClassCount) { this.totalLoadedClassCount = totalLoadedClassCount; }
        
        public long getUnloadedClassCount() { return unloadedClassCount; }
        public void setUnloadedClassCount(long unloadedClassCount) { this.unloadedClassCount = unloadedClassCount; }
    }
    
    /**
     * Garbage Collection information
     */
    public static class GCInfo {
        private String name;
        private long collectionCount;
        private long collectionTime;
        private List<String> memoryPoolNames;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public long getCollectionCount() { return collectionCount; }
        public void setCollectionCount(long collectionCount) { this.collectionCount = collectionCount; }
        
        public long getCollectionTime() { return collectionTime; }
        public void setCollectionTime(long collectionTime) { this.collectionTime = collectionTime; }
        
        public List<String> getMemoryPoolNames() { return memoryPoolNames; }
        public void setMemoryPoolNames(List<String> memoryPoolNames) { this.memoryPoolNames = memoryPoolNames; }
    }
    
    /**
     * Memory pool information
     */
    public static class MemoryPoolInfo {
        private String name;
        private String type;
        private MemoryUsage usage;
        private MemoryUsage peakUsage;
        private MemoryUsage collectionUsage;
        private List<String> managerNames;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public MemoryUsage getUsage() { return usage; }
        public void setUsage(MemoryUsage usage) { this.usage = usage; }
        
        public MemoryUsage getPeakUsage() { return peakUsage; }
        public void setPeakUsage(MemoryUsage peakUsage) { this.peakUsage = peakUsage; }
        
        public MemoryUsage getCollectionUsage() { return collectionUsage; }
        public void setCollectionUsage(MemoryUsage collectionUsage) { this.collectionUsage = collectionUsage; }
        
        public List<String> getManagerNames() { return managerNames; }
        public void setManagerNames(List<String> managerNames) { this.managerNames = managerNames; }
    }
    
    /**
     * Network information container
     */
    public static class NetworkInfo {
        private String hostName;
        private String hostAddress;
        private String canonicalHostName;
        private List<NetworkInterfaceInfo> networkInterfaces;
        private boolean internetConnectivity;
        private List<String> dnsServers;
        private Map<String, Object> proxyInfo;
        
        // Getters and setters
        public String getHostName() { return hostName; }
        public void setHostName(String hostName) { this.hostName = hostName; }
        
        public String getHostAddress() { return hostAddress; }
        public void setHostAddress(String hostAddress) { this.hostAddress = hostAddress; }
        
        public String getCanonicalHostName() { return canonicalHostName; }
        public void setCanonicalHostName(String canonicalHostName) { this.canonicalHostName = canonicalHostName; }
        
        public List<NetworkInterfaceInfo> getNetworkInterfaces() { return networkInterfaces; }
        public void setNetworkInterfaces(List<NetworkInterfaceInfo> networkInterfaces) { this.networkInterfaces = networkInterfaces; }
        
        public boolean isInternetConnectivity() { return internetConnectivity; }
        public void setInternetConnectivity(boolean internetConnectivity) { this.internetConnectivity = internetConnectivity; }
        
        public List<String> getDnsServers() { return dnsServers; }
        public void setDnsServers(List<String> dnsServers) { this.dnsServers = dnsServers; }
        
        public Map<String, Object> getProxyInfo() { return proxyInfo; }
        public void setProxyInfo(Map<String, Object> proxyInfo) { this.proxyInfo = proxyInfo; }
    }
    
    /**
     * Network interface information
     */
    public static class NetworkInterfaceInfo {
        private String name;
        private String displayName;
        private int mtu;
        private boolean isLoopback;
        private boolean isPointToPoint;
        private boolean isVirtual;
        private boolean supportsMulticast;
        private String hardwareAddress;
        private List<String> ipAddresses;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public int getMtu() { return mtu; }
        public void setMtu(int mtu) { this.mtu = mtu; }
        
        public boolean isLoopback() { return isLoopback; }
        public void setLoopback(boolean loopback) { isLoopback = loopback; }
        
        public boolean isPointToPoint() { return isPointToPoint; }
        public void setPointToPoint(boolean pointToPoint) { isPointToPoint = pointToPoint; }
        
        public boolean isVirtual() { return isVirtual; }
        public void setVirtual(boolean virtual) { isVirtual = virtual; }
        
        public boolean isSupportsMulticast() { return supportsMulticast; }
        public void setSupportsMulticast(boolean supportsMulticast) { this.supportsMulticast = supportsMulticast; }
        
        public String getHardwareAddress() { return hardwareAddress; }
        public void setHardwareAddress(String hardwareAddress) { this.hardwareAddress = hardwareAddress; }
        
        public List<String> getIpAddresses() { return ipAddresses; }
        public void setIpAddresses(List<String> ipAddresses) { this.ipAddresses = ipAddresses; }
    }
    
    /**
     * Security information container
     */
    public static class SecurityInfo {
        private boolean securityManagerEnabled;
        private List<String> securityProviders;
        private Map<String, Object> sslInfo;
        private boolean currentDirReadable;
        private boolean currentDirWritable;
        private boolean currentDirExecutable;
        private Map<String, String> securityProperties;
        
        // Getters and setters
        public boolean isSecurityManagerEnabled() { return securityManagerEnabled; }
        public void setSecurityManagerEnabled(boolean securityManagerEnabled) { this.securityManagerEnabled = securityManagerEnabled; }
        
        public List<String> getSecurityProviders() { return securityProviders; }
        public void setSecurityProviders(List<String> securityProviders) { this.securityProviders = securityProviders; }
        
        public Map<String, Object> getSslInfo() { return sslInfo; }
        public void setSslInfo(Map<String, Object> sslInfo) { this.sslInfo = sslInfo; }
        
        public boolean isCurrentDirReadable() { return currentDirReadable; }
        public void setCurrentDirReadable(boolean currentDirReadable) { this.currentDirReadable = currentDirReadable; }
        
        public boolean isCurrentDirWritable() { return currentDirWritable; }
        public void setCurrentDirWritable(boolean currentDirWritable) { this.currentDirWritable = currentDirWritable; }
        
        public boolean isCurrentDirExecutable() { return currentDirExecutable; }
        public void setCurrentDirExecutable(boolean currentDirExecutable) { this.currentDirExecutable = currentDirExecutable; }
        
        public Map<String, String> getSecurityProperties() { return securityProperties; }
        public void setSecurityProperties(Map<String, String> securityProperties) { this.securityProperties = securityProperties; }
    }
    
    /**
     * Application information container
     */
    public static class ApplicationInfo {
        private Map<String, Object> mavenInfo;
        private Map<String, Object> gitInfo;
        private Map<String, Object> applicationProperties;
        private String frameworkName;
        private String frameworkVersion;
        private String buildTimestamp;
        
        // Getters and setters
        public Map<String, Object> getMavenInfo() { return mavenInfo; }
        public void setMavenInfo(Map<String, Object> mavenInfo) { this.mavenInfo = mavenInfo; }
        
        public Map<String, Object> getGitInfo() { return gitInfo; }
        public void setGitInfo(Map<String, Object> gitInfo) { this.gitInfo = gitInfo; }
        
        public Map<String, Object> getApplicationProperties() { return applicationProperties; }
        public void setApplicationProperties(Map<String, Object> applicationProperties) { this.applicationProperties = applicationProperties; }
        
        public String getFrameworkName() { return frameworkName; }
        public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }
        
        public String getFrameworkVersion() { return frameworkVersion; }
        public void setFrameworkVersion(String frameworkVersion) { this.frameworkVersion = frameworkVersion; }
        
        public String getBuildTimestamp() { return buildTimestamp; }
        public void setBuildTimestamp(String buildTimestamp) { this.buildTimestamp = buildTimestamp; }
    }
    
    /**
     * Browser information container
     */
    public static class BrowserInfo {
        private List<String> availableBrowsers;
        private String defaultBrowser;
        private Map<String, String> browserVersions;
        
        // Getters and setters
        public List<String> getAvailableBrowsers() { return availableBrowsers; }
        public void setAvailableBrowsers(List<String> availableBrowsers) { this.availableBrowsers = availableBrowsers; }
        
        public String getDefaultBrowser() { return defaultBrowser; }
        public void setDefaultBrowser(String defaultBrowser) { this.defaultBrowser = defaultBrowser; }
        
        public Map<String, String> getBrowserVersions() { return browserVersions; }
        public void setBrowserVersions(Map<String, String> browserVersions) { this.browserVersions = browserVersions; }
    }
    
    /**
     * Selenium information container
     */
    public static class SeleniumInfo {
        private String seleniumVersion;
        private Map<String, Object> webDriverInfo;
        private List<String> availableDrivers;
        
        // Getters and setters
        public String getSeleniumVersion() { return seleniumVersion; }
        public void setSeleniumVersion(String seleniumVersion) { this.seleniumVersion = seleniumVersion; }
        
        public Map<String, Object> getWebDriverInfo() { return webDriverInfo; }
        public void setWebDriverInfo(Map<String, Object> webDriverInfo) { this.webDriverInfo = webDriverInfo; }
        
        public List<String> getAvailableDrivers() { return availableDrivers; }
        public void setAvailableDrivers(List<String> availableDrivers) { this.availableDrivers = availableDrivers; }
    }
    
    /**
     * Test framework information container
     */
    public static class TestFrameworkInfo {
        private String testNgVersion;
        private String junitVersion;
        private Map<String, String> frameworkProperties;
        
        // Getters and setters
        public String getTestNgVersion() { return testNgVersion; }
        public void setTestNgVersion(String testNgVersion) { this.testNgVersion = testNgVersion; }
        
        public String getJunitVersion() { return junitVersion; }
        public void setJunitVersion(String junitVersion) { this.junitVersion = junitVersion; }
        
        public Map<String, String> getFrameworkProperties() { return frameworkProperties; }
        public void setFrameworkProperties(Map<String, String> frameworkProperties) { this.frameworkProperties = frameworkProperties; }
    }
    
    /**
     * Database information container
     */
    public static class DatabaseInfo {
        private List<String> availableDrivers;
        private Map<String, String> driverVersions;
        private List<String> supportedDatabases;
        
        // Getters and setters
        public List<String> getAvailableDrivers() { return availableDrivers; }
        public void setAvailableDrivers(List<String> availableDrivers) { this.availableDrivers = availableDrivers; }
        
        public Map<String, String> getDriverVersions() { return driverVersions; }
        public void setDriverVersions(Map<String, String> driverVersions) { this.driverVersions = driverVersions; }
        
        public List<String> getSupportedDatabases() { return supportedDatabases; }
        public void setSupportedDatabases(List<String> supportedDatabases) { this.supportedDatabases = supportedDatabases; }
    }
}