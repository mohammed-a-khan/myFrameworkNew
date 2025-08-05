package com.testforge.cs.waits;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Custom wait conditions for advanced scenarios
 */
public class CSWaitConditions {
    private static final Logger logger = LoggerFactory.getLogger(CSWaitConditions.class);
    
    /**
     * Wait for element to have specific text
     */
    public static ExpectedCondition<Boolean> elementTextEquals(By locator, String expectedText) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    return expectedText.equals(element.getText());
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return String.format("element %s to have text '%s'", locator, expectedText);
            }
        };
    }
    
    /**
     * Wait for element to contain specific text
     */
    public static ExpectedCondition<Boolean> elementTextContains(By locator, String expectedText) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    return element.getText().contains(expectedText);
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return String.format("element %s to contain text '%s'", locator, expectedText);
            }
        };
    }
    
    /**
     * Wait for element attribute to have specific value
     */
    public static ExpectedCondition<Boolean> elementAttributeEquals(By locator, String attribute, String expectedValue) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    String actualValue = element.getAttribute(attribute);
                    return expectedValue.equals(actualValue);
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return String.format("element %s attribute '%s' to equal '%s'", locator, attribute, expectedValue);
            }
        };
    }
    
    /**
     * Wait for element CSS value to have specific value
     */
    public static ExpectedCondition<Boolean> elementCssValueEquals(By locator, String property, String expectedValue) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    String actualValue = element.getCssValue(property);
                    return expectedValue.equals(actualValue);
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return String.format("element %s CSS property '%s' to equal '%s'", locator, property, expectedValue);
            }
        };
    }
    
    /**
     * Wait for JavaScript condition
     */
    public static ExpectedCondition<Boolean> javaScriptCondition(String script) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    Object result = ((JavascriptExecutor) driver).executeScript(script);
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return "JavaScript condition: " + script;
            }
        };
    }
    
    /**
     * Wait for number of elements
     */
    public static ExpectedCondition<Boolean> numberOfElements(By locator, int expectedCount) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    List<WebElement> elements = driver.findElements(locator);
                    return elements.size() == expectedCount;
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return String.format("number of elements %s to be %d", locator, expectedCount);
            }
        };
    }
    
    /**
     * Wait for element to be enabled
     */
    public static ExpectedCondition<Boolean> elementEnabled(By locator) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    return element.isEnabled();
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return "element " + locator + " to be enabled";
            }
        };
    }
    
    /**
     * Wait for element to be selected
     */
    public static ExpectedCondition<Boolean> elementSelected(By locator) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = driver.findElement(locator);
                    return element.isSelected();
                } catch (Exception e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return "element " + locator + " to be selected";
            }
        };
    }
    
    /**
     * Wait for any of multiple conditions
     */
    @SafeVarargs
    public static ExpectedCondition<Boolean> anyOf(ExpectedCondition<Boolean>... conditions) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                for (ExpectedCondition<Boolean> condition : conditions) {
                    try {
                        if (Boolean.TRUE.equals(condition.apply(driver))) {
                            return true;
                        }
                    } catch (Exception e) {
                        // Continue to next condition
                    }
                }
                return false;
            }
            
            @Override
            public String toString() {
                return "any of " + conditions.length + " conditions";
            }
        };
    }
    
    /**
     * Wait for all of multiple conditions
     */
    @SafeVarargs
    public static ExpectedCondition<Boolean> allOf(ExpectedCondition<Boolean>... conditions) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                for (ExpectedCondition<Boolean> condition : conditions) {
                    try {
                        if (!Boolean.TRUE.equals(condition.apply(driver))) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
                return true;
            }
            
            @Override
            public String toString() {
                return "all of " + conditions.length + " conditions";
            }
        };
    }
    
    /**
     * Wait for custom function
     */
    public static <T> ExpectedCondition<T> custom(Function<WebDriver, T> function, String description) {
        return new ExpectedCondition<T>() {
            @Override
            public T apply(WebDriver driver) {
                return function.apply(driver);
            }
            
            @Override
            public String toString() {
                return description;
            }
        };
    }
    
    /**
     * Wait for AJAX calls to complete
     */
    public static ExpectedCondition<Boolean> ajaxComplete() {
        return javaScriptCondition("return (typeof jQuery !== 'undefined') ? jQuery.active == 0 : true");
    }
    
    /**
     * Wait for Angular to be ready
     */
    public static ExpectedCondition<Boolean> angularReady() {
        return javaScriptCondition(
            "return (typeof angular !== 'undefined') ? " +
            "angular.element(document).injector().get('$http').pendingRequests.length === 0 : true"
        );
    }
    
    /**
     * Wait for document ready state
     */
    public static ExpectedCondition<Boolean> documentReady() {
        return javaScriptCondition("return document.readyState == 'complete'");
    }
}