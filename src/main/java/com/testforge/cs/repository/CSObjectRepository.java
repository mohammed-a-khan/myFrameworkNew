package com.testforge.cs.repository;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSRepositoryException;
import com.testforge.cs.utils.CSFileUtils;
import com.testforge.cs.utils.CSJsonUtils;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Production-ready Object Repository Management System
 * Centralized management of page objects, locators, and test data
 * Thread-safe with caching and hot-reload capabilities
 */
public class CSObjectRepository {
    private static final Logger logger = LoggerFactory.getLogger(CSObjectRepository.class);
    private static volatile CSObjectRepository instance;
    
    // Repository storage
    private final Map<String, CSPageObjectDefinition> pageObjects = new ConcurrentHashMap<>();
    private final Map<String, CSElementDefinition> elements = new ConcurrentHashMap<>();
    private final Map<String, CSTestDataSet> testDataSets = new ConcurrentHashMap<>();
    private final Map<String, CSEnvironmentConfig> environments = new ConcurrentHashMap<>();
    
    // Caching and monitoring
    private final Map<String, LocalDateTime> fileLastModified = new ConcurrentHashMap<>();
    private final Object reloadLock = new Object();
    
    // Configuration
    private String repositoryBasePath;
    private boolean autoReloadEnabled;
    private int cacheTimeoutMinutes;
    private boolean strictModeEnabled;
    
    private CSObjectRepository() {
        initialize();
    }
    
    public static CSObjectRepository getInstance() {
        if (instance == null) {
            synchronized (CSObjectRepository.class) {
                if (instance == null) {
                    instance = new CSObjectRepository();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize repository system
     */
    private void initialize() {
        try {
            CSConfigManager config = CSConfigManager.getInstance();
            
            // Load configuration
            repositoryBasePath = config.getProperty("repository.base.path", "src/test/resources/repository");
            autoReloadEnabled = Boolean.parseBoolean(config.getProperty("repository.auto.reload.enabled", "true"));
            cacheTimeoutMinutes = Integer.parseInt(config.getProperty("repository.cache.timeout.minutes", "30"));
            strictModeEnabled = Boolean.parseBoolean(config.getProperty("repository.strict.mode.enabled", "true"));
            
            // Create repository directories if they don't exist
            createRepositoryStructure();
            
            // Load all repository data
            loadAllRepositoryData();
            
            // Start auto-reload monitoring if enabled
            if (autoReloadEnabled) {
                startAutoReloadMonitoring();
            }
            
            logger.info("Object repository initialized - Path: {}, AutoReload: {}, StrictMode: {}",
                repositoryBasePath, autoReloadEnabled, strictModeEnabled);
                
        } catch (Exception e) {
            logger.error("Failed to initialize object repository", e);
            throw new CSRepositoryException("Repository initialization failed", e);
        }
    }
    
    /**
     * Create repository directory structure
     */
    private void createRepositoryStructure() throws Exception {
        Path basePath = Paths.get(repositoryBasePath);
        
        // Create main directories
        Files.createDirectories(basePath);
        Files.createDirectories(basePath.resolve("pages"));
        Files.createDirectories(basePath.resolve("elements"));
        Files.createDirectories(basePath.resolve("data"));
        Files.createDirectories(basePath.resolve("environments"));
        
        logger.debug("Created repository directory structure at: {}", repositoryBasePath);
    }
    
    /**
     * Load all repository data
     */
    private void loadAllRepositoryData() {
        synchronized (reloadLock) {
            try {
                loadPageObjects();
                loadElements();
                loadTestDataSets();
                loadEnvironments();
                
                logger.info("Loaded repository data - Pages: {}, Elements: {}, DataSets: {}, Environments: {}",
                    pageObjects.size(), elements.size(), testDataSets.size(), environments.size());
                    
            } catch (Exception e) {
                logger.error("Error loading repository data", e);
                if (strictModeEnabled) {
                    throw new CSRepositoryException("Failed to load repository data", e);
                }
            }
        }
    }
    
    /**
     * Load page object definitions
     */
    private void loadPageObjects() throws Exception {
        Path pagesDir = Paths.get(repositoryBasePath, "pages");
        if (!Files.exists(pagesDir)) {
            return;
        }
        
        Files.walk(pagesDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(path -> {
                try {
                    String content = CSFileUtils.readFileAsString(path.toString());
                    CSPageObjectDefinition pageObject = CSJsonUtils.fromJson(content, CSPageObjectDefinition.class);
                    
                    // Validate page object
                    validatePageObject(pageObject);
                    
                    pageObjects.put(pageObject.getName(), pageObject);
                    fileLastModified.put(path.toString(), LocalDateTime.now());
                    
                    logger.debug("Loaded page object: {} from {}", pageObject.getName(), path);
                    
                } catch (Exception e) {
                    logger.error("Failed to load page object from: {}", path, e);
                    if (strictModeEnabled) {
                        throw new CSRepositoryException("Failed to load page object: " + path, e);
                    }
                }
            });
    }
    
    /**
     * Load element definitions
     */
    private void loadElements() throws Exception {
        Path elementsDir = Paths.get(repositoryBasePath, "elements");
        if (!Files.exists(elementsDir)) {
            return;
        }
        
        Files.walk(elementsDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(path -> {
                try {
                    String content = CSFileUtils.readFileAsString(path.toString());
                    
                    // Support both single element and array of elements
                    if (content.trim().startsWith("[")) {
                        List<CSElementDefinition> elementList = CSJsonUtils.fromJson(
                            content, 
                            new com.fasterxml.jackson.core.type.TypeReference<List<CSElementDefinition>>() {}
                        );
                        
                        for (CSElementDefinition element : elementList) {
                            validateElement(element);
                            elements.put(element.getName(), element);
                        }
                        
                        logger.debug("Loaded {} elements from {}", elementList.size(), path);
                        
                    } else {
                        CSElementDefinition element = CSJsonUtils.fromJson(content, CSElementDefinition.class);
                        validateElement(element);
                        elements.put(element.getName(), element);
                        
                        logger.debug("Loaded element: {} from {}", element.getName(), path);
                    }
                    
                    fileLastModified.put(path.toString(), LocalDateTime.now());
                    
                } catch (Exception e) {
                    logger.error("Failed to load elements from: {}", path, e);
                    if (strictModeEnabled) {
                        throw new CSRepositoryException("Failed to load elements: " + path, e);
                    }
                }
            });
    }
    
    /**
     * Load test data sets
     */
    private void loadTestDataSets() throws Exception {
        Path dataDir = Paths.get(repositoryBasePath, "data");
        if (!Files.exists(dataDir)) {
            return;
        }
        
        Files.walk(dataDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(path -> {
                try {
                    String content = CSFileUtils.readFileAsString(path.toString());
                    CSTestDataSet dataSet = CSJsonUtils.fromJson(content, CSTestDataSet.class);
                    
                    validateTestDataSet(dataSet);
                    testDataSets.put(dataSet.getName(), dataSet);
                    fileLastModified.put(path.toString(), LocalDateTime.now());
                    
                    logger.debug("Loaded test data set: {} from {}", dataSet.getName(), path);
                    
                } catch (Exception e) {
                    logger.error("Failed to load test data set from: {}", path, e);
                    if (strictModeEnabled) {
                        throw new CSRepositoryException("Failed to load test data set: " + path, e);
                    }
                }
            });
    }
    
    /**
     * Load environment configurations
     */
    private void loadEnvironments() throws Exception {
        Path envDir = Paths.get(repositoryBasePath, "environments");
        if (!Files.exists(envDir)) {
            return;
        }
        
        Files.walk(envDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(path -> {
                try {
                    String content = CSFileUtils.readFileAsString(path.toString());
                    CSEnvironmentConfig envConfig = CSJsonUtils.fromJson(content, CSEnvironmentConfig.class);
                    
                    validateEnvironmentConfig(envConfig);
                    environments.put(envConfig.getName(), envConfig);
                    fileLastModified.put(path.toString(), LocalDateTime.now());
                    
                    logger.debug("Loaded environment config: {} from {}", envConfig.getName(), path);
                    
                } catch (Exception e) {
                    logger.error("Failed to load environment config from: {}", path, e);
                    if (strictModeEnabled) {
                        throw new CSRepositoryException("Failed to load environment config: " + path, e);
                    }
                }
            });
    }
    
    /**
     * Start auto-reload monitoring
     */
    private void startAutoReloadMonitoring() {
        // Simple file system monitoring using a background thread
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    checkForFileChanges();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in auto-reload monitoring", e);
                }
            }
        }, "CS-Repository-Monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
        
        logger.info("Started auto-reload monitoring for object repository");
    }
    
    /**
     * Check for file changes and reload if necessary
     */
    private void checkForFileChanges() {
        try {
            Path basePath = Paths.get(repositoryBasePath);
            
            Files.walk(basePath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        LocalDateTime currentModified = Files.getLastModifiedTime(path).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                            
                        LocalDateTime lastKnown = fileLastModified.get(path.toString());
                        
                        if (lastKnown == null || currentModified.isAfter(lastKnown)) {
                            logger.info("Detected file change, reloading: {}", path);
                            loadAllRepositoryData();
                        }
                        
                    } catch (Exception e) {
                        logger.warn("Error checking file modification time: {}", path, e);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error checking for file changes", e);
        }
    }
    
    // Validation methods
    private void validatePageObject(CSPageObjectDefinition pageObject) {
        if (pageObject.getName() == null || pageObject.getName().trim().isEmpty()) {
            throw new CSRepositoryException("Page object name cannot be null or empty");
        }
        
        if (pageObject.getElements() == null || pageObject.getElements().isEmpty()) {
            throw new CSRepositoryException("Page object must have at least one element: " + pageObject.getName());
        }
        
        // Validate all elements in the page object
        for (CSElementDefinition element : pageObject.getElements()) {
            validateElement(element);
        }
    }
    
    private void validateElement(CSElementDefinition element) {
        if (element.getName() == null || element.getName().trim().isEmpty()) {
            throw new CSRepositoryException("Element name cannot be null or empty");
        }
        
        if (element.getLocators() == null || element.getLocators().isEmpty()) {
            throw new CSRepositoryException("Element must have at least one locator: " + element.getName());
        }
        
        // Validate locators
        for (CSLocatorDefinition locator : element.getLocators()) {
            if (locator.getType() == null || locator.getValue() == null) {
                throw new CSRepositoryException("Locator type and value cannot be null: " + element.getName());
            }
        }
    }
    
    private void validateTestDataSet(CSTestDataSet dataSet) {
        if (dataSet.getName() == null || dataSet.getName().trim().isEmpty()) {
            throw new CSRepositoryException("Test data set name cannot be null or empty");
        }
        
        if (dataSet.getData() == null || dataSet.getData().isEmpty()) {
            throw new CSRepositoryException("Test data set must have at least one data row: " + dataSet.getName());
        }
    }
    
    private void validateEnvironmentConfig(CSEnvironmentConfig envConfig) {
        if (envConfig.getName() == null || envConfig.getName().trim().isEmpty()) {
            throw new CSRepositoryException("Environment config name cannot be null or empty");
        }
        
        if (envConfig.getBaseUrl() == null || envConfig.getBaseUrl().trim().isEmpty()) {
            throw new CSRepositoryException("Environment config must have a base URL: " + envConfig.getName());
        }
    }
    
    // Public API methods
    
    /**
     * Check and reload if needed
     */
    private void reloadIfNeeded() {
        if (autoReloadEnabled) {
            // Check if files have been modified
            try {
                boolean needsReload = fileLastModified.entrySet().stream()
                    .anyMatch(entry -> {
                        Path path = Paths.get(entry.getKey());
                        try {
                            LocalDateTime lastModified = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(path).toInstant(),
                                java.time.ZoneOffset.UTC
                            );
                            return lastModified.isAfter(entry.getValue());
                        } catch (Exception e) {
                            return false;
                        }
                    });
                
                if (needsReload) {
                    reload();
                }
            } catch (Exception e) {
                logger.error("Error checking for file modifications", e);
            }
        }
    }
    
    /**
     * Get locator by key
     */
    public String getLocator(String locatorKey) {
        reloadIfNeeded();
        
        // First try elements
        CSElementDefinition element = elements.get(locatorKey);
        if (element != null && !element.getLocators().isEmpty()) {
            return element.getLocators().get(0).getValue();
        }
        
        // Then try page objects
        CSPageObjectDefinition pageObject = pageObjects.get(locatorKey);
        if (pageObject != null) {
            // Return first element's first locator if available
            for (CSElementDefinition el : pageObject.getElements()) {
                if (!el.getLocators().isEmpty()) {
                    return el.getLocators().get(0).getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get page object by name
     */
    public CSPageObjectDefinition getPageObject(String name) {
        CSPageObjectDefinition pageObject = pageObjects.get(name);
        if (pageObject == null && strictModeEnabled) {
            throw new CSRepositoryException("Page object not found: " + name);
        }
        return pageObject;
    }
    
    /**
     * Get element by name
     */
    public CSElementDefinition getElement(String name) {
        CSElementDefinition element = elements.get(name);
        if (element == null && strictModeEnabled) {
            throw new CSRepositoryException("Element not found: " + name);
        }
        return element;
    }
    
    /**
     * Get test data set by name
     */
    public CSTestDataSet getTestDataSet(String name) {
        CSTestDataSet dataSet = testDataSets.get(name);
        if (dataSet == null && strictModeEnabled) {
            throw new CSRepositoryException("Test data set not found: " + name);
        }
        return dataSet;
    }
    
    /**
     * Get environment config by name
     */
    public CSEnvironmentConfig getEnvironmentConfig(String name) {
        CSEnvironmentConfig envConfig = environments.get(name);
        if (envConfig == null && strictModeEnabled) {
            throw new CSRepositoryException("Environment config not found: " + name);
        }
        return envConfig;
    }
    
    /**
     * Search elements by criteria
     */
    public List<CSElementDefinition> searchElements(String namePattern, String locatorType, String tag) {
        return elements.values().stream()
            .filter(element -> {
                boolean matches = true;
                
                if (namePattern != null) {
                    matches = element.getName().matches(namePattern.replace("*", ".*"));
                }
                
                if (matches && locatorType != null) {
                    matches = element.getLocators().stream()
                        .anyMatch(locator -> locator.getType().equals(locatorType));
                }
                
                if (matches && tag != null) {
                    matches = element.getTags() != null && element.getTags().contains(tag);
                }
                
                return matches;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get all page object names
     */
    public Set<String> getAllPageObjectNames() {
        return new HashSet<>(pageObjects.keySet());
    }
    
    /**
     * Get all element names
     */
    public Set<String> getAllElementNames() {
        return new HashSet<>(elements.keySet());
    }
    
    /**
     * Get all test data set names
     */
    public Set<String> getAllTestDataSetNames() {
        return new HashSet<>(testDataSets.keySet());
    }
    
    /**
     * Get all environment names
     */
    public Set<String> getAllEnvironmentNames() {
        return new HashSet<>(environments.keySet());
    }
    
    /**
     * Reload all repository data
     */
    public void reload() {
        logger.info("Manual reload of object repository triggered");
        loadAllRepositoryData();
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        synchronized (reloadLock) {
            pageObjects.clear();
            elements.clear();
            testDataSets.clear();
            environments.clear();
            fileLastModified.clear();
            
            logger.info("Cleared object repository cache");
        }
    }
    
    /**
     * Get repository statistics
     */
    public RepositoryStats getStats() {
        return new RepositoryStats(
            pageObjects.size(),
            elements.size(),
            testDataSets.size(),
            environments.size(),
            fileLastModified.size()
        );
    }
    
    /**
     * Get Selenium By object from repository key
     * This method converts repository element definitions to Selenium By objects
     * 
     * @param repositoryKey The key to look up in repository (e.g., "login.button", "header.logo")
     * @return By object for the element
     * @throws CSRepositoryException if element not found or has no valid locators
     */
    public static org.openqa.selenium.By getBy(String repositoryKey) {
        CSObjectRepository repository = getInstance();
        
        // First try to get element directly
        CSElementDefinition element = repository.getElement(repositoryKey);
        
        // If not found directly, try to parse as page.element format
        if (element == null && repositoryKey.contains(".")) {
            String[] parts = repositoryKey.split("\\.", 2);
            String pageName = parts[0];
            String elementName = parts[1];
            
            CSPageObjectDefinition pageObject = repository.getPageObject(pageName);
            if (pageObject != null) {
                // Find element in page object
                element = pageObject.getElements().stream()
                    .filter(el -> el.getName().equals(elementName))
                    .findFirst()
                    .orElse(null);
            }
        }
        
        // If still not found, throw exception
        if (element == null) {
            throw new CSRepositoryException("Element not found in repository: " + repositoryKey);
        }
        
        // Get primary locator and convert to By
        CSLocatorDefinition primaryLocator = element.getPrimaryLocator();
        if (primaryLocator == null) {
            throw new CSRepositoryException("Element has no locators defined: " + repositoryKey);
        }
        
        return primaryLocator.toSeleniumBy();
    }
    
    /**
     * Repository statistics
     */
    public static class RepositoryStats {
        private final int pageObjectCount;
        private final int elementCount;
        private final int testDataSetCount;
        private final int environmentCount;
        private final int fileCount;
        
        public RepositoryStats(int pageObjectCount, int elementCount, int testDataSetCount,
                             int environmentCount, int fileCount) {
            this.pageObjectCount = pageObjectCount;
            this.elementCount = elementCount;
            this.testDataSetCount = testDataSetCount;
            this.environmentCount = environmentCount;
            this.fileCount = fileCount;
        }
        
        public int getPageObjectCount() { return pageObjectCount; }
        public int getElementCount() { return elementCount; }
        public int getTestDataSetCount() { return testDataSetCount; }
        public int getEnvironmentCount() { return environmentCount; }
        public int getFileCount() { return fileCount; }
        
        @Override
        public String toString() {
            return String.format("RepositoryStats{pages=%d, elements=%d, dataSets=%d, environments=%d, files=%d}",
                pageObjectCount, elementCount, testDataSetCount, environmentCount, fileCount);
        }
    }
}