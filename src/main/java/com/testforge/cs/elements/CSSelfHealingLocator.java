package com.testforge.cs.elements;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.locators.CSLocatorResolver;
import com.testforge.cs.reporting.CSReportManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-healing locator implementation
 * Automatically tries alternative locators when primary fails
 */
public class CSSelfHealingLocator {
    private static final Logger logger = LoggerFactory.getLogger(CSSelfHealingLocator.class);
    private static final CSSelfHealingLocator instance = new CSSelfHealingLocator();
    
    private final CSLocatorResolver locatorResolver = CSLocatorResolver.getInstance();
    private final CSReportManager reportManager = CSReportManager.getInstance();
    private final CSConfigManager config = CSConfigManager.getInstance();
    
    // Cache successful locators for performance
    private final Map<String, By> successfulLocators = new ConcurrentHashMap<>();
    
    private CSSelfHealingLocator() {}
    
    public static CSSelfHealingLocator getInstance() {
        return instance;
    }
    
    /**
     * Create CSElement with self-healing capabilities
     */
    public CSElement createElement(WebDriver driver, CSLocator annotation, String fieldName) {
        logger.debug("Creating CSElement for field: {}", fieldName);
        
        // Get primary locator
        By primaryLocator = locatorResolver.resolveLocator(annotation);
        logger.debug("Resolved primary locator: {}", primaryLocator);
        
        // Get alternative locators
        List<By> alternatives = locatorResolver.getAlternativeLocators(annotation);
        logger.debug("Found {} alternative locators", alternatives.size());
        
        // Convert to string array for CSElement
        String[] altLocatorStrings = alternatives.stream()
            .map(By::toString)
            .toArray(String[]::new);
        
        // Create element with self-healing
        CSElement element = new CSElement(
            driver,
            primaryLocator,
            annotation.description().isEmpty() ? fieldName : annotation.description(),
            annotation.aiEnabled(),
            altLocatorStrings
        );
        
        logger.debug("Created CSElement: {} with primary locator: {} and {} alternatives", 
            fieldName, primaryLocator, altLocatorStrings.length);
        
        return element;
    }
    
    /**
     * Find element with self-healing
     */
    public WebElement findElement(WebDriver driver, By primaryLocator, List<By> alternatives) {
        String cacheKey = primaryLocator.toString();
        
        // Try cached successful locator first
        By cachedLocator = successfulLocators.get(cacheKey);
        if (cachedLocator != null) {
            try {
                WebElement element = driver.findElement(cachedLocator);
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (Exception e) {
                // Cached locator failed, remove from cache
                successfulLocators.remove(cacheKey);
            }
        }
        
        // Try primary locator
        try {
            WebElement element = driver.findElement(primaryLocator);
            if (element.isDisplayed()) {
                // Cache successful locator
                successfulLocators.put(cacheKey, primaryLocator);
                return element;
            }
        } catch (Exception e) {
            logger.debug("Primary locator failed: {}", primaryLocator);
        }
        
        // Try alternative locators
        for (By alternative : alternatives) {
            try {
                WebElement element = driver.findElement(alternative);
                if (element.isDisplayed()) {
                    // Log healing
                    logger.info("Self-healing: Primary locator {} failed, healed with {}", 
                               primaryLocator, alternative);
                    reportManager.logInfo("Self-healing activated: " + alternative);
                    
                    // Cache successful alternative
                    successfulLocators.put(cacheKey, alternative);
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Alternative locator failed: {}", alternative);
            }
        }
        
        // All locators failed
        throw new org.openqa.selenium.NoSuchElementException(
            "Unable to locate element with primary locator: " + primaryLocator + 
            " and " + alternatives.size() + " alternatives"
        );
    }
    
    /**
     * Create List of CSElements with self-healing capabilities
     */
    public List<CSElement> createElementList(WebDriver driver, CSLocator annotation, String fieldName) {
        // Get primary locator
        By primaryLocator = locatorResolver.resolveLocator(annotation);
        
        // Get alternative locators
        List<By> alternatives = locatorResolver.getAlternativeLocators(annotation);
        
        // Find all matching web elements
        List<WebElement> webElements = findElements(driver, primaryLocator, alternatives);
        
        // Convert to CSElement list
        List<CSElement> csElements = new ArrayList<>();
        for (int i = 0; i < webElements.size(); i++) {
            String elementDescription = annotation.description().isEmpty() ? 
                fieldName + "[" + i + "]" : 
                annotation.description() + "[" + i + "]";
                
            // Create indexed locator for this specific element
            By indexedLocator = createIndexedLocator(primaryLocator, i + 1);
            
            CSElement element = new CSElement(
                driver,
                indexedLocator,
                elementDescription,
                annotation.aiEnabled(),
                new String[0] // Individual elements don't need alternative locators
            );
            
            csElements.add(element);
        }
        
        logger.debug("Created {} CSElements for field: {}", csElements.size(), fieldName);
        return csElements;
    }
    
    /**
     * Create an indexed locator for accessing a specific element in a list
     */
    private By createIndexedLocator(By originalLocator, int index) {
        String locatorString = originalLocator.toString();
        
        if (locatorString.startsWith("By.xpath: ")) {
            // Handle XPath locators
            String xpath = locatorString.replace("By.xpath: ", "");
            
            // If xpath already ends with [position], replace it
            if (xpath.matches(".*\\[\\d+\\]$")) {
                xpath = xpath.replaceAll("\\[\\d+\\]$", "[" + index + "]");
            } else {
                // Add index to the xpath
                xpath = "(" + xpath + ")[" + index + "]";
            }
            
            return By.xpath(xpath);
            
        } else if (locatorString.startsWith("By.cssSelector: ")) {
            // Handle CSS selectors
            String css = locatorString.replace("By.cssSelector: ", "");
            css = "(" + css + "):nth-of-type(" + index + ")";
            return By.cssSelector(css);
            
        } else {
            // For other locator types, wrap with xpath
            String xpath = "(" + locatorString + ")[" + index + "]";
            return By.xpath(xpath);
        }
    }

    /**
     * Find elements with self-healing
     */
    public List<WebElement> findElements(WebDriver driver, By primaryLocator, List<By> alternatives) {
        // Try primary locator
        try {
            List<WebElement> elements = driver.findElements(primaryLocator);
            if (!elements.isEmpty()) {
                return elements;
            }
        } catch (Exception e) {
            logger.debug("Primary locator failed for elements: {}", primaryLocator);
        }
        
        // Try alternative locators
        for (By alternative : alternatives) {
            try {
                List<WebElement> elements = driver.findElements(alternative);
                if (!elements.isEmpty()) {
                    logger.info("Self-healing: Found elements with alternative locator {}", alternative);
                    return elements;
                }
            } catch (Exception e) {
                logger.debug("Alternative locator failed for elements: {}", alternative);
            }
        }
        
        // Return empty list if no elements found
        return new ArrayList<>();
    }
    
    /**
     * Clear the successful locator cache
     */
    public void clearCache() {
        successfulLocators.clear();
    }
}