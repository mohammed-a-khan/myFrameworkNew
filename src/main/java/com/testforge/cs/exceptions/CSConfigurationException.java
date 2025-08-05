package com.testforge.cs.exceptions;

/**
 * Exception for configuration-related errors
 */
public class CSConfigurationException extends CSFrameworkException {
    
    private String configKey;
    private String configFile;
    
    public CSConfigurationException(String message) {
        super("CONFIG_ERROR", "Configuration", message);
    }
    
    public CSConfigurationException(String message, Throwable cause) {
        super("CONFIG_ERROR", "Configuration", message, cause);
    }
    
    public CSConfigurationException(String configKey, String configFile, String message) {
        super("CONFIG_ERROR", "Configuration", message);
        this.configKey = configKey;
        this.configFile = configFile;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public String getConfigFile() {
        return configFile;
    }
}