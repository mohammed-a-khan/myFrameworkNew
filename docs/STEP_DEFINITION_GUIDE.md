# Step Definition Guide

This guide explains how to write step definitions in the CS TestForge Framework using the new cleaner placeholder format.

## Overview

The framework now supports two formats for step definitions:
1. **New Placeholder Format** (Recommended) - Clean and readable
2. **Legacy Regex Format** - Still supported for backward compatibility

**Important Update**: Step definitions no longer require specifying a step type. Any step definition can be used with any Gherkin keyword (Given, When, Then, And, But).

## New Placeholder Format

### Basic Syntax

Instead of complex regex patterns, use simple placeholders:

```java
// OLD regex format
@CSStep(value = "^I enter username \"([^\"]*)\" and password \"([^\"]*)\"$")
public void login(String username, String password) { }

// NEW placeholder format - Much cleaner!
@CSStep(description = "I enter username {username} and password {password}")
public void login(String username, String password) { }
```

### Supported Placeholders

| Placeholder | Description | Matches | Example |
|------------|-------------|---------|---------|
| `{paramName}` | Named parameter | Quoted or unquoted text | `"admin"` or `admin` |
| `{string}` | String parameter | Quoted text only | `"hello world"` |
| `{int}` | Integer parameter | Whole numbers | `123` |
| `{float}` | Float parameter | Decimal numbers | `12.34` |
| `{number}` | Number parameter | Integer or float | `123` or `12.34` |
| `{word}` | Single word | Alphanumeric word | `button1` |

### Examples

#### Simple Steps

```java
@CSStep(description = "I am on the login page", type = CSStep.StepType.GIVEN)
public void navigateToLogin() {
    // No parameters
}

@CSStep(description = "I click the {buttonName} button", type = CSStep.StepType.WHEN)
public void clickButton(String buttonName) {
    // Single parameter
}
```

#### Multiple Parameters

```java
@CSStep(description = "I enter {username} in the username field and {password} in the password field")
public void enterCredentials(String username, String password) {
    // Multiple parameters
}

@CSStep(description = "I scroll to position {x} and {y}")
public void scrollTo(int x, int y) {
    // Integer parameters
}
```

#### Different Parameter Types

```java
@CSStep(description = "I wait for {seconds} seconds")
public void waitForSeconds(int seconds) {
    // Integer parameter
}

@CSStep(description = "I set the price to {price}")
public void setPrice(double price) {
    // Double parameter
}

@CSStep(description = "I verify {element} contains text {expectedText}")
public void verifyText(String element, String expectedText) {
    // Multiple string parameters
}
```

## Using Step Definitions

### In Feature Files

The feature files remain the same - the framework automatically handles both formats:

```gherkin
Feature: User Login

  Scenario: Successful login
    Given I am on the login page
    When I enter username "admin" and password "pass123"
    And I click the Login button
    Then I should see the dashboard

  Scenario: Using unquoted parameters
    When I enter admin in the username field and pass123 in the password field
    And I wait for 3 seconds
    Then I verify header contains text "Welcome"
```

### Flexible Step Usage

Step definitions no longer require type specification. Any step can be used with any Gherkin keyword:

```java
// Define once, use anywhere
@CSStep(description = "I am on the {pageName} page")
public void navigateToPage(String pageName) { }

@CSStep(description = "I click {element}")
public void clickElement(String element) { }

@CSStep(description = "I should see {message}")
public void verifyMessage(String message) { }
```

These steps can be used flexibly in feature files:

```gherkin
# All of these are valid:
Given I am on the "login" page
When I am on the "dashboard" page
Then I am on the "profile" page
And I click "Submit"
But I click "Cancel"
```

### Utility Steps

Common utility steps can be used in any context:

```java
@CSStep(description = "I take a screenshot named {name}")
public void takeScreenshot(String name) {
    // Can be used as:
    // Given I take a screenshot named "login_page"
    // When I take a screenshot named "after_click"
    // Then I take a screenshot named "final_state"
}

@CSStep(description = "I wait for {seconds} seconds")
public void waitSeconds(int seconds) { }

@CSStep(description = "I log {message}")
public void logMessage(String message) { }
```

## Migration Guide

### Converting Regex to Placeholder Format

1. **Simple string parameters**:
   ```java
   // Before
   @CSStep(value = "^I click \"([^\"]*)\"$")
   
   // After
   @CSStep(description = "I click {element}")
   ```

2. **Multiple parameters**:
   ```java
   // Before
   @CSStep(value = "^I enter \"([^\"]*)\" and \"([^\"]*)\"$")
   
   // After
   @CSStep(description = "I enter {username} and {password}")
   ```

3. **Number parameters**:
   ```java
   // Before
   @CSStep(value = "^I wait for (\\d+) seconds$")
   
   // After
   @CSStep(description = "I wait for {seconds} seconds")
   ```

## Best Practices

1. **Use descriptive parameter names**: `{username}` instead of `{param1}`
2. **Keep steps simple and focused**: One action per step
3. **Use appropriate parameter types**: `{int}` for numbers, `{word}` for identifiers
4. **Be consistent**: Use the same parameter names across similar steps

## Complete Example

```java
package com.example.steps;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;

@CSFeature(name = "User Management", tags = {"@users"})
public class UserSteps extends CSStepDefinitions {
    
    @CSStep(description = "I navigate to the {pageName} page")
    public void navigateToPage(String pageName) {
        logger.info("Navigating to {} page", pageName);
        // Implementation
    }
    
    @CSStep(description = "I create a user with name {userName} and role {role}")
    public void createUser(String userName, String role) {
        logger.info("Creating user: {} with role: {}", userName, role);
        // Implementation
    }
    
    @CSStep(description = "I should see {count} users in the list")
    public void verifyUserCount(int count) {
        logger.info("Verifying user count: {}", count);
        // Implementation
    }
    
    @CSStep(description = "I take a screenshot named {screenshotName}")
    public void takeScreenshot(String screenshotName) {
        logger.info("Taking screenshot: {}", screenshotName);
        captureScreenshot(screenshotName);
    }
}
```

## Backward Compatibility

The framework still supports regex patterns for existing step definitions:

```java
// This still works
@CSStep(value = "^I enter \"([^\"]*)\" in the \"([^\"]*)\" field$")
public void enterValueInField(String value, String field) { }
```

The framework automatically detects regex patterns (containing `^`, `$`, or `\`) and processes them accordingly.

## Accessing Complete Data Row

In data-driven scenarios, you can access the complete data row in your step definitions using multiple approaches:

### Method 1: Using @CSDataRow Annotation

Inject the complete data row as a parameter:

```java
@CSStep(description = "I verify all user data")
public void verifyAllUserData(@CSDataRow Map<String, String> dataRow) {
    String username = dataRow.get("username");
    String email = dataRow.get("email");
    // Access any field from the data row
}
```

### Method 2: Include Metadata

Get data row with metadata (data source type, file, browser, environment):

```java
@CSStep(description = "I log test information")
public void logTestInfo(@CSDataRow(includeMetadata = true) Map<String, String> fullDataRow) {
    String dataSource = fullDataRow.get("dataSourceType"); // "CSV", "JSON", etc.
    String sourceFile = fullDataRow.get("dataSourceFile"); // File path
}
```

### Method 3: Mix Regular Parameters with Data Row

Combine explicit parameters with data row access:

```java
@CSStep(description = "I create {userType} user with additional data")
public void createUser(String userType, @CSDataRow Map<String, String> dataRow) {
    // Use regular parameter
    logger.info("Creating {} user", userType);
    
    // Use data row
    String email = dataRow.get("email");
}
```

### Method 4: Using Helper Methods

Access data row without annotations using inherited methods:

```java
@CSStep(description = "I verify user profile")
public void verifyProfile() {
    // Get complete data row
    Map<String, String> dataRow = getDataRow();
    
    // Get specific value
    String username = getDataValue("username");
    
    // Check if field exists
    if (hasDataKey("phone")) {
        String phone = getDataValue("phone");
    }
}
```

### Example Feature File

```gherkin
Scenario Outline: Verify user data
  Given I am on the user page
  When I search for user "<username>"
  Then I verify all user data
  
  Examples:
    | username | email            | role    | department |
    | jdoe     | jdoe@example.com | Admin   | IT         |
    | asmith   | asmith@test.com  | Manager | HR         |
```

The step "I verify all user data" will receive the complete row as a Map.

## Validation and Error Handling

The framework performs comprehensive validation to ensure step definitions are unique and properly defined:

### Duplicate Step Definitions
If the same step pattern exists in multiple classes, you'll see:
```
ERROR: Duplicate step definition found for pattern 'I click {button}':
  Found in multiple locations:
    - com.example.steps.LoginSteps.clickButton
    - com.example.steps.CommonSteps.clickElement
  Please ensure each step pattern is unique across all step definition classes.
```

### Duplicate Method Names
The framework warns about methods with the same name in different classes:
```
WARNING: Method name 'clickButton' found in multiple classes:
    - com.example.steps.LoginSteps
    - com.example.steps.NavigationSteps
  Consider using unique method names to avoid confusion.
```

### Step Not Found
When a step from your feature file doesn't match any definition:
```
No matching step definition found for: 'I perform an unknown action'

Please ensure:
1. The step definition exists in your step classes
2. The step pattern matches exactly (including parameters)
3. The step class is properly registered

Did you mean one of these?
  - I perform a search
  - I perform login action
```

## Troubleshooting

### Step not found
- Ensure parameter names in the step definition match the feature file
- Check that the step class is registered
- The framework will suggest similar steps if available

### Parameter type mismatch
- Make sure method parameter types match the placeholder type
- Use `{int}` for integer parameters, not `{paramName}`
- Use `{float}` or `{number}` for decimal values

### Data row is empty
- Ensure you're running a data-driven scenario (with Examples or external data source)
- Check that the scenario has data rows defined
- Verify the data source file exists and is properly formatted