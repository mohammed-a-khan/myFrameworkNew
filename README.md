# CS TestForge Selenium Framework

A comprehensive Selenium automation framework with native BDD support, Azure DevOps integration, and extensive reporting capabilities built on Java 17 and TestNG.

## 🚀 Latest Features (August 2024)

- **Azure DevOps Integration**: Full integration with Test Plans, Test Suites, and automatic result publishing with test point updates
- **Certificate Authentication**: Support for client certificates (PFX, P12, PEM) for secure API testing  
- **Native BDD Implementation**: Cucumber-style testing without Cucumber dependency using CSBDDRunner
- **Enhanced Reporting**: ExtentReports integration with screenshots, logs, and timeline visualization
- **Object Repository**: Centralized element management with hot-reload capability

## Table of Contents
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Page Object Model](#page-object-model)
- [BDD Testing](#bdd-testing)
- [Data-Driven Testing](#data-driven-testing)
- [API Testing & Certificate Authentication](#api-testing--certificate-authentication)
- [Azure DevOps Integration](#azure-devops-integration)
- [Test Suites](#test-suites)
- [Reporting](#reporting)
- [Configuration](#configuration)
- [Running Tests](#running-tests)

## Features

### Core Framework
- **CSBaseTest & CSBasePage**: Base classes providing common functionality
- **CSElement**: Enhanced WebElement wrapper with 50+ convenience methods including advanced typing and wait strategies
- **CSDriverFactory**: Automatic WebDriver management with WebDriverManager
- **CSThreadContext**: Thread-safe execution for parallel testing
- **Custom Annotations**: @CSTest, @CSPage, @CSLocator, @CSRetry, @CSDataSource, @CSStep

### Testing Capabilities
- **BDD Support**: Native implementation with @CSStep annotations and CSBDDRunner
- **Data Providers**: Excel, CSV, JSON, and Database support
- **Parallel Execution**: Thread-safe with configurable thread pools
- **Retry Mechanism**: Automatic retry with @CSRetry annotation
- **Screenshot/Video**: Automatic capture on failures

### Integrations
- **Azure DevOps**: Test Plans, Test Suites, Test Points with automatic result publishing
- **ExtentReports**: Rich HTML reports with charts and timeline
- **WebDriverManager**: Automatic driver downloads
- **Apache POI**: Excel data handling
- **Jackson**: JSON processing

## Project Structure

```
cs-framework/
├── src/
│   ├── main/java/com/testforge/cs/
│   │   ├── annotations/         # Custom annotations
│   │   ├── api/                # API testing & certificates
│   │   ├── azuredevops/        # Azure DevOps integration
│   │   ├── bdd/                # BDD implementation
│   │   ├── config/             # Configuration management
│   │   ├── core/               # Base classes
│   │   ├── dataprovider/       # Data providers
│   │   ├── driver/             # WebDriver management
│   │   ├── elements/           # Element wrappers
│   │   ├── listeners/          # TestNG listeners
│   │   ├── reporting/          # Report generation
│   │   ├── repository/         # Object repository
│   │   ├── security/           # Encryption utilities
│   │   └── utils/              # Utility classes
│   └── test/
│       ├── java/com/
│       │   ├── orangehrm/      # OrangeHRM test implementation
│       │   │   ├── pages/      # Page objects
│       │   │   ├── stepdefs/   # Step definitions
│       │   │   └── tests/      # Test classes
│       │   └── testforge/cs/
│       │       └── tests/api/  # API tests
│       └── resources/
│           ├── certificates/    # SSL certificates
│           ├── config/         # Configuration files
│           └── features/       # BDD feature files
├── features/                    # BDD feature files
├── suites/                     # TestNG suite files
└── object-repository/          # Element definitions
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Chrome/Firefox/Edge browser
- Azure DevOps account (optional)

### Installation

```bash
# Clone repository
git clone <repository-url>
cd cs-framework

# Install dependencies
mvn clean install -DskipTests

# Run sample test
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-simple-only.xml
```

## CSElement - Enhanced WebElement Wrapper

### Overview
CSElement is the core of the framework, providing an enhanced WebElement wrapper with 50+ convenience methods, advanced typing strategies, comprehensive wait methods, and built-in reporting.

### Key Features
- **Advanced Typing Methods**: Multiple strategies for handling problematic input fields
- **Comprehensive Wait Methods**: Extensive wait conditions with custom timeouts
- **Built-in Retry Logic**: Automatic retry on failures with configurable attempts
- **Screenshot Capture**: Automatic screenshots on failures
- **Cross-browser Event Support**: JavaScript event triggering compatible with legacy and modern browsers
- **Framework Integration**: Works seamlessly with React, Angular, Vue, and jQuery

### Enhanced Typing Methods

CSElement provides multiple typing strategies to handle different types of input fields:

```java
// Standard typing methods
element.type("text");                    // Basic typing
element.clearAndType("text");           // Clear then type

// For problematic React/Angular/Vue inputs
element.typeSlowly("text");             // Types character by character with delay
element.clearAndTypeSlowly("text");     // Clear then type slowly
element.typeSlowly("text", 150);        // Custom delay between characters

// For fields with complex validation or formatting
element.typeUsingJS("text");            // JavaScript typing with comprehensive events
element.clearAndTypeUsingJS("text");    // Clear and type using JavaScript

// Alternative approaches
element.clearAndTypeWithActions("text"); // Using Selenium Actions API
element.clearCompletely();              // Multi-method field clearing
```

### Comprehensive Wait Methods

CSElement provides extensive wait conditions with both default (30s) and custom timeouts:

```java
// Element presence
element.waitForPresent();               // Wait for element in DOM
element.waitForPresent(10);             // Custom timeout
element.waitForNotPresent();            // Wait for element to disappear

// Element state
element.waitForEnabled();               // Wait for element to be enabled
element.waitForEnabled(5);              // Custom timeout
element.waitForDisabled();              // Wait for element to be disabled
element.waitForSelected();              // Wait for checkbox/radio selection

// Element visibility
element.waitForVisible();               // Wait for element to be visible
element.waitForInvisible();             // Wait for element to be invisible
element.waitForClickable();             // Wait for element to be clickable

// Element content
element.waitForText("Success");         // Wait for specific text
element.waitForText("Success", 10);     // Custom timeout
element.waitForAttribute("class", "active"); // Wait for attribute value

// Method chaining
element.waitForPresent(10)
       .waitForVisible(5)
       .waitForEnabled(5)
       .clearAndTypeSlowly("text")
       .waitForAttribute("value", "text")
       .click();
```

### Boolean Methods (Exception-Safe)

All boolean methods return true/false instead of throwing exceptions:

```java
if (element.isDisplayed()) {            // Safe - returns false if not found
    element.click();
}

if (element.isEnabled()) {              // Safe - returns false if not found
    element.type("text");
}

if (element.isPresent()) {              // Safe - returns false if not found
    String text = element.getText();
}

if (element.exists()) {                 // Safe - returns false if not found
    element.waitForVisible();
}
```

### Direct Selenium Access

You can access the underlying WebElement when needed:

```java
CSElement element = page.findElement("id:username", "Username field");

// Direct Selenium access (loses framework benefits)
WebElement webElement = element.getElement();
webElement.sendKeys("text");

// Recommended: Use CSElement methods for full framework benefits
element.clearAndTypeSlowly("text");     // Includes retry, reporting, screenshots
```

### Exception Handling and Reporting

CSElement automatically handles exceptions and provides detailed reporting:

```java
// Automatic exception handling with retries
element.waitForPresent(10);             // Throws CSElementNotFoundException after timeout

// Detailed error messages
element.waitForText("Expected", 5);     // Shows actual vs expected text in error

// Automatic screenshot capture on failures
element.click();                        // Screenshots captured automatically on failure

// Built-in reporting integration
element.clearAndType("text");           // Automatically logged to reports
```

## Page Object Model

### Creating Page Objects

Pages extend `CSBasePage` and use `@CSLocator` annotations:

```java
package com.orangehrm.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;

@CSPage(name = "OrangeHRM Login Page")
public class LoginPageNew extends CSBasePage {
    
    // Using Object Repository
    @CSLocator(locatorKey = "login.username.field")
    private CSElement usernameField;
    
    @CSLocator(locatorKey = "login.password.field")
    private CSElement passwordField;
    
    @CSLocator(locatorKey = "login.submit.button")
    private CSElement loginButton;
    
    @CSLocator(locatorKey = "login.error.message")
    private CSElement errorMessage;
    
    private static final String LOGIN_PATH = "/web/index.php/auth/login";
    
    public void navigateTo() {
        String baseUrl = config.getProperty("app.base.url");
        navigateTo(baseUrl + LOGIN_PATH);
        waitForPageLoad();
    }
    
    public void enterUsername(String username) {
        logger.info("Entering username: {}", username);
        usernameField.waitForVisible()
                    .waitForEnabled(5)
                    .clearAndTypeSlowly(username);  // Uses slow typing for reliability
    }
    
    public void enterPassword(String password) {
        logger.info("Entering password");
        passwordField.waitForPresent()
                    .clearAndType(password);
    }
    
    public boolean isLoginButtonEnabled() {
        return loginButton.isEnabled();  // Safe boolean method
    }
    
    public void waitForLoginButtonToBeEnabled() {
        loginButton.waitForEnabled(10);  // Wait up to 10 seconds
    }
    
    public void clickLogin() {
        logger.info("Clicking login button");
        loginButton.click();
    }
    
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }
}
```

## BDD Testing

### Feature Files

Create feature files in `features/` directory:

```gherkin
@simple @quick
Feature: OrangeHRM Simple Tests
  Quick tests demonstrating clean step syntax

  @login-simple
  Scenario: Simple login test
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard

  @login-negative
  Scenario: Invalid login test
    Given I am on the login page
    When I login with username "invalid" and password "wrong"
    Then I should see an error message "Invalid credentials"

  @data-driven-simple
  Scenario Outline: Multiple login attempts
    Given I am on the login page
    When I login with username "<username>" and password "<password>"
    Then I should see an error message "<errorMessage>"
    
    Examples:
      | username  | password    | errorMessage         |
      | testuser  | wrongpass   | Invalid credentials  |
      | admin     | badpass     | Invalid credentials  |
      | ""        | ""          | Invalid credentials  |
```

### Step Definitions

Step definitions use `@CSStep` annotation:

```java
package com.orangehrm.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;

@CSFeature(name = "OrangeHRM Steps", tags = {"@orangehrm", "@all"})
public class OrangeHRMSteps extends CSStepDefinitions {
    
    private LoginPageNew loginPage;
    private DashboardPageNew dashboardPage;
    
    @CSStep(description = "I am on the login page")
    public void navigateToLoginPage() {
        logger.info("Navigating to login page");
        loginPage = getPage(LoginPageNew.class);
        loginPage.navigateTo();
    }
    
    @CSStep(description = "I enter username {username} and password {password}")
    public void enterCredentials(String username, String password) {
        logger.info("Entering credentials - Username: {}", username);
        loginPage = getPage(LoginPageNew.class);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }
    
    @CSStep(description = "I click the login button")
    public void clickLoginButton() {
        logger.info("Clicking login button");
        loginPage = getPage(LoginPageNew.class);
        loginPage.clickLogin();
    }
    
    @CSStep(description = "I should see the dashboard")
    public void verifyDashboard() {
        logger.info("Verifying dashboard is displayed");
        dashboardPage = getPage(DashboardPageNew.class);
        assertTrue(dashboardPage.isDisplayed(), "Dashboard should be displayed");
    }
    
    @CSStep(description = "I should see an error message {errorMessage}")
    public void verifyErrorMessage(String errorMessage) {
        logger.info("Verifying error message: {}", errorMessage);
        loginPage = getPage(LoginPageNew.class);
        String actualError = loginPage.getErrorMessage();
        assertTrue(actualError.contains(errorMessage), 
            "Expected error: " + errorMessage);
    }
}
```

## Data-Driven Testing

### Excel Data Provider

```java
@Test(dataProvider = "excelData", dataProviderClass = CSDataProvider.class)
@CSDataSource(
    type = CSDataSource.Type.EXCEL,
    path = "src/test/resources/data/testdata.xlsx",
    sheet = "LoginData"
)
public void testWithExcelData(Map<String, String> data) {
    loginPage.login(data.get("Username"), data.get("Password"));
    // Assertions
}
```

### JSON Data Provider

```java
@Test(dataProvider = "jsonData", dataProviderClass = CSDataProvider.class)
@CSDataSource(
    type = CSDataSource.Type.JSON,
    path = "src/test/resources/data/users.json"
)
public void testWithJsonData(Map<String, Object> userData) {
    // Use JSON data
}
```

## API Testing & Certificate Authentication

### Certificate Authentication Test

```java
package com.testforge.cs.tests.api;

import com.testforge.cs.api.CSCertificateManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class CSCertificateAuthenticationTest {
    
    private CSCertificateManager certificateManager;
    
    @Test
    public void testAPICallWithClientCertificate() {
        certificateManager = CSCertificateManager.getInstance();
        
        // Load certificate
        SSLContext sslContext = certificateManager.loadCertificate(
            "certificates/badssl.com-client.p12",
            "badssl.com"
        );
        
        // Create HTTP client with certificate
        SSLConnectionSocketFactory sslSocketFactory = 
            new SSLConnectionSocketFactory(sslContext);
        
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .build()) {
            
            HttpGet request = new HttpGet("https://client.badssl.com/");
            HttpResponse response = httpClient.execute(request);
            
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
        }
    }
}
```

## Azure DevOps Integration

### Configuration

1. Set environment variables:
```bash
export ADO_ORGANIZATION_URL=https://dev.azure.com/yourorg
export ADO_PROJECT_NAME=YourProject
export ADO_PAT_TOKEN_ENCRYPTED=<encrypted-token>
export CS_ENCRYPTION_KEY=<your-key>
```

2. Encrypt PAT token:
```bash
CS_ENCRYPTION_KEY="your-key" java -cp target/classes \
    com.testforge.cs.security.CSEncryptionUtils encrypt "your-pat-token"
```

### Test Mapping

Use annotations to map tests to Azure DevOps:

```java
@Test
@TestCaseId(419)  // Azure DevOps Test Case ID
public void testLogin() {
    // Test implementation
}
```

### BDD Suite for Azure DevOps

```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Azure DevOps BDD Test Suite" verbose="2" parallel="none">
    
    <parameter name="browserName" value="chrome"/>
    <parameter name="headless" value="false"/>
    <parameter name="featuresPath" value="features/ado-mapped-tests.feature"/>
    <parameter name="stepDefPackages" value="com.orangehrm.stepdefs"/>
    
    <test name="ADO BDD Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

## Test Suites

### Available Test Suites

The framework includes numerous pre-configured suites in the `suites/` directory:

#### OrangeHRM Test Suites
- `orangehrm-simple-only.xml` - Basic login tests
- `orangehrm-comprehensive-only.xml` - Full test coverage
- `orangehrm-parallel-test.xml` - Parallel execution demo
- `orangehrm-sequential-test.xml` - Sequential execution

#### Azure DevOps Suites  
- `ado-bdd-suite.xml` - BDD tests with ADO integration
- `ado-mapped-suite.xml` - Tests mapped to ADO test cases
- `ado-hierarchy-example.xml` - Hierarchical test organization

#### Feature-Specific Suites
- `certificate-auth-suite.xml` - Certificate authentication tests
- `reporting-demo-suite.xml` - Reporting features demo
- `simple-login-test.xml` - Minimal login test

### Example Suite Configuration

```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="OrangeHRM Comprehensive Test Suite" verbose="1">
    
    <parameter name="browser.name" value="chrome"/>
    <parameter name="browser.headless" value="true"/>
    <parameter name="environment.name" value="qa"/>
    
    <!-- BDD feature configuration -->
    <parameter name="cs.feature.path" 
               value="features/orangehrm-comprehensive-tests.feature"/>
    <parameter name="cs.step.packages" value="com.orangehrm.stepdefs"/>
    
    <test name="OrangeHRM Comprehensive Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

## Reporting

### ExtentReports Integration

The framework generates comprehensive HTML reports with:
- Test execution summary with pass/fail charts
- Detailed step-by-step logs
- Screenshots on failures
- System and environment information
- Execution timeline

Reports are generated at: `target/cs-reports/`

### Custom Reporting in Tests

```java
public class ReportingExampleTest extends CSBaseTest {
    
    @Test(description = "Demo test showing reporting features")
    public void testWithCustomReporting() {
        // Logging with different levels
        reportManager.logInfo("═══════════════════════════════════");
        reportManager.logInfo("TEST: Custom Reporting Demo");
        reportManager.logInfo("Started at: " + LocalDateTime.now());
        reportManager.logInfo("═══════════════════════════════════");
        
        // Step logging
        reportManager.logInfo("STEP 1: Navigate to login page");
        LoginPageNew loginPage = new LoginPageNew();
        loginPage.navigateTo();
        
        // Conditional logging
        if (loginPage.isDisplayed()) {
            reportManager.logInfo("✓ PASS: Login page loaded");
        } else {
            reportManager.logError("✗ FAIL: Login page failed to load");
        }
        
        // Screenshot capture
        reportManager.captureScreenshot("login-page");
    }
}
```

## Configuration

### config.properties

```properties
# Application settings
app.base.url=https://opensource-demo.orangehrmlive.com

# Browser configuration
browser.name=chrome
browser.headless=false
browser.window.maximize=true
browser.implicit.wait=10
browser.page.load.timeout=30

# Test execution
test.retry.count=2
test.parallel.enabled=true
test.thread.count=3
screenshot.on.failure=true

# Reporting
report.dir=target/cs-reports
report.name=Test Execution Report
extent.report.enabled=true

# Azure DevOps (optional)
ado.enabled=true
ado.organization.url=https://dev.azure.com/yourorg
ado.project.name=YourProject
ado.test.plan.id=46
ado.test.suite.id=47

# Encryption
cs.encryption.key=${CS_ENCRYPTION_KEY}
```

### Object Repository (object-repository.json)

```json
{
  "login.username.field": {
    "locator": "//input[@name='username']",
    "type": "xpath"
  },
  "login.password.field": {
    "locator": "//input[@name='password']",
    "type": "xpath"
  },
  "login.submit.button": {
    "locator": "//button[@type='submit']",
    "type": "xpath"
  },
  "login.error.message": {
    "locator": "//p[contains(@class,'alert')]",
    "type": "xpath"
  }
}
```

## Running Tests

### Maven Commands

```bash
# Run specific suite
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-simple-only.xml

# Run with browser options
mvn test -Dbrowser.name=chrome -Dbrowser.headless=true

# Run BDD tests
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-comprehensive-only.xml

# Run with Azure DevOps integration
CS_ENCRYPTION_KEY="your-key" mvn test \
    -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml

# Run parallel tests
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-parallel-test.xml

# Run certificate authentication tests
mvn test -Dtest=CSCertificateAuthenticationTest
```

### Direct TestNG Execution

```bash
# Using TestNG directly
java -cp "target/classes:target/test-classes:target/dependency/*" \
     org.testng.TestNG suites/orangehrm-simple-only.xml

# With environment variables for ADO
export CS_ENCRYPTION_KEY="your-key"
java -cp "target/classes:target/test-classes:target/dependency/*" \
     -Dcs.encryption.key="$CS_ENCRYPTION_KEY" \
     org.testng.TestNG suites/ado-bdd-suite.xml
```

## Best Practices

1. **Page Objects**: Keep page classes focused on element interactions
2. **Step Definitions**: Use descriptive step patterns with placeholders
3. **Data Management**: Externalize test data in Excel/JSON files
4. **Object Repository**: Centralize element locators for maintainability
5. **Parallel Execution**: Design tests to be thread-safe
6. **Reporting**: Use meaningful log messages and capture screenshots
7. **Azure DevOps**: Map test cases using @TestCaseId annotations

## Troubleshooting

### Common Issues

1. **WebDriver Issues**
   - WebDriverManager automatically downloads drivers
   - Check browser version compatibility
   - Verify PATH settings

2. **Input Field Issues**
   - If only last character is typed, use `element.clearAndTypeSlowly("text")`
   - For React/Angular/Vue fields, try `element.typeUsingJS("text")`
   - If element.type() fails, use `element.clearAndTypeWithActions("text")`
   - For stubborn fields, use `element.clearCompletely()` then type

3. **Element Wait Issues**
   - Use `element.waitForPresent(timeout)` before interacting
   - For button enabling after form validation, use `element.waitForEnabled(timeout)`
   - For dynamic content, use `element.waitForText("expected text", timeout)`
   - Chain waits: `element.waitForPresent().waitForVisible().click()`

4. **Boolean Method Issues**
   - All boolean methods (isDisplayed, isEnabled, etc.) return false instead of throwing exceptions
   - Use `if (element.isPresent())` instead of try-catch blocks
   - Boolean methods are safe to use in conditions without exception handling

5. **Azure DevOps Integration**
   - Ensure PAT token has correct permissions
   - Verify test case IDs exist in ADO
   - Check network connectivity

6. **Certificate Authentication**
   - Verify certificate files exist in `src/test/resources/certificates/`
   - Check certificate password is correct
   - Ensure certificate is not expired

7. **BDD Tests**
   - Verify feature files exist in specified path
   - Check step definitions package is correct
   - Ensure CSBDDRunner is used in suite file

### CSElement Method Selection Guide

| Problem | Recommended Solution |
|---------|---------------------|
| Only last character typed | `element.clearAndTypeSlowly("text")` |
| Input validation prevents typing | `element.typeUsingJS("text")` |
| Button doesn't enable after form fill | `element.waitForEnabled(timeout)` |
| Element not found exceptions | Use `element.isPresent()` first |
| React/Angular controlled inputs | `element.clearAndTypeSlowly("text")` |
| Field won't clear properly | `element.clearCompletely()` |
| Element appears after delay | `element.waitForPresent(timeout)` |
| Text appears dynamically | `element.waitForText("expected", timeout)` |

## Support

For issues and support:
- Review test examples in `src/test/java/com/orangehrm/`
- Check suite configurations in `suites/` directory
- Review feature files in `features/` directory

---

**Version**: 1.0.0  
**Last Updated**: August 2024  
**Framework**: CS TestForge