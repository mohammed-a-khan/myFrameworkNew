package com.testforge.cs.config;

import com.testforge.cs.exceptions.CSConfigurationException;
import com.testforge.cs.security.CSEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Configuration Manager for CS Framework
 * Manages all configuration properties with support for multiple environments
 * Thread-safe singleton implementation
 */
public class CSConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(CSConfigManager.class);
    private static CSConfigManager instance;
    private static final Object lock = new Object();
    
    private final Map<String, Properties> propertiesCache = new ConcurrentHashMap<>();
    private final Properties mergedProperties = new Properties();
    private String currentEnvironment;
    
    private static final String DEFAULT_CONFIG_PATH = "resources/config/";
    private static final String OBJECT_REPOSITORIES_PATH = "object-repositories/";
    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String OBJECT_REPOSITORY_PROPERTIES = "object-repository.properties";
    private static final String SQL_QUERIES_PROPERTIES = "SqlQueries.properties";
    
    private CSConfigManager() {
        initialize();
    }
    
    /**
     * Get singleton instance
     */
    public static CSConfigManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CSConfigManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize configuration manager
     */
    private void initialize() {
        try {
            // Load default configurations
            loadDefaultConfigurations();
            
            // Determine current environment
            currentEnvironment = System.getProperty("environment.name", getProperty("environment.name", "qa"));
            logger.info("Initialized CSConfigManager with environment: {}", currentEnvironment);
            
            // Load environment-specific configurations
            loadEnvironmentConfigurations();
            
        } catch (Exception e) {
            logger.error("Failed to initialize CSConfigManager", e);
            throw new CSConfigurationException("Failed to initialize configuration manager", e);
        }
    }
    
    /**
     * Load default configuration files
     */
    private void loadDefaultConfigurations() {
        // Load application and SQL properties from config folder
        loadPropertiesFromFile(DEFAULT_CONFIG_PATH + APPLICATION_PROPERTIES);
        loadPropertiesFromFile(DEFAULT_CONFIG_PATH + SQL_QUERIES_PROPERTIES);
        
        // Load object repositories from object-repositories folder
        loadObjectRepositories();
    }
    
    /**
     * Load all object repository files from object-repositories folder
     */
    private void loadObjectRepositories() {
        File orFolder = new File(OBJECT_REPOSITORIES_PATH);
        if (orFolder.exists() && orFolder.isDirectory()) {
            File[] propFiles = orFolder.listFiles((dir, name) -> name.endsWith(".properties"));
            if (propFiles != null) {
                for (File file : propFiles) {
                    loadPropertiesFromFile(file.getPath());
                    logger.info("Loaded object repository: {}", file.getName());
                }
            }
        } else {
            logger.warn("Object repositories folder not found: {}", OBJECT_REPOSITORIES_PATH);
        }
    }
    
    /**
     * Load environment-specific configurations
     */
    private void loadEnvironmentConfigurations() {
        String envFile = String.format("application-%s.properties", currentEnvironment);
        // Load only from project root resources/config folder
        loadPropertiesFromFile(DEFAULT_CONFIG_PATH + envFile);
    }
    
    /**
     * Load properties from classpath
     */
    private void loadPropertiesFromClasspath(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config/" + fileName)) {
            if (inputStream != null) {
                Properties props = new Properties();
                props.load(inputStream);
                propertiesCache.put(fileName, props);
                mergedProperties.putAll(props);
                logger.debug("Loaded properties from classpath: {}", fileName);
            }
        } catch (IOException e) {
            logger.warn("Could not load properties from classpath: {}", fileName);
        }
    }
    
    /**
     * Load properties from file system
     */
    private void loadPropertiesFromFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                Properties props = new Properties();
                props.load(inputStream);
                propertiesCache.put(filePath, props);
                mergedProperties.putAll(props);
                logger.debug("Loaded properties from file: {}", filePath);
            } catch (IOException e) {
                logger.warn("Could not load properties from file: {}", filePath);
            }
        }
    }
    
    /**
     * Get property value
     * Automatically decrypts values that are encrypted (wrapped in ENC())
     */
    public String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = mergedProperties.getProperty(key);
        }
        if (value == null) {
            logger.warn("Property not found: {}", key);
            return null;
        }
        
        // Automatically decrypt if the value is encrypted
        if (CSEncryptionUtils.isEncrypted(value)) {
            String decrypted = CSEncryptionUtils.decrypt(value);
            logger.debug("Decrypted value for key: {}", key);
            return decrypted;
        }
        
        return value;
    }
    
    /**
     * Get property value with default
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get integer property
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get boolean property
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * Get long property
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get double property
     */
    public double getDoubleProperty(String key, double defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid double value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get list property (comma-separated values)
     */
    public List<String> getListProperty(String key) {
        String value = getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    /**
     * Get map property (key1=value1,key2=value2)
     */
    public Map<String, String> getMapProperty(String key) {
        String value = getProperty(key);
        Map<String, String> map = new HashMap<>();
        if (value != null && !value.trim().isEmpty()) {
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> s.contains("="))
                    .forEach(s -> {
                        String[] parts = s.split("=", 2);
                        map.put(parts[0].trim(), parts[1].trim());
                    });
        }
        return map;
    }
    
    /**
     * Set property value
     */
    public void setProperty(String key, String value) {
        mergedProperties.setProperty(key, value);
        logger.debug("Property set: {} = {}", key, value);
    }
    
    /**
     * Get all properties
     */
    public Properties getAllProperties() {
        return new Properties(mergedProperties);
    }
    
    /**
     * Get properties by prefix
     */
    public Properties getPropertiesByPrefix(String prefix) {
        Properties filtered = new Properties();
        mergedProperties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix))
                .forEach(key -> filtered.setProperty(key, mergedProperties.getProperty(key)));
        return filtered;
    }
    
    /**
     * Get current environment
     */
    public String getCurrentEnvironment() {
        return currentEnvironment;
    }
    
    /**
     * Reload configurations
     */
    public void reload() {
        synchronized (lock) {
            propertiesCache.clear();
            mergedProperties.clear();
            initialize();
            logger.info("Configuration reloaded");
        }
    }
    
    /**
     * Load additional properties file
     */
    public void loadAdditionalProperties(String filePath) {
        loadPropertiesFromFile(filePath);
    }
    
    /**
     * Get environment-specific property
     */
    public String getEnvironmentProperty(String key) {
        String envKey = String.format("env.%s.%s", currentEnvironment, key);
        return getProperty(envKey);
    }
    
    /**
     * Get browser configuration
     */
    public BrowserConfig getBrowserConfig() {
        return new BrowserConfig(
            getProperty("browser.name", "chrome"),
            getBooleanProperty("browser.headless", false),
            getBooleanProperty("browser.maximize", true),
            getIntProperty("browser.implicit.wait", 10),
            getIntProperty("browser.explicit.wait", 30),
            getIntProperty("browser.page.load.timeout", 60),
            getProperty("browser.download.directory", "target/downloads"),
            getIntProperty("browser.window.width", 1920),
            getIntProperty("browser.window.height", 1080)
        );
    }
    
    // Convenience methods with simplified names
    
    public String getString(String key) {
        return getProperty(key);
    }
    
    public String getString(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }
    
    public int getInt(String key) {
        return getIntProperty(key, 0);
    }
    
    public int getInt(String key, int defaultValue) {
        return getIntProperty(key, defaultValue);
    }
    
    public long getLong(String key) {
        return getLongProperty(key, 0L);
    }
    
    public long getLong(String key, long defaultValue) {
        return getLongProperty(key, defaultValue);
    }
    
    public double getDouble(String key) {
        return getDoubleProperty(key, 0.0);
    }
    
    public double getDouble(String key, double defaultValue) {
        return getDoubleProperty(key, defaultValue);
    }
    
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }
    
    public float getFloat(String key, float defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid float value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key) {
        return getBooleanProperty(key, false);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBooleanProperty(key, defaultValue);
    }
    
    public List<String> getList(String key) {
        return getListProperty(key);
    }
    
    /**
     * Check if property exists
     */
    public boolean hasProperty(String key) {
        String value = getProperty(key);
        return value != null && !value.isEmpty();
    }
    
    /**
     * Resolve value - handles placeholders and environment variables
     */
    public String resolveValue(String value) {
        if (value == null) {
            return null;
        }
        
        // Replace ${property} placeholders
        String resolved = value;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getProperty(placeholder);
            if (replacement != null) {
                resolved = resolved.replace("${" + placeholder + "}", replacement);
            }
        }
        
        return resolved;
    }
    
    /**
     * Browser configuration class
     */
    public static class BrowserConfig {
        private final String defaultBrowser;
        private final boolean headless;
        private final boolean maximize;
        private final int implicitWait;
        private final int explicitWait;
        private final int pageLoadTimeout;
        private final String downloadDirectory;
        private final int windowWidth;
        private final int windowHeight;
        
        public BrowserConfig(String defaultBrowser, boolean headless, boolean maximize,
                           int implicitWait, int explicitWait, int pageLoadTimeout,
                           String downloadDirectory, int windowWidth, int windowHeight) {
            this.defaultBrowser = defaultBrowser;
            this.headless = headless;
            this.maximize = maximize;
            this.implicitWait = implicitWait;
            this.explicitWait = explicitWait;
            this.pageLoadTimeout = pageLoadTimeout;
            this.downloadDirectory = downloadDirectory;
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
        }
        
        // Getters
        public String getDefaultBrowser() { return defaultBrowser; }
        public boolean isHeadless() { return headless; }
        public boolean isMaximize() { return maximize; }
        public int getImplicitWait() { return implicitWait; }
        public int getExplicitWait() { return explicitWait; }
        public int getPageLoadTimeout() { return pageLoadTimeout; }
        public String getDownloadDirectory() { return downloadDirectory; }
        public int getWindowWidth() { return windowWidth; }
        public int getWindowHeight() { return windowHeight; }
    }
}