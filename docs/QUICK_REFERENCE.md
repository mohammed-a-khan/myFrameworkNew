# CS TestForge Framework - Quick Reference

## CSElement Quick Reference

### Enhanced Typing Methods
```java
// Basic typing
element.type("text");                    // Standard typing
element.clearAndType("text");           // Clear then type

// For problematic inputs (React/Angular/Vue)
element.typeSlowly("text");             // Character by character with delay
element.clearAndTypeSlowly("text");     // Clear then type slowly
element.typeSlowly("text", 200);        // Custom delay (200ms)

// JavaScript typing (bypasses validation)
element.typeUsingJS("text");            // Direct JS typing with events
element.clearAndTypeUsingJS("text");    // Clear and type with JS

// Alternative methods
element.clearAndTypeWithActions("text"); // Using Selenium Actions
element.clearCompletely();              // Aggressive field clearing
```

### Wait Methods
```java
// Element presence
element.waitForPresent();               // Default 30s timeout
element.waitForPresent(10);             // Custom timeout
element.waitForNotPresent();            // Wait to disappear

// Element state
element.waitForEnabled();               // Wait for enabled
element.waitForEnabled(5);              // Custom timeout
element.waitForDisabled();              // Wait for disabled
element.waitForSelected();              // Wait for checkbox/radio

// Element visibility
element.waitForVisible();               // Wait for visible
element.waitForInvisible();             // Wait for invisible
element.waitForClickable();             // Wait for clickable

// Element content
element.waitForText("Success");         // Wait for text
element.waitForAttribute("class", "active"); // Wait for attribute

// Method chaining
element.waitForPresent().waitForVisible().click();
```

### Boolean Methods (Exception-Safe)
```java
// All return true/false, never throw exceptions
if (element.isDisplayed()) { /* safe */ }
if (element.isEnabled()) { /* safe */ }
if (element.isPresent()) { /* safe */ }
if (element.exists()) { /* safe */ }
if (element.isSelected()) { /* safe */ }
```

### Common Patterns
```java
// Safe element interaction
if (element.isPresent() && element.isEnabled()) {
    element.clearAndTypeSlowly("text");
}

// Wait then interact
element.waitForPresent(10)
       .waitForEnabled(5)
       .clearAndType("text");

// Handle search button enabling
searchField.clearAndTypeSlowly("query");
searchButton.waitForEnabled(5).click();
```

## Parallel Execution Settings

### Change Thread Count
```bash
# 5 threads
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Dthreadcount=5 -Ddataproviderthreadcount=5

# 10 threads
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Dthreadcount=10 -Ddataproviderthreadcount=10
```

### Switch to Sequential
```bash
# Command line
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Dparallel=none

# Or in suite XML
<suite name="Your Suite" parallel="none">
```

### Suite XML Options
```xml
<!-- Parallel execution with 3 threads -->
<suite name="Your Suite" parallel="methods" thread-count="3" data-provider-thread-count="3">

<!-- Sequential execution -->
<suite name="Your Suite" parallel="none">

<!-- High parallelism -->
<suite name="Your Suite" parallel="methods" thread-count="10" data-provider-thread-count="10">
```

## Common Commands

```bash
# Run specific suite
mvn test -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml

# Run with specific browser
mvn test -Dbrowser.default=chrome

# Run in headless mode
mvn test -Dbrowser.headless=true

# Run specific test class
mvn test -Dtest=CSBDDRunner

# Clean and run
mvn clean test

# Skip tests
mvn install -DskipTests
```

## Data-Driven Testing

### CSV Data Source
```gherkin
Examples: {"type": "csv", "source": "testdata/users.csv"}
```

### Excel Data Source
```gherkin
Examples: {"type": "excel", "source": "testdata/users.xlsx", "sheet": "Sheet1"}
```

### JSON Data Source
```gherkin
Examples: {"type": "json", "source": "testdata/login.json", "path": "$.testData[*]"}
```

### Database Data Source
```gherkin
Examples: {"type": "database", "name": "testdb", "query": "SELECT * FROM users"}
```