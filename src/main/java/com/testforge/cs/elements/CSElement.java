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
        CSReportManager.info("[INFO] Clearing and typing '" + text + "' into " + description);
        
        return performActionWithValue("clearAndType", text, () -> {
            WebElement el = getElement();
            el.clear();
            el.sendKeys(text);
            CSReportManager.pass("[PASS] Entered text '" + text + "' into " + description);
        });
    }
    
    /**
     * Type text without clearing
     */
    public CSElement type(String text) {
        logger.debug("Type '{}' into element: {}", text, description);
        CSReportManager.info("Typing '" + text + "' into " + description);
        
        return performActionWithValue("type", text, () -> {
            getElement().sendKeys(text);
            CSReportManager.pass("Typed text '" + text + "' into " + description);
        });
    }
    
    /**
     * Click element
     */
    public CSElement click() {
        logger.debug("Click element: {}", description);
        CSReportManager.info("[INFO] Clicking on " + description);
        
        return performAction("click", () -> {
            getElement().click();
            CSReportManager.pass("[PASS] Clicked on " + description);
        });
    }
    
    /**
     * Double click element
     */
    public CSElement doubleClick() {
        logger.debug("Double click element: {}", description);
        CSReportManager.info("Double clicking on " + description);
        
        return performAction("doubleClick", () -> {
            Actions actions = new Actions(driver);
            actions.doubleClick(getElement()).perform();
            CSReportManager.pass("Double clicked on " + description);
        });
    }
    
    /**
     * Right click element
     */
    public CSElement rightClick() {
        logger.debug("Right click element: {}", description);
        CSReportManager.info("Right clicking on " + description);
        
        return performAction("rightClick", () -> {
            Actions actions = new Actions(driver);
            actions.contextClick(getElement()).perform();
            CSReportManager.pass("Right clicked on " + description);
        });
    }
    
    /**
     * Clear element
     */
    public CSElement clear() {
        logger.debug("Clear element: {}", description);
        CSReportManager.info("Clearing " + description);
        
        return performAction("clear", () -> {
            getElement().clear();
            CSReportManager.pass("Cleared " + description);
        });
    }
    
    /**
     * Submit form
     */
    public CSElement submit() {
        logger.debug("Submit element: {}", description);
        CSReportManager.info("Submitting form via " + description);
        
        return performAction("submit", () -> {
            getElement().submit();
            CSReportManager.pass("Submitted " + description);
        });
    }
    
    /**
     * Get text from element
     */
    public String getText() {
        logger.debug("Get text from element: {}", description);
        CSReportManager.info("Getting text from " + description);
        
        return performFunction("getText", () -> {
            String text = getElement().getText();
            CSReportManager.pass("Got text '" + text + "' from " + description);
            return text;
        });
    }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String attributeName) {
        logger.debug("Get attribute '{}' from element: {}", attributeName, description);
        CSReportManager.info("Getting attribute '" + attributeName + "' from " + description);
        
        return performFunction("getAttribute", () -> {
            String value = getElement().getAttribute(attributeName);
            CSReportManager.pass("Got attribute '" + attributeName + "' = '" + value + "' from " + description);
            return value;
        });
    }
    
    /**
     * Get CSS value
     */
    public String getCssValue(String propertyName) {
        logger.debug("Get CSS value '{}' from element: {}", propertyName, description);
        CSReportManager.info("Getting CSS value '" + propertyName + "' from " + description);
        
        return performFunction("getCssValue", () -> {
            String value = getElement().getCssValue(propertyName);
            CSReportManager.pass("Got CSS value '" + propertyName + "' = '" + value + "' from " + description);
            return value;
        });
    }
    
    /**
     * Check if element is displayed with custom timeout
     */
    public boolean isDisplayed(int timeoutSeconds) {
        logger.debug("Check if element is displayed: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("[INFO] Checking if " + description + " is displayed");
        
        try {
            WebElement element = CSWaitUtils.waitForElementPresent(driver, locator, timeoutSeconds);
            boolean displayed = element != null && element.isDisplayed();
            if (displayed) {
                CSReportManager.pass("[PASS] " + description + " is displayed");
            } else {
                CSReportManager.fail("[FAIL] " + description + " is not displayed");
            }
            return displayed;
        } catch (Exception e) {
            logger.debug("Element not displayed: {}", e.getMessage());
            CSReportManager.fail("[FAIL] " + description + " is not displayed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if element is displayed
     */
    public boolean isDisplayed() {
        logger.debug("Check if element is displayed: {}", description);
        CSReportManager.info("Checking if " + description + " is displayed");
        
        return performFunction("isDisplayed", () -> {
            boolean displayed = getElement().isDisplayed();
            CSReportManager.info("Element " + description + " is " + (displayed ? "displayed" : "not displayed"));
            return displayed;
        });
    }
    
    /**
     * Check if element is enabled
     */
    public boolean isEnabled() {
        logger.debug("Check if element is enabled: {}", description);
        CSReportManager.info("Checking if " + description + " is enabled");
        
        return performFunction("isEnabled", () -> {
            boolean enabled = getElement().isEnabled();
            CSReportManager.info("Element " + description + " is " + (enabled ? "enabled" : "disabled"));
            return enabled;
        });
    }
    
    /**
     * Check if element is selected
     */
    public boolean isSelected() {
        logger.debug("Check if element is selected: {}", description);
        CSReportManager.info("Checking if " + description + " is selected");
        
        return performFunction("isSelected", () -> {
            boolean selected = getElement().isSelected();
            CSReportManager.info("Element " + description + " is " + (selected ? "selected" : "not selected"));
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
        CSReportManager.info("Waiting for " + description + " to be visible (timeout: " + timeoutSeconds + "s)");
        CSWaitUtils.waitForElementVisible(driver, locator, timeoutSeconds);
        CSReportManager.pass("Element " + description + " is visible");
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
        CSReportManager.info("Waiting for " + description + " to be clickable (timeout: " + timeoutSeconds + "s)");
        CSWaitUtils.waitForElementClickable(driver, locator, timeoutSeconds);
        CSReportManager.pass("Element " + description + " is clickable");
        return this;
    }
    
    /**
     * Select dropdown by visible text
     */
    public CSElement selectByVisibleText(String text) {
        logger.debug("Select by visible text '{}' in element: {}", text, description);
        CSReportManager.info("Selecting '" + text + "' from " + description);
        
        return performAction("selectByVisibleText", () -> {
            Select select = new Select(getElement());
            select.selectByVisibleText(text);
            CSReportManager.pass("Selected '" + text + "' from " + description);
        });
    }
    
    /**
     * Select dropdown by value
     */
    public CSElement selectByValue(String value) {
        logger.debug("Select by value '{}' in element: {}", value, description);
        CSReportManager.info("Selecting value '" + value + "' from " + description);
        
        return performActionWithValue("selectByValue", value, () -> {
            Select select = new Select(getElement());
            select.selectByValue(value);
            CSReportManager.pass("Selected value '" + value + "' from " + description);
        });
    }
    
    /**
     * Select dropdown by index
     */
    public CSElement selectByIndex(int index) {
        logger.debug("Select by index '{}' in element: {}", index, description);
        CSReportManager.info("Selecting index " + index + " from " + description);
        
        return performActionWithValue("selectByIndex", String.valueOf(index), () -> {
            Select select = new Select(getElement());
            select.selectByIndex(index);
            CSReportManager.pass("Selected index " + index + " from " + description);
        });
    }
    
    /**
     * Get all dropdown options
     */
    public List<String> getDropdownOptions() {
        logger.debug("Get dropdown options from element: {}", description);
        CSReportManager.info("Getting dropdown options from " + description);
        
        return performFunction("getDropdownOptions", () -> {
            Select select = new Select(getElement());
            List<String> options = select.getOptions().stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
            CSReportManager.info("Got " + options.size() + " options from " + description);
            return options;
        });
    }
    
    /**
     * Hover over element
     */
    public CSElement hover() {
        logger.debug("Hover over element: {}", description);
        CSReportManager.info("Hovering over " + description);
        
        return performAction("hover", () -> {
            Actions actions = new Actions(driver);
            actions.moveToElement(getElement()).perform();
            CSReportManager.pass("Hovered over " + description);
        });
    }
    
    /**
     * Drag and drop to another element
     */
    public CSElement dragAndDropTo(CSElement target) {
        logger.debug("Drag element {} and drop to {}", description, target.description);
        CSReportManager.info("Dragging " + description + " to " + target.description);
        
        return performAction("dragAndDrop", () -> {
            Actions actions = new Actions(driver);
            actions.dragAndDrop(getElement(), target.getElement()).perform();
            CSReportManager.pass("Dragged " + description + " and dropped to " + target.description);
        });
    }
    
    /**
     * Scroll element into view
     */
    public CSElement scrollIntoView() {
        logger.debug("Scroll element into view: {}", description);
        CSReportManager.info("Scrolling " + description + " into view");
        
        return performAction("scrollIntoView", () -> {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", getElement());
            CSReportManager.pass("Scrolled " + description + " into view");
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
        CSReportManager.info("Uploading file '" + filePath + "' to " + description);
        
        return performActionWithValue("uploadFile", filePath, () -> {
            getElement().sendKeys(filePath);
            CSReportManager.pass("Uploaded file '" + filePath + "' to " + description);
        });
    }
    
    /**
     * Press Enter key
     */
    public CSElement pressEnter() {
        logger.debug("Press Enter key on element: {}", description);
        CSReportManager.info("Pressing Enter key on " + description);
        
        return performAction("pressEnter", () -> {
            getElement().sendKeys(Keys.ENTER);
            CSReportManager.pass("Pressed Enter key on " + description);
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
                CSReportManager.pass("Checked " + description);
            } else {
                CSReportManager.info(description + " was already checked");
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
        return performActionWithValue(actionName, null, action);
    }
    
    /**
     * Get human-readable action description
     */
    private String getActionDescription(String actionName) {
        switch (actionName) {
            case "click": return "Click element";
            case "clearAndType": return "Clear and type text";
            case "type": return "Type text";
            case "clear": return "Clear field";
            case "submit": return "Submit form";
            case "selectByText": return "Select by text";
            case "selectByValue": return "Select by value";
            case "selectByIndex": return "Select by index";
            case "hover": return "Hover over element";
            case "dragAndDrop": return "Drag and drop";
            case "scrollIntoView": return "Scroll into view";
            case "highlight": return "Highlight element";
            case "uploadFile": return "Upload file";
            case "pressEnter": return "Press Enter key";
            case "check": return "Check checkbox";
            case "uncheck": return "Uncheck checkbox";
            default: return actionName;
        }
    }
    
    /**
     * Perform action with value and retry handling
     */
    private CSElement performActionWithValue(String actionName, String value, Runnable action) {
        Exception lastException = null;
        
        // Add action to current step report with value
        String actionDescription = getActionDescription(actionName);
        CSReportManager.addAction(actionName, actionDescription, description, value);
        
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
        
        // Mark action as failed
        CSReportManager.failAction("Failed to perform action '" + actionName + "': " + lastException.getMessage());
        
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