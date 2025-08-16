#!/bin/bash

echo "Testing Intelligent Failure Analysis..."
echo "========================================="
echo ""
echo "This script will create test scenarios with different failure types"
echo "to demonstrate the intelligent failure categorization and analysis."
echo ""

# Create a test that will demonstrate different failure types
cat > src/test/java/com/testforge/cs/tests/FailureAnalysisDemo.java << 'EOF'
package com.testforge.cs.tests;

import com.testforge.cs.base.CSBaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.time.Duration;

public class FailureAnalysisDemo extends CSBaseTest {
    
    @Test(description = "Test with Element Not Found (Flaky)")
    public void testElementNotFound() {
        driver.get("https://www.google.com");
        // This will fail with NoSuchElementException - categorized as flaky
        driver.findElement(By.id("non-existent-element")).click();
    }
    
    @Test(description = "Test with Assertion Failure (Genuine)")
    public void testAssertionFailure() {
        driver.get("https://www.google.com");
        String title = driver.getTitle();
        // This is a genuine assertion failure - not flaky
        Assert.assertEquals(title, "Wrong Title", "Title validation failed");
    }
    
    @Test(description = "Test with Timeout (Flaky)")
    public void testTimeoutFailure() {
        driver.get("https://www.google.com");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        // This will timeout - categorized as flaky
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("never-appears")));
    }
    
    @Test(description = "Test with Stale Element (Flaky)")
    public void testStaleElementFailure() {
        driver.get("https://www.google.com");
        WebElement element = driver.findElement(By.name("q"));
        driver.navigate().refresh();
        // This will fail with StaleElementReferenceException - categorized as flaky
        element.sendKeys("test");
    }
    
    @Test(description = "Test with Click Intercepted (Flaky)")
    public void testClickIntercepted() {
        driver.get("https://www.google.com");
        // Simulate element not clickable scenario
        driver.executeScript("document.body.innerHTML = '<div style=\"position:fixed;top:0;left:0;width:100%;height:100%;z-index:9999;\"></div>' + document.body.innerHTML");
        driver.findElement(By.name("q")).click();
    }
    
    @Test(description = "Test that passes")
    public void testSuccess() {
        driver.get("https://www.google.com");
        Assert.assertTrue(driver.getTitle().contains("Google"));
    }
}
EOF

echo "Created test file: FailureAnalysisDemo.java"
echo ""
echo "Compiling tests..."
mvn compile test-compile

echo ""
echo "Running tests with failure analysis..."
echo "========================================="

# Run the tests and let them fail to demonstrate the analysis
mvn test -Dtest=FailureAnalysisDemo -DfailIfNoTests=false 2>&1 | tee test-output.log

echo ""
echo "========================================="
echo "Test execution complete!"
echo ""
echo "Check the report at: cs-reports/latest-report.html"
echo "The report will show:"
echo "1. Flaky tests correctly identified in Test Reliability Metrics"
echo "2. Detailed failure analysis for each failed test"
echo "3. Root cause identification and recommendations"
echo "4. Categorization of failures (Flaky vs Genuine)"
echo ""

# Display summary from log
echo "Failure Analysis Summary:"
echo "-------------------------"
grep -E "Failure Analysis for.*Category=|Flaky=|Score=" test-output.log | tail -10

echo ""
echo "Open the HTML report to see the full intelligent failure analysis!"