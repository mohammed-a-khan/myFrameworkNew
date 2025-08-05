# Data-Driven Testing in CS Framework

The CS Framework supports data-driven testing in BDD scenarios using two approaches:
1. `@CSDataSource` annotation on scenarios
2. JSON configuration in the `Examples:` section of Scenario Outlines

Both approaches allow you to run the same scenario multiple times with different data sets from external sources.

## Supported Data Sources

1. **Excel** (.xlsx, .xls)
2. **CSV** (.csv)
3. **JSON** (.json)
4. **Database** (via SQL queries)
5. **Properties** (.properties)

## Usage

### Method 1: Using @CSDataSource Annotation

Add the `@CSDataSource` annotation above your scenario:

```gherkin
@CSDataSource("type=excel, source=testdata/users.xlsx, sheet=ValidUsers, filter=Status=Active")
Scenario: Login with different users
  When I enter username "<Username>"
  And I enter password "<Password>"
  Then I should see "<ExpectedResult>"
```

### Method 2: Using Examples with JSON Configuration

Use JSON configuration in the Examples section of a Scenario Outline:

```gherkin
Scenario Outline: Create products from external data
  When I create a product with name "<ProductName>"
  And I set category to "<Category>"
  Then product should be created successfully
  Examples: {"type": "excel", "source": "testdata/products.xlsx", "sheet": "ProductData", "filter": "Status=Active"}
```

### Parameters

- **type**: The type of data source (excel, csv, json, database, properties)
- **source/path/location**: Path to the data file (relative to src/test/resources)
- **sheet**: Sheet name for Excel files
- **key/keyField**: Key field for filtering specific rows
- **keyValues**: Comma-separated list of key values to include
- **filter**: Additional filter conditions (format: Field1=Value1;Field2=Value2)
- **database/name**: Database name for SQL queries
- **query**: Direct SQL query
- **queryKey**: Reference to query in queries.properties file
- **param.XXX**: Parameters for SQL queries

## Examples

### Excel Data Source

Using @CSDataSource annotation:
```gherkin
@CSDataSource("type=excel, source=testdata/employees.xlsx, sheet=NewEmployees, key=TestID, filter=Department=IT;Status=Active")
Scenario: Add employees from Excel
  When I add employee "<FirstName>" "<LastName>"
  Then employee "<EmployeeID>" should exist
```

Using Examples JSON:
```gherkin
Scenario Outline: Add employees from Excel
  When I add employee "<FirstName>" "<LastName>"
  Then employee "<EmployeeID>" should exist
  Examples: {"type": "excel", "source": "testdata/employees.xlsx", "sheet": "NewEmployees", "filter": "Department=IT;Status=Active"}
```

### CSV Data Source

```gherkin
@CSDataSource("type=csv, source=testdata/test_data.csv, key=TestCaseID, keyValues=TC01,TC02,TC03")
Scenario: Process CSV data
  Given I have data from CSV
  When I process "<DataField>"
  Then result should be "<ExpectedResult>"
```

### JSON Data Source

```gherkin
@CSDataSource("type=json, source=testdata/api_data.json, filter=Priority=High")
Scenario: API test with JSON data
  When I call API with payload:
    """
    {
      "name": "<Name>",
      "email": "<Email>"
    }
    """
  Then response code should be <StatusCode>
```

### Database Data Source

Using @CSDataSource annotation:
```gherkin
@CSDataSource("type=database, name=testdb, queryKey=query.active.users, param.status=Active, param.role=Admin")
Scenario: Verify database records
  When I search for user "<user_id>"
  Then I should see user details:
    | Name  | <first_name> <last_name> |
    | Email | <email>                  |
    | Role  | <role>                   |
```

Using Examples JSON with direct query:
```gherkin
Scenario Outline: Verify employee records
  When I search for employee "<emp_id>"
  Then I should see name "<first_name> <last_name>"
  And department should be "<department>"
  Examples: {"type": "database", "name": "hrmsdb", "query": "SELECT emp_id, first_name, last_name, department FROM employees WHERE status = 'Active'"}
```

### Properties Data Source

```gherkin
@CSDataSource("type=properties, source=testdata/config.properties")
Scenario: Configuration-driven test
  Given I navigate to "<app.url>"
  When I login with "<test.username>" and "<test.password>"
  Then I should see "<expected.dashboard>"
```

## Data Access in Step Definitions

Step definitions can access data row values using the `getDataValue()` method:

```java
@CSStep(value = "^I enter username \"([^\"]*)\"$")
public void iEnterUsername(String username) {
    // If username is a placeholder like <Username>, it will be replaced
    String actualUsername = resolveDataValue(username);
    findElement("id:username", "Username field").type(actualUsername);
}

private String resolveDataValue(String value) {
    if (value.startsWith("<") && value.endsWith(">")) {
        String columnName = value.substring(1, value.length() - 1);
        String dataValue = getDataValue(columnName);
        if (dataValue != null) {
            return dataValue;
        }
    }
    return value;
}
```

## How It Works

1. The `CSFeatureParser` identifies:
   - Scenarios with `@CSDataSource` annotations
   - Scenario Outlines with JSON configuration in Examples section
2. The `CSDataSourceProcessor` loads data from the specified source
3. The scenario is expanded into multiple scenarios, one for each data row
4. Each expanded scenario has access to its data row through the scenario context
5. Step definitions can retrieve data values using placeholder syntax `<ColumnName>`

## Choosing Between @CSDataSource and Examples JSON

- **Use @CSDataSource when:**
  - You have a regular Scenario (not Scenario Outline)
  - You want to keep the traditional Gherkin syntax clean
  - You're converting existing scenarios to data-driven

- **Use Examples JSON when:**
  - You're already using Scenario Outline
  - You want the data source configuration close to the scenario
  - You prefer the Examples section to indicate data-driven behavior

## Best Practices

1. Use descriptive column names in your data sources
2. Apply filters to run specific subsets of data
3. Keep test data files in the `src/test/resources/testdata` directory
4. Use the `key` parameter to identify specific test cases
5. Combine with Azure DevOps annotations for traceability

## Troubleshooting

1. **Data not loading**: Check file path is relative to src/test/resources
2. **Placeholders not replaced**: Ensure column names match exactly (case-sensitive)
3. **Filter not working**: Verify filter syntax (Field=Value;Field2=Value2)
4. **Database connection issues**: Check database configuration in config files