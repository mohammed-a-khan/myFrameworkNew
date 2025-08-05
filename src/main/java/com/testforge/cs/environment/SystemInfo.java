package com.testforge.cs.environment;

import java.util.Map;

/**
 * System information container
 */
public class SystemInfo {
    private String osName;
    private String osVersion;
    private String osArch;
    private String computerName;
    private String userName;
    private String userHome;
    private String userDir;
    private String timeZone;
    private String locale;
    private String charsetDefault;
    private String fileSeparator;
    private String pathSeparator;
    private String lineSeparator;
    private String tempDir;
    private int availableProcessors;
    private double systemLoadAverage;
    private long totalPhysicalMemory;
    private long freePhysicalMemory;
    private long totalSwapSpace;
    private long freeSwapSpace;
    private long committedVirtualMemory;
    private double processCpuLoad;
    private double systemCpuLoad;
    private long processCpuTime;
    private long systemUptime;
    private Map<String, String> environmentVariables;
    
    // Getters and setters
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    
    public String getOsArch() { return osArch; }
    public void setOsArch(String osArch) { this.osArch = osArch; }
    
    public String getComputerName() { return computerName; }
    public void setComputerName(String computerName) { this.computerName = computerName; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserHome() { return userHome; }
    public void setUserHome(String userHome) { this.userHome = userHome; }
    
    public String getUserDir() { return userDir; }
    public void setUserDir(String userDir) { this.userDir = userDir; }
    
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    
    public String getCharsetDefault() { return charsetDefault; }
    public void setCharsetDefault(String charsetDefault) { this.charsetDefault = charsetDefault; }
    
    public String getFileSeparator() { return fileSeparator; }
    public void setFileSeparator(String fileSeparator) { this.fileSeparator = fileSeparator; }
    
    public String getPathSeparator() { return pathSeparator; }
    public void setPathSeparator(String pathSeparator) { this.pathSeparator = pathSeparator; }
    
    public String getLineSeparator() { return lineSeparator; }
    public void setLineSeparator(String lineSeparator) { this.lineSeparator = lineSeparator; }
    
    public String getTempDir() { return tempDir; }
    public void setTempDir(String tempDir) { this.tempDir = tempDir; }
    
    public int getAvailableProcessors() { return availableProcessors; }
    public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
    
    public double getSystemLoadAverage() { return systemLoadAverage; }
    public void setSystemLoadAverage(double systemLoadAverage) { this.systemLoadAverage = systemLoadAverage; }
    
    public long getTotalPhysicalMemory() { return totalPhysicalMemory; }
    public void setTotalPhysicalMemory(long totalPhysicalMemory) { this.totalPhysicalMemory = totalPhysicalMemory; }
    
    public long getFreePhysicalMemory() { return freePhysicalMemory; }
    public void setFreePhysicalMemory(long freePhysicalMemory) { this.freePhysicalMemory = freePhysicalMemory; }
    
    public long getTotalSwapSpace() { return totalSwapSpace; }
    public void setTotalSwapSpace(long totalSwapSpace) { this.totalSwapSpace = totalSwapSpace; }
    
    public long getFreeSwapSpace() { return freeSwapSpace; }
    public void setFreeSwapSpace(long freeSwapSpace) { this.freeSwapSpace = freeSwapSpace; }
    
    public long getCommittedVirtualMemory() { return committedVirtualMemory; }
    public void setCommittedVirtualMemory(long committedVirtualMemory) { this.committedVirtualMemory = committedVirtualMemory; }
    
    public double getProcessCpuLoad() { return processCpuLoad; }
    public void setProcessCpuLoad(double processCpuLoad) { this.processCpuLoad = processCpuLoad; }
    
    public double getSystemCpuLoad() { return systemCpuLoad; }
    public void setSystemCpuLoad(double systemCpuLoad) { this.systemCpuLoad = systemCpuLoad; }
    
    public long getProcessCpuTime() { return processCpuTime; }
    public void setProcessCpuTime(long processCpuTime) { this.processCpuTime = processCpuTime; }
    
    public long getSystemUptime() { return systemUptime; }
    public void setSystemUptime(long systemUptime) { this.systemUptime = systemUptime; }
    
    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) { this.environmentVariables = environmentVariables; }
}