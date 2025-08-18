package com.testforge.cs.core;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.exceptions.CSElementNotFoundException;
import com.testforge.cs.exceptions.CSFrameworkException;
import com.testforge.cs.factory.CSPageFactory;
import com.testforge.cs.reporting.CSReportManager;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base page class with comprehensive helper methods for page objects
 */
public abstract class CSBasePage {
    protected static final Logger logger = LoggerFactory.getLogger(CSBasePage.class);
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor jsExecutor;
    protected Actions actions;
    protected CSConfigManager config;
    
    private final Map<String, WebElement> elementCache = new ConcurrentHashMap<>();
    private final Map<String, By> locatorCache = new ConcurrentHashMap<>();
    
    public CSBasePage() {
        this.driver = CSWebDriverManager.getDriver();
        this.config = CSConfigManager.getInstance();
        
        if (this.driver == null) {
            logger.error("Driver is null in CSBasePage constructor for thread: {}. This will cause NullPointerException!", Thread.currentThread().getName());
            throw new RuntimeException("WebDriver is not initialized. Ensure test setup is complete before creating page objects.");
        }
        
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getIntProperty("cs.browser.explicit.wait", 30)));
        this.jsExecutor = (JavascriptExecutor) driver;
        this.actions = new Actions(driver);
        
        initializePage();
        
        // Use CSPageFactory for self-healing element initialization
        CSPageFactory.initElements(this);
    }
    
    /**
     * Initialize page with @CSPage annotation
     */
    private void initializePage() {
        CSPage pageAnnotation = this.getClass().getAnnotation(CSPage.class);
        if (pageAnnotation != null) {
            if (pageAnnotation.autoNavigate() && !pageAnnotation.url().isEmpty()) {
                navigateTo(pageAnnotation.url());
            }
            
            // Only validate if we've navigated
            if (pageAnnotation.validateOnLoad() && pageAnnotation.autoNavigate()) {
                waitForPageLoad(pageAnnotation.waitTime());
                
                if (!pageAnnotation.readyScript().isEmpty()) {
                    waitForJavaScriptCondition(pageAnnotation.readyScript(), pageAnnotation.waitTime());
                }
                
                if (!pageAnnotation.title().isEmpty()) {
                    waitForTitle(pageAnnotation.title());
                }
            }
        }
    }
    
    
    /**
     * Create By locator from annotation
     */
    private By createBy(CSLocator locator) {
        // Check locatorKey first (object repository reference)
        if (!locator.locatorKey().isEmpty()) {
            String repoValue = config.getProperty(locator.locatorKey());
            if (repoValue != null) {
                return parseLocatorString(repoValue);
            }
            throw new CSFrameworkException("Locator key not found in repository: " + locator.locatorKey());
        }
        
        // Check specific locator types
        if (!locator.id().isEmpty()) {
            return By.id(locator.id());
        }
        if (!locator.name().isEmpty()) {
            return By.name(locator.name());
        }
        if (!locator.css().isEmpty()) {
            return By.cssSelector(locator.css());
        }
        if (!locator.xpath().isEmpty()) {
            return By.xpath(locator.xpath());
        }
        if (!locator.className().isEmpty()) {
            return By.className(locator.className());
        }
        if (!locator.tagName().isEmpty()) {
            return By.tagName(locator.tagName());
        }
        if (!locator.linkText().isEmpty()) {
            return By.linkText(locator.linkText());
        }
        if (!locator.partialLinkText().isEmpty()) {
            return By.partialLinkText(locator.partialLinkText());
        }
        
        // Check value field as fallback
        if (!locator.value().isEmpty()) {
            return parseLocatorString(locator.value());
        }
        
        throw new CSFrameworkException("No locator strategy defined in @CSLocator annotation");
    }
    
    /**
     * Parse locator string to determine type
     */
    private By parseLocatorString(String locatorStr) {
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
            return By.xpath(locatorStr);
        } else if (locatorStr.startsWith("#") || locatorStr.contains(".") || locatorStr.contains(":")) {
            return By.cssSelector(locatorStr);
        } else {
            return By.id(locatorStr);
        }
    }
    
    // ===== Navigation Methods =====
    
    public void navigateTo(String url) {
        logger.info("Navigating to: {}", url);
        CSReportManager.info("[INFO] Navigating to URL: " + url);
        CSReportManager.addAction("navigate", "Navigate to URL", url);
        driver.get(url);
        CSReportManager.pass("[PASS] Successfully navigated to: " + url);
    }
    
    public void navigateTo() {
        // Navigate to the URL configured for this page
        CSPage pageAnnotation = this.getClass().getAnnotation(CSPage.class);
        if (pageAnnotation != null && !pageAnnotation.url().isEmpty()) {
            String url = config.resolveValue(pageAnnotation.url());
            navigateTo(url);
        } else {
            throw new CSFrameworkException("No URL configured for page: " + this.getClass().getName());
        }
    }
    
    public void navigateBack() {
        logger.info("Navigating back");
        driver.navigate().back();
    }
    
    public void navigateForward() {
        logger.info("Navigating forward");
        driver.navigate().forward();
    }
    
    public void refresh() {
        logger.info("Refreshing page");
        driver.navigate().refresh();
    }
    
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
    
    public String getTitle() {
        return driver.getTitle();
    }
    
    // ===== Element Finding Methods =====
    
    public WebElement findElement(By by) {
        return findElement(by, config.getIntProperty("cs.browser.explicit.wait", 30));
    }
    
    /**
     * Find element using locator string and description
     */
    public CSElement findElement(String locatorString, String description) {
        By by = parseLocatorString(locatorString);
        return new CSElement(driver, by, description);
    }
    
    public WebElement findElement(By by, int timeoutSeconds) {
        try {
            WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return customWait.until(ExpectedConditions.presenceOfElementLocated(by));
        } catch (TimeoutException e) {
            throw new CSElementNotFoundException(by.toString(), timeoutSeconds, e);
        }
    }
    
    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }
    
    public WebElement findVisibleElement(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }
    
    public WebElement findClickableElement(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }
    
    // ===== Wait Methods =====
    
    public void waitForPageLoad() {
        CSReportManager.info("[INFO] Waiting for page to load");
        CSReportManager.addAction("wait", "Wait for page load");
        waitForPageLoad(config.getIntProperty("cs.browser.page.load.timeout", 60));
        CSReportManager.pass("[PASS] Page loaded successfully");
    }
    
    public void waitForPageLoad(int timeoutSeconds) {
        WebDriverWait pageWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        pageWait.until(driver -> jsExecutor.executeScript("return document.readyState").equals("complete"));
    }
    
    public void waitForAjax() {
        waitForAjax(config.getIntProperty("cs.browser.explicit.wait", 30));
    }
    
    public void waitForAjax(int timeoutSeconds) {
        WebDriverWait ajaxWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        ajaxWait.until(driver -> {
            Boolean jQueryDone = (Boolean) jsExecutor.executeScript("return jQuery.active == 0");
            Boolean jsReady = (Boolean) jsExecutor.executeScript("return document.readyState == 'complete'");
            return jQueryDone && jsReady;
        });
    }
    
    public void waitForTitle(String title) {
        CSReportManager.addAction("wait", "Wait for title", title);
        wait.until(ExpectedConditions.titleIs(title));
    }
    
    public void waitForTitleContains(String titlePart) {
        CSReportManager.addAction("wait", "Wait for title to contain", titlePart);
        wait.until(ExpectedConditions.titleContains(titlePart));
    }
    
    public void waitForUrl(String url) {
        CSReportManager.addAction("wait", "Wait for URL", url);
        wait.until(ExpectedConditions.urlToBe(url));
    }
    
    public void waitForUrlContains(String urlPart) {
        CSReportManager.addAction("wait", "Wait for URL to contain", urlPart);
        wait.until(ExpectedConditions.urlContains(urlPart));
    }
    
    public void waitForElementVisible(By by) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }
    
    public void waitForElementInvisible(By by) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(by));
    }
    
    public void waitForElementClickable(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by));
    }
    
    public void waitForTextPresent(By by, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(by, text));
    }
    
    public void waitForAttributeValue(By by, String attribute, String value) {
        wait.until(ExpectedConditions.attributeToBe(by, attribute, value));
    }
    
    public <T> T waitForCondition(Function<WebDriver, T> condition, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(condition);
    }
    
    public void waitForJavaScriptCondition(String script, int timeoutSeconds) {
        WebDriverWait jsWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        jsWait.until(driver -> (Boolean) jsExecutor.executeScript(script));
    }
    
    // ===== Action Methods =====
    
    public void click(By by) {
        CSReportManager.info("Clicking element: " + by);
        WebElement element = findClickableElement(by);
        highlightElement(element);
        element.click();
        logger.info("Clicked element: {}", by);
        CSReportManager.pass("Successfully clicked element: " + by);
    }
    
    public void doubleClick(By by) {
        WebElement element = findElement(by);
        highlightElement(element);
        actions.doubleClick(element).perform();
        logger.info("Double clicked element: {}", by);
    }
    
    public void rightClick(By by) {
        WebElement element = findElement(by);
        highlightElement(element);
        actions.contextClick(element).perform();
        logger.info("Right clicked element: {}", by);
    }
    
    public void type(By by, String text) {
        CSReportManager.info("Typing '" + text + "' into element: " + by);
        WebElement element = findVisibleElement(by);
        highlightElement(element);
        element.clear();
        element.sendKeys(text);
        logger.info("Typed '{}' into element: {}", text, by);
        CSReportManager.pass("Successfully typed '" + text + "' into element: " + by);
    }
    
    public void typeWithoutClear(By by, String text) {
        WebElement element = findVisibleElement(by);
        highlightElement(element);
        element.sendKeys(text);
        logger.info("Typed '{}' into element without clearing: {}", text, by);
    }
    
    public void clear(By by) {
        WebElement element = findElement(by);
        element.clear();
        logger.info("Cleared element: {}", by);
    }
    
    public void selectByText(By by, String text) {
        CSReportManager.info("Selecting '" + text + "' from dropdown: " + by);
        Select select = new Select(findElement(by));
        select.selectByVisibleText(text);
        logger.info("Selected '{}' from dropdown: {}", text, by);
        CSReportManager.pass("Successfully selected '" + text + "' from dropdown: " + by);
    }
    
    public void selectByValue(By by, String value) {
        Select select = new Select(findElement(by));
        select.selectByValue(value);
        logger.info("Selected value '{}' from dropdown: {}", value, by);
    }
    
    public void selectByIndex(By by, int index) {
        Select select = new Select(findElement(by));
        select.selectByIndex(index);
        logger.info("Selected index {} from dropdown: {}", index, by);
    }
    
    // ===== Mouse Actions =====
    
    public void hoverOver(By by) {
        WebElement element = findElement(by);
        actions.moveToElement(element).perform();
        logger.info("Hovered over element: {}", by);
    }
    
    public void dragAndDrop(By source, By target) {
        WebElement sourceElement = findElement(source);
        WebElement targetElement = findElement(target);
        actions.dragAndDrop(sourceElement, targetElement).perform();
        logger.info("Dragged from {} to {}", source, target);
    }
    
    // ===== JavaScript Methods =====
    
    public void clickJS(By by) {
        WebElement element = findElement(by);
        jsExecutor.executeScript("arguments[0].click();", element);
        logger.info("Clicked element using JavaScript: {}", by);
    }
    
    public void scrollToElement(By by) {
        WebElement element = findElement(by);
        jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
        logger.info("Scrolled to element: {}", by);
    }
    
    public void scrollToTop() {
        jsExecutor.executeScript("window.scrollTo(0, 0);");
        logger.info("Scrolled to top of page");
    }
    
    public void scrollToBottom() {
        jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        logger.info("Scrolled to bottom of page");
    }
    
    public void highlightElement(WebElement element) {
        if (config.getBooleanProperty("cs.element.highlight.elements", true)) {
            String originalStyle = element.getAttribute("style");
            jsExecutor.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "border: 2px solid " + config.getProperty("cs.element.highlight.color", "red") + "; " + originalStyle
            );
            
            try {
                Thread.sleep(config.getIntProperty("cs.element.wait.animation", 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            jsExecutor.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                originalStyle
            );
        }
    }
    
    // ===== Verification Methods =====
    
    public boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            CSReportManager.info("Element is present: " + by);
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            CSReportManager.info("Element is not present: " + by);
            return false;
        }
    }
    
    public boolean isElementVisible(By by) {
        try {
            boolean visible = findElement(by).isDisplayed();
            CSReportManager.info("Element visibility check for " + by + ": " + visible);
            return visible;
        } catch (Exception e) {
            CSReportManager.info("Element not found or not visible: " + by);
            return false;
        }
    }
    
    public boolean isElementEnabled(By by) {
        try {
            return findElement(by).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isElementSelected(By by) {
        try {
            return findElement(by).isSelected();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getText(By by) {
        return findElement(by).getText();
    }
    
    public String getAttribute(By by, String attribute) {
        return findElement(by).getAttribute(attribute);
    }
    
    public String getCssValue(By by, String property) {
        return findElement(by).getCssValue(property);
    }
    
    // ===== Alert Methods =====
    
    public void acceptAlert() {
        CSReportManager.info("Accepting alert");
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String alertText = alert.getText();
        alert.accept();
        logger.info("Accepted alert");
        CSReportManager.pass("Accepted alert with text: " + alertText);
    }
    
    public void dismissAlert() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.dismiss();
        logger.info("Dismissed alert");
    }
    
    public String getAlertText() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        return alert.getText();
    }
    
    public void typeInAlert(String text) {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(text);
        logger.info("Typed '{}' in alert", text);
    }
    
    // ===== Frame Methods =====
    
    public void switchToFrame(String frameNameOrId) {
        CSReportManager.info("Switching to frame: " + frameNameOrId);
        driver.switchTo().frame(frameNameOrId);
        logger.info("Switched to frame: {}", frameNameOrId);
        CSReportManager.pass("Successfully switched to frame: " + frameNameOrId);
    }
    
    public void switchToFrame(int frameIndex) {
        driver.switchTo().frame(frameIndex);
        logger.info("Switched to frame index: {}", frameIndex);
    }
    
    public void switchToFrame(WebElement frameElement) {
        driver.switchTo().frame(frameElement);
        logger.info("Switched to frame element");
    }
    
    public void switchToDefaultContent() {
        CSReportManager.info("Switching to default content");
        driver.switchTo().defaultContent();
        logger.info("Switched to default content");
        CSReportManager.pass("Successfully switched to default content");
    }
    
    // ===== Window Methods =====
    
    public void switchToWindow(String windowHandle) {
        driver.switchTo().window(windowHandle);
        logger.info("Switched to window: {}", windowHandle);
    }
    
    public void switchToNewWindow() {
        String currentWindow = driver.getWindowHandle();
        Set<String> windows = driver.getWindowHandles();
        for (String window : windows) {
            if (!window.equals(currentWindow)) {
                driver.switchTo().window(window);
                logger.info("Switched to new window: {}", window);
                break;
            }
        }
    }
    
    public void closeCurrentWindow() {
        driver.close();
        logger.info("Closed current window");
    }
    
    // ===== Screenshot Methods =====
    
    public File takeScreenshot() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = config.getProperty("cs.report.directory", "target/screenshots") + "/screenshot_" + timestamp + ".png";
        return CSWebDriverManager.takeScreenshot(filePath);
    }
    
    public byte[] takeScreenshotAsBytes() {
        return CSWebDriverManager.takeScreenshot();
    }
    
    /**
     * Capture screenshot with custom name
     */
    public void captureScreenshot(String name) {
        CSReportManager.info("Capturing screenshot: " + name);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = config.getProperty("cs.report.directory", "target/screenshots") + "/" + name + "_" + timestamp + ".png";
        File screenshot = CSWebDriverManager.takeScreenshot(filePath);
        if (screenshot != null && screenshot.exists()) {
            CSReportManager.pass("Screenshot captured: " + name);
        } else {
            CSReportManager.warn("Failed to capture screenshot: " + name);
        }
    }
    
    // ===== Dynamic Element Creation Methods =====
    
    /**
     * Find element dynamically using parameterized locator from repository
     * This method replaces placeholders {0}, {1}, etc. with actual runtime values
     * Example: findDynamicElement("dynamic.menu.item.xpath", "Settings")
     * where dynamic.menu.item.xpath=//a[contains(text(),'{0}')]
     */
    public CSElement findDynamicElement(String patternKey, Object... params) {
        CSReportManager.info("Creating dynamic element using pattern: " + patternKey);
        
        String locatorPattern = config.getProperty(patternKey);
        if (locatorPattern == null) {
            CSReportManager.fail("Pattern not found in repository: " + patternKey);
            throw new CSFrameworkException("Pattern key not found: " + patternKey);
        }
        
        // Replace placeholders {0}, {1}, etc. with actual values
        String actualLocator = locatorPattern;
        for (int i = 0; i < params.length; i++) {
            actualLocator = actualLocator.replace("{" + i + "}", params[i].toString());
        }
        
        CSReportManager.info("Resolved dynamic locator: " + actualLocator);
        return findElement(actualLocator, "Dynamic element from " + patternKey);
    }
    
    /**
     * Find menu item dynamically by text
     */
    public CSElement findMenuItemByText(String menuText) {
        CSReportManager.info("Finding menu item with text: " + menuText);
        return findDynamicElement("dynamic.menu.item.xpath", menuText);
    }
    
    /**
     * Find button dynamically by text
     */
    public CSElement findButtonByText(String buttonText) {
        CSReportManager.info("Finding button with text: " + buttonText);
        String xpath = String.format("//button[contains(text(),'%s')]", buttonText);
        return findElement(xpath, "Button: " + buttonText);
    }
    
    /**
     * Find link dynamically by text
     */
    public CSElement findLinkByText(String linkText) {
        CSReportManager.info("Finding link with text: " + linkText);
        String xpath = String.format("//a[contains(text(),'%s')]", linkText);
        return findElement(xpath, "Link: " + linkText);
    }
    
    /**
     * Find table cell dynamically by coordinates
     */
    public CSElement findTableCell(String tableId, int row, int column) {
        CSReportManager.info(String.format("Finding table cell at [%d,%d] in table: %s", row, column, tableId));
        return findDynamicElement("dynamic.table.cell.xpath", tableId, row, column);
    }
    
    /**
     * Find input field by associated label
     */
    public CSElement findInputByLabel(String labelText) {
        CSReportManager.info("Finding input field with label: " + labelText);
        String xpath = String.format("//label[contains(text(),'%s')]/following-sibling::input[1]", labelText);
        CSElement element = findElement(xpath, "Input for: " + labelText);
        
        if (element.isPresent()) {
            CSReportManager.pass("Found input field for label: " + labelText);
        } else {
            CSReportManager.warn("Input field not found for label: " + labelText);
        }
        return element;
    }
    
    /**
     * Find element by data attribute
     */
    public CSElement findElementByDataAttribute(String attribute, String value) {
        CSReportManager.info(String.format("Finding element with data-%s='%s'", attribute, value));
        String css = String.format("[data-%s='%s']", attribute, value);
        return findElement("css:" + css, String.format("Element[data-%s='%s']", attribute, value));
    }
    
    /**
     * Find dropdown option dynamically
     */
    public CSElement findDropdownOption(String dropdownId, String optionText) {
        CSReportManager.info(String.format("Finding option '%s' in dropdown: %s", optionText, dropdownId));
        return findDynamicElement("dynamic.dropdown.option.xpath", dropdownId, optionText);
    }
    
    /**
     * Find nth element from a list
     */
    public CSElement findNthElement(String baseLocator, int index) {
        CSReportManager.info(String.format("Finding element at index %d for locator: %s", index, baseLocator));
        String xpath = String.format("(%s)[%d]", baseLocator, index);
        return findElement("xpath:" + xpath, String.format("Element[%d]", index));
    }
    
    /**
     * Find element by multiple attributes
     */
    public CSElement findElementByAttributes(String tagName, Map<String, String> attributes) {
        CSReportManager.info(String.format("Finding <%s> element with attributes: %s", tagName, attributes));
        
        StringBuilder xpath = new StringBuilder("//" + tagName);
        if (!attributes.isEmpty()) {
            xpath.append("[");
            int count = 0;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (count > 0) xpath.append(" and ");
                xpath.append(String.format("@%s='%s'", entry.getKey(), entry.getValue()));
                count++;
            }
            xpath.append("]");
        }
        
        return findElement("xpath:" + xpath.toString(), tagName + " with attributes");
    }
    
    /**
     * Find element within a container
     */
    public CSElement findElementInContainer(String containerId, String elementSelector) {
        CSReportManager.info(String.format("Finding element '%s' within container: %s", elementSelector, containerId));
        String css = String.format("#%s %s", containerId, elementSelector);
        return findElement("css:" + css, "Element in " + containerId);
    }
    
    /**
     * Check if dynamic element exists and log result
     */
    public boolean isDynamicElementPresent(String patternKey, Object... params) {
        try {
            CSElement element = findDynamicElement(patternKey, params);
            boolean present = element.isPresent();
            
            if (present) {
                CSReportManager.pass("Dynamic element is present: " + patternKey);
            } else {
                CSReportManager.info("Dynamic element is not present: " + patternKey);
            }
            return present;
        } catch (Exception e) {
            CSReportManager.warn("Failed to check dynamic element presence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get text from dynamic element with validation
     */
    public String getDynamicElementText(String patternKey, Object... params) {
        CSReportManager.info("Getting text from dynamic element: " + patternKey);
        
        try {
            CSElement element = findDynamicElement(patternKey, params);
            String text = element.getText();
            
            if (text != null && !text.isEmpty()) {
                CSReportManager.pass("Retrieved text: " + text);
            } else {
                CSReportManager.warn("Element has no text content");
            }
            return text;
        } catch (Exception e) {
            CSReportManager.fail("Failed to get text from dynamic element: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Click dynamic element with reporting
     */
    public void clickDynamicElement(String patternKey, Object... params) {
        CSReportManager.info("Clicking dynamic element: " + patternKey);
        
        try {
            CSElement element = findDynamicElement(patternKey, params);
            element.click();
            CSReportManager.pass("Successfully clicked dynamic element");
        } catch (Exception e) {
            CSReportManager.fail("Failed to click dynamic element: " + e.getMessage());
            throw e;
        }
    }
}