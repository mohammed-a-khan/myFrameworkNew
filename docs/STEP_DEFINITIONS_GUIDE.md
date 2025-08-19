# CS TestForge Framework - Step Definitions Guide

## Creating Custom Step Definition Classes

When creating step definition classes for the CS TestForge Framework, follow these guidelines to ensure proper registration and execution.

## Basic Structure

### Option 1: Extend CSStepDefinitions (Recommended)
```java
package com.cct.automation.crru.stepdefs;

import com.testforge.cs.stepdefs.CSStepDefinitions;
import com.testforge.cs.annotations.CSStep;

public class CRRUStepDefinitions extends CSStepDefinitions {
    
    // No-argument constructor (optional - framework will handle it)
    public CRRUStepDefinitions() {
        super();
    }
    
    @CSStep("User navigates to CRRU application")
    public void navigateToCRRU() {
        driver.navigate("https://crru.example.com");
    }
    
    @CSStep("User enters CRRU username {string}")
    public void enterUsername(String username) {
        driver.findElement("@crru.login.username").sendKeys(username);
    }
}
```

### Option 2: Standalone Step Definition Class
```java
package com.cct.automation.crru.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.driver.CSWebDriverManager;

public class CRRUStepDefinitions {
    
    // REQUIRED: No-argument constructor
    public CRRUStepDefinitions() {
        // Initialize any required fields here
    }
    
    @CSStep("User performs CRRU action")
    public void performAction() {
        // Access driver through CSWebDriverManager
        WebDriver driver = CSWebDriverManager.getDriver();
        // Perform actions
    }
}
```

## Configuration in Suite XML

### Single Package Configuration
```xml
<suite name="CRRU Test Suite">
    <parameter name="cs.bdd.stepdefs.packages" value="com.cct.automation.crru.stepdefs"/>
    
    <test name="CRRU Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

### Multiple Package Configuration
```xml
<suite name="Multi-Package Suite">
    <parameter name="cs.bdd.stepdefs.packages" value="com.cct.automation.crru.stepdefs,com.orangehrm.stepdefs,com.akhan.stepdefs"/>
    
    <test name="Combined Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

## Common Issues and Solutions

### Issue 1: "Failed to instantiate step class"
**Cause**: The step definition class doesn't have a no-argument constructor.

**Solution**: 
- Add a public no-argument constructor to your class
- OR extend CSStepDefinitions which handles initialization

### Issue 2: "No matching step definition found"
**Cause**: Step definitions not being scanned or registered.

**Solution**:
1. Verify package name in suite XML matches your actual package structure
2. Ensure @CSStep annotations are present
3. Check that classes are compiled and in classpath

### Issue 3: Class Not Found
**Cause**: Package or class not in classpath.

**Solution**:
1. Verify the class is compiled: `target/test-classes/com/cct/automation/crru/stepdefs/`
2. Check Maven build: `mvn clean compile test-compile`
3. Ensure package structure matches declaration

## Best Practices

### 1. Package Organization
```
src/test/java/
└── com/
    └── cct/
        └── automation/
            └── crru/
                ├── stepdefs/
                │   ├── CRRULoginSteps.java
                │   ├── CRRUNavigationSteps.java
                │   └── CRRUValidationSteps.java
                ├── pages/
                │   └── CRRUPages.java
                └── utils/
                    └── CRRUHelpers.java
```

### 2. Step Definition Patterns
```java
// Use descriptive patterns
@CSStep("User logs into CRRU with username {string} and password {string}")
public void loginToCRRU(String username, String password) {
    // Implementation
}

// Support data tables
@CSStep("User enters CRRU details:")
public void enterDetails(Map<String, String> details) {
    // Implementation
}

// Use parameters effectively
@CSStep("User selects CRRU option {int} from dropdown {string}")
public void selectOption(int index, String dropdown) {
    // Implementation
}
```

### 3. Inheritance for Shared Functionality
```java
// Base class for common CRRU functionality
public abstract class CRRUBaseSteps extends CSStepDefinitions {
    
    protected void waitForCRRUPage() {
        // Common wait logic
    }
    
    protected void validateCRRUSession() {
        // Common validation
    }
}

// Specific step definitions
public class CRRULoginSteps extends CRRUBaseSteps {
    
    @CSStep("User performs CRRU login")
    public void performLogin() {
        waitForCRRUPage();
        // Login logic
        validateCRRUSession();
    }
}
```

## Debugging Step Registration

### Enable Debug Logging
```xml
<!-- In your logback.xml or log4j2.xml -->
<logger name="com.testforge.cs.bdd.CSStepRegistry" level="DEBUG"/>
<logger name="com.testforge.cs.bdd.CSBDDRunner" level="DEBUG"/>
```

### Verify Registration at Runtime
```java
// In your test setup
@BeforeClass
public void verifyStepRegistration() {
    CSStepRegistry registry = CSStepRegistry.getInstance();
    Map<CSStepDefinition.StepType, List<CSStepDefinition>> allSteps = registry.getAllSteps();
    
    System.out.println("Registered step definitions:");
    allSteps.forEach((type, steps) -> {
        steps.forEach(step -> {
            System.out.println("  " + type + ": " + step.getOriginalPattern());
        });
    });
}
```

## Example: Complete CRRU Step Definition Class

```java
package com.cct.automation.crru.stepdefs;

import com.testforge.cs.stepdefs.CSStepDefinitions;
import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.pages.CSBasePage;
import org.testng.Assert;
import java.util.Map;

public class CRRUStepDefinitions extends CSStepDefinitions {
    
    // Page object for CRRU
    private CRRUPage crruPage;
    
    public CRRUStepDefinitions() {
        super();
        // Initialize page object will happen when driver is available
    }
    
    @CSStep("User navigates to CRRU application")
    public void navigateToCRRU() {
        String crruUrl = config.getProperty("crru.application.url", "https://crru.example.com");
        driver.navigate(crruUrl);
        
        // Initialize page object with current driver
        crruPage = new CRRUPage(driver.getWebDriver());
    }
    
    @CSStep("User logs into CRRU with username {string} and password {string}")
    public void loginToCRRU(String username, String password) {
        driver.findElement("@crru.login.username").sendKeys(username);
        driver.findElement("@crru.login.password").sendKeys(password);
        driver.findElement("@crru.login.button").click();
        
        // Wait for login to complete
        driver.waitForElement("@crru.dashboard", 10);
    }
    
    @CSStep("User should see CRRU dashboard")
    public void verifyCRRUDashboard() {
        Assert.assertTrue(driver.isDisplayed("@crru.dashboard"), 
            "CRRU Dashboard should be visible after login");
    }
    
    @CSStep("User performs CRRU search with criteria:")
    public void performCRRUSearch(Map<String, String> criteria) {
        criteria.forEach((field, value) -> {
            String locator = "@crru.search." + field.toLowerCase();
            driver.findElement(locator).sendKeys(value);
        });
        
        driver.findElement("@crru.search.button").click();
    }
    
    @CSStep("User should see {int} CRRU results")
    public void verifyCRRUResults(int expectedCount) {
        driver.waitForElement("@crru.results.table", 10);
        int actualCount = driver.findElements("@crru.results.row").size();
        
        Assert.assertEquals(actualCount, expectedCount, 
            "CRRU search results count mismatch");
    }
    
    // Inner page class (optional)
    public static class CRRUPage extends CSBasePage {
        
        @CSLocator("crru.dashboard")
        private CSElement dashboard;
        
        @CSLocator("crru.search.button")
        private CSElement searchButton;
        
        public CRRUPage(WebDriver driver) {
            super(driver);
        }
        
        public boolean isDashboardVisible() {
            return dashboard.isDisplayed();
        }
        
        public void clickSearch() {
            searchButton.click();
        }
    }
}
```

## Troubleshooting Checklist

1. ✅ Step definition class has a no-argument constructor
2. ✅ Class is in the correct package as specified in suite XML
3. ✅ @CSStep annotations are present on methods
4. ✅ Package name in suite XML matches actual package structure
5. ✅ Classes are compiled and present in target/test-classes
6. ✅ No compilation errors in step definition classes
7. ✅ All required dependencies are in classpath
8. ✅ Step patterns match the feature file steps exactly

## Need Help?

If you continue to face issues:

1. Enable debug logging to see detailed registration process
2. Check the console output for specific error messages
3. Verify the framework can access your classes using the test classpath
4. Ensure your VDI environment has all required dependencies available