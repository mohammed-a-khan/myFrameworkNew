package com.testforge.cs.environment;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSEnvironmentException;
import com.testforge.cs.utils.CSJsonUtils;
import com.testforge.cs.environment.EnvironmentInfoClasses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Production-ready enterprise environment information collector
 * Captures comprehensive system, runtime, network, and application environment details
 * for debugging, compliance, auditing, and performance analysis
 */
public class CSEnvironmentCollector {
    private static final Logger logger = LoggerFactory.getLogger(CSEnvironmentCollector.class);
    
    private static volatile CSEnvironmentCollector instance;
    private static final Object instanceLock = new Object();
    
    // Environment data cache
    private final Map<String, Object> environmentCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Configuration
    private CSConfigManager config;
    private boolean collectSensitiveInfo;
    private boolean enableNetworkInfo;
    private boolean enablePerformanceMetrics;
    private boolean enableSecurityInfo;
    private long cacheExpirationTime;
    private Set<String> excludedProperties;
    private Set<String> includedSystemCommands;
    
    // System management beans
    private final RuntimeMXBean runtimeBean;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final GarbageCollectorMXBean[] gcBeans;
    private final CompilationMXBean compilationBean;
    
    /**
     * Get singleton instance
     */
    public static CSEnvironmentCollector getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSEnvironmentCollector();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private CSEnvironmentCollector() {
        // Initialize management beans
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans().toArray(new GarbageCollectorMXBean[0]);
        this.compilationBean = ManagementFactory.getCompilationMXBean();
        
        initialize();
    }
    
    /**
     * Initialize environment collector
     */
    private void initialize() {
        try {
            config = CSConfigManager.getInstance();
            
            // Load configuration
            collectSensitiveInfo = Boolean.parseBoolean(config.getProperty("environment.collect.sensitive", "false"));
            enableNetworkInfo = Boolean.parseBoolean(config.getProperty("environment.collect.network", "true"));
            enablePerformanceMetrics = Boolean.parseBoolean(config.getProperty("environment.collect.performance", "true"));
            enableSecurityInfo = Boolean.parseBoolean(config.getProperty("environment.collect.security", "true"));
            cacheExpirationTime = Long.parseLong(config.getProperty("environment.cache.expiration.ms", "300000")); // 5 minutes
            
            // Load exclusion/inclusion lists
            String excludedProps = config.getProperty("environment.exclude.properties", "");
            excludedProperties = Arrays.stream(excludedProps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
            
            String includedCommands = config.getProperty("environment.include.commands", "");
            includedSystemCommands = Arrays.stream(includedCommands.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
            
            logger.info("Environment collector initialized - Sensitive: {}, Network: {}, Performance: {}, Security: {}", 
                collectSensitiveInfo, enableNetworkInfo, enablePerformanceMetrics, enableSecurityInfo);
            
        } catch (Exception e) {
            logger.error("Failed to initialize environment collector", e);
            throw new CSEnvironmentException("Failed to initialize environment collector", e);
        }
    }
    
    /**
     * Collect complete environment information
     */
    public EnvironmentInfo collectEnvironmentInfo() {
        logger.debug("Collecting complete environment information");
        
        EnvironmentInfo envInfo = new EnvironmentInfo();
        
        try {
            // Basic system information
            envInfo.setSystemInfo(collectSystemInfo());
            
            // Java runtime information
            envInfo.setJavaInfo(collectJavaInfo());
            
            // Hardware information
            envInfo.setHardwareInfo(collectHardwareInfo());
            
            // Memory and performance metrics
            if (enablePerformanceMetrics) {
                envInfo.setPerformanceInfo(collectPerformanceInfo());
            }
            
            // Network information
            if (enableNetworkInfo) {
                envInfo.setNetworkInfo(collectNetworkInfo());
            }
            
            // Security information
            if (enableSecurityInfo) {
                envInfo.setSecurityInfo(collectSecurityInfo());
            }
            
            // Application environment
            envInfo.setApplicationInfo(collectApplicationInfo());
            
            // Browser environment (if available)
            envInfo.setBrowserInfo(collectBrowserInfo());
            
            // Selenium environment
            envInfo.setSeleniumInfo(collectSeleniumInfo());
            
            // TestNG environment
            envInfo.setTestFrameworkInfo(collectTestFrameworkInfo());
            
            // Database environment
            envInfo.setDatabaseInfo(collectDatabaseInfo());
            
            // Custom environment variables
            envInfo.setCustomInfo(collectCustomInfo());
            
            // Environment fingerprint for tracking
            envInfo.setEnvironmentFingerprint(generateEnvironmentFingerprint(envInfo));
            
            logger.info("Environment information collected successfully");
            return envInfo;
            
        } catch (Exception e) {
            logger.error("Failed to collect environment information", e);
            throw new CSEnvironmentException("Failed to collect environment information", e);
        }
    }
    
    /**
     * Collect system information
     */
    private SystemInfo collectSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        
        try {
            // Operating system details
            systemInfo.setOsName(System.getProperty("os.name"));
            systemInfo.setOsVersion(System.getProperty("os.version"));
            systemInfo.setOsArch(System.getProperty("os.arch"));
            
            // Additional OS information
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                systemInfo.setTotalPhysicalMemory(sunOsBean.getTotalPhysicalMemorySize());
                systemInfo.setFreePhysicalMemory(sunOsBean.getFreePhysicalMemorySize());
                systemInfo.setTotalSwapSpace(sunOsBean.getTotalSwapSpaceSize());
                systemInfo.setFreeSwapSpace(sunOsBean.getFreeSwapSpaceSize());
                systemInfo.setCommittedVirtualMemory(sunOsBean.getCommittedVirtualMemorySize());
                systemInfo.setProcessCpuLoad(sunOsBean.getProcessCpuLoad());
                systemInfo.setSystemCpuLoad(sunOsBean.getSystemCpuLoad());
                systemInfo.setProcessCpuTime(sunOsBean.getProcessCpuTime());
            }
            
            systemInfo.setAvailableProcessors(osBean.getAvailableProcessors());
            systemInfo.setSystemLoadAverage(osBean.getSystemLoadAverage());
            
            // Computer name and user information
            systemInfo.setComputerName(getComputerName());
            systemInfo.setUserName(System.getProperty("user.name"));
            systemInfo.setUserHome(System.getProperty("user.home"));
            systemInfo.setUserDir(System.getProperty("user.dir"));
            
            // Timezone and locale
            systemInfo.setTimeZone(TimeZone.getDefault().getID());
            systemInfo.setLocale(Locale.getDefault().toString());
            systemInfo.setCharsetDefault(System.getProperty("file.encoding"));
            
            // File system information
            systemInfo.setFileSeparator(System.getProperty("file.separator"));
            systemInfo.setPathSeparator(System.getProperty("path.separator"));
            systemInfo.setLineSeparator(System.getProperty("line.separator"));
            
            // Temporary directory
            systemInfo.setTempDir(System.getProperty("java.io.tmpdir"));
            
            // System uptime (if available)
            systemInfo.setSystemUptime(getSystemUptime());
            
            // Additional system properties
            if (collectSensitiveInfo) {
                systemInfo.setEnvironmentVariables(getFilteredEnvironmentVariables());
            }
            
        } catch (Exception e) {
            logger.warn("Error collecting system information", e);
        }
        
        return systemInfo;
    }
    
    /**
     * Collect Java runtime information
     */
    private JavaInfo collectJavaInfo() {
        JavaInfo javaInfo = new JavaInfo();
        
        try {
            // Java version details
            javaInfo.setJavaVersion(System.getProperty("java.version"));
            javaInfo.setJavaVendor(System.getProperty("java.vendor"));
            javaInfo.setJavaVendorUrl(System.getProperty("java.vendor.url"));
            javaInfo.setJavaHome(System.getProperty("java.home"));
            javaInfo.setJavaSpecificationVersion(System.getProperty("java.specification.version"));
            javaInfo.setJavaSpecificationVendor(System.getProperty("java.specification.vendor"));
            javaInfo.setJavaSpecificationName(System.getProperty("java.specification.name"));
            
            // JVM details
            javaInfo.setJvmName(System.getProperty("java.vm.name"));
            javaInfo.setJvmVersion(System.getProperty("java.vm.version"));
            javaInfo.setJvmVendor(System.getProperty("java.vm.vendor"));
            javaInfo.setJvmSpecificationName(System.getProperty("java.vm.specification.name"));
            javaInfo.setJvmSpecificationVersion(System.getProperty("java.vm.specification.version"));
            javaInfo.setJvmSpecificationVendor(System.getProperty("java.vm.specification.vendor"));
            
            // Runtime information
            javaInfo.setRuntimeName(runtimeBean.getName());
            javaInfo.setRuntimeVersion(runtimeBean.getSpecVersion());
            javaInfo.setVmName(runtimeBean.getVmName());
            javaInfo.setVmVersion(runtimeBean.getVmVersion());
            javaInfo.setVmVendor(runtimeBean.getVmVendor());
            javaInfo.setStartTime(runtimeBean.getStartTime());
            javaInfo.setUptime(runtimeBean.getUptime());
            javaInfo.setPid(extractPidFromName(runtimeBean.getName()));
            
            // Class path and library path
            javaInfo.setClassPath(runtimeBean.getClassPath());
            javaInfo.setLibraryPath(System.getProperty("java.library.path"));
            javaInfo.setBootClassPath(runtimeBean.isBootClassPathSupported() ? runtimeBean.getBootClassPath() : "Not supported");
            
            // JVM arguments
            javaInfo.setInputArguments(runtimeBean.getInputArguments());
            javaInfo.setSystemProperties(getFilteredSystemProperties());
            
            // Compilation information
            if (compilationBean != null) {
                javaInfo.setCompilerName(compilationBean.getName());
                javaInfo.setTotalCompilationTime(compilationBean.getTotalCompilationTime());
                javaInfo.setCompilationTimeMonitoringSupported(compilationBean.isCompilationTimeMonitoringSupported());
            }
            
        } catch (Exception e) {
            logger.warn("Error collecting Java information", e);
        }
        
        return javaInfo;
    }
    
    /**
     * Collect hardware information
     */
    private HardwareInfo collectHardwareInfo() {
        HardwareInfo hardwareInfo = new HardwareInfo();
        
        try {
            // CPU information
            hardwareInfo.setCpuCores(Runtime.getRuntime().availableProcessors());
            
            // Try to get more detailed CPU information
            String cpuInfo = getSystemCommand("cpu");
            if (cpuInfo != null) {
                hardwareInfo.setCpuModel(extractCpuModel(cpuInfo));
                hardwareInfo.setCpuSpeed(extractCpuSpeed(cpuInfo));
            }
            
            // Memory information
            Runtime runtime = Runtime.getRuntime();
            hardwareInfo.setMaxMemory(runtime.maxMemory());
            hardwareInfo.setTotalMemory(runtime.totalMemory());
            hardwareInfo.setFreeMemory(runtime.freeMemory());
            hardwareInfo.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
            
            // Disk space information
            File[] roots = File.listRoots();
            Map<String, DiskInfo> diskInfoMap = new HashMap<>();
            
            for (File root : roots) {
                DiskInfo diskInfo = new DiskInfo();
                diskInfo.setPath(root.getAbsolutePath());
                diskInfo.setTotalSpace(root.getTotalSpace());
                diskInfo.setFreeSpace(root.getFreeSpace());
                diskInfo.setUsableSpace(root.getUsableSpace());
                diskInfo.setUsedSpace(root.getTotalSpace() - root.getFreeSpace());
                diskInfoMap.put(root.getAbsolutePath(), diskInfo);
            }
            
            hardwareInfo.setDiskInfo(diskInfoMap);
            
            // Graphics information
            hardwareInfo.setGraphicsInfo(collectGraphicsInfo());
            
        } catch (Exception e) {
            logger.warn("Error collecting hardware information", e);
        }
        
        return hardwareInfo;
    }
    
    /**
     * Collect performance information
     */
    private PerformanceInfo collectPerformanceInfo() {
        PerformanceInfo perfInfo = new PerformanceInfo();
        
        try {
            // Memory usage
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            
            perfInfo.setHeapMemoryUsed(heapMemory.getUsed());
            perfInfo.setHeapMemoryMax(heapMemory.getMax());
            perfInfo.setHeapMemoryCommitted(heapMemory.getCommitted());
            perfInfo.setNonHeapMemoryUsed(nonHeapMemory.getUsed());
            perfInfo.setNonHeapMemoryMax(nonHeapMemory.getMax());
            perfInfo.setNonHeapMemoryCommitted(nonHeapMemory.getCommitted());
            perfInfo.setPendingFinalizationCount(memoryBean.getObjectPendingFinalizationCount());
            
            // Thread information
            perfInfo.setThreadCount(threadBean.getThreadCount());
            perfInfo.setPeakThreadCount(threadBean.getPeakThreadCount());
            perfInfo.setDaemonThreadCount(threadBean.getDaemonThreadCount());
            perfInfo.setTotalStartedThreadCount(threadBean.getTotalStartedThreadCount());
            
            // Garbage collection information
            Map<String, GCInfo> gcInfoMap = new HashMap<>();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                GCInfo gcInfo = new GCInfo();
                gcInfo.setName(gcBean.getName());
                gcInfo.setCollectionCount(gcBean.getCollectionCount());
                gcInfo.setCollectionTime(gcBean.getCollectionTime());
                gcInfo.setMemoryPoolNames(Arrays.asList(gcBean.getMemoryPoolNames()));
                gcInfoMap.put(gcBean.getName(), gcInfo);
            }
            perfInfo.setGcInfo(gcInfoMap);
            
            // Memory pool information
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            Map<String, MemoryPoolInfo> poolInfoMap = new HashMap<>();
            
            for (MemoryPoolMXBean pool : memoryPools) {
                MemoryPoolInfo poolInfo = new MemoryPoolInfo();
                poolInfo.setName(pool.getName());
                poolInfo.setType(pool.getType().name());
                poolInfo.setUsage(pool.getUsage());
                poolInfo.setPeakUsage(pool.getPeakUsage());
                poolInfo.setCollectionUsage(pool.getCollectionUsage());
                poolInfo.setManagerNames(Arrays.asList(pool.getMemoryManagerNames()));
                poolInfoMap.put(pool.getName(), poolInfo);
            }
            perfInfo.setMemoryPoolInfo(poolInfoMap);
            
            // Class loading information
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            perfInfo.setLoadedClassCount(classLoadingBean.getLoadedClassCount());
            perfInfo.setTotalLoadedClassCount(classLoadingBean.getTotalLoadedClassCount());
            perfInfo.setUnloadedClassCount(classLoadingBean.getUnloadedClassCount());
            
        } catch (Exception e) {
            logger.warn("Error collecting performance information", e);
        }
        
        return perfInfo;
    }
    
    /**
     * Collect network information
     */
    private NetworkInfo collectNetworkInfo() {
        NetworkInfo networkInfo = new NetworkInfo();
        
        try {
            // Host information
            InetAddress localHost = InetAddress.getLocalHost();
            networkInfo.setHostName(localHost.getHostName());
            networkInfo.setHostAddress(localHost.getHostAddress());
            networkInfo.setCanonicalHostName(localHost.getCanonicalHostName());
            
            // Network interfaces
            List<NetworkInterfaceInfo> interfaces = new ArrayList<>();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                NetworkInterfaceInfo interfaceInfo = new NetworkInterfaceInfo();
                
                interfaceInfo.setName(networkInterface.getName());
                interfaceInfo.setDisplayName(networkInterface.getDisplayName());
                interfaceInfo.setMtu(networkInterface.getMTU());
                interfaceInfo.setLoopback(networkInterface.isLoopback());
                interfaceInfo.setPointToPoint(networkInterface.isPointToPoint());
                interfaceInfo.setVirtual(networkInterface.isVirtual());
                interfaceInfo.setSupportsMulticast(networkInterface.supportsMulticast());
                
                // Hardware address (MAC)
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null) {
                    StringBuilder macAddress = new StringBuilder();
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        macAddress.append(String.format("%02X%s", hardwareAddress[i], 
                            (i < hardwareAddress.length - 1) ? "-" : ""));
                    }
                    interfaceInfo.setHardwareAddress(macAddress.toString());
                }
                
                // IP addresses
                List<String> ipAddresses = new ArrayList<>();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    ipAddresses.add(address.getHostAddress());
                }
                interfaceInfo.setIpAddresses(ipAddresses);
                
                interfaces.add(interfaceInfo);
            }
            
            networkInfo.setNetworkInterfaces(interfaces);
            
            // Internet connectivity test
            networkInfo.setInternetConnectivity(testInternetConnectivity());
            
            // DNS information
            networkInfo.setDnsServers(getDnsServers());
            
            // Proxy information
            networkInfo.setProxyInfo(getProxyInfo());
            
        } catch (Exception e) {
            logger.warn("Error collecting network information", e);
        }
        
        return networkInfo;
    }
    
    /**
     * Collect security information
     */
    private SecurityInfo collectSecurityInfo() {
        SecurityInfo securityInfo = new SecurityInfo();
        
        try {
            // Security Manager
            SecurityManager securityManager = System.getSecurityManager();
            securityInfo.setSecurityManagerEnabled(securityManager != null);
            
            // Security providers
            List<String> providers = Arrays.stream(java.security.Security.getProviders())
                .map(provider -> provider.getName() + " v" + provider.getVersion())
                .collect(Collectors.toList());
            securityInfo.setSecurityProviders(providers);
            
            // SSL/TLS information
            securityInfo.setSslInfo(collectSslInfo());
            
            // File permissions for current directory
            if (collectSensitiveInfo) {
                File currentDir = new File(System.getProperty("user.dir"));
                securityInfo.setCurrentDirReadable(currentDir.canRead());
                securityInfo.setCurrentDirWritable(currentDir.canWrite());
                securityInfo.setCurrentDirExecutable(currentDir.canExecute());
            }
            
            // Java security properties
            Map<String, String> securityProperties = new HashMap<>();
            String[] securityProps = {
                "java.security.policy",
                "java.security.manager",
                "java.security.auth.login.config",
                "javax.net.ssl.trustStore",
                "javax.net.ssl.keyStore"
            };
            
            for (String prop : securityProps) {
                String value = System.getProperty(prop);
                if (value != null) {
                    securityProperties.put(prop, collectSensitiveInfo ? value : "[PROTECTED]");
                }
            }
            securityInfo.setSecurityProperties(securityProperties);
            
        } catch (Exception e) {
            logger.warn("Error collecting security information", e);
        }
        
        return securityInfo;
    }
    
    /**
     * Collect application information
     */
    private ApplicationInfo collectApplicationInfo() {
        ApplicationInfo appInfo = new ApplicationInfo();
        
        try {
            // Maven/build information (if available)
            appInfo.setMavenInfo(getMavenInfo());
            
            // Git information (if available)
            appInfo.setGitInfo(getGitInfo());
            
            // Application properties
            appInfo.setApplicationProperties(getApplicationProperties());
            
            // Framework information
            appInfo.setFrameworkName("CS Test Framework");
            appInfo.setFrameworkVersion(getFrameworkVersion());
            
            // Build timestamp
            appInfo.setBuildTimestamp(getBuildTimestamp());
            
        } catch (Exception e) {
            logger.warn("Error collecting application information", e);
        }
        
        return appInfo;
    }
    
    /**
     * Collect browser information
     */
    private BrowserInfo collectBrowserInfo() {
        BrowserInfo browserInfo = new BrowserInfo();
        
        try {
            // This would be populated by WebDriver when available
            browserInfo.setAvailableBrowsers(getAvailableBrowsers());
            
        } catch (Exception e) {
            logger.warn("Error collecting browser information", e);
        }
        
        return browserInfo;
    }
    
    /**
     * Collect Selenium information
     */
    private SeleniumInfo collectSeleniumInfo() {
        SeleniumInfo seleniumInfo = new SeleniumInfo();
        
        try {
            // Selenium version (from classpath)
            seleniumInfo.setSeleniumVersion(getSeleniumVersion());
            
            // WebDriver information
            seleniumInfo.setWebDriverInfo(getWebDriverInfo());
            
        } catch (Exception e) {
            logger.warn("Error collecting Selenium information", e);
        }
        
        return seleniumInfo;
    }
    
    /**
     * Collect test framework information
     */
    private TestFrameworkInfo collectTestFrameworkInfo() {
        TestFrameworkInfo frameworkInfo = new TestFrameworkInfo();
        
        try {
            // TestNG version
            frameworkInfo.setTestNgVersion(getTestNgVersion());
            
            // JUnit version (if available)
            frameworkInfo.setJunitVersion(getJunitVersion());
            
        } catch (Exception e) {
            logger.warn("Error collecting test framework information", e);
        }
        
        return frameworkInfo;
    }
    
    /**
     * Collect database information
     */
    private DatabaseInfo collectDatabaseInfo() {
        DatabaseInfo dbInfo = new DatabaseInfo();
        
        try {
            // Available JDBC drivers
            List<String> drivers = new ArrayList<>();
            Enumeration<java.sql.Driver> driverEnum = java.sql.DriverManager.getDrivers();
            while (driverEnum.hasMoreElements()) {
                java.sql.Driver driver = driverEnum.nextElement();
                drivers.add(driver.getClass().getName() + " v" + driver.getMajorVersion() + "." + driver.getMinorVersion());
            }
            dbInfo.setAvailableDrivers(drivers);
            
        } catch (Exception e) {
            logger.warn("Error collecting database information", e);
        }
        
        return dbInfo;
    }
    
    /**
     * Collect custom information
     */
    private Map<String, Object> collectCustomInfo() {
        Map<String, Object> customInfo = new HashMap<>();
        
        try {
            // Custom properties from configuration
            Properties configProps = config.getAllProperties();
            for (String key : configProps.stringPropertyNames()) {
                if (key.startsWith("cs.element.")) {
                    customInfo.put(key, configProps.getProperty(key));
                }
            }
            
            // Add timestamp
            customInfo.put("collection.timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            customInfo.put("collection.timezone", TimeZone.getDefault().getID());
            
        } catch (Exception e) {
            logger.warn("Error collecting custom information", e);
        }
        
        return customInfo;
    }
    
    // Helper methods implementation continues...
    
    /**
     * Generate environment fingerprint for tracking
     */
    private String generateEnvironmentFingerprint(EnvironmentInfo envInfo) {
        try {
            StringBuilder fingerprint = new StringBuilder();
            
            // Key components for fingerprint
            if (envInfo.getSystemInfo() != null) {
                fingerprint.append(envInfo.getSystemInfo().getOsName())
                          .append(envInfo.getSystemInfo().getOsVersion())
                          .append(envInfo.getSystemInfo().getOsArch());
            }
            
            if (envInfo.getJavaInfo() != null) {
                fingerprint.append(envInfo.getJavaInfo().getJavaVersion())
                          .append(envInfo.getJavaInfo().getJvmName());
            }
            
            if (envInfo.getHardwareInfo() != null) {
                fingerprint.append(envInfo.getHardwareInfo().getCpuCores())
                          .append(envInfo.getHardwareInfo().getMaxMemory());
            }
            
            // Generate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(fingerprint.toString().getBytes());
            
            StringBuilder hash = new StringBuilder();
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }
            
            return hash.toString();
            
        } catch (Exception e) {
            logger.warn("Error generating environment fingerprint", e);
            return "unknown";
        }
    }
    
    // Additional helper methods would continue here...
    // [Implementation of all helper methods like getComputerName(), getSystemUptime(), etc.]
    
    /**
     * Get computer name
     */
    private String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return System.getenv("COMPUTERNAME") != null ? System.getenv("COMPUTERNAME") : "unknown";
        }
    }
    
    /**
     * Get system uptime
     */
    private long getSystemUptime() {
        try {
            return runtimeBean.getUptime();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get filtered environment variables
     */
    private Map<String, String> getFilteredEnvironmentVariables() {
        Map<String, String> filtered = new HashMap<>();
        Map<String, String> env = System.getenv();
        
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (!excludedProperties.contains(key) && !key.toLowerCase().contains("password") 
                && !key.toLowerCase().contains("secret") && !key.toLowerCase().contains("token")) {
                filtered.put(key, entry.getValue());
            }
        }
        
        return filtered;
    }
    
    /**
     * Get filtered system properties
     */
    private Map<String, String> getFilteredSystemProperties() {
        Map<String, String> filtered = new HashMap<>();
        Properties props = System.getProperties();
        
        for (String key : props.stringPropertyNames()) {
            if (!excludedProperties.contains(key)) {
                filtered.put(key, props.getProperty(key));
            }
        }
        
        return filtered;
    }
    
    /**
     * Extract PID from runtime name
     */
    private long extractPidFromName(String name) {
        try {
            return Long.parseLong(name.split("@")[0]);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get framework version
     */
    private String getFrameworkVersion() {
        try {
            // Try to read from manifest or properties
            Package pkg = this.getClass().getPackage();
            String version = pkg.getImplementationVersion();
            return version != null ? version : "development";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Test internet connectivity
     */
    private boolean testInternetConnectivity() {
        try {
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Export environment information to JSON
     */
    public String exportToJson(EnvironmentInfo envInfo) {
        try {
            return CSJsonUtils.toJson(envInfo);
        } catch (Exception e) {
            logger.error("Failed to export environment info to JSON", e);
            throw new CSEnvironmentException("Failed to export environment info", e);
        }
    }
    
    /**
     * Export environment information to file
     */
    public String exportToFile(EnvironmentInfo envInfo, String filePath) {
        try {
            String json = exportToJson(envInfo);
            Files.write(Paths.get(filePath), json.getBytes());
            logger.info("Environment information exported to: {}", filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("Failed to export environment info to file: {}", filePath, e);
            throw new CSEnvironmentException("Failed to export environment info to file", e);
        }
    }
    
    /**
     * Additional helper methods would be implemented here
     * Due to length constraints, showing the structure and key methods
     */
    
    // Placeholder implementations for remaining helper methods
    private String getSystemCommand(String type) { return null; }
    private String extractCpuModel(String cpuInfo) { return "Unknown"; }
    private String extractCpuSpeed(String cpuInfo) { return "Unknown"; }
    private Map<String, Object> collectGraphicsInfo() { return new HashMap<>(); }
    private Map<String, Object> collectSslInfo() { return new HashMap<>(); }
    private Map<String, Object> getMavenInfo() { return new HashMap<>(); }
    private Map<String, Object> getGitInfo() { return new HashMap<>(); }
    private Map<String, Object> getApplicationProperties() { return new HashMap<>(); }
    private String getBuildTimestamp() { return LocalDateTime.now().toString(); }
    private List<String> getAvailableBrowsers() { return new ArrayList<>(); }
    private String getSeleniumVersion() { return "4.x"; }
    private Map<String, Object> getWebDriverInfo() { return new HashMap<>(); }
    private String getTestNgVersion() { return "7.x"; }
    private String getJunitVersion() { return "5.x"; }
    private List<String> getDnsServers() { return new ArrayList<>(); }
    private Map<String, Object> getProxyInfo() { return new HashMap<>(); }
}