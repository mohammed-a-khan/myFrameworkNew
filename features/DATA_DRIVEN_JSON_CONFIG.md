# Data-Driven Testing with JSON Configuration in Examples

## Overview
The CS Framework supports JSON configuration strings in the Examples section of Scenario Outlines. This allows dynamic data loading from various sources without hardcoding test data.

## Syntax
```gherkin
Examples: {"type": "source_type", "source": "path/to/data", ...configuration...}
  | column1 | column2 | column3 |
```

## Supported Data Source Types

### 1. Excel
```gherkin
Examples: {"type": "excel", "source": "testdata/users.xlsx", "sheet": "ValidLogins", "key": "TestCase", "filter": "Environment=QA;Status=Active"}
  | TestCase | username | password | expectedUser |
```
- `sheet`: Excel sheet name
- `key`: Unique identifier column
- `filter`: Filter criteria (key=value pairs separated by semicolon)

### 2. CSV
```gherkin
Examples: {"type": "csv", "source": "testdata/invalid_credentials.csv", "filter": "Priority=High"}
  | username | password | errorMessage |
```

### 3. JSON
```gherkin
Examples: {"type": "json", "source": "testdata/login_test_scenarios.json", "path": "$.testCases[*]", "filter": "TestType=LoginValidation"}
  | username | password | expectedResult | expectedTitle |
```
- `path`: JSONPath expression to locate test data

### 4. Database
```gherkin
Examples: {"type": "database", "query": "SELECT * FROM test_data WHERE active = 1", "connection": "default"}
  | username | password | expectedResult |
```
- `query`: SQL query to fetch data
- `connection`: Database connection name from config

### 5. API
```gherkin
Examples: {"type": "api", "endpoint": "https://api.example.com/testdata", "method": "GET", "headers": {"Authorization": "Bearer ${TOKEN}"}}
  | username | password | expectedUser |
```
- `endpoint`: API URL
- `method`: HTTP method
- `headers`: Request headers
- Environment variables supported with ${VAR_NAME}

### 6. Properties
```gherkin
Examples: {"type": "properties", "source": "test.properties", "prefix": "login.test."}
  | testId | username | password | expectedResult |
```
- `prefix`: Property key prefix to filter

### 7. YAML
```gherkin
Examples: {"type": "yaml", "source": "tests.yaml", "path": "tests.login", "filter": "priority=1"}
  | username | password |
```

## Filter Options
- Simple: `"filter": "Environment=QA"`
- Multiple: `"filter": "Environment=QA;Status=Active"`
- Complex: `"filter": "Priority=High;TestType!=Performance"`

## Benefits
1. **Dynamic Data**: Load test data at runtime
2. **Environment Specific**: Filter data based on environment
3. **Maintainable**: Update test data without changing feature files
4. **Reusable**: Share data across multiple scenarios
5. **Version Controlled**: Track data changes separately from tests

## Best Practices
1. Keep column headers in Examples section matching data source columns
2. Use meaningful filter criteria
3. Store sensitive data (passwords, tokens) in secure locations
4. Use environment variables for configuration values
5. Validate data source accessibility before test execution