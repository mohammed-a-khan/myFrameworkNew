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
     */
    private void loadObjectRepository() {
        // Try multiple locations for the object repository
        String[] repositoryFiles = {
            "locators.properties",
            "object-repository.properties",
            "config/object-repository.properties",
            "src/test/resources/locators.properties"
        };
        
        boolean loaded = false;
        for (String repositoryFile : repositoryFiles) {
            // Try as classpath resource
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(repositoryFile)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    props.forEach((key, value) -> objectRepository.put(key.toString(), value.toString()));
                    logger.info("Loaded {} locators from object repository: {}", objectRepository.size(), repositoryFile);
                    loaded = true;
                    break;
                }
            } catch (Exception e) {
                // Try next file
            }
            
            // Also try as file path
            try {
                File file = new File(repositoryFile);
                if (file.exists()) {
                    Properties props = new Properties();
                    props.load(new FileInputStream(file));
                    props.forEach((key, value) -> objectRepository.put(key.toString(), value.toString()));
                    logger.info("Loaded {} locators from object repository file: {}", objectRepository.size(), repositoryFile);
                    loaded = true;
                    break;
                }
            } catch (Exception e) {
                // Try next file
            }
        }
        
        if (!loaded) {
            logger.warn("Object repository file not found in any location");
        }
    }
    
    /**
     * Resolve locator from annotation
     */
    public By resolveLocator(CSLocator annotation) {
        // First check if locatorKey is specified
        if (!annotation.locatorKey().isEmpty()) {
            return resolveFromRepository(annotation.locatorKey());
        }
        
        // Check direct locator specifications
        if (!annotation.id().isEmpty()) {
            return By.id(annotation.id());
        }
        if (!annotation.name().isEmpty()) {
            return By.name(annotation.name());
        }
        if (!annotation.css().isEmpty()) {
            return By.cssSelector(annotation.css());
        }
        if (!annotation.xpath().isEmpty()) {
            return By.xpath(annotation.xpath());
        }
        if (!annotation.className().isEmpty()) {
            return By.className(annotation.className());
        }
        if (!annotation.tagName().isEmpty()) {
            return By.tagName(annotation.tagName());
        }
        if (!annotation.linkText().isEmpty()) {
            return By.linkText(annotation.linkText());
        }
        if (!annotation.partialLinkText().isEmpty()) {
            return By.partialLinkText(annotation.partialLinkText());
        }
        
        // Check value attribute
        if (!annotation.value().isEmpty()) {
            return parseLocatorString(annotation.value());
        }
        
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
    private By parseLocatorString(String locatorString) {
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
        
        for (String altLocator : annotation.alternativeLocators()) {
            try {
                // Check if it's a repository key or direct locator
                if (objectRepository.containsKey(altLocator)) {
                    alternatives.add(resolveFromRepository(altLocator));
                } else {
                    alternatives.add(parseLocatorString(altLocator));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse alternative locator: {}", altLocator, e);
            }
        }
        
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