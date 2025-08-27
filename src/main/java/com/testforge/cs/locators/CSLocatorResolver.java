package com.testforge.cs.locators;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSLocatorException;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves locators from annotations and object repository
 */
public class CSLocatorResolver {
    private static final Logger logger = LoggerFactory.getLogger(CSLocatorResolver.class);
    private static volatile CSLocatorResolver instance;
    
    private final Map<String, String> objectRepository = new ConcurrentHashMap<>();
    private final CSConfigManager config = CSConfigManager.getInstance();
    
    private CSLocatorResolver() {
        loadObjectRepository();
    }
    
    public static CSLocatorResolver getInstance() {
        if (instance == null) {
            synchronized (CSLocatorResolver.class) {
                if (instance == null) {
                    instance = new CSLocatorResolver();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load object repository from properties file
     * Uses configuration value cs.object.repository.paths instead of hardcoded paths
     */
    private void loadObjectRepository() {
        // Get configured paths from CSConfigManager
        CSConfigManager configManager = CSConfigManager.getInstance();
        String repositoryPaths = configManager.getString("cs.object.repository.paths", "object-repositories");
        
        // Split comma-separated paths
        String[] paths = repositoryPaths.split(",");
        boolean loaded = false;
        
        for (String path : paths) {
            path = path.trim();
            if (path.isEmpty()) continue;
            
            File pathFile = new File(path);
            
            // Check if it's a directory
            if (pathFile.isDirectory()) {
                // Load all .properties files from directory
                File[] propFiles = pathFile.listFiles((dir, name) -> name.endsWith(".properties"));
                if (propFiles != null) {
                    for (File file : propFiles) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            Properties props = new Properties();
                            props.load(fis);
                            props.forEach((key, value) -> objectRepository.put(key.toString(), value.toString()));
                            logger.info("Loaded {} locators from: {}", props.size(), file.getPath());
                            loaded = true;
                        } catch (Exception e) {
                            logger.warn("Failed to load repository file: {}", file.getPath(), e);
                        }
                    }
                }
            } else if (pathFile.isFile() && path.endsWith(".properties")) {
                // Load single file
                try (FileInputStream fis = new FileInputStream(pathFile)) {
                    Properties props = new Properties();
                    props.load(fis);
                    props.forEach((key, value) -> objectRepository.put(key.toString(), value.toString()));
                    logger.info("Loaded {} locators from: {}", props.size(), pathFile.getPath());
                    loaded = true;
                } catch (Exception e) {
                    logger.warn("Failed to load repository file: {}", pathFile.getPath(), e);
                }
            } else {
                // Try as classpath resource
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                    if (is != null) {
                        Properties props = new Properties();
                        props.load(is);
                        props.forEach((key, value) -> objectRepository.put(key.toString(), value.toString()));
                        logger.info("Loaded {} locators from classpath: {}", props.size(), path);
                        loaded = true;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load repository from classpath: {}", path, e);
                }
            }
        }
        
        if (loaded) {
            logger.info("Total locators loaded in CSLocatorResolver: {}", objectRepository.size());
        } else {
            logger.warn("No object repository files loaded from paths: {}", repositoryPaths);
        }
    }
    
    /**
     * Resolve locator from annotation
     */
    public By resolveLocator(CSLocator annotation) {
        logger.debug("Resolving locator from annotation:");
        logger.debug("  locatorKey: '{}'", annotation.locatorKey());
        logger.debug("  xpath: '{}'", annotation.xpath());
        logger.debug("  id: '{}'", annotation.id());
        logger.debug("  css: '{}'", annotation.css());
        logger.debug("  value: '{}'", annotation.value());
        
        // First check if locatorKey is specified
        if (!annotation.locatorKey().isEmpty()) {
            logger.debug("Using locatorKey: {}", annotation.locatorKey());
            return resolveFromRepository(annotation.locatorKey());
        }
        
        // Check direct locator specifications
        if (!annotation.id().isEmpty()) {
            logger.debug("Using id: {}", annotation.id());
            return By.id(annotation.id());
        }
        if (!annotation.name().isEmpty()) {
            logger.debug("Using name: {}", annotation.name());
            return By.name(annotation.name());
        }
        if (!annotation.css().isEmpty()) {
            logger.debug("Using css: {}", annotation.css());
            return By.cssSelector(annotation.css());
        }
        if (!annotation.xpath().isEmpty()) {
            logger.debug("Using xpath: {}", annotation.xpath());
            return By.xpath(annotation.xpath());
        }
        if (!annotation.className().isEmpty()) {
            logger.debug("Using className: {}", annotation.className());
            return By.className(annotation.className());
        }
        if (!annotation.tagName().isEmpty()) {
            logger.debug("Using tagName: {}", annotation.tagName());
            return By.tagName(annotation.tagName());
        }
        if (!annotation.linkText().isEmpty()) {
            logger.debug("Using linkText: {}", annotation.linkText());
            return By.linkText(annotation.linkText());
        }
        if (!annotation.partialLinkText().isEmpty()) {
            logger.debug("Using partialLinkText: {}", annotation.partialLinkText());
            return By.partialLinkText(annotation.partialLinkText());
        }
        
        // Check value attribute
        if (!annotation.value().isEmpty()) {
            logger.debug("Using value: {}", annotation.value());
            return parseLocatorString(annotation.value());
        }
        
        logger.error("No valid locator specified in annotation");
        throw new CSLocatorException("No valid locator specified in annotation");
    }
    
    /**
     * Resolve locator from repository key
     */
    public By resolveFromRepository(String key) {
        String locatorValue = objectRepository.get(key);
        if (locatorValue == null) {
            throw new CSLocatorException("Locator key not found in repository: " + key);
        }
        return parseLocatorString(locatorValue);
    }
    
    /**
     * Parse locator string in format "type:value"
     */
    public By parseLocatorString(String locatorString) {
        if (locatorString.contains(":")) {
            String[] parts = locatorString.split(":", 2);
            String type = parts[0].toLowerCase();
            String value = parts[1];
            
            switch (type) {
                case "id":
                    return By.id(value);
                case "name":
                    return By.name(value);
                case "css":
                    return By.cssSelector(value);
                case "xpath":
                    return By.xpath(value);
                case "class":
                case "classname":
                    return By.className(value);
                case "tag":
                case "tagname":
                    return By.tagName(value);
                case "link":
                case "linktext":
                    return By.linkText(value);
                case "partial":
                case "partiallinktext":
                    return By.partialLinkText(value);
                default:
                    throw new CSLocatorException("Unknown locator type: " + type);
            }
        }
        
        // If no type specified, try to guess
        if (locatorString.startsWith("//")) {
            return By.xpath(locatorString);
        } else if (locatorString.contains("=") || locatorString.contains("[")) {
            return By.cssSelector(locatorString);
        } else {
            return By.id(locatorString);
        }
    }
    
    /**
     * Get alternative locators
     */
    public List<By> getAlternativeLocators(CSLocator annotation) {
        List<By> alternatives = new ArrayList<>();
        
        logger.debug("Processing {} alternative locators", annotation.alternativeLocators().length);
        
        for (String altLocator : annotation.alternativeLocators()) {
            logger.debug("Processing alternative locator: '{}'", altLocator);
            try {
                // Check if it's a repository key or direct locator
                if (objectRepository.containsKey(altLocator)) {
                    logger.debug("Found '{}' in object repository", altLocator);
                    By resolved = resolveFromRepository(altLocator);
                    alternatives.add(resolved);
                    logger.debug("Resolved to: {}", resolved);
                } else {
                    logger.debug("'{}' not in repository, treating as direct locator", altLocator);
                    By parsed = parseLocatorString(altLocator);
                    alternatives.add(parsed);
                    logger.debug("Parsed to: {}", parsed);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse alternative locator '{}': {}", altLocator, e.getMessage());
            }
        }
        
        logger.debug("Total alternative locators resolved: {}", alternatives.size());
        return alternatives;
    }
    
    /**
     * Reload object repository
     */
    public void reloadRepository() {
        objectRepository.clear();
        loadObjectRepository();
    }
}