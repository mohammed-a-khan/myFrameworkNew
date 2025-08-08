package com.testforge.cs.waits;

import com.testforge.cs.reporting.CSReportManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Production-ready wait utilities for Selenium
 */
public class CSWaitUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSWaitUtils.class);
    private final WebDriver driver;
    
    /**
     * Constructor for instance-based usage
     */
    public CSWaitUtils(WebDriver driver) {
        this.driver = driver;
    }
    
    /**
     * Default constructor for static usage
     */
    public CSWaitUtils() {
        this.driver = null;
    }
    
    /**
     * Wait for element to be visible
     */
    public static WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        logger.debug("Waiting for element to be visible: {}", locator);
        CSReportManager.addAction("wait", "Wait for element to be visible", locator.toString());
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    /**
     * Wait for element to be clickable
     */
    public static WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSeconds) {
        logger.debug("Waiting for element to be clickable: {}", locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    /**
     * Wait for element to be present
     */
    public static WebElement waitForElementPresent(WebDriver driver, By locator, int timeoutSeconds) {
        logger.debug("Waiting for element to be present: {}", locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    /**
     * Wait for all elements to be visible
     */
    public static List<WebElement> waitForAllElementsVisible(WebDriver driver, By locator, int timeoutSeconds) {
        logger.debug("Waiting for all elements to be visible: {}", locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }
    
    /**
     * Wait for element to disappear
     */
    public static boolean waitForElementInvisible(WebDriver driver, By locator, int timeoutSeconds) {
        logger.debug("Waiting for element to be invisible: {}", locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
    
    /**
     * Wait for text to be present in element
     */
    public static boolean waitForTextInElement(WebDriver driver, By locator, String text, int timeoutSeconds) {
        logger.debug("Waiting for text '{}' in element: {}", text, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }
    
    /**
     * Wait for text to be present (alias)
     */
    public static boolean waitForTextPresent(WebDriver driver, By locator, String text, int timeoutSeconds) {
        return waitForTextInElement(driver, locator, text, timeoutSeconds);
    }
    
    /**
     * Wait for attribute value
     */
    public static boolean waitForAttributeValue(WebDriver driver, By locator, String attribute, String value, int timeoutSeconds) {
        logger.debug("Waiting for attribute '{}' to have value '{}' in element: {}", attribute, value, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }
    
    /**
     * Wait for custom condition
     */
    public static <T> T waitForCondition(WebDriver driver, Function<WebDriver, T> condition, int timeoutSeconds, String message) {
        logger.debug("Waiting for custom condition: {}", message);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.withMessage(message);
        return wait.until(condition);
    }
    
    /**
     * Wait for custom condition (overloaded for ExpectedCondition)
     */
    public static <T> T waitForCondition(WebDriver driver, ExpectedCondition<T> condition, int timeoutSeconds) {
        return waitForCondition(driver, condition, timeoutSeconds, "Waiting for condition");
    }
    
    /**
     * Wait for page load complete
     */
    public static void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        logger.debug("Waiting for page load complete");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until((ExpectedCondition<Boolean>) wd -> {
            String readyState = ((JavascriptExecutor) wd).executeScript("return document.readyState").toString();
            return "complete".equals(readyState);
        });
    }
    
    /**
     * Wait for jQuery to complete
     */
    public static void waitForJQuery(WebDriver driver, int timeoutSeconds) {
        logger.debug("Waiting for jQuery to complete");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until((ExpectedCondition<Boolean>) wd -> {
            try {
                return (Boolean) ((JavascriptExecutor) wd).executeScript("return jQuery.active == 0");
            } catch (Exception e) {
                // jQuery not present
                return true;
            }
        });
    }
    
    /**
     * Wait for Angular to complete
     */
    public static void waitForAngular(WebDriver driver, int timeoutSeconds) {
        logger.debug("Waiting for Angular to complete");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until((ExpectedCondition<Boolean>) wd -> {
            try {
                return (Boolean) ((JavascriptExecutor) wd).executeScript(
                    "return window.getAllAngularTestabilities().findIndex(x => !x.isStable()) === -1"
                );
            } catch (Exception e) {
                // Angular not present
                return true;
            }
        });
    }
    
    /**
     * Wait for element count
     */
    public static boolean waitForElementCount(WebDriver driver, By locator, int expectedCount, int timeoutSeconds) {
        logger.debug("Waiting for {} elements matching: {}", expectedCount, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until((ExpectedCondition<Boolean>) wd -> {
            List<WebElement> elements = wd.findElements(locator);
            return elements.size() == expectedCount;
        });
    }
    
    /**
     * Wait for URL to contain
     */
    public static boolean waitForUrlContains(WebDriver driver, String urlPart, int timeoutSeconds) {
        logger.debug("Waiting for URL to contain: {}", urlPart);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.urlContains(urlPart));
    }
    
    /**
     * Wait for title to contain
     */
    public static boolean waitForTitleContains(WebDriver driver, String titlePart, int timeoutSeconds) {
        logger.debug("Waiting for title to contain: {}", titlePart);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.titleContains(titlePart));
    }
    
    /**
     * Wait for alert present
     */
    public static boolean waitForAlertPresent(WebDriver driver, int timeoutSeconds) {
        logger.debug("Waiting for alert to be present");
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.alertIsPresent());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Fluent wait with polling
     */
    public static <T> T fluentWait(WebDriver driver, Function<WebDriver, T> condition, 
                                   int timeoutSeconds, int pollingMillis, String message) {
        logger.debug("Fluent wait with polling every {}ms: {}", pollingMillis, message);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.pollingEvery(Duration.ofMillis(pollingMillis));
        wait.withMessage(message);
        return wait.until(condition);
    }
    
    // Instance-based methods
    
    /**
     * Wait for element to be visible (instance method)
     */
    public WebElement waitForVisible(By locator, int timeoutSeconds) {
        return waitForElementVisible(driver, locator, timeoutSeconds);
    }
    
    /**
     * Wait for element to be clickable (instance method)
     */
    public WebElement waitForClickable(By locator, int timeoutSeconds) {
        return waitForElementClickable(driver, locator, timeoutSeconds);
    }
    
    /**
     * Wait for element to be present (instance method)
     */
    public WebElement waitForPresent(By locator, int timeoutSeconds) {
        return waitForElementPresent(driver, locator, timeoutSeconds);
    }
    
    /**
     * Wait for text to be present in element (instance method)
     */
    public boolean waitForText(By locator, String text, int timeoutSeconds) {
        return waitForTextPresent(driver, locator, text, timeoutSeconds);
    }
    
    /**
     * Wait for element to be invisible (instance method)
     */
    public boolean waitForInvisible(By locator, int timeoutSeconds) {
        return waitForElementInvisible(driver, locator, timeoutSeconds);
    }
    
    /**
     * Wait for custom condition (instance method)
     */
    public <T> T waitFor(ExpectedCondition<T> condition, int timeoutSeconds) {
        return waitForCondition(driver, condition, timeoutSeconds);
    }
    
    /**
     * Simple wait for specified seconds
     */
    public static void waitForSeconds(int seconds) {
        try {
            logger.debug("Waiting for {} seconds", seconds);
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Wait interrupted after {} seconds", seconds);
        }
    }
}