# CS Test Automation Framework

An enterprise-grade test automation framework built with Java 17, Selenium, and TestNG, designed for scalability, maintainability, and ease of use. This framework provides comprehensive testing capabilities without relying on third-party libraries for reporting, BDD, or API testing.

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Framework Components](#framework-components)
- [Usage Examples](#usage-examples)
- [Configuration](#configuration)
- [Reporting](#reporting)
- [Advanced Features](#advanced-features)
- [Best Practices](#best-practices)

## Features

### Core Features
- **Complete Selenium Abstraction**: Users never interact with WebDriver/WebElement directly
- **Thread-Safe Parallel Execution**: Built-in support for multi-threaded test execution
- **Annotation-Driven Architecture**: Simplified test creation with custom annotations
- **Native HTML Reporting**: Beautiful reports without third-party dependencies
- **BDD Support**: Cucumber-style testing without Cucumber
- **Data-Driven Testing**: Support for Excel, CSV, JSON, and Database data sources
- **API Testing**: Native HTTP client for REST and SOAP testing
- **Azure DevOps Integration**: Native integration without external libraries

### Advanced Features
- **Video Recording**: Automatic test execution recording
- **Screenshot Capture**: Automatic screenshots on failures
- **Object Repository**: JSON-based element definitions with hot-reload
- **SQL Query Manager**: Centralized query management
- **Event System**: Asynchronous event processing
- **Timeline Visualization**: Parallel execution visualization
- **Environment Collection**: Comprehensive system information gathering

## Architecture

```
cs-framework/
├── src/main/java/com/testforge/cs/
│   ├── annotations/        # Custom annotations (@CSTest, @CSPage, etc.)
│   ├── core/              # Core classes (CSBaseTest, CSBasePage)
│   ├── driver/            # WebDriver management
│   ├── elements/          # Element wrapper classes
│   ├── config/            # Configuration management
│   ├── utils/             # Utility classes
│   ├── reporting/         # Report generation
│   ├── bdd/               # BDD implementation
│   ├── api/               # API testing components
│   ├── dataprovider/      # Data provider implementations
│   ├── events/            # Event management
│   ├── exceptions/        # Custom exceptions
│   └── ...
└── src/test/java/
    └── examples/          # Example tests and pages
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- IDE with Maven support (IntelliJ IDEA, Eclipse)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd cs-framework
```

2. Build the project:
```bash
mvn clean install
```

3. Run example tests:
```bash
mvn test
```

## Framework Components

### 1. Page Objects

Create page objects by extending `CSBasePage` and using `@CSPage` annotation:

```java
@CSPage(name = "LoginPage", url = "${app.url}/login")
public class LoginPage extends CSBasePage {
    
    @CSLocator(
        value = "username",
        type = CSLocator.LocatorType.ID,
        name = "Username field"
    )
    public CSElement usernameField;
    
    @CSLocator(
        value = "password",
        type = CSLocator.LocatorType.ID,
        name = "Password field"
    )
    public CSElement passwordField;
    
    @CSLocator(
        value = "button[type='submit']",
        type = CSLocator.LocatorType.CSS,
        name = "Login button"
    )
    public CSElement loginButton;
    
    public void login(String username, String password) {
        usernameField.clearAndType(username);
        passwordField.clearAndType(password);
        loginButton.click();
        waitForPageLoad();
    }
}
```

### 2. Test Classes

Create test classes by extending `CSBaseTest`:

```java
@CSTest(
    name = "Login Test Suite",
    description = "Tests for login functionality",
    category = "Smoke",
    tags = {"login", "authentication"}
)
public class LoginTest extends CSBaseTest {
    
    private LoginPage loginPage;
    
    @CSBeforeMethod
    public void setupPages() {
        loginPage = new LoginPage();
    }
    
    @Test(description = "Test successful login")
    @CSRetry(maxAttempts = 2, delay = 1000)
    public void testSuccessfulLogin() {
        loginPage.navigateTo();
        loginPage.login("testuser", "password123");
        
        Assert.assertTrue(homePage.isUserLoggedIn(), 
            "User should be logged in");
    }
}
```

### 3. Data-Driven Testing

Use `@CSDataSource` annotation for data-driven tests:

```java
@Test(
    description = "Test with Excel data",
    dataProvider = "excelData",
    dataProviderClass = CSDataProvider.class
)
@CSDataSource(
    type = CSDataSource.Type.EXCEL,
    path = "src/test/resources/data/test_data.xlsx",
    sheet = "LoginData",
    keyField = "TestCase"
)
public void testWithExcelData(Map<String, String> testData) {
    String username = testData.get("Username");
    String password = testData.get("Password");
    
    loginPage.login(username, password);
    // Assertions...
}
```

### 4. BDD Testing

Create BDD-style tests without Cucumber:

```java
@CSFeature(
    name = "User Authentication",
    description = "As a user, I want to login to the application"
)
public class BDDLoginTest extends CSBaseTest {
    
    @CSBeforeFeature
    public void setupFeature() {
        CSStepRegistry registry = CSStepRegistry.getInstance();
        
        registry.registerStep("I am on the login page", () -> {
            loginPage.navigateTo();
        });
        
        registry.registerStep("I enter username {string} and password {string}", 
            (String username, String password) -> {
                loginPage.usernameField.clearAndType(username);
                loginPage.passwordField.clearAndType(password);
            });
    }
    
    @Test
    @CSScenario(
        name = "Successful login",
        steps = {
            @CSStep("Given I am on the login page"),
            @CSStep("When I enter username \"testuser\" and password \"pass123\""),
            @CSStep("Then I should be logged in successfully")
        }
    )
    public void testSuccessfulLogin() {
        // Steps are executed automatically
    }
}
```

### 5. API Testing

Test REST and SOAP APIs using native HTTP client:

```java
public class APITest extends CSBaseTest {
    
    private CSHttpClient httpClient;
    private CSSoapClient soapClient;
    
    @Test(description = "Test REST API")
    public void testRestApi() {
        CSHttpResponse response = httpClient.get(endpoint)
            .header("Authorization", "Bearer token")
            .queryParam("page", "1")
            .execute();
        
        Assert.assertEquals(response.getStatusCode(), 200);
        
        Map<String, Object> body = CSJsonUtils.jsonToMap(response.getBody());
        Assert.assertNotNull(body.get("data"));
    }
    
    @Test(description = "Test SOAP API")
    public void testSoapApi() {
        String soapRequest = "<soap:Envelope>...</soap:Envelope>";
        
        CSSoapResponse response = soapClient.sendRequest(
            endpoint, soapAction, soapRequest);
        
        Assert.assertFalse(response.hasFault());
        String value = response.getNodeValue("//Result");
    }
}
```

## Configuration

### 1. Framework Configuration

Create `config.properties` in `src/test/resources`:

```properties
# Browser configuration
browser.type=chrome
browser.headless=false
browser.implicit.wait=10
browser.page.load.timeout=30

# Application configuration
app.url=https://example.com
api.base.url=https://api.example.com

# Test configuration
test.environment=QA
test.parallel.enabled=true
test.thread.count=5

# Reporting
report.dir=target/test-reports
report.screenshots=true
report.video.recording=false

# Database configuration
db.default.url=jdbc:mysql://localhost:3306/testdb
db.default.username=testuser
db.default.password=password
```

### 2. Environment-Specific Configuration

Create environment-specific files:
- `config-dev.properties`
- `config-qa.properties`
- `config-prod.properties`

Run with specific environment:
```bash
mvn test -Dtest.environment=qa
```

### 3. Object Repository

Define elements in `object-repository.json`:

```json
{
  "pages": {
    "LoginPage": {
      "elements": {
        "username": {
          "locator": "id=username",
          "alternativeLocators": ["css=#username", "name=user"],
          "description": "Username input field"
        },
        "password": {
          "locator": "id=password",
          "description": "Password input field"
        }
      }
    }
  }
}
```

## Reporting

### HTML Reports

The framework generates comprehensive HTML reports with:
- Test execution overview
- Pass/fail statistics with charts
- Detailed test logs
- Screenshots and videos
- Performance metrics
- Timeline visualization

Reports are generated at: `target/test-reports/index.html`

### Report Features
- Interactive charts using Canvas API
- Responsive design
- Search and filter capabilities
- Export to PDF functionality
- Real-time updates during execution

## Advanced Features

### 1. Parallel Execution

Configure parallel execution in `testng.xml`:

```xml
<suite name="Test Suite" parallel="methods" thread-count="5">
    <test name="Parallel Tests">
        <classes>
            <class name="com.example.tests.LoginTest"/>
            <class name="com.example.tests.SearchTest"/>
        </classes>
    </test>
</suite>
```

### 2. Custom Wait Conditions

```java
// Wait for custom condition
waitUtils.waitForCondition(driver -> 
    element.getText().equals("Ready"),
    30, "Element should show 'Ready'"
);

// Wait for JavaScript
csDriver.waitForJQuery();
csDriver.waitForAngular();
```

### 3. Event Listeners

```java
CSEventManager.getInstance().addEventListener(new CSEventListener() {
    @Override
    public void onEvent(EventType eventType, Object eventData) {
        if (eventType == EventType.TEST_FAILED) {
            // Custom action on test failure
        }
    }
});
```

### 4. Performance Testing

```java
@Test
public void testPerformance() {
    long startTime = System.currentTimeMillis();
    
    // Test actions...
    
    long duration = System.currentTimeMillis() - startTime;
    reportManager.addPerformanceMetric("loginTime", duration);
    
    Assert.assertTrue(duration < 3000, 
        "Login should complete within 3 seconds");
}
```

## Best Practices

### 1. Page Object Guidelines
- Keep page objects focused on element definitions and basic interactions
- Use descriptive names for elements
- Implement wait strategies in page methods
- Avoid assertions in page objects

### 2. Test Design
- Follow AAA pattern (Arrange, Act, Assert)
- Use meaningful test names and descriptions
- Keep tests independent and atomic
- Use data-driven approach for similar scenarios

### 3. Framework Usage
- Leverage custom annotations for metadata
- Use configuration files for environment-specific data
- Implement proper error handling
- Take advantage of built-in retry mechanisms

### 4. Maintenance
- Regularly update object repository
- Keep test data organized
- Monitor test execution trends
- Clean up old reports and recordings

## Troubleshooting

### Common Issues

1. **WebDriver not found**
   - Ensure drivers are in PATH or use WebDriverManager
   - Check browser version compatibility

2. **Parallel execution failures**
   - Verify thread-safe implementation
   - Check resource conflicts
   - Review CSThreadContext usage

3. **Report generation issues**
   - Check write permissions
   - Verify report directory exists
   - Review log files for errors

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add/update tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions and support:
- Create an issue in the repository
- Contact the framework team
- Check the documentation wiki

---

Built with ❤️ by the TestForge Team