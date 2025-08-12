# Azure DevOps Integration for CS TestForge Framework

## Overview
This document describes the comprehensive Azure DevOps (ADO) integration implemented in the CS TestForge Framework, ported from the Playwright TypeScript framework. The integration provides advanced test result reporting, test case mapping, and evidence management capabilities.

## Key Features

### 1. Tag-Based Test Case Mapping
Similar to the Playwright framework's feature file tags, tests can be mapped to Azure DevOps test cases using multiple approaches:

#### Method 1: Using Annotations (Recommended)
```java
@TestPlanId(417)    // Test Plan ID at class level
@TestSuiteId(418)    // Test Suite ID at class level
public class CSADOMappedTest extends CSBaseTest {
    
    @Test(description = "Valid Login Test")
    @TestCaseId(419)  // Direct annotation for test case mapping
    public void testValidLogin() {
        // Test implementation
    }
}
```

#### Method 2: Using Tags in Test Description
```java
@Test(description = "@TestCaseId:419 @TestPlanId:417 @TestSuiteId:418 - Valid Login Test")
public void testValidLogin() {
    // Test implementation
}
```

#### Method 3: Using Method Name Pattern
```java
@Test(description = "Valid Login Test")
public void testCase419_ValidLogin() {
    // Method name contains TestCase419 which is automatically extracted
}
```

## Architecture Components

### 1. Annotations Package (`com.testforge.cs.azuredevops.annotations`)
- **@TestCaseId**: Maps a test method to an Azure DevOps test case
- **@TestPlanId**: Specifies the Azure DevOps test plan ID
- **@TestSuiteId**: Specifies the Azure DevOps test suite ID

### 2. Tag Extractor (`CSADOTagExtractor`)
Extracts ADO metadata from test methods using a priority system:
1. Method-level annotations
2. Class-level annotations
3. Test description tags
4. Method name patterns
5. Configuration defaults

### 3. Enhanced ADO Client (`CSEnhancedADOClient`)
- Retry logic with exponential backoff
- Proxy support for corporate networks
- Batch operations for performance
- Request/response logging
- Connection testing

### 4. Test Suite Manager (`CSTestSuiteManager`)
- Test suite and test point management
- Test case to test point mapping
- Cache management for performance
- Pattern matching for test case IDs

### 5. Test Run Manager (`CSTestRunManager`)
- Test run creation and management
- Result uploading with metadata
- Bug creation for failed tests
- Support for test plan/suite/case hierarchy

### 6. Evidence Uploader (`CSEvidenceUploader`)
- Asynchronous evidence upload
- Screenshot, video, and log attachment
- Archive creation for multiple files
- Configurable upload settings

## Configuration

### Application Properties
```properties
# Azure DevOps Configuration
ado.enabled=true
ado.organization=mdakhan
ado.project=myproject
ado.pat=${ADO_PAT_TOKEN}
ado.test.plan.id=417
ado.test.suite.id=418

# Optional configurations
ado.upload.attachments=true
ado.upload.screenshots=true
ado.create.bugs.on.failure=true
ado.bug.assignedTo=team@example.com
ado.bug.priority=2
ado.bug.severity=3 - Medium
```

### Environment Variables
- `ADO_PAT_TOKEN`: Personal Access Token for authentication
- `ADO_PROXY_HOST`: Proxy host (if required)
- `ADO_PROXY_PORT`: Proxy port (if required)

## Usage Examples

### Example 1: Test Class with Annotations
```java
@TestPlanId(417)
@TestSuiteId(418)
public class MyTestClass extends CSBaseTest {
    
    @Test
    @TestCaseId(419)
    public void testLogin() {
        // Test implementation
    }
}
```

### Example 2: Test with Tag-Based Description
```java
@Test(description = "@TestCaseId:420 @TestPlanId:417 @TestSuiteId:418 - Invalid Login Test")
public void testInvalidLogin() {
    // Test implementation
}
```

### Example 3: Programmatic Test Result Publishing
```java
@AfterMethod
public void publishResultToADO(ITestResult result, Method method) {
    // Extract ADO metadata
    CSADOTagExtractor.ADOMetadata adoMetadata = 
        CSADOTagExtractor.extractADOMetadata(method, this.getClass());
    
    // Create test result
    CSTestResult testResult = new CSTestResult();
    // ... populate test result ...
    
    // Add ADO metadata
    Map<String, String> metadataMap = CSADOTagExtractor.toMetadataMap(adoMetadata);
    testResult.getMetadata().putAll(metadataMap);
    
    // Publish to ADO
    adoPublisher.publishTestResult(testResult);
}
```

## Test Suites

### ADO Mapped Suite (`suites/ado-mapped-suite.xml`)
```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="ADO Mapped Test Suite">
    <test name="ADO Mapped Tests">
        <classes>
            <class name="com.testforge.cs.tests.CSADOMappedTest"/>
        </classes>
    </test>
</suite>
```

### ADO Tagged Suite (`suites/ado-tagged-suite.xml`)
```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="ADO Tagged Test Suite">
    <test name="ADO Tagged Tests">
        <classes>
            <class name="com.testforge.cs.tests.CSADOMappedTest"/>
            <class name="com.testforge.cs.tests.CSADOTaggedTest"/>
        </classes>
    </test>
</suite>
```

## Running Tests

### Command Line
```bash
# Run ADO mapped tests
mvn test -Dsurefire.suiteXmlFiles=suites/ado-mapped-suite.xml

# Run with specific test class
mvn test -Dtest=CSADOMappedTest

# Run with PAT token
export ADO_PAT_TOKEN=your_pat_token_here
mvn test -Dsurefire.suiteXmlFiles=suites/ado-tagged-suite.xml
```

### Windows Batch File
```batch
@echo off
set ADO_PAT_TOKEN=your_pat_token_here
mvn test -Dsurefire.suiteXmlFiles=suites/ado-mapped-suite.xml
```

## Features Comparison with Playwright Framework

| Feature | Playwright Framework | CS TestForge Framework |
|---------|---------------------|------------------------|
| Tag-based mapping | ✓ (Feature file tags) | ✓ (Annotations & descriptions) |
| Test Plan support | ✓ | ✓ |
| Test Suite support | ✓ | ✓ |
| Test Case mapping | ✓ | ✓ |
| Evidence upload | ✓ | ✓ |
| Bug creation | ✓ | ✓ |
| Retry logic | ✓ | ✓ |
| Proxy support | ✓ | ✓ |
| Batch operations | ✓ | ✓ |
| Custom fields | ✓ | ✓ |

## Advanced Features

### 1. Automatic Test Point Discovery
The framework automatically discovers and maps test points based on:
- Test case IDs in annotations
- Test case IDs in method names
- Test case IDs in descriptions

### 2. Evidence Management
- Screenshots are automatically captured and uploaded
- Videos can be attached to test results
- Log files are archived and uploaded
- Custom attachments supported

### 3. Bug Creation Templates
Failed tests can automatically create bugs with:
- Customizable title format
- Detailed error information
- Stack traces
- Reproduction steps
- Configurable priority and severity

### 4. Performance Optimizations
- Connection pooling for API calls
- Batch upload of test results
- Asynchronous evidence upload
- Response caching

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   - Ensure PAT token has correct permissions
   - Check organization and project names

2. **Test Points Not Found**
   - Verify test plan and suite IDs
   - Ensure test cases exist in Azure DevOps

3. **Proxy Issues**
   - Configure proxy settings in application.properties
   - Set proxy environment variables

### Debug Logging
Enable debug logging for troubleshooting:
```properties
logging.level.com.testforge.cs.azuredevops=DEBUG
```

## API Endpoints Used
- Test Plans: `/{project}/_apis/test/plans`
- Test Suites: `/{project}/_apis/test/plans/{planId}/suites`
- Test Points: `/{project}/_apis/test/plans/{planId}/suites/{suiteId}/points`
- Test Runs: `/{project}/_apis/test/runs`
- Test Results: `/{project}/_apis/test/runs/{runId}/results`
- Work Items: `/{project}/_apis/wit/workitems`

## Security Considerations
- PAT tokens should never be committed to source control
- Use environment variables or secure vaults for credentials
- Implement least-privilege access for service accounts
- Regular rotation of PAT tokens recommended

## Future Enhancements
- Support for OAuth authentication
- Integration with Azure Pipelines
- Real-time test result streaming
- Test impact analysis
- Historical trend reporting

## References
- [Azure DevOps REST API Documentation](https://docs.microsoft.com/en-us/rest/api/azure/devops/)
- [TestNG Documentation](https://testng.org/doc/)
- [Selenium Documentation](https://www.selenium.dev/documentation/)