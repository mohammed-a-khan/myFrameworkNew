package com.testforge.cs.elements;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSFrameworkException;
import org.openqa.selenium.By;

/**
 * Helper class for object repository lookups
 */
public class CSObjectRepositoryHelper {
    
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    /**
     * Get locator from repository by key
     */
    public static By getLocator(String key) {
        String locatorValue = config.getProperty(key);
        if (locatorValue == null) {
            throw new CSFrameworkException("Locator key not found in repository: " + key);
        }
        
        return parseLocatorString(locatorValue);
    }
    
    /**
     * Parse locator string to By object
     */
    private static By parseLocatorString(String locatorStr) {
        if (locatorStr.startsWith("id:")) {
            return By.id(locatorStr.substring(3));
        } else if (locatorStr.startsWith("name:")) {
            return By.name(locatorStr.substring(5));
        } else if (locatorStr.startsWith("css:")) {
            return By.cssSelector(locatorStr.substring(4));
        } else if (locatorStr.startsWith("xpath:")) {
            return By.xpath(locatorStr.substring(6));
        } else if (locatorStr.startsWith("class:")) {
            return By.className(locatorStr.substring(6));
        } else if (locatorStr.startsWith("tag:")) {
            return By.tagName(locatorStr.substring(4));
        } else if (locatorStr.startsWith("link:")) {
            return By.linkText(locatorStr.substring(5));
        } else if (locatorStr.startsWith("partialLink:")) {
            return By.partialLinkText(locatorStr.substring(12));
        } else if (locatorStr.startsWith("//") || locatorStr.contains("[")) {
            // Looks like xpath
            return By.xpath(locatorStr);
        } else if (locatorStr.startsWith("#") || locatorStr.contains(".") || locatorStr.contains(" ")) {
            // Looks like CSS selector
            return By.cssSelector(locatorStr);
        } else {
            // Default to ID
            return By.id(locatorStr);
        }
    }
}