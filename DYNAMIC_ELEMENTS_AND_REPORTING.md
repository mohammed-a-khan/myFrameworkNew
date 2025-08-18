# Dynamic Element Creation and Report Logging Guide

## Overview
This document explains the dynamic element creation methods now available in CSBasePage, how to use CSReportManager for report logging that appears in generated HTML reports, and best practices for using the @CSPage annotation.

## 1. Dynamic Element Creation Methods in CSBasePage

### Core Method: `findDynamicElement()`
```java
// Find element using parameterized locator from repository
public CSElement findDynamicElement(String patternKey, Object... params)
```

This is the main method that:
- Takes a pattern key from `object-repository.properties`
- Replaces `{0}`, `{1}`, `{2}` placeholders with runtime values
- Creates and returns a CSElement with automatic retry logic

**Example:**
```java
// In object-repository.properties:
// dynamic.menu.item.xpath=//a[contains(text(),'{0}')]

// In your page object:
CSElement menuItem = findDynamicElement("dynamic.menu.item.xpath", "Settings");
```

### Convenience Methods (All Available in Every Page Object)

#### 1. `findMenuItemByText(String menuText)`
```java
CSElement homeMenu = findMenuItemByText("Home");
```

#### 2. `findButtonByText(String buttonText)`
```java
CSElement submitBtn = findButtonByText("Submit");
```

#### 3. `findLinkByText(String linkText)`
```java
CSElement helpLink = findLinkByText("Help");
```

#### 4. `findTableCell(String tableId, int row, int column)`
```java
CSElement cell = findTableCell("resultsTable", 2, 3);
String cellValue = cell.getText();
```

#### 5. `findInputByLabel(String labelText)`
```java
CSElement emailInput = findInputByLabel("Email Address");
emailInput.clearAndType("test@example.com");
```

#### 6. `findElementByDataAttribute(String attribute, String value)`
```java
CSElement element = findElementByDataAttribute("testid", "submit-button");
```

#### 7. `findDropdownOption(String dropdownId, String optionText)`
```java
CSElement option = findDropdownOption("countrySelect", "United States");
```

#### 8. `findNthElement(String baseLocator, int index)`
```java
CSElement thirdItem = findNthElement("//li[@class='item']", 3);
```

#### 9. `findElementByAttributes(String tagName, Map<String, String> attributes)`
```java
Map<String, String> attrs = new HashMap<>();
attrs.put("class", "btn");
attrs.put("type", "submit");
CSElement button = findElementByAttributes("button", attrs);
```

#### 10. `findElementInContainer(String containerId, String elementSelector)`
```java
CSElement element = findElementInContainer("sidebar", "a.nav-link");
```

### Methods with Built-in Reporting

#### `isDynamicElementPresent(String patternKey, Object... params)`
```java
if (isDynamicElementPresent("dynamic.button.text.xpath", "Submit")) {
    // Element exists - CSReportManager.pass() already logged
}
```

#### `getDynamicElementText(String patternKey, Object... params)`
```java
String text = getDynamicElementText("dynamic.table.cell.xpath", "table1", 1, 2);
// Automatically logs pass/warn based on whether text is found
```

#### `clickDynamicElement(String patternKey, Object... params)`
```java
clickDynamicElement("dynamic.menu.item.xpath", "Settings");
// Automatically logs pass/fail for the click action
```

## 2. CSReportManager Logging

All CSReportManager messages appear in the generated HTML test reports under their respective test steps.

### Logging Levels

#### INFO - General Information
```java
CSReportManager.info("Starting login process");
CSReportManager.info("Navigating to ESSS/Series module");
```

#### PASS - Successful Operations
```java
CSReportManager.pass("Login successful");
CSReportManager.pass("✓ All validations passed");
CSReportManager.pass(String.format("Found %d results in %d ms", count, time));
```

#### WARN - Non-Critical Issues
```java
CSReportManager.warn("Element not found - using default");
CSReportManager.warn("⚠ Search is slow: " + searchTime + " ms");
```

#### FAIL - Critical Failures
```java
CSReportManager.fail("Login failed: " + errorMessage);
CSReportManager.fail("✗ Required element not found");
```

## 3. Object Repository Patterns

In `config/object-repository.properties`:

```properties
# Dynamic patterns with placeholders
dynamic.menu.item.xpath=//a[contains(text(),'{0}')]
dynamic.table.cell.xpath=//table[@id='{0}']//tr[{1}]//td[{2}]
dynamic.button.by.text=//button[contains(text(),'{0}')]
dynamic.input.by.label=//label[contains(text(),'{0}')]/following-sibling::input[1]
dynamic.dropdown.option=//select[@id='{0}']/option[text()='{1}']
dynamic.element.by.attribute=//{0}[@{1}='{2}']
dynamic.data.attribute=[data-{0}='{1}']
dynamic.element.nth.child=//*[@class='{0}']:nth-child({1})
```

## 4. Usage Examples

### Example 1: Login Page with Report Logging
```java
public void login(String username, String password) {
    CSReportManager.info("Starting login for user: " + username);
    
    enterUsername(username);
    enterPassword(password);
    clickLoginButton();
    
    if (isErrorMessageDisplayed()) {
        String error = getErrorMessage();
        CSReportManager.warn("Login failed with error: " + error);
    } else {
        CSReportManager.pass("Login completed successfully");
    }
}
```

### Example 2: Dynamic Search Operation
```java
public void performDynamicSearch(String searchType, String value) {
    CSReportManager.info("Performing dynamic search");
    
    // Find and click dropdown dynamically
    CSElement typeDropdown = findDynamicElement("dynamic.dropdown", searchType);
    typeDropdown.click();
    
    // Find and fill search field dynamically
    CSElement searchField = findInputByLabel("Search");
    searchField.clearAndType(value);
    
    // Click search button dynamically
    clickDynamicElement("dynamic.button.by.text", "Search");
    
    CSReportManager.pass("Search completed");
}
```

### Example 3: Table Validation with Dynamic Elements
```java
public void validateTableData(String tableId, int row, int col, String expected) {
    CSReportManager.info("Validating table data");
    
    String actual = getDynamicElementText("dynamic.table.cell.xpath", tableId, row, col);
    
    if (expected.equals(actual)) {
        CSReportManager.pass("✓ Table cell validation passed");
    } else {
        CSReportManager.fail(String.format("✗ Expected: %s, Actual: %s", expected, actual));
    }
}
```

### Example 4: Performance Monitoring with Reporting
```java
public void searchWithPerformanceCheck(String query) {
    CSReportManager.info("Starting search with performance monitoring");
    
    long start = System.currentTimeMillis();
    performSearch(query);
    long duration = System.currentTimeMillis() - start;
    
    if (duration < 1000) {
        CSReportManager.pass("✓ Search performance EXCELLENT: " + duration + " ms");
    } else if (duration < 3000) {
        CSReportManager.warn("⚠ Search performance ACCEPTABLE: " + duration + " ms");
    } else {
        CSReportManager.fail("✗ Search performance POOR: " + duration + " ms");
    }
}
```

## 5. Best Practices

1. **Use meaningful pattern keys** in object repository:
   ```properties
   # Good
   dynamic.navigation.menu.item=//nav//a[text()='{0}']
   
   # Bad
   pattern1=//a[text()='{0}']
   ```

2. **Always include CSReportManager logging** for visibility:
   ```java
   CSReportManager.info("Starting operation X");
   // perform operation
   CSReportManager.pass("Operation X completed");
   ```

3. **Use the right logging level**:
   - INFO: Normal flow, navigation, starting operations
   - PASS: Successful validations, completed operations
   - WARN: Optional elements missing, slow performance
   - FAIL: Required elements missing, validations failed

4. **Leverage inherited methods** instead of writing custom ones:
   ```java
   // Use this (inherited from CSBasePage)
   CSElement button = findButtonByText("Submit");
   
   // Instead of writing custom
   CSElement button = findElement("//button[text()='Submit']", "Submit button");
   ```

5. **Use dynamic elements for data-driven tests**:
   ```java
   @Test(dataProvider = "menuItems")
   public void testAllMenuItems(String menuText) {
       CSElement menuItem = findMenuItemByText(menuText);
       Assert.assertTrue(menuItem.isPresent());
   }
   ```

## 6. @CSPage Annotation Usage

The `@CSPage` annotation configures page object behavior. Understanding its attributes helps you decide what to include.

### Annotation Attributes

| Attribute | Type | Default | Purpose | Impact if Not Provided |
|-----------|------|---------|---------|------------------------|
| `name` | String | "" | Page identification in reports | No impact on functionality |
| `url` | String | "" | URL for navigation | Can't use parameterless `navigateTo()` |
| `title` | String | "" | Expected page title | No validation if empty |
| `autoNavigate` | boolean | `false` | Auto-navigate on page creation | No auto-navigation (usually desired) |
| `validateOnLoad` | boolean | `true` | Validate page after navigation | **Will attempt validation** (may cause issues) |
| `waitTime` | int | 30 | Wait timeout in seconds | Uses 30 seconds default |
| `repositoryPrefix` | String | "" | Prefix for object repository keys | No prefix filtering |
| `readyScript` | String | "" | JavaScript for readiness check | No JS validation |
| `description` | String | "" | Page description | No impact |

### Common Usage Patterns

#### Pattern 1: Minimal Configuration (Recommended for most cases)
```java
@CSPage(name = "Login Page")
public class LoginPage extends CSBasePage {
    // Simplest form - all defaults apply
    // - No auto-navigation (good)
    // - validateOnLoad defaults to true (may need attention)
    // - No URL stored (use navigateTo(url) with parameter)
}
```

#### Pattern 2: With URL for Convenience
```java
@CSPage(
    name = "Login Page",
    url = "${cs.akhan.url}/login"  // Supports property substitution
)
public class LoginPage extends CSBasePage {
    // Can now use:
    // - navigateTo() without parameters
    // - URL is resolved from properties
}
```

#### Pattern 3: Explicit Control (Most Common)
```java
@CSPage(
    name = "Login Page",
    url = "${cs.akhan.url}/login",
    validateOnLoad = false  // Explicitly disable validation
)
public class LoginPage extends CSBasePage {
    // Clear intent: URL stored but no automatic validation
}
```

#### Pattern 4: Full Auto-Navigation and Validation
```java
@CSPage(
    name = "Dashboard",
    url = "${cs.akhan.url}/dashboard",
    title = "Dashboard - Akhan",
    autoNavigate = true,        // Navigate on creation
    validateOnLoad = true,       // Validate after navigation
    waitTime = 10,              // Custom timeout
    readyScript = "return jQuery.active == 0"  // Wait for AJAX
)
public class DashboardPage extends CSBasePage {
    // Page automatically navigates and validates when created
    // Useful for pages that should always be in a known state
}
```

### How Attributes Are Used

#### `url` Attribute
```java
// If url is provided in @CSPage
public void navigateTo() {
    // Uses URL from annotation
    // Resolves properties like ${cs.akhan.url}
}

// If url is NOT provided
public void navigateTo() {
    // Throws CSFrameworkException
}

// Always works regardless of annotation
public void navigateTo(String url) {
    // Uses provided parameter
}
```

#### `autoNavigate` and `validateOnLoad` Flow
```java
// During page object construction:
if (autoNavigate && !url.isEmpty()) {
    navigateTo(url);  // Navigates automatically
    
    if (validateOnLoad) {
        waitForPageLoad(waitTime);
        
        if (!readyScript.isEmpty()) {
            // Waits for JavaScript condition
        }
        
        if (!title.isEmpty()) {
            // Validates page title
        }
    }
}
```

### Best Practices

1. **Explicitly set `validateOnLoad = false`** unless you need automatic validation
   ```java
   @CSPage(name = "My Page", validateOnLoad = false)
   ```

2. **Include URL if you'll use parameterless navigation**
   ```java
   @CSPage(name = "My Page", url = "${base.url}/mypage")
   ```

3. **Avoid `autoNavigate = true`** unless the page should always auto-navigate
   - Most tests prefer explicit navigation control

4. **Use property substitution in URLs**
   ```java
   url = "${cs.akhan.url}/login"  // Good - environment-specific
   url = "https://test.akhan.com/login"  // Bad - hardcoded
   ```

5. **Minimal is often best**
   ```java
   @CSPage(name = "Login Page", validateOnLoad = false)
   // Simple, clear, no surprises
   ```

### Common Issues and Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Unexpected validation failures | `validateOnLoad` defaults to `true` | Set `validateOnLoad = false` |
| Can't use `navigateTo()` | No `url` provided | Add `url` attribute or use `navigateTo(String)` |
| Page navigates unexpectedly | `autoNavigate = true` | Remove or set to `false` |
| Property not resolved in URL | Wrong property key | Check `application.properties` |

## Summary

The CS TestForge Framework now provides:
- **Powerful dynamic element creation** methods in CSBasePage
- **Comprehensive report logging** via CSReportManager
- **Parameterized locators** in object repository
- **Built-in reporting** in dynamic element methods
- **Flexible @CSPage annotation** for page configuration

All methods are available to any page object that extends CSBasePage, making test automation more maintainable and reports more informative. The @CSPage annotation provides optional configuration - use only what you need.