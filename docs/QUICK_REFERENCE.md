# CS TestForge Framework - Quick Reference

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