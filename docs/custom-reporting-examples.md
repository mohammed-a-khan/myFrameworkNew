# Custom Reporting Examples - CS TestForge Framework

This document provides practical examples of how to use custom reporting in your tests.

## Example 1: Basic Reporting in Test Classes

```java
import com.testforge.cs.core.CSBaseTest;

public class MyTest extends CSBaseTest {
    @Test
    public void testWithReporting() {
        // Basic logging
        reportManager.logInfo("Starting test execution");
        reportManager.logWarning("Using test environment");
        reportManager.logError("Connection failed (simulated)");
        
        // Your test logic here
        // ...
    }
}
```

## Example 2: Using CSReportingUtils

```java
import com.testforge.cs.utils.CSReportingUtils;

public class MyTest extends CSBaseTest {
    @Test
    public void testWithUtils() {
        // Test boundaries
        CSReportingUtils.logTestStart("User Login Test");
        
        // Step logging
        CSReportingUtils.logStep(1, "Navigate to login page");
        CSReportingUtils.logStep(2, "Enter credentials");
        CSReportingUtils.logStep(3, "Click login button");
        
        // Pass/Fail logging
        CSReportingUtils.logPass("Login successful");
        CSReportingUtils.logFail("Validation failed");
        CSReportingUtils.logWarning("Slow response time");
        
        // Performance logging
        CSReportingUtils.logPerformance("Page Load", 1500);
        
        // Test end
        CSReportingUtils.logTestEnd("User Login Test", true);
    }
}
```

## Example 3: BDD Test with Logging

```gherkin
Feature: Login with reporting

  Scenario: Login with detailed logging
    Given I log "═══════════════════════════════════"
    And I log "Starting login test"
    And I log "Test Time: 2025-08-07 18:00:00"
    And I log "═══════════════════════════════════"
    
    When I log "Step 1: Navigate to login"
    And I am on the login page
    Then I log pass "Login page loaded"
    
    When I log "Step 2: Enter credentials"
    And I enter username "Admin" and password "admin123"
    Then I log pass "Credentials entered"
    
    When I click the login button
    Then I should see the dashboard
    And I log pass "Login successful"
    
    And I log "═══════════════════════════════════"
    And I log "Test completed successfully"
    And I log "═══════════════════════════════════"
```

## Example 4: Page Object with Logging

```java
public class LoginPage extends CSBasePage {
    
    public void login(String username, String password) {
        logger.info("Attempting login with username: " + username);
        
        // Enter username
        usernameField.sendKeys(username);
        logger.info("Username entered");
        
        // Enter password
        passwordField.sendKeys(password);
        logger.info("Password entered");
        
        // Click login
        loginButton.click();
        logger.info("Login button clicked");
        
        // Check result
        if (isErrorDisplayed()) {
            logger.error("Login failed - error message displayed");
        } else {
            logger.info("Login successful");
        }
    }
}
```

## Example 5: Data-Driven Test with Logging

```java
@Test(dataProvider = "loginData")
public void testMultipleLogins(String username, String password, String expected) {
    // Log test data
    reportManager.logInfo("═══════════════════════════════════");
    reportManager.logInfo("Test Data Set:");
    reportManager.logInfo("  Username: " + username);
    reportManager.logInfo("  Password: " + (password.isEmpty() ? "<empty>" : "***"));
    reportManager.logInfo("  Expected: " + expected);
    reportManager.logInfo("═══════════════════════════════════");
    
    // Perform test
    LoginPage loginPage = new LoginPage();
    loginPage.navigateTo();
    loginPage.login(username, password);
    
    // Log result
    if ("success".equals(expected)) {
        if (dashboardPage.isDisplayed()) {
            reportManager.logInfo("✅ PASS: Login successful as expected");
        } else {
            reportManager.logError("❌ FAIL: Expected success but login failed");
        }
    } else {
        if (loginPage.isErrorDisplayed()) {
            reportManager.logInfo("✅ PASS: Login failed as expected");
        } else {
            reportManager.logError("❌ FAIL: Expected failure but login succeeded");
        }
    }
}
```

## Example 6: Performance Tracking

```gherkin
Scenario: Login with performance metrics
    Given I log "[PERFORMANCE TEST] Starting"
    And I record the start time
    
    When I am on the login page
    And I record the page load time
    And I log "[METRIC] Page load completed"
    
    When I login with username "Admin" and password "admin123"
    And I wait for dashboard to load
    Then I record the total execution time
    
    And I log "[PERFORMANCE SUMMARY]"
    And I log "Test completed within acceptable limits"
```

## Example 7: Structured Logging

```java
public void testWithStructuredLogs() {
    // Configuration
    reportManager.logInfo("[CONFIG] Browser: " + config.getProperty("browser.name"));
    reportManager.logInfo("[CONFIG] Environment: " + config.getProperty("environment.name"));
    
    // Actions
    reportManager.logInfo("[ACTION] Navigate to application");
    reportManager.logInfo("[RESULT] SUCCESS - Page loaded");
    
    // Validations
    reportManager.logInfo("[VALIDATE] Check page title");
    reportManager.logInfo("[EXPECTED] Welcome Page");
    reportManager.logInfo("[ACTUAL] Welcome Page");
    reportManager.logInfo("[RESULT] PASS");
    
    // Summary
    reportManager.logInfo("[SUMMARY] All validations passed");
}
```

## Example 8: Error Handling with Logging

```java
@Test
public void testWithErrorHandling() {
    try {
        reportManager.logInfo("Attempting risky operation");
        
        // Risky operation
        performRiskyOperation();
        
        reportManager.logInfo("✅ Operation completed successfully");
        
    } catch (Exception e) {
        reportManager.logError("❌ Operation failed: " + e.getMessage());
        reportManager.logError("Stack trace: " + Arrays.toString(e.getStackTrace()));
        
        // Take screenshot on failure
        captureScreenshot("error_screenshot");
        reportManager.logInfo("Screenshot captured: error_screenshot.png");
        
        throw e; // Re-throw to fail the test
    }
}
```

## Best Practices Summary

1. **Use Clear Section Headers**
   - Start and end major test sections with visual separators
   - Use consistent formatting for readability

2. **Log at Appropriate Levels**
   - INFO: Normal flow, successful operations
   - WARN: Non-critical issues, using defaults
   - ERROR: Test failures, critical problems

3. **Include Context**
   - Always include relevant data in log messages
   - Use structured formats for easy parsing

4. **Be Concise but Descriptive**
   - Avoid overly verbose messages
   - Include enough detail to understand what happened

5. **Use Visual Indicators**
   - ✅ for success/pass
   - ❌ for failure
   - ⚠️ for warnings
   - ℹ️ for information
   - ⏱️ for performance metrics

## Viewing the Reports

After running tests, open the HTML report:
```
cs-reports/latest-report.html
```

Your custom log messages will appear in:
- Test execution timeline
- Test details section
- Step execution details (for BDD tests)