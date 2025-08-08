# Akhan Application Test Framework

This document provides context about the Akhan application test implementation using CS TestForge Framework.

## Project Overview
- **Application**: Akhan Web Application
- **URL**: https://akhan-ui-sit.myshare.net/
- **Environment**: SIT (System Integration Testing)
- **Framework**: CS TestForge - Selenium-based BDD framework

## Key Features Demonstrated

### 1. Object Repository Pattern
All element locators are centralized in `object-repository.properties`:
- Uses meaningful keys like `akhan.login.username`
- Supports dynamic locators with placeholders
- Alternative locators for self-healing

### 2. Page Object Model (POM)
- `LoginPage.java` - Login functionality with enhanced reporting
- `HomePage.java` - Navigation and menu verification
- `ESSSeriesPage.java` - ESSS/Series module with complex dropdowns

### 3. BDD with Gherkin
- Feature files with business-readable scenarios
- Background steps for common setup
- Scenario Outlines for data-driven testing
- Tags for test organization (@login, @navigation, @esss-search)

### 4. Custom Reporting
- Step-by-step logging with CSReportingUtils
- Performance metrics tracking
- Visual indicators (✅, ❌, ⚠️, ⏱️)
- Screenshots at key points
- Detailed test execution timeline

### 5. Data-Driven Testing
- JSON test data: `akhan-test-data.json`
- CSV test data: `esss-search-data.csv`
- Excel support via @CSDataSource
- Dynamic data from Examples tables

### 6. Wait Strategies
- Explicit waits for elements
- Page load synchronization
- Custom wait conditions
- Performance tracking

### 7. Parallel Execution
- Module navigation tests run in parallel
- Thread-safe implementation
- Configurable via suite XML

### 8. Error Handling
- Graceful failure scenarios
- Screenshot on failure
- Detailed error reporting
- Alternative locator fallback

## Test Structure

### Feature Files
1. `akhan-application.feature` - Basic test scenarios
2. `akhan-comprehensive.feature` - All framework features

### Step Definitions
1. `AkhanSteps.java` - Core step implementations
2. `AkhanComprehensiveSteps.java` - Enhanced steps with all features

### Test Suites
1. `akhan-test-suite.xml` - Basic test execution
2. `akhan-comprehensive-suite.xml` - Full feature demonstration

## Running Tests

### Basic Tests
```bash
mvn test -DsuiteXmlFile=suites/akhan-test-suite.xml
```

### Comprehensive Tests
```bash
mvn test -DsuiteXmlFile=suites/akhan-comprehensive-suite.xml
```

### Specific Scenarios
```bash
mvn test -DsuiteXmlFile=suites/akhan-test-suite.xml -Dcucumber.options="--tags @login"
```

## Key Implementation Details

### Dynamic Locators
Menu items use dynamic XPath with placeholders:
```
//div[@id='abcdNavigatorBody']//a[text()='{}']
```

### Special Cases
1. System Admin uses span instead of anchor tag
2. File Upload verification uses span element instead of h1
3. ESSS search table iteration checks column 2 for "ESSS" type

### Performance Thresholds
- Page Load: < 2 seconds
- Search Execution: < 3 seconds
- Total Operation: < 5 seconds

## Reports
Generated reports include:
- Test execution timeline with thread details
- Step-by-step execution logs
- Performance metrics
- Screenshots at key points
- Pass/Fail statistics
- Data source information

## Maintenance Notes
- Update `akhan-application.properties` for environment-specific settings
- Add new locators to object repository following naming convention
- Use CSReportingUtils for consistent logging
- Follow existing patterns for new page objects