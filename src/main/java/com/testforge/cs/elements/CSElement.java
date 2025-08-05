package com.testforge.cs.elements;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSElementException;
import com.testforge.cs.exceptions.CSElementNotFoundException;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.waits.CSWaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production-ready wrapper for Selenium WebElement
 * Provides enhanced functionality with automatic waits, retries, and error handling
 * Users never interact with WebElement directly
 */
public class CSElement {
    private static final Logger logger = LoggerFactory.getLogger(CSElement.class);
    
    private final WebDriver driver;
    private final By locator;
    private WebElement element;
    private final String description;
    private final boolean aiEnabled;
    private final String[] alternativeLocators;
    private final CSConfigManager config;
    private final CSReportManager reportManager;
    
    // Configuration
    private final int maxRetries;
    private final long retryDelay;
    private final boolean highlightElements;
    private final boolean screenshotOnAction;
    
    public CSElement(WebDriver driver, By locator, String description) {
        this(driver, locator, description, false, new String[0]);
    }
    
    public CSElement(WebDriver driver, By locator, String description, 
                    boolean aiEnabled, String[] alternativeLocators) {
        this.driver = driver;
        this.locator = locator;
        this.description = description != null ? description : locator.toString();
        this.aiEnabled = aiEnabled;
        this.alternativeLocators = alternativeLocators;
        this.config = CSConfigManager.getInstance();
        this.reportManager = CSReportManager.getInstance();
        
        // Load configuration
        this.maxRetries = config.getInt("cs.element.max.retries", 3);
        this.retryDelay = config.getLong("cs.element.retry.delay", 500);
        this.highlightElements = config.getBoolean("cs.element.highlight", false);
        this.screenshotOnAction = config.getBoolean("cs.screenshot.on.action", false);
    }
    
    /**
     * Clear field and type text - most common pattern
     */
    public CSElement clearAndType(String text) {
        logger.debug("Clear and type '{}' into element: {}", text, description);
        
        return performAction("clearAndType", () -> {
            WebElement el = getElement();
            el.clear();
            el.sendKeys(text);
            reportManager.logStep("Entered text '" + text + "' into " + description, true);
        });
    }
    
    /**
     * Type text without clearing
     */
    public CSElement type(String text) {
        logger.debug("Type '{}' into element: {}", text, description);
        
        return performAction("type", () -> {
            getElement().sendKeys(text);
            reportManager.logStep("Typed text '" + text + "' into " + description, true);
        });
    }
    
    /**
     * Click element
     */
    public CSElement click() {
        logger.debug("Click element: {}", description);
        
        return performAction("click", () -> {
            getElement().click();
            reportManager.logStep("Clicked on " + description, true);
        });
    }
    
    /**
     * Double click element
     */
    public CSElement doubleClick() {
        logger.debug("Double click element: {}", description);
        
        return performAction("doubleClick", () -> {
            Actions actions = new Actions(driver);
            actions.doubleClick(getElement()).perform();
            reportManager.logStep("Double clicked on " + description, true);
        });
    }
    
    /**
     * Right click element
     */
    public CSElement rightClick() {
        logger.debug("Right click element: {}", description);
        
        return performAction("rightClick", () -> {
            Actions actions = new Actions(driver);
            actions.contextClick(getElement()).perform();
            reportManager.logStep("Right clicked on " + description, true);
        });
    }
    
    /**
     * Clear element
     */
    public CSElement clear() {
        logger.debug("Clear element: {}", description);
        
        return performAction("clear", () -> {
            getElement().clear();
            reportManager.logStep("Cleared " + description, true);
        });
    }
    
    /**
     * Submit form
     */
    public CSElement submit() {
        logger.debug("Submit element: {}", description);
        
        return performAction("submit", () -> {
            getElement().submit();
            reportManager.logStep("Submitted " + description, true);
        });
    }
    
    /**
     * Get text from element
     */
    public String getText() {
        logger.debug("Get text from element: {}", description);
        
        return performFunction("getText", () -> {
            String text = getElement().getText();
            reportManager.logStep("Got text '" + text + "' from " + description, true);
            return text;
        });
    }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String attributeName) {
        logger.debug("Get attribute '{}' from element: {}", attributeName, description);
        
        return performFunction("getAttribute", () -> {
            String value = getElement().getAttribute(attributeName);
            reportManager.logStep("Got attribute '" + attributeName + "' = '" + value + "' from " + description, true);
            return value;
        });
    }
    
    /**
     * Get CSS value
     */
    public String getCssValue(String propertyName) {
        logger.debug("Get CSS value '{}' from element: {}", propertyName, description);
        
        return performFunction("getCssValue", () -> {
            String value = getElement().getCssValue(propertyName);
            reportManager.logStep("Got CSS value '" + propertyName + "' = '" + value + "' from " + description, true);
            return value;
        });
    }
    
    /**
     * Check if element is displayed
     */
    public boolean isDisplayed() {
        logger.debug("Check if element is displayed: {}", description);
        
        return performFunction("isDisplayed", () -> {
            boolean displayed = getElement().isDisplayed();
            reportManager.logStep("Element " + description + " is " + (displayed ? "displayed" : "not displayed"), true);
            return displayed;
        });
    }
    
    /**
     * Check if element is enabled
     */
    public boolean isEnabled() {
        logger.debug("Check if element is enabled: {}", description);
        
        return performFunction("isEnabled", () -> {
            boolean enabled = getElement().isEnabled();
            reportManager.logStep("Element " + description + " is " + (enabled ? "enabled" : "disabled"), true);
            return enabled;
        });
    }
    
    /**
     * Check if element is selected
     */
    public boolean isSelected() {
        logger.debug("Check if element is selected: {}", description);
        
        return performFunction("isSelected", () -> {
            boolean selected = getElement().isSelected();
            reportManager.logStep("Element " + description + " is " + (selected ? "selected" : "not selected"), true);
            return selected;
        });
    }
    
    /**
     * Check if element exists (present in DOM)
     */
    public boolean exists() {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    
    /**
     * Check if element is present (alias for exists)
     */
    public boolean isPresent() {
        return exists();
    }
    
    /**
     * Wait for element to be visible
     */
    public CSElement waitForVisible() {
        return waitForVisible(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be visible with custom timeout
     */
    public CSElement waitForVisible(int timeoutSeconds) {
        logger.debug("Wait for element to be visible: {}", description);
        CSWaitUtils.waitForElementVisible(driver, locator, timeoutSeconds);
        reportManager.logStep("Element " + description + " is visible", true);
        return this;
    }
    
    /**
     * Wait for element to be clickable
     */
    public CSElement waitForClickable() {
        return waitForClickable(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be clickable with custom timeout
     */
    public CSElement waitForClickable(int timeoutSeconds) {
        logger.debug("Wait for element to be clickable: {}", description);
        CSWaitUtils.waitForElementClickable(driver, locator, timeoutSeconds);
        reportManager.logStep("Element " + description + " is clickable", true);
        return this;
    }
    
    /**
     * Select dropdown by visible text
     */
    public CSElement selectByVisibleText(String text) {
        logger.debug("Select by visible text '{}' in element: {}", text, description);
        
        return performAction("selectByVisibleText", () -> {
            Select select = new Select(getElement());
            select.selectByVisibleText(text);
            reportManager.logStep("Selected '" + text + "' from " + description, true);
        });
    }
    
    /**
     * Select dropdown by value
     */
    public CSElement selectByValue(String value) {
        logger.debug("Select by value '{}' in element: {}", value, description);
        
        return performAction("selectByValue", () -> {
            Select select = new Select(getElement());
            select.selectByValue(value);
            reportManager.logStep("Selected value '" + value + "' from " + description, true);
        });
    }
    
    /**
     * Select dropdown by index
     */
    public CSElement selectByIndex(int index) {
        logger.debug("Select by index '{}' in element: {}", index, description);
        
        return performAction("selectByIndex", () -> {
            Select select = new Select(getElement());
            select.selectByIndex(index);
            reportManager.logStep("Selected index " + index + " from " + description, true);
        });
    }
    
    /**
     * Get all dropdown options
     */
    public List<String> getDropdownOptions() {
        logger.debug("Get dropdown options from element: {}", description);
        
        return performFunction("getDropdownOptions", () -> {
            Select select = new Select(getElement());
            List<String> options = select.getOptions().stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
            reportManager.logStep("Got " + options.size() + " options from " + description, true);
            return options;
        });
    }
    
    /**
     * Hover over element
     */
    public CSElement hover() {
        logger.debug("Hover over element: {}", description);
        
        return performAction("hover", () -> {
            Actions actions = new Actions(driver);
            actions.moveToElement(getElement()).perform();
            reportManager.logStep("Hovered over " + description, true);
        });
    }
    
    /**
     * Drag and drop to another element
     */
    public CSElement dragAndDropTo(CSElement target) {
        logger.debug("Drag element {} and drop to {}", description, target.description);
        
        return performAction("dragAndDrop", () -> {
            Actions actions = new Actions(driver);
            actions.dragAndDrop(getElement(), target.getElement()).perform();
            reportManager.logStep("Dragged " + description + " and dropped to " + target.description, true);
        });
    }
    
    /**
     * Scroll element into view
     */
    public CSElement scrollIntoView() {
        logger.debug("Scroll element into view: {}", description);
        
        return performAction("scrollIntoView", () -> {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", getElement());
            reportManager.logStep("Scrolled " + description + " into view", true);
        });
    }
    
    /**
     * Highlight element (for debugging)
     */
    public CSElement highlight() {
        return highlight("yellow", 2);
    }
    
    /**
     * Highlight element with custom color and duration
     */
    public CSElement highlight(String color, int durationSeconds) {
        logger.debug("Highlight element: {}", description);
        
        return performAction("highlight", () -> {
            WebElement el = getElement();
            String originalStyle = el.getAttribute("style");
            
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                el, "border: 3px solid " + color + "; " + originalStyle
            );
            
            if (durationSeconds > 0) {
                try {
                    Thread.sleep(durationSeconds * 1000);
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].setAttribute('style', arguments[1]);",
                        el, originalStyle
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    /**
     * Upload file
     */
    public CSElement uploadFile(String filePath) {
        logger.debug("Upload file '{}' to element: {}", filePath, description);
        
        return performAction("uploadFile", () -> {
            getElement().sendKeys(filePath);
            reportManager.logStep("Uploaded file '" + filePath + "' to " + description, true);
        });
    }
    
    /**
     * Press Enter key
     */
    public CSElement pressEnter() {
        logger.debug("Press Enter key on element: {}", description);
        
        return performAction("pressEnter", () -> {
            getElement().sendKeys(Keys.ENTER);
            reportManager.logStep("Pressed Enter key on " + description, true);
        });
    }
    
    /**
     * Check checkbox or radio button
     */
    public CSElement check() {
        logger.debug("Check element: {}", description);
        
        return performAction("check", () -> {
            WebElement el = getElement();
            if (!el.isSelected()) {
                el.click();
                reportManager.logStep("Checked " + description, true);
            } else {
                reportManager.logStep(description + " was already checked", true);
            }
        });
    }
    
    /**
     * Get element for advanced operations
     */
    public WebElement getElement() {
        if (element == null || isStale()) {
            element = findElement();
        }
        
        if (highlightElements) {
            highlightBriefly();
        }
        
        return element;
    }
    
    /**
     * Find element with retry logic
     */
    private WebElement findElement() {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                // Try primary locator
                return driver.findElement(locator);
            } catch (NoSuchElementException e) {
                lastException = e;
                
                // Try alternative locators
                if (alternativeLocators != null && alternativeLocators.length > 0) {
                    for (String altLocator : alternativeLocators) {
                        try {
                            By by = parseLocator(altLocator);
                            return driver.findElement(by);
                        } catch (NoSuchElementException altE) {
                            // Continue to next alternative
                        }
                    }
                }
                
                attempts++;
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new CSElementNotFoundException(
            "Element not found: " + description + " using locator: " + locator, 
            10
        );
    }
    
    /**
     * Parse locator string to By object
     */
    private By parseLocator(String locatorString) {
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
                    return By.className(value);
                case "tag":
                    return By.tagName(value);
                case "link":
                    return By.linkText(value);
                case "partial":
                    return By.partialLinkText(value);
                default:
                    // Assume it's a repository key
                    return CSObjectRepositoryHelper.getLocator(locatorString);
            }
        }
        
        // Assume it's a repository key
        return CSObjectRepositoryHelper.getLocator(locatorString);
    }
    
    /**
     * Check if element is stale
     */
    private boolean isStale() {
        if (element == null) {
            return true;
        }
        
        try {
            element.isEnabled();
            return false;
        } catch (StaleElementReferenceException e) {
            return true;
        }
    }
    
    /**
     * Perform action with retry and error handling
     */
    private CSElement performAction(String actionName, Runnable action) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                action.run();
                
                if (screenshotOnAction) {
                    captureScreenshot(actionName);
                }
                
                return this;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Action '{}' failed on attempt {}: {}", actionName, attempt + 1, e.getMessage());
                
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                        element = null; // Force re-find
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new CSElementException(
            "Failed to perform action '" + actionName + "' on element: " + description, 
            lastException
        );
    }
    
    /**
     * Perform function with retry and error handling
     */
    private <T> T performFunction(String functionName, java.util.function.Supplier<T> function) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return function.get();
            } catch (Exception e) {
                lastException = e;
                logger.warn("Function '{}' failed on attempt {}: {}", functionName, attempt + 1, e.getMessage());
                
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                        element = null; // Force re-find
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new CSElementException(
            "Failed to execute function '" + functionName + "' on element: " + description, 
            lastException
        );
    }
    
    /**
     * Highlight element briefly
     */
    private void highlightBriefly() {
        try {
            String originalStyle = element.getAttribute("style");
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element, "border: 2px solid red; " + originalStyle
            );
            Thread.sleep(100);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element, originalStyle
            );
        } catch (Exception e) {
            // Ignore highlight errors
        }
    }
    
    /**
     * Capture screenshot
     */
    private void captureScreenshot(String actionName) {
        // Implementation would use screenshot utility
    }
    
    @Override
    public String toString() {
        return String.format("CSElement[%s]", description);
    }
}