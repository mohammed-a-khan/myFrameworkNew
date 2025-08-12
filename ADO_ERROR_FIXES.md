# Azure DevOps Integration Error Fixes

## Errors Fixed

### 1. 405 Method Not Allowed Error on PATCH Request
**Error:** `ADO request failed: 405 Unknown` when completing test run

**Cause:** Malformed URL - the API version was being appended incorrectly
- Wrong: `/test/runs?api-version=7.1/82`
- Correct: `/test/runs/82?api-version=7.1`

**Fix Applied:** Modified URL construction in `CSTestRunManager.java`:
```java
// Before
String url = config.buildUrl(config.getEndpoints().getTestRuns(), null) + "/" + currentTestRun.id;

// After  
String url = config.buildUrl(config.getEndpoints().getTestRuns() + "/" + currentTestRun.id, null);
```

### 2. JSON Parsing Error When Posting Test Results
**Error:** `Cannot deserialize value of type java.util.ArrayList from Object value`

**Cause:** Azure DevOps API returns test results wrapped in an object with a "value" property, not as a direct array

**Response Structure:**
```json
{
  "value": [
    { "id": 1, "testCase": {...}, "outcome": "Passed" },
    { "id": 2, "testCase": {...}, "outcome": "Failed" }
  ],
  "count": 2
}
```

**Fix Applied:** Updated response parsing in `CSTestRunManager.java`:
```java
// Before - expecting List directly
CSEnhancedADOClient.ADOResponse<List<Map<String, Object>>> response = 
    client.post(url, results, List.class);

// After - expecting Map with "value" property
CSEnhancedADOClient.ADOResponse<Map<String, Object>> response = 
    client.post(url, results, Map.class);
List<Map<String, Object>> createdResults = 
    (List<Map<String, Object>>) response.data.get("value");
```

## Files Modified
- `/src/main/java/com/testforge/cs/azuredevops/managers/CSTestRunManager.java`
  - Fixed `completeTestRun()` method URL construction
  - Fixed `abortTestRun()` method URL construction  
  - Fixed `addTestResult()` method response parsing
  - Fixed `addTestResults()` method response parsing

## Testing
After these fixes, the ADO integration should:
1. ✅ Create test run successfully
2. ✅ Post test results without JSON parsing errors
3. ✅ Complete test run without 405 errors
4. ✅ Update test run status to "Completed"

## How to Verify
1. Set your PAT token:
```bash
export ADO_PAT_TOKEN=your_pat_token
```

2. Run the fixed test:
```bash
./test-ado-fixed.sh
```

3. Check Azure DevOps:
- Go to https://dev.azure.com/mdakhan/myproject/_testManagement
- Look for the test run with status "Completed"
- Verify test results show Pass/Fail outcomes
- Check for uploaded screenshots

## Expected Behavior
- Test run should be created with status "InProgress"
- Test results should be posted for each test case
- Screenshots should be uploaded as attachments
- Test run should be marked as "Completed" after all tests finish
- No JSON parsing errors in the logs
- No 405 Method Not Allowed errors