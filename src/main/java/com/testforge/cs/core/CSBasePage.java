package com.testforge.cs.core;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.exceptions.CSElementNotFoundException;
import com.testforge.cs.exceptions.CSFrameworkException;
import com.testforge.cs.factory.CSPageFactory;
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
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(config.getIntProperty("browser.explicit.wait", 30)));
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
        driver.get(url);
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
        return findElement(by, config.getIntProperty("browser.explicit.wait", 30));
    }
    
    /**
     * Find element using locator string and description
     */
    public com.testforge.cs.elements.CSElement findElement(String locatorString, String description) {
        By by = parseLocatorString(locatorString);
        return new com.testforge.cs.elements.CSElement(driver, by, description);
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
        waitForPageLoad(config.getIntProperty("browser.page.load.timeout", 60));
    }
    
    public void waitForPageLoad(int timeoutSeconds) {
        WebDriverWait pageWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        pageWait.until(driver -> jsExecutor.executeScript("return document.readyState").equals("complete"));
    }
    
    public void waitForAjax() {
        waitForAjax(config.getIntProperty("browser.explicit.wait", 30));
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
        wait.until(ExpectedConditions.titleIs(title));
    }
    
    public void waitForTitleContains(String titlePart) {
        wait.until(ExpectedConditions.titleContains(titlePart));
    }
    
    public void waitForUrl(String url) {
        wait.until(ExpectedConditions.urlToBe(url));
    }
    
    public void waitForUrlContains(String urlPart) {
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
        WebElement element = findClickableElement(by);
        highlightElement(element);
        element.click();
        logger.info("Clicked element: {}", by);
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
        WebElement element = findVisibleElement(by);
        highlightElement(element);
        element.clear();
        element.sendKeys(text);
        logger.info("Typed '{}' into element: {}", text, by);
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
        Select select = new Select(findElement(by));
        select.selectByVisibleText(text);
        logger.info("Selected '{}' from dropdown: {}", text, by);
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
        if (config.getBooleanProperty("custom.highlight.elements", true)) {
            String originalStyle = element.getAttribute("style");
            jsExecutor.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "border: 2px solid " + config.getProperty("custom.highlight.color", "red") + "; " + originalStyle
            );
            
            try {
                Thread.sleep(config.getIntProperty("custom.wait.animation", 500));
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
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }
    
    public boolean isElementVisible(By by) {
        try {
            return findElement(by).isDisplayed();
        } catch (Exception e) {
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
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();
        logger.info("Accepted alert");
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
        driver.switchTo().frame(frameNameOrId);
        logger.info("Switched to frame: {}", frameNameOrId);
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
        driver.switchTo().defaultContent();
        logger.info("Switched to default content");
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
        String filePath = config.getProperty("report.directory", "target/screenshots") + "/screenshot_" + timestamp + ".png";
        return CSWebDriverManager.takeScreenshot(filePath);
    }
    
    public byte[] takeScreenshotAsBytes() {
        return CSWebDriverManager.takeScreenshot();
    }
}