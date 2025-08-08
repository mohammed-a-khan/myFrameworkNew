# Custom Reporting in CS TestForge Framework

## Quick Start Guide

The CS TestForge Framework provides multiple ways to add custom reporting statements that appear in your HTML test reports.

## 1. In Your Test Classes (Extending CSBaseTest)

```java
public class MyTest extends CSBaseTest {
    @Test
    public void myTest() {
        // Use reportManager instance available in all tests
        reportManager.logInfo("Starting test execution");
        reportManager.logWarning("Using fallback configuration");
        reportManager.logError("Connection failed to service");
    }
}
```

## 2. In Your Page Objects

```java
public class MyPage extends CSBasePage {
    public void performAction() {
        // Use logger instance available in all pages
        logger.info("Clicking submit button");
        logger.warn("Response time exceeded threshold");
        logger.error("Element not found");
    }
}
```

## 3. In BDD Tests (Gherkin)

```gherkin
Scenario: Test with logging
    Given I log "Starting test"
    When I log pass "Login successful"
    And I log warning "Using test data"
    Then I log fail "Validation failed"
```

## 4. Using the Utility Class

```java
import com.testforge.cs.utils.CSReportingUtils;

// In your test
CSReportingUtils.logPass("Login successful");
CSReportingUtils.logFail("Validation failed");
CSReportingUtils.logWarning("Slow response time");
CSReportingUtils.logInfo("Test data loaded");

// Structured logging
CSReportingUtils.logTestStart("User Login Test");
CSReportingUtils.logStep(1, "Navigate to login page");
CSReportingUtils.logStep(2, "Enter credentials");
CSReportingUtils.logTestEnd("User Login Test", true);

// Performance logging
CSReportingUtils.logPerformance("Page Load", 1500);

// Validation logging
CSReportingUtils.logValidation("Email Format", "user@example.com", "user@example.com", true);
```

## 5. Available BDD Steps for Reporting

- `I log "{message}"` - General log message
- `I log pass "{message}"` - Success message with ✅
- `I log fail "{message}"` - Failure message with ❌
- `I log warning "{message}"` - Warning message with ⚠️
- `I record the start time` - Start performance timing
- `I record the page load time` - Log page load duration
- `I record the total execution time` - Log total execution time

## Examples

### Running the Demo
```bash
# Run reporting demo tests
mvn test -DsuiteXmlFile=suites/reporting-demo-suite.xml

# Run specific example test
mvn test -Dtest=ReportingExampleTest
```

### View Reports
Open the generated HTML report at:
```
cs-reports/latest-report.html
```

## Best Practices

1. **Use Descriptive Messages**
   ```java
   // Good
   reportManager.logInfo("Login successful for user: admin@test.com");
   
   // Bad
   reportManager.logInfo("Success");
   ```

2. **Include Context**
   ```java
   reportManager.logError("API call failed - Endpoint: /users, Status: 500");
   ```

3. **Use Appropriate Log Levels**
   - INFO: Normal flow, successful operations
   - WARN: Non-critical issues, fallbacks
   - ERROR: Test failures, critical issues

4. **Structure Your Output**
   ```java
   reportManager.logInfo("═══════════════════════════════");
   reportManager.logInfo("TEST: User Authentication");
   reportManager.logInfo("═══════════════════════════════");
   ```

## Report Output

All custom messages appear in:
- Test execution timeline
- Test details section
- Console output (if enabled)

Messages are color-coded:
- INFO: Blue
- WARN: Orange
- ERROR: Red
- PASS: Green (✅)
- FAIL: Red (❌)