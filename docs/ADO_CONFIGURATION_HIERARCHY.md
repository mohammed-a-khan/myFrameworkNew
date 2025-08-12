# Azure DevOps Configuration Hierarchy

## Overview

The CS TestForge Framework provides flexible configuration for Azure DevOps Test Plan and Test Suite IDs through a three-level hierarchy system. This allows you to configure these values at different levels based on your testing needs.

## Configuration Hierarchy

The framework follows this priority order (highest to lowest):

### 1. **Suite XML Parameters** (Highest Priority)
Define Test Plan and Suite IDs directly in your TestNG suite XML file.

```xml
<parameter name="azure.devops.test.plan.id" value="500"/>
<parameter name="azure.devops.test.suite.id" value="501"/>
```

**When to use:**
- Running different test suites against different test plans
- Environment-specific test execution (Dev, QA, UAT, Prod)
- CI/CD pipelines with dynamic test plan selection

### 2. **Feature File Tags** (Medium Priority)
Add tags to your feature files or scenarios.

```gherkin
@TestPlanId:400 @TestSuiteId:401
Feature: User Authentication

@TestCaseId:12345
Scenario: Valid login
```

**When to use:**
- Feature-specific test plan mapping
- Organizing tests by functional areas
- When different features belong to different test plans

### 3. **Application Properties** (Lowest Priority)
Configure default values in `application.properties`.

```properties
ado.test.plan.id=417
ado.test.suite.id=418
```

**When to use:**
- Default fallback values
- Single test plan for entire project
- Development and local testing

## Examples

### Example 1: Environment-Specific Test Plans

**dev-suite.xml:**
```xml
<suite name="Development Tests">
    <parameter name="azure.devops.test.plan.id" value="100"/>
    <parameter name="azure.devops.test.suite.id" value="101"/>
    <test name="Dev BDD Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

**uat-suite.xml:**
```xml
<suite name="UAT Tests">
    <parameter name="azure.devops.test.plan.id" value="200"/>
    <parameter name="azure.devops.test.suite.id" value="201"/>
    <test name="UAT BDD Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

### Example 2: Feature-Level Configuration

**authentication.feature:**
```gherkin
@TestPlanId:300 @TestSuiteId:301
Feature: Authentication Module
  All authentication related tests

  @TestCaseId:3001
  Scenario: User login with valid credentials
    Given user is on login page
    When user enters valid credentials
    Then user should be logged in successfully
```

**payment.feature:**
```gherkin
@TestPlanId:400 @TestSuiteId:401
Feature: Payment Processing
  Payment gateway integration tests

  @TestCaseId:4001
  Scenario: Process credit card payment
    Given user has items in cart
    When user completes payment with credit card
    Then payment should be processed successfully
```

### Example 3: Mixed Configuration

You can mix different levels. The framework will always use the highest priority value available:

**suite.xml:**
```xml
<suite name="Regression Suite">
    <!-- Only define Test Plan at suite level -->
    <parameter name="azure.devops.test.plan.id" value="500"/>
    <!-- Test Suite ID will come from feature tags or properties -->
</suite>
```

**feature file:**
```gherkin
@TestSuiteId:502
Feature: Critical Business Flow
  # Test Plan ID comes from suite.xml (500)
  # Test Suite ID comes from this tag (502)
```

## Configuration Precedence Table

| Configuration Level | Test Plan ID | Test Suite ID | Test Case ID |
|-------------------|--------------|---------------|--------------|
| Suite XML | ✅ Highest | ✅ Highest | ❌ N/A |
| Scenario Tags | ✅ Medium | ✅ Medium | ✅ Always from tags |
| Feature Tags | ✅ Medium | ✅ Medium | ❌ N/A |
| Properties File | ✅ Lowest | ✅ Lowest | ❌ N/A |

## Best Practices

1. **Use Suite XML for CI/CD**: Pass Test Plan/Suite IDs as parameters in your CI/CD pipeline
   ```bash
   mvn test -Dsurefire.suiteXmlFiles=regression.xml \
            -Dazure.devops.test.plan.id=600 \
            -Dazure.devops.test.suite.id=601
   ```

2. **Use Feature Tags for Organization**: Group related features under same test plans
   ```gherkin
   @TestPlanId:700 @TestSuiteId:701
   Feature: Customer Management
   ```

3. **Use Properties for Defaults**: Set organization-wide defaults in properties
   ```properties
   # Default test plan for all tests
   ado.test.plan.id=1000
   ado.test.suite.id=1001
   ```

4. **Override When Needed**: Higher priority configurations override lower ones
   - Running specific suite? Use suite XML parameters
   - Testing specific feature? Use feature tags
   - General execution? Use properties defaults

## Debugging Configuration

The framework logs which configuration source is being used:

```
INFO  - Using Test Plan ID from suite XML: 500
INFO  - Using Test Suite ID from feature tag: 502
INFO  - Using Test Case ID from scenario tag: 5001
INFO  - ADO Metadata - Test Plan: 500, Test Suite: 502, Test Case: 5001
```

Enable debug logging to see the complete hierarchy evaluation:

```properties
logging.level.com.testforge.cs.bdd=DEBUG
```

## Common Scenarios

### Scenario 1: Single Test Plan for Everything
Set in `application.properties` and don't use suite parameters or tags:
```properties
ado.test.plan.id=100
ado.test.suite.id=101
```

### Scenario 2: Different Plans per Environment
Use different suite XML files with parameters:
```bash
# Development
mvn test -Dsurefire.suiteXmlFiles=dev-suite.xml

# Production
mvn test -Dsurefire.suiteXmlFiles=prod-suite.xml
```

### Scenario 3: Feature-Specific Plans
Tag each feature file:
```gherkin
@TestPlanId:200 @TestSuiteId:201
Feature: Feature A

@TestPlanId:300 @TestSuiteId:301  
Feature: Feature B
```

### Scenario 4: Dynamic CI/CD Configuration
Pass parameters from your CI/CD system:
```yaml
# Azure DevOps Pipeline
- task: Maven@3
  inputs:
    mavenPomFile: 'pom.xml'
    goals: 'test'
    options: '-Dsurefire.suiteXmlFiles=suite.xml -Dazure.devops.test.plan.id=$(TestPlanId) -Dazure.devops.test.suite.id=$(TestSuiteId)'
```

## Troubleshooting

1. **Test Plan/Suite not being picked up:**
   - Check the hierarchy - higher priority sources override lower ones
   - Enable debug logging to see which source is being used
   - Verify parameter names match exactly

2. **Wrong Test Plan/Suite being used:**
   - Check if suite XML has parameters that override your tags
   - Verify no typos in parameter names or tag formats

3. **Feature tags not working:**
   - Ensure tag format is correct: `@TestPlanId:123` or `@TestPlanId-123`
   - Tags are case-insensitive but must have correct format

## Summary

This hierarchy system provides maximum flexibility:
- **Production**: Use suite XML parameters for controlled execution
- **Development**: Use feature tags for quick testing
- **Defaults**: Use properties file for fallback values

The framework automatically determines the correct Test Plan and Suite IDs based on the available configurations, following the defined hierarchy.