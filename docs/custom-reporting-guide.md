# Custom Reporting Guide for CS TestForge Framework

This guide explains how to add custom reporting statements (pass, fail, info, warn) to your tests that will appear in the HTML reports.

## Overview

The CS TestForge Framework provides multiple ways to add custom log messages and reporting statements that will be visible in the generated HTML reports. These messages help track test execution flow, debug issues, and provide meaningful information in test reports.

## 1. Using CSReportManager (Available in All Test Classes)

When extending `CSBaseTest` or using BDD tests, you have access to `reportManager` instance:

```java
// In your test class that extends CSBaseTest
public class MyTest extends CSBaseTest {
    
    @Test
    public void myTestMethod() {
        // Log different types of messages
        reportManager.logInfo("Starting login process");
        reportManager.logWarning("Using deprecated API endpoint");
        reportManager.logError("Failed to connect to database");
        
        // Custom messages with test context
        reportManager.logInfo("Test data: " + testData.toString());
    }
}
```

## 2. In Page Objects (Extending CSBasePage)

Page objects have built-in logger that integrates with reports:

```java
@CSPage(name = "My Custom Page")
public class MyPage extends CSBasePage {
    
    public void performAction() {
        logger.info("Clicking submit button");
        submitButton.click();
        
        logger.warn("Response time exceeded threshold");
        
        if (errorOccurred) {
            logger.error("Operation failed: " + errorMessage);
        }
    }
}
```

## 3. In BDD Step Definitions

### Using Built-in Logging Steps

The framework provides ready-to-use logging steps:

```gherkin
Feature: My Feature
  
  Scenario: Test with custom logging
    Given I log "Starting test execution"
    When I perform some action
    And I log "Action completed successfully"
    Then I verify the result
    And I log "Test completed"
```

### In Step Definition Classes

```java
public class MySteps extends CSStepDefinitions {
    
    @CSStep(description = "I perform a custom action")
    public void performCustomAction() {
        // Use logger for different message types
        logger.info("PASS: Successfully validated user input");
        logger.warn("WARN: Using fallback configuration");
        logger.error("FAIL: Expected element not found");
        
        // Access report manager
        CSReportManager.getInstance().logInfo("Custom validation completed");
    }
}
```

## 4. Adding Structured Test Data

For more structured reporting, add custom data to test results:

```java
// In test methods
@Test
public void testWithCustomData() {
    // Add metadata
    testResult.getMetadata().put("apiVersion", "v2.1");
    testResult.getMetadata().put("testEnvironment", "staging");
    
    // Add steps manually
    testResult.getSteps().add("Step 1: Login completed");
    testResult.getSteps().add("Step 2: Navigation successful");
    
    // Add attachments
    testResult.getAttachments().put("requestPayload", "{'user':'test'}");
    testResult.getAttachments().put("responsePayload", "{'status':'success'}");
}
```

## 5. Custom Assertions with Reporting

Create custom assertions that automatically log to reports:

```java
public class ReportingAssertions {
    
    public static void assertWithReport(boolean condition, String passMessage, String failMessage) {
        CSReportManager reporter = CSReportManager.getInstance();
        
        if (condition) {
            reporter.logInfo("✓ PASS: " + passMessage);
            Assert.assertTrue(true);
        } else {
            reporter.logError("✗ FAIL: " + failMessage);
            Assert.fail(failMessage);
        }
    }
    
    public static void verifyWithWarning(boolean condition, String warningMessage) {
        CSReportManager reporter = CSReportManager.getInstance();
        
        if (!condition) {
            reporter.logWarning("⚠ WARNING: " + warningMessage);
        }
    }
}
```

Usage:
```java
@Test
public void testWithCustomAssertions() {
    // Pass case
    ReportingAssertions.assertWithReport(
        element.isDisplayed(),
        "Login button is visible",
        "Login button not found"
    );
    
    // Warning case
    ReportingAssertions.verifyWithWarning(
        responseTime < 2000,
        "Response time exceeded 2 seconds"
    );
}
```

## 6. Test Execution Flow Logging

For detailed test flow tracking:

```java
public class TestFlowLogger {
    private static final CSReportManager reporter = CSReportManager.getInstance();
    
    public static void logTestStart(String testName) {
        reporter.logInfo("═══════════════════════════════════════");
        reporter.logInfo("TEST START: " + testName);
        reporter.logInfo("Time: " + LocalDateTime.now());
        reporter.logInfo("═══════════════════════════════════════");
    }
    
    public static void logTestStep(int stepNumber, String description) {
        reporter.logInfo(String.format("Step %d: %s", stepNumber, description));
    }
    
    public static void logTestEnd(String testName, boolean passed) {
        reporter.logInfo("═══════════════════════════════════════");
        reporter.logInfo("TEST END: " + testName);
        reporter.logInfo("Status: " + (passed ? "PASSED ✓" : "FAILED ✗"));
        reporter.logInfo("═══════════════════════════════════════");
    }
}
```

## 7. Data-Driven Test Logging

For data-driven tests, log test data clearly:

```java
@Test(dataProvider = "testData")
public void dataDriverTest(String username, String password, String expected) {
    CSReportManager reporter = CSReportManager.getInstance();
    
    // Log test data
    reporter.logInfo("Test Data:");
    reporter.logInfo("  Username: " + username);
    reporter.logInfo("  Password: " + (password.isEmpty() ? "<empty>" : "***"));
    reporter.logInfo("  Expected: " + expected);
    
    // Perform test
    // ...
}
```

## 8. Best Practices

1. **Use Meaningful Messages**: Make messages descriptive and actionable
   ```java
   // Good
   logger.info("Login successful for user: admin@test.com");
   
   // Bad
   logger.info("Success");
   ```

2. **Include Context**: Add relevant data to help debugging
   ```java
   logger.error("API call failed - Endpoint: " + endpoint + ", Status: " + statusCode);
   ```

3. **Use Appropriate Log Levels**:
   - `INFO`: Normal test flow, successful operations
   - `WARN`: Non-critical issues, fallback behavior
   - `ERROR`: Test failures, critical issues

4. **Structure Your Logs**: Use consistent formatting
   ```java
   logger.info("[VALIDATION] Email format check: PASSED");
   logger.info("[API] POST /users - Status: 201");
   logger.info("[DB] Query executed in 45ms");
   ```

5. **Log Test Boundaries**: Clearly mark test start/end
   ```java
   @BeforeMethod
   public void beforeMethod(Method method) {
       logger.info("▶▶▶ Starting Test: " + method.getName());
   }
   
   @AfterMethod
   public void afterMethod(Method method) {
       logger.info("◀◀◀ Completed Test: " + method.getName());
   }
   ```

## Example: Complete Test with Custom Reporting

```java
public class LoginTestWithReporting extends CSBaseTest {
    
    @Test
    public void testLoginWithDetailedReporting() {
        // Test start
        reportManager.logInfo("========================================");
        reportManager.logInfo("Test: Verify login with valid credentials");
        reportManager.logInfo("========================================");
        
        // Navigate to login
        reportManager.logInfo("Step 1: Navigate to login page");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.navigateTo();
        reportManager.logInfo("✓ Login page loaded successfully");
        
        // Enter credentials
        reportManager.logInfo("Step 2: Enter user credentials");
        String username = "admin@test.com";
        loginPage.enterUsername(username);
        loginPage.enterPassword("Admin123!");
        reportManager.logInfo("✓ Credentials entered for user: " + username);
        
        // Submit login
        reportManager.logInfo("Step 3: Submit login form");
        loginPage.clickLogin();
        
        // Verify login
        reportManager.logInfo("Step 4: Verify successful login");
        DashboardPage dashboard = new DashboardPage(driver);
        
        if (dashboard.isDisplayed()) {
            reportManager.logInfo("✓ PASS: Login successful - Dashboard displayed");
            reportManager.logInfo("✓ User role: " + dashboard.getUserRole());
            reportManager.logInfo("✓ Last login: " + dashboard.getLastLoginTime());
        } else {
            reportManager.logError("✗ FAIL: Login failed - Dashboard not displayed");
            reportManager.logError("Current URL: " + driver.getCurrentUrl());
            Assert.fail("Dashboard not displayed after login");
        }
        
        // Test completion
        reportManager.logInfo("========================================");
        reportManager.logInfo("Test completed successfully");
        reportManager.logInfo("========================================");
    }
}
```

## Viewing Reports

All custom log messages will appear in the generated HTML report under:
- Test Details section
- Execution Timeline
- Console Output (if enabled)

The messages are color-coded based on their type:
- INFO: Blue/Default
- WARN: Orange/Yellow
- ERROR: Red
- Custom PASS: Green
- Custom FAIL: Red

## Advanced: Creating Custom Report Sections

For specialized reporting needs, you can extend the reporting capability by adding custom sections to CSTestResult and updating the report generator to display them.