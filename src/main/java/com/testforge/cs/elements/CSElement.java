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
     * Type text slowly character by character - for React/Angular/Vue inputs
     * This method helps with inputs that have JavaScript event listeners
     */
    public CSElement typeSlowly(String text) {
        return typeSlowly(text, 100); // Default 100ms delay between characters
    }
    
    /**
     * Type text slowly with custom delay between characters
     */
    public CSElement typeSlowly(String text, int delayMs) {
        logger.debug("Type slowly '{}' into element: {} (delay: {}ms)", text, description, delayMs);
        CSReportManager.info("Typing slowly '" + text + "' into " + description);
        
        return performActionWithValue("typeSlowly", text, () -> {
            WebElement el = getElement();
            for (char c : text.toCharArray()) {
                el.sendKeys(String.valueOf(c));
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            CSReportManager.pass("Typed text slowly '" + text + "' into " + description);
        });
    }
    
    /**
     * Clear and type text slowly - for problematic React/Angular/Vue inputs
     */
    public CSElement clearAndTypeSlowly(String text) {
        return clearAndTypeSlowly(text, 100);
    }
    
    /**
     * Clear and type text slowly with custom delay
     */
    public CSElement clearAndTypeSlowly(String text, int delayMs) {
        logger.debug("Clear and type slowly '{}' into element: {} (delay: {}ms)", text, description, delayMs);
        CSReportManager.info("Clearing and typing slowly '" + text + "' into " + description);
        
        return performActionWithValue("clearAndTypeSlowly", text, () -> {
            WebElement el = getElement();
            el.clear();
            try {
                Thread.sleep(200); // Wait after clear
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            for (char c : text.toCharArray()) {
                el.sendKeys(String.valueOf(c));
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            CSReportManager.pass("Cleared and typed text slowly '" + text + "' into " + description);
        });
    }
    
    /**
     * Type text using JavaScript - bypasses all event listeners
     */
    public CSElement typeUsingJS(String text) {
        logger.debug("Type using JS '{}' into element: {}", text, description);
        CSReportManager.info("Typing using JavaScript '" + text + "' into " + description);
        
        return performActionWithValue("typeUsingJS", text, () -> {
            WebElement el = getElement();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Focus the element first
            js.executeScript("arguments[0].focus();", el);
            
            // Set the value
            js.executeScript("arguments[0].value = arguments[1];", el, text);
            
            // Trigger comprehensive set of events - compatible with both legacy and modern browsers
            String eventScript = 
                "var element = arguments[0];" +
                "var textValue = arguments[1];" +
                "" +
                "// Helper function to create and dispatch events (works in IE and modern browsers)" +
                "function triggerEvent(element, eventName) {" +
                "    var event;" +
                "    if (typeof Event === 'function') {" +
                "        // Modern browsers" +
                "        event = new Event(eventName, { bubbles: true, cancelable: true });" +
                "    } else if (document.createEvent) {" +
                "        // Legacy browsers (IE)" +
                "        event = document.createEvent('HTMLEvents');" +
                "        event.initEvent(eventName, true, true);" +
                "    } else if (element.fireEvent) {" +
                "        // Very old IE" +
                "        element.fireEvent('on' + eventName);" +
                "        return;" +
                "    }" +
                "    element.dispatchEvent(event);" +
                "}" +
                "" +
                "// Trigger all relevant events" +
                "triggerEvent(element, 'focus');" +
                "triggerEvent(element, 'keydown');" +
                "triggerEvent(element, 'keypress');" +
                "triggerEvent(element, 'input');" +
                "triggerEvent(element, 'keyup');" +
                "triggerEvent(element, 'change');" +
                "triggerEvent(element, 'blur');" +
                "" +
                "// Handle React's synthetic events if React is present" +
                "if (typeof React !== 'undefined' && element._valueTracker) {" +
                "    try {" +
                "        var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
                "            window.HTMLInputElement.prototype, 'value').set;" +
                "        if (nativeInputValueSetter) {" +
                "            nativeInputValueSetter.call(element, textValue);" +
                "        }" +
                "        triggerEvent(element, 'input');" +
                "    } catch(e) {" +
                "        // Fallback if React handling fails" +
                "    }" +
                "}" +
                "" +
                "// Trigger jQuery events if jQuery is present" +
                "if (typeof jQuery !== 'undefined' && jQuery(element).length) {" +
                "    jQuery(element).trigger('change').trigger('input');" +
                "}";
            
            js.executeScript(eventScript, el, text);
            
            CSReportManager.pass("Typed text using JS '" + text + "' into " + description);
        });
    }
    
    /**
     * Clear and type using JavaScript
     */
    public CSElement clearAndTypeUsingJS(String text) {
        logger.debug("Clear and type using JS '{}' into element: {}", text, description);
        CSReportManager.info("Clearing and typing using JavaScript '" + text + "' into " + description);
        
        return performActionWithValue("clearAndTypeUsingJS", text, () -> {
            WebElement el = getElement();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Enhanced JavaScript that simulates actual keyboard events more realistically
            String enhancedEventScript = 
                "var element = arguments[0];" +
                "var textValue = arguments[1];" +
                "" +
                "// Focus the element" +
                "element.focus();" +
                "" +
                "// Helper to create keyboard events with proper key codes" +
                "function createKeyboardEvent(eventType, keyChar) {" +
                "    var keyCode = keyChar ? keyChar.charCodeAt(0) : 0;" +
                "    var event;" +
                "    " +
                "    if (typeof KeyboardEvent === 'function') {" +
                "        // Modern browsers" +
                "        event = new KeyboardEvent(eventType, {" +
                "            key: keyChar || ''," +
                "            code: 'Key' + (keyChar ? keyChar.toUpperCase() : '')," +
                "            keyCode: keyCode," +
                "            which: keyCode," +
                "            charCode: eventType === 'keypress' ? keyCode : 0," +
                "            bubbles: true," +
                "            cancelable: true," +
                "            composed: true" +
                "        });" +
                "    } else if (document.createEvent) {" +
                "        // Legacy browsers" +
                "        event = document.createEvent('KeyboardEvent');" +
                "        if (event.initKeyboardEvent) {" +
                "            // Chrome/Safari/Opera" +
                "            event.initKeyboardEvent(eventType, true, true, window, keyChar, 0, '', false, '');" +
                "        } else if (event.initKeyEvent) {" +
                "            // Firefox" +
                "            event.initKeyEvent(eventType, true, true, window, false, false, false, false, keyCode, keyCode);" +
                "        }" +
                "        // Set keyCode/which manually as some browsers don't set it properly" +
                "        Object.defineProperty(event, 'keyCode', { value: keyCode, writable: true });" +
                "        Object.defineProperty(event, 'which', { value: keyCode, writable: true });" +
                "        Object.defineProperty(event, 'charCode', { value: eventType === 'keypress' ? keyCode : 0, writable: true });" +
                "    }" +
                "    return event;" +
                "}" +
                "" +
                "// Helper to trigger input event" +
                "function triggerInputEvent(element) {" +
                "    var event;" +
                "    if (typeof InputEvent === 'function') {" +
                "        event = new InputEvent('input', { bubbles: true, cancelable: true, composed: true });" +
                "    } else if (typeof Event === 'function') {" +
                "        event = new Event('input', { bubbles: true, cancelable: true });" +
                "    } else if (document.createEvent) {" +
                "        event = document.createEvent('HTMLEvents');" +
                "        event.initEvent('input', true, true);" +
                "    }" +
                "    if (event) element.dispatchEvent(event);" +
                "}" +
                "" +
                "// Clear the field first" +
                "element.value = '';" +
                "triggerInputEvent(element);" +
                "" +
                "// Type each character with proper keyboard events" +
                "for (var i = 0; i < textValue.length; i++) {" +
                "    var char = textValue.charAt(i);" +
                "    " +
                "    // Trigger keydown" +
                "    element.dispatchEvent(createKeyboardEvent('keydown', char));" +
                "    " +
                "    // Trigger keypress" +
                "    element.dispatchEvent(createKeyboardEvent('keypress', char));" +
                "    " +
                "    // Update the value" +
                "    element.value += char;" +
                "    " +
                "    // Trigger input event after each character" +
                "    triggerInputEvent(element);" +
                "    " +
                "    // Trigger keyup" +
                "    element.dispatchEvent(createKeyboardEvent('keyup', char));" +
                "}" +
                "" +
                "// Final change event" +
                "var changeEvent;" +
                "if (typeof Event === 'function') {" +
                "    changeEvent = new Event('change', { bubbles: true, cancelable: true });" +
                "} else if (document.createEvent) {" +
                "    changeEvent = document.createEvent('HTMLEvents');" +
                "    changeEvent.initEvent('change', true, true);" +
                "}" +
                "if (changeEvent) element.dispatchEvent(changeEvent);" +
                "" +
                "// Handle React if present" +
                "if (typeof React !== 'undefined') {" +
                "    try {" +
                "        var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
                "            window.HTMLInputElement.prototype, 'value').set;" +
                "        if (nativeInputValueSetter) {" +
                "            nativeInputValueSetter.call(element, textValue);" +
                "            triggerInputEvent(element);" +
                "        }" +
                "    } catch(e) {}" +
                "}" +
                "" +
                "// Handle Angular if present" + 
                "if (typeof angular !== 'undefined') {" +
                "    try {" +
                "        angular.element(element).val(textValue).trigger('input').trigger('change');" +
                "    } catch(e) {}" +
                "}" +
                "" +
                "// Handle jQuery if present" +
                "if (typeof jQuery !== 'undefined') {" +
                "    try {" +
                "        jQuery(element).val(textValue).trigger('input').trigger('change');" +
                "    } catch(e) {}" +
                "}" +
                "" +
                "// Blur and refocus to ensure validation runs" +
                "element.blur();" +
                "element.focus();";
            
            js.executeScript(enhancedEventScript, el, text);
            
            CSReportManager.pass("Cleared and typed text using JS '" + text + "' into " + description);
        });
    }
    
    /**
     * Clear and type with Actions API - alternative approach
     */
    public CSElement clearAndTypeWithActions(String text) {
        logger.debug("Clear and type with Actions '{}' into element: {}", text, description);
        CSReportManager.info("Clearing and typing with Actions '" + text + "' into " + description);
        
        return performActionWithValue("clearAndTypeWithActions", text, () -> {
            WebElement el = getElement();
            Actions actions = new Actions(driver);
            
            // Click to focus
            actions.click(el).perform();
            
            // Select all and delete
            actions.keyDown(Keys.CONTROL)
                   .sendKeys("a")
                   .keyUp(Keys.CONTROL)
                   .sendKeys(Keys.DELETE)
                   .perform();
            
            // Type the text
            actions.sendKeys(text).perform();
            
            CSReportManager.pass("Cleared and typed with Actions '" + text + "' into " + description);
        });
    }
    
    /**
     * Clear field using multiple methods - for stubborn fields
     */
    public CSElement clearCompletely() {
        logger.debug("Clear completely element: {}", description);
        CSReportManager.info("Clearing completely " + description);
        
        return performAction("clearCompletely", () -> {
            WebElement el = getElement();
            
            // Method 1: Standard clear
            el.clear();
            
            // Method 2: Select all and delete
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            
            // Method 3: JavaScript clear
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].value = '';", el);
            
            CSReportManager.pass("Cleared completely " + description);
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
        
        try {
            boolean displayed = getElement().isDisplayed();
            CSReportManager.info("Element " + description + " is " + (displayed ? "displayed" : "not displayed"));
            return displayed;
        } catch (Exception e) {
            logger.debug("Element not displayed: {}", e.getMessage());
            CSReportManager.info("Element " + description + " is not displayed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if element is enabled
     */
    public boolean isEnabled() {
        logger.debug("Check if element is enabled: {}", description);
        CSReportManager.info("Checking if " + description + " is enabled");
        
        try {
            boolean enabled = getElement().isEnabled();
            CSReportManager.info("Element " + description + " is " + (enabled ? "enabled" : "disabled"));
            return enabled;
        } catch (Exception e) {
            logger.debug("Element not enabled/found: {}", e.getMessage());
            CSReportManager.info("Element " + description + " is not enabled/found: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if element is selected
     */
    public boolean isSelected() {
        logger.debug("Check if element is selected: {}", description);
        CSReportManager.info("Checking if " + description + " is selected");
        
        try {
            boolean selected = getElement().isSelected();
            CSReportManager.info("Element " + description + " is " + (selected ? "selected" : "not selected"));
            return selected;
        } catch (Exception e) {
            logger.debug("Element not selected/found: {}", e.getMessage());
            CSReportManager.info("Element " + description + " is not selected/found: " + e.getMessage());
            return false;
        }
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
     * Wait for element to be present in DOM (doesn't have to be visible)
     */
    public CSElement waitForPresent() {
        return waitForPresent(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be present with custom timeout
     */
    public CSElement waitForPresent(int timeoutSeconds) {
        logger.debug("Wait for element to be present: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to be present (timeout: " + timeoutSeconds + "s)");
        
        try {
            WebElement element = CSWaitUtils.waitForElementPresent(driver, locator, timeoutSeconds);
            if (element != null) {
                CSReportManager.pass("Element " + description + " is present");
                this.element = element; // Cache the found element
                return this;
            }
        } catch (Exception e) {
            String errorMsg = String.format("Element '%s' not present after %d seconds", description, timeoutSeconds);
            logger.error(errorMsg);
            CSReportManager.fail(errorMsg);
            throw new CSElementNotFoundException(errorMsg, timeoutSeconds);
        }
        
        String errorMsg = String.format("Element '%s' not found after %d seconds", description, timeoutSeconds);
        throw new CSElementNotFoundException(errorMsg, timeoutSeconds);
    }
    
    /**
     * Wait for element to be enabled
     */
    public CSElement waitForEnabled() {
        return waitForEnabled(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be enabled with custom timeout
     */
    public CSElement waitForEnabled(int timeoutSeconds) {
        logger.debug("Wait for element to be enabled: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to be enabled (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                WebElement el = getElement();
                if (el != null && el.isEnabled()) {
                    CSReportManager.pass("Element " + description + " is enabled");
                    return this;
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Element not enabled yet: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500); // Check every 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' not enabled after %d seconds", description, timeoutSeconds);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
    }
    
    /**
     * Wait for element to be disabled
     */
    public CSElement waitForDisabled() {
        return waitForDisabled(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be disabled with custom timeout
     */
    public CSElement waitForDisabled(int timeoutSeconds) {
        logger.debug("Wait for element to be disabled: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to be disabled (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                WebElement el = getElement();
                if (el != null && !el.isEnabled()) {
                    CSReportManager.pass("Element " + description + " is disabled");
                    return this;
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Element not disabled yet: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' not disabled after %d seconds", description, timeoutSeconds);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
    }
    
    /**
     * Wait for element to be selected (for checkboxes, radio buttons)
     */
    public CSElement waitForSelected() {
        return waitForSelected(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be selected with custom timeout
     */
    public CSElement waitForSelected(int timeoutSeconds) {
        logger.debug("Wait for element to be selected: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to be selected (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                WebElement el = getElement();
                if (el != null && el.isSelected()) {
                    CSReportManager.pass("Element " + description + " is selected");
                    return this;
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Element not selected yet: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' not selected after %d seconds", description, timeoutSeconds);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
    }
    
    /**
     * Wait for element to disappear/not be present
     */
    public CSElement waitForNotPresent() {
        return waitForNotPresent(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to disappear with custom timeout
     */
    public CSElement waitForNotPresent(int timeoutSeconds) {
        logger.debug("Wait for element to disappear: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to disappear (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            try {
                driver.findElement(locator);
                // Element still exists, wait and try again
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (NoSuchElementException e) {
                // Element is gone - success!
                CSReportManager.pass("Element " + description + " has disappeared");
                this.element = null; // Clear cached element
                return this;
            }
        }
        
        String errorMsg = String.format("Element '%s' still present after %d seconds", description, timeoutSeconds);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg);
    }
    
    /**
     * Wait for element to be invisible (present in DOM but not visible)
     */
    public CSElement waitForInvisible() {
        return waitForInvisible(config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to be invisible with custom timeout
     */
    public CSElement waitForInvisible(int timeoutSeconds) {
        logger.debug("Wait for element to be invisible: {} (timeout: {}s)", description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to be invisible (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                WebElement el = driver.findElement(locator);
                if (!el.isDisplayed()) {
                    CSReportManager.pass("Element " + description + " is invisible");
                    return this;
                }
            } catch (NoSuchElementException e) {
                // Element not found - also counts as invisible
                CSReportManager.pass("Element " + description + " is not present (invisible)");
                return this;
            } catch (Exception e) {
                lastException = e;
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' still visible after %d seconds", description, timeoutSeconds);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
    }
    
    /**
     * Wait for element to contain specific text
     */
    public CSElement waitForText(String expectedText) {
        return waitForText(expectedText, config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element to contain text with custom timeout
     */
    public CSElement waitForText(String expectedText, int timeoutSeconds) {
        logger.debug("Wait for element to contain text '{}': {} (timeout: {}s)", expectedText, description, timeoutSeconds);
        CSReportManager.info("Waiting for " + description + " to contain text '" + expectedText + "' (timeout: " + timeoutSeconds + "s)");
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        String actualText = "";
        
        while (System.currentTimeMillis() < endTime) {
            try {
                actualText = getElement().getText();
                if (actualText.contains(expectedText)) {
                    CSReportManager.pass("Element " + description + " contains text '" + expectedText + "'");
                    return this;
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Text not found yet: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' does not contain text '%s' after %d seconds. Actual text: '%s'", 
            description, expectedText, timeoutSeconds, actualText);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
    }
    
    /**
     * Wait for element's attribute to have specific value
     */
    public CSElement waitForAttribute(String attributeName, String expectedValue) {
        return waitForAttribute(attributeName, expectedValue, config.getInt("cs.wait.timeout", 30));
    }
    
    /**
     * Wait for element's attribute with custom timeout
     */
    public CSElement waitForAttribute(String attributeName, String expectedValue, int timeoutSeconds) {
        logger.debug("Wait for element attribute '{}' to be '{}': {} (timeout: {}s)", 
            attributeName, expectedValue, description, timeoutSeconds);
        CSReportManager.info(String.format("Waiting for %s attribute '%s' to be '%s' (timeout: %ds)", 
            description, attributeName, expectedValue, timeoutSeconds));
        
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        Exception lastException = null;
        String actualValue = "";
        
        while (System.currentTimeMillis() < endTime) {
            try {
                actualValue = getElement().getAttribute(attributeName);
                if (expectedValue.equals(actualValue)) {
                    CSReportManager.pass(String.format("Element %s attribute '%s' has value '%s'", 
                        description, attributeName, expectedValue));
                    return this;
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Attribute not matching yet: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        String errorMsg = String.format("Element '%s' attribute '%s' is not '%s' after %d seconds. Actual: '%s'", 
            description, attributeName, expectedValue, timeoutSeconds, actualValue);
        logger.error(errorMsg);
        CSReportManager.fail(errorMsg);
        throw new CSElementException(errorMsg, lastException);
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
        long startTime = System.currentTimeMillis();
        
        // Get implicit wait timeout from driver
        int implicitWaitSeconds = config.getInt("selenium.implicit.wait", 10);
        
        logger.debug("Finding element: {} with locator: {} (max retries: {}, implicit wait: {}s)", 
            description, locator, maxRetries, implicitWaitSeconds);
        
        while (attempts < maxRetries) {
            long attemptStartTime = System.currentTimeMillis();
            
            try {
                logger.debug("Attempt {}/{} to find element: {}", attempts + 1, maxRetries, description);
                // Try primary locator
                return driver.findElement(locator);
            } catch (NoSuchElementException e) {
                lastException = e;
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                logger.debug("Element not found on attempt {}/{} after {}ms", 
                    attempts + 1, maxRetries, attemptDuration);
                
                // Try alternative locators
                if (alternativeLocators != null && alternativeLocators.length > 0) {
                    logger.debug("Trying {} alternative locators", alternativeLocators.length);
                    for (String altLocator : alternativeLocators) {
                        try {
                            By by = parseLocator(altLocator);
                            logger.debug("Trying alternative locator: {}", altLocator);
                            return driver.findElement(by);
                        } catch (NoSuchElementException altE) {
                            logger.debug("Alternative locator failed: {}", altLocator);
                            // Continue to next alternative
                        }
                    }
                }
                
                attempts++;
                if (attempts < maxRetries) {
                    logger.debug("Waiting {}ms before retry {}/{}", retryDelay, attempts + 1, maxRetries);
                    CSReportManager.info(String.format("[RETRY] Element not found, retrying in %dms (attempt %d/%d)", 
                        retryDelay, attempts + 1, maxRetries));
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        double totalSeconds = totalDuration / 1000.0;
        
        logger.error("Element not found after {} attempts and {} seconds: {}", 
            attempts, String.format("%.1f", totalSeconds), description);
        
        throw new CSElementNotFoundException(
            "Element not found: " + description + " using locator: " + locator, 
            Math.round(totalSeconds)
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
            case "typeSlowly": return "Type text slowly";
            case "clearAndTypeSlowly": return "Clear and type text slowly";
            case "typeUsingJS": return "Type text using JavaScript";
            case "clearAndTypeUsingJS": return "Clear and type using JavaScript";
            case "clearAndTypeWithActions": return "Clear and type with Actions";
            case "clearCompletely": return "Clear field completely";
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
     * Perform function with single attempt (no retries at function level)
     * Use this for functions where the underlying operations already have retries
     */
    private <T> T performFunctionSingleAttempt(String functionName, java.util.function.Supplier<T> function) {
        long startTime = System.currentTimeMillis();
        
        logger.debug("Executing function '{}' on element: {} (single attempt)", functionName, description);
        
        try {
            T result = function.get();
            long totalDuration = System.currentTimeMillis() - startTime;
            logger.debug("Function '{}' succeeded after {}ms", functionName, totalDuration);
            return result;
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            double totalSeconds = totalDuration / 1000.0;
            
            // Check if it's an element not found exception
            if (e.getCause() instanceof CSElementNotFoundException) {
                CSElementNotFoundException enfe = (CSElementNotFoundException) e.getCause();
                logger.warn("Function '{}' failed: Element not found after {} seconds", 
                    functionName, enfe.getTimeoutSeconds());
                CSReportManager.warn(String.format("Function '%s' failed: Element not found after %d seconds",
                    functionName, enfe.getTimeoutSeconds()));
            } else {
                logger.warn("Function '{}' failed after {}ms: {}", 
                    functionName, totalDuration, e.getMessage());
                CSReportManager.warn(String.format("Function '%s' failed: %s", functionName, e.getMessage()));
            }
            
            String errorMessage = String.format(
                "Failed to execute function '%s' on element: %s after %.1f seconds", 
                functionName, description, totalSeconds
            );
            
            throw new CSElementException(errorMessage, e);
        }
    }
    
    /**
     * Perform function with retry and error handling
     */
    private <T> T performFunction(String functionName, java.util.function.Supplier<T> function) {
        Exception lastException = null;
        long startTime = System.currentTimeMillis();
        
        logger.debug("Executing function '{}' on element: {} (max retries: {})", 
            functionName, description, maxRetries);
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            long attemptStartTime = System.currentTimeMillis();
            
            try {
                logger.debug("Function '{}' attempt {}/{}", functionName, attempt + 1, maxRetries);
                T result = function.get();
                
                long totalDuration = System.currentTimeMillis() - startTime;
                logger.debug("Function '{}' succeeded after {}ms", functionName, totalDuration);
                
                return result;
            } catch (Exception e) {
                lastException = e;
                long attemptDuration = System.currentTimeMillis() - attemptStartTime;
                
                // Check if it's an element not found exception
                if (e.getCause() instanceof CSElementNotFoundException) {
                    CSElementNotFoundException enfe = (CSElementNotFoundException) e.getCause();
                    logger.warn("Function '{}' failed on attempt {}/{}: Element not found after {} seconds", 
                        functionName, attempt + 1, maxRetries, enfe.getTimeoutSeconds());
                    CSReportManager.warn(String.format("Function '%s' failed on attempt %d: Element not found after %d seconds",
                        functionName, attempt + 1, enfe.getTimeoutSeconds()));
                } else {
                    logger.warn("Function '{}' failed on attempt {}/{} after {}ms: {}", 
                        functionName, attempt + 1, maxRetries, attemptDuration, e.getMessage());
                    CSReportManager.warn(String.format("Function '%s' failed on attempt %d: %s",
                        functionName, attempt + 1, e.getMessage()));
                }
                
                if (attempt < maxRetries - 1) {
                    logger.debug("Waiting {}ms before retry {}/{} for function '{}'", 
                        retryDelay, attempt + 2, maxRetries, functionName);
                    CSReportManager.info(String.format("[RETRY] Function '%s' will retry in %dms (attempt %d/%d)",
                        functionName, retryDelay, attempt + 2, maxRetries));
                    
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
        
        long totalDuration = System.currentTimeMillis() - startTime;
        double totalSeconds = totalDuration / 1000.0;
        
        logger.error("Function '{}' failed after {} attempts and {} seconds on element: {}", 
            functionName, maxRetries, String.format("%.1f", totalSeconds), description);
        
        String errorMessage = String.format(
            "Failed to execute function '%s' on element: %s after %d attempts and %.1f seconds", 
            functionName, description, maxRetries, totalSeconds
        );
        
        throw new CSElementException(errorMessage, lastException);
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