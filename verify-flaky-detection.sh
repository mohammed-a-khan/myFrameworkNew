#!/bin/bash

echo "Verifying Flaky Test Detection"
echo "==============================="
echo ""

# Create a test that will have different types of failures
cat > src/test/java/com/testforge/cs/tests/FlakyTestDemo.java << 'EOF'
package com.testforge.cs.tests;

import com.testforge.cs.core.CSBaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.time.Duration;

public class FlakyTestDemo extends CSBaseTest {
    
    @Test(description = "Flaky - Element Not Found")
    public void testElementNotFound() {
        driver.get("https://www.google.com");
        // This will fail with NoSuchElementException - should be categorized as FLAKY
        driver.findElement(By.id("non-existent-element-12345")).click();
    }
    
    @Test(description = "Flaky - Timeout")  
    public void testTimeout() {
        driver.get("https://www.google.com");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
        // This will timeout - should be categorized as FLAKY
        wait.until(d -> d.findElement(By.id("never-appears")));
    }
    
    @Test(description = "Genuine - Assertion Failure")
    public void testAssertionFailure() {
        driver.get("https://www.google.com");
        // This is a genuine assertion failure - NOT flaky
        Assert.assertEquals("Wrong Title", driver.getTitle(), "Title mismatch");
    }
}
EOF

echo "Created FlakyTestDemo.java with different failure types"
echo ""

# Create suite file
cat > suites/flaky-test-suite.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Flaky Test Detection Suite" parallel="none">
    <parameter name="browser.name" value="chrome"/>
    <parameter name="cs.browser.headless" value="true"/>
    
    <test name="Flaky Test Detection">
        <classes>
            <class name="com.testforge.cs.tests.FlakyTestDemo"/>
        </classes>
    </test>
</suite>
EOF

echo "Compiling tests..."
mvn compile test-compile 2>&1 | grep -E "SUCCESS|FAILURE" | tail -1

echo ""
echo "Running tests with failure analysis..."
echo "--------------------------------------"
mvn test -Dsurefire.suiteXmlFiles=suites/flaky-test-suite.xml 2>&1 | tee flaky-test.log | grep -E "Failure Analysis|Category=|Flaky=|Score=" 

echo ""
echo "Checking Test Results..."
echo "------------------------"

# Count flaky vs genuine
FLAKY_COUNT=$(grep -c "Flaky=true" flaky-test.log)
GENUINE_COUNT=$(grep -c "Flaky=false" flaky-test.log)

echo "Flaky failures detected: $FLAKY_COUNT"
echo "Genuine failures detected: $GENUINE_COUNT"

# Check the HTML report
REPORT_DIR=$(ls -dt cs-reports/test-run-* 2>/dev/null | head -1)
if [ -d "$REPORT_DIR" ]; then
    echo ""
    echo "Checking HTML Report Metrics..."
    echo "-------------------------------"
    
    # Extract metrics from HTML
    FLAKY_IN_HTML=$(grep -oE ">Flaky Tests</.*?metric-value\">[0-9]+" "$REPORT_DIR/cs_test_run_report.html" | grep -oE "[0-9]+$")
    BROKEN_IN_HTML=$(grep -oE ">Broken Tests</.*?metric-value\">[0-9]+" "$REPORT_DIR/cs_test_run_report.html" | grep -oE "[0-9]+$")
    
    echo "Flaky Tests shown in HTML: ${FLAKY_IN_HTML:-0}"
    echo "Broken Tests shown in HTML: ${BROKEN_IN_HTML:-0}"
    
    # Check if numbers match
    if [ "$FLAKY_COUNT" -eq "${FLAKY_IN_HTML:-0}" ]; then
        echo "✅ Flaky test count matches!"
    else
        echo "❌ Mismatch: Log shows $FLAKY_COUNT flaky, HTML shows ${FLAKY_IN_HTML:-0}"
    fi
fi

echo ""
echo "Report available at: $REPORT_DIR/cs_test_run_report.html"