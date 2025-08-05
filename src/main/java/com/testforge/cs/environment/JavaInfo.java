package com.testforge.cs.environment;

import java.util.List;
import java.util.Map;

/**
 * Java runtime information container
 */
public class JavaInfo {
    private String javaVersion;
    private String javaVendor;
    private String javaVendorUrl;
    private String javaHome;
    private String javaSpecificationVersion;
    private String javaSpecificationVendor;
    private String javaSpecificationName;
    private String jvmName;
    private String jvmVersion;
    private String jvmVendor;
    private String jvmSpecificationName;
    private String jvmSpecificationVersion;
    private String jvmSpecificationVendor;
    private String runtimeName;
    private String runtimeVersion;
    private String vmName;
    private String vmVersion;
    private String vmVendor;
    private long startTime;
    private long uptime;
    private long pid;
    private String classPath;
    private String libraryPath;
    private String bootClassPath;
    private List<String> inputArguments;
    private Map<String, String> systemProperties;
    private String compilerName;
    private long totalCompilationTime;
    private boolean compilationTimeMonitoringSupported;
    
    // Getters and setters
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    
    public String getJavaVendor() { return javaVendor; }
    public void setJavaVendor(String javaVendor) { this.javaVendor = javaVendor; }
    
    public String getJavaVendorUrl() { return javaVendorUrl; }
    public void setJavaVendorUrl(String javaVendorUrl) { this.javaVendorUrl = javaVendorUrl; }
    
    public String getJavaHome() { return javaHome; }
    public void setJavaHome(String javaHome) { this.javaHome = javaHome; }
    
    public String getJavaSpecificationVersion() { return javaSpecificationVersion; }
    public void setJavaSpecificationVersion(String javaSpecificationVersion) { this.javaSpecificationVersion = javaSpecificationVersion; }
    
    public String getJavaSpecificationVendor() { return javaSpecificationVendor; }
    public void setJavaSpecificationVendor(String javaSpecificationVendor) { this.javaSpecificationVendor = javaSpecificationVendor; }
    
    public String getJavaSpecificationName() { return javaSpecificationName; }
    public void setJavaSpecificationName(String javaSpecificationName) { this.javaSpecificationName = javaSpecificationName; }
    
    public String getJvmName() { return jvmName; }
    public void setJvmName(String jvmName) { this.jvmName = jvmName; }
    
    public String getJvmVersion() { return jvmVersion; }
    public void setJvmVersion(String jvmVersion) { this.jvmVersion = jvmVersion; }
    
    public String getJvmVendor() { return jvmVendor; }
    public void setJvmVendor(String jvmVendor) { this.jvmVendor = jvmVendor; }
    
    public String getJvmSpecificationName() { return jvmSpecificationName; }
    public void setJvmSpecificationName(String jvmSpecificationName) { this.jvmSpecificationName = jvmSpecificationName; }
    
    public String getJvmSpecificationVersion() { return jvmSpecificationVersion; }
    public void setJvmSpecificationVersion(String jvmSpecificationVersion) { this.jvmSpecificationVersion = jvmSpecificationVersion; }
    
    public String getJvmSpecificationVendor() { return jvmSpecificationVendor; }
    public void setJvmSpecificationVendor(String jvmSpecificationVendor) { this.jvmSpecificationVendor = jvmSpecificationVendor; }
    
    public String getRuntimeName() { return runtimeName; }
    public void setRuntimeName(String runtimeName) { this.runtimeName = runtimeName; }
    
    public String getRuntimeVersion() { return runtimeVersion; }
    public void setRuntimeVersion(String runtimeVersion) { this.runtimeVersion = runtimeVersion; }
    
    public String getVmName() { return vmName; }
    public void setVmName(String vmName) { this.vmName = vmName; }
    
    public String getVmVersion() { return vmVersion; }
    public void setVmVersion(String vmVersion) { this.vmVersion = vmVersion; }
    
    public String getVmVendor() { return vmVendor; }
    public void setVmVendor(String vmVendor) { this.vmVendor = vmVendor; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getUptime() { return uptime; }
    public void setUptime(long uptime) { this.uptime = uptime; }
    
    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }
    
    public String getClassPath() { return classPath; }
    public void setClassPath(String classPath) { this.classPath = classPath; }
    
    public String getLibraryPath() { return libraryPath; }
    public void setLibraryPath(String libraryPath) { this.libraryPath = libraryPath; }
    
    public String getBootClassPath() { return bootClassPath; }
    public void setBootClassPath(String bootClassPath) { this.bootClassPath = bootClassPath; }
    
    public List<String> getInputArguments() { return inputArguments; }
    public void setInputArguments(List<String> inputArguments) { this.inputArguments = inputArguments; }
    
    public Map<String, String> getSystemProperties() { return systemProperties; }
    public void setSystemProperties(Map<String, String> systemProperties) { this.systemProperties = systemProperties; }
    
    public String getCompilerName() { return compilerName; }
    public void setCompilerName(String compilerName) { this.compilerName = compilerName; }
    
    public long getTotalCompilationTime() { return totalCompilationTime; }
    public void setTotalCompilationTime(long totalCompilationTime) { this.totalCompilationTime = totalCompilationTime; }
    
    public boolean isCompilationTimeMonitoringSupported() { return compilationTimeMonitoringSupported; }
    public void setCompilationTimeMonitoringSupported(boolean compilationTimeMonitoringSupported) { this.compilationTimeMonitoringSupported = compilationTimeMonitoringSupported; }
}