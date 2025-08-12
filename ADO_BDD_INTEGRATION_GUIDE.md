# Azure DevOps BDD Integration Guide

## Overview
This guide explains how to run BDD (Behavior-Driven Development) tests with Azure DevOps integration, automatically updating test results in Azure DevOps Test Plans.

## Feature File with ADO Tags

The feature file `features/ado-mapped-tests.feature` contains ADO tags that map scenarios to Azure DevOps test cases:

```gherkin
@ado-integration @TestPlanId:417 @TestSuiteId:418
Feature: Azure DevOps Mapped Tests
  Tests specifically mapped to Azure DevOps test cases

  @TestCaseId:419 @login @smoke
  Scenario: ADO Test Case 419 - Valid Login Test
    Given I am on the OrangeHRM login page
    When I enter username "Admin" and password "admin123"
    And I click the login button
    Then I should see the dashboard page
    And the dashboard header should display "Dashboard"
    And I take a screenshot "ado_test_419_success"

  @TestCaseId:420 @login-failure @negative
  Scenario: ADO Test Case 420 - Invalid Login Test
    Given I am on the OrangeHRM login page
    When I enter username "invalid" and password "wrongpassword"
    And I click the login button
    Then I should see an error message containing "Invalid credentials"
    And I should remain on the login page
    And I take a screenshot "ado_test_420_validation"
```

## Tag Structure

### Feature-Level Tags
- `@TestPlanId:417` - Maps all scenarios in the feature to Test Plan 417
- `@TestSuiteId:418` - Maps all scenarios to Test Suite 418

### Scenario-Level Tags
- `@TestCaseId:419` - Maps the specific scenario to Test Case 419 in Azure DevOps
- Additional tags like `@smoke`, `@login`, `@negative` for test categorization

## Setup Instructions

### 1. Set PAT Token
You need to set your Azure DevOps Personal Access Token (PAT) as an environment variable:

**Windows (Command Prompt):**
```batch
set ADO_PAT_TOKEN=your_pat_token_here
```

**Windows (PowerShell):**
```powershell
$env:ADO_PAT_TOKEN="your_pat_token_here"
```

**Linux/Mac:**
```bash
export ADO_PAT_TOKEN=your_pat_token_here
```

### 2. Configure ADO Settings
Ensure `resources/config/application.properties` has the correct ADO configuration:

```properties
# Azure DevOps Configuration
ado.enabled=true
ado.organization=mdakhan
ado.project=myproject
ado.pat=${ADO_PAT_TOKEN}
ado.test.plan.id=417
ado.test.suite.id=418
ado.upload.attachments=true
ado.upload.screenshots=true
```

## Running the Tests

### Option 1: Using Batch/Shell Scripts

**Windows:**
```batch
run-ado-bdd-tests.bat
```

**Linux/Mac:**
```bash
./run-ado-bdd-tests.sh
```

### Option 2: Using Maven Command

```bash
# Set PAT token first
export ADO_PAT_TOKEN=your_pat_token_here

# Run the tests
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml
```

### Option 3: Using TestNG Suite File

Run the `suites/ado-bdd-suite.xml` file directly from your IDE.

## Test Execution Flow

1. **BDD Runner Initialization**
   - `CSADOBDDRunner` extends `CSBDDRunner` with ADO capabilities
   - Initializes Azure DevOps publisher
   - Creates a new test run in Azure DevOps

2. **Tag Extraction**
   - Extracts `@TestCaseId`, `@TestPlanId`, `@TestSuiteId` from feature/scenario tags
   - Falls back to configuration defaults if tags not found

3. **Test Execution**
   - Executes each scenario using Cucumber-like step definitions
   - Captures screenshots as configured
   - Tracks test status (PASSED/FAILED)

4. **Result Publishing**
   - Creates `CSTestResult` with all test metadata
   - Adds ADO metadata (test case, plan, suite IDs)
   - Publishes result to Azure DevOps via REST API
   - Updates test case status in Test Plan

5. **Test Run Completion**
   - Completes the test run in Azure DevOps
   - Results visible in Azure DevOps Test Plans

## Viewing Results in Azure DevOps

1. Navigate to: https://dev.azure.com/mdakhan/myproject/_testManagement
2. Go to Test Plans → Test Plan 417
3. Open Test Suite 418
4. View test cases 419 and 420 with their latest execution results

## Test Result Details

Each test result in Azure DevOps includes:
- **Outcome**: Passed/Failed
- **Duration**: Test execution time
- **Error Details**: Error message and stack trace for failures
- **Attachments**: Screenshots captured during execution
- **Test Run**: Link to the complete test run

## Troubleshooting

### PAT Token Issues
```
ERROR: Azure DevOps connection test: FAILED
```
**Solution**: Verify PAT token has correct permissions:
- Test Management (Read & Write)
- Work Items (Read & Write)

### Test Points Not Found
```
WARN: No test point found for test case
```
**Solution**: Ensure test cases exist in the specified test plan and suite in Azure DevOps.

### Connection Issues
```
ERROR: Failed to connect to Azure DevOps
```
**Solution**: Check network connectivity and proxy settings if behind corporate firewall.

## Advanced Configuration

### Custom Test Plan/Suite per Execution
Override default test plan/suite via system properties:
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml \
  -Dado.test.plan.id=500 \
  -Dado.test.suite.id=501
```

### Running Specific Scenarios
Use tags to run specific scenarios:
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml \
  -Dcucumber.filter.tags="@smoke"
```

### Parallel Execution
Modify suite XML for parallel execution:
```xml
<suite name="ADO BDD Suite" parallel="methods" thread-count="3">
```

## Integration Architecture

```
Feature File (.feature)
    ↓ (contains @TestCaseId tags)
CSADOBDDRunner
    ↓ (extracts ADO metadata)
CSADOTagExtractor
    ↓ (maps to test cases)
CSAzureDevOpsPublisher
    ↓ (publishes results)
Azure DevOps REST API
    ↓
Test Plan/Suite/Case Updates
```

## Best Practices

1. **Tag Placement**
   - Place `@TestPlanId` and `@TestSuiteId` at feature level
   - Place `@TestCaseId` at scenario level
   - Use consistent tag format: `@TestCaseId:123`

2. **Test Organization**
   - Group related test cases in the same feature file
   - Use meaningful scenario names that match ADO test case titles

3. **Error Handling**
   - Tests continue executing even if ADO publishing fails
   - Check logs for ADO-related errors

4. **Performance**
   - Results are published asynchronously
   - Screenshots are uploaded in parallel
   - Connection pooling for API calls

## Sample Output

```
======================================
Running Azure DevOps BDD Tests
======================================
Test Configuration:
  Organization: mdakhan
  Project: myproject
  Test Plan ID: 417
  Test Suite ID: 418
  Test Cases: 419, 420

[INFO] Azure DevOps integration initialized successfully
[INFO] Started Azure DevOps test run: BDD Test Run - 2025-08-12T11:30:00
[INFO] Executing BDD scenario with ADO integration: ADO Test Case 419 - Valid Login Test
[INFO] Found ADO mapping - Test Case: 419, Test Plan: 417, Test Suite: 418
[INFO] Published test result to Azure DevOps for test case: 419
[INFO] Executing BDD scenario with ADO integration: ADO Test Case 420 - Invalid Login Test
[INFO] Found ADO mapping - Test Case: 420, Test Plan: 417, Test Suite: 418
[INFO] Published test result to Azure DevOps for test case: 420
[INFO] Completed Azure DevOps test run

Test execution completed!
Check Azure DevOps for test results:
https://dev.azure.com/mdakhan/myproject/_testManagement
======================================
```

## Support

For issues or questions:
1. Check the logs in `target/surefire-reports/`
2. Enable debug logging: `-Dlogging.level.com.testforge.cs.azuredevops=DEBUG`
3. Verify ADO configuration in `application.properties`
4. Ensure test cases exist in Azure DevOps