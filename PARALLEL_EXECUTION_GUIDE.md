# Parallel Execution Guide

This guide explains how to run tests in parallel using the CS TestForge Framework.

## Available Parallel Suite Files

### 1. **orangehrm-parallel-tests.xml** (Basic Parallel)
- Runs all tests in parallel at the method level
- Single test group with configurable thread count
- Best for: Quick parallel execution of all tests

```bash
./run-parallel-tests.sh parallel chrome true 3
```

### 2. **orangehrm-parallel-advanced.xml** (Multi-Browser Parallel)
- Runs different test groups in parallel
- Each group can use different browsers
- Test groups run in parallel, methods within each group also run in parallel
- Best for: Cross-browser testing

```bash
./run-parallel-tests.sh parallel-advanced chrome true 2
```

### 3. **orangehrm-parallel-tags.xml** (Tag-Based Parallel)
- Runs tests based on tags (@smoke, @data-driven, @complex)
- Different thread counts for different test types
- Best for: Organized test execution by priority/type

```bash
./run-parallel-tests.sh parallel-tags chrome true 3
```

## Parallel Execution Modes

### Method-Level Parallelism
```xml
<suite name="Suite" parallel="methods" thread-count="3">
```
- Each test method runs in its own thread
- Maximum parallelism
- Requires thread-safe test design

### Test-Level Parallelism
```xml
<suite name="Suite" parallel="tests" thread-count="2">
```
- Each `<test>` tag runs in its own thread
- Good for grouping related tests
- Easier to manage test data isolation

### Class-Level Parallelism
```xml
<suite name="Suite" parallel="classes" thread-count="2">
```
- Each test class runs in its own thread
- Good balance between speed and isolation

## Configuration Options

### 1. Thread Count
Control the number of parallel threads:
```xml
<parameter name="test.thread.count" value="3"/>
```
Or via command line:
```bash
-Dtest.thread.count=5
```

### 2. Browser Configuration
Each thread gets its own browser instance:
```xml
<parameter name="browser.name" value="chrome"/>
<parameter name="browser.headless" value="true"/>
```

### 3. Data Provider Parallel Execution
The framework's DataProvider is configured for parallel execution:
```java
@DataProvider(name = "featureFiles", parallel = true)
```

## Best Practices

### 1. Thread Safety
- Each thread maintains its own WebDriver instance
- Use ThreadLocal for thread-specific data
- Avoid static shared state

### 2. Test Independence
- Tests should not depend on execution order
- Each test should set up its own data
- Clean up after each test

### 3. Resource Management
- Browsers are reused within a thread
- Cleanup happens at suite level
- Screenshots are thread-safe with unique names

### 4. Optimal Thread Count
- CPU cores: 2-4 threads per core
- Memory: ~500MB per browser instance
- Network: Consider API rate limits
- Recommended: Start with 3-5 threads

## Running Parallel Tests

### Using Shell Scripts
```bash
# Basic parallel execution
./run-parallel-tests.sh

# Specify suite and parameters
./run-parallel-tests.sh parallel-advanced firefox false 4

# Available options:
# $1: Suite type (parallel, parallel-advanced, parallel-tags)
# $2: Browser (chrome, firefox, edge)
# $3: Headless (true, false)
# $4: Thread count (1-10)
```

### Using Maven Directly
```bash
# Basic parallel
mvn test -DsuiteXmlFile=suites/orangehrm-parallel-tests.xml

# With custom thread count
mvn test -DsuiteXmlFile=suites/orangehrm-parallel-tests.xml -Dtest.thread.count=5

# With all options
mvn test \
  -DsuiteXmlFile=suites/orangehrm-parallel-advanced.xml \
  -Dbrowser.name=chrome \
  -Dbrowser.headless=true \
  -Dtest.thread.count=4 \
  -Denvironment.name=qa
```

## Monitoring Parallel Execution

### Console Output
Watch for thread names in logs:
```
[TestNG-PoolService-0] Starting test...
[TestNG-PoolService-1] Starting test...
[TestNG-PoolService-2] Starting test...
```

### HTML Report
The report shows:
- Execution timeline
- Thread distribution
- Parallel execution metrics

### Performance Metrics
Monitor:
- Total execution time
- Thread utilization
- Resource usage
- Failure patterns

## Troubleshooting

### Common Issues

1. **Browser Timeout**
   - Increase timeout: `-Dbrowser.page.load.timeout=90`
   - Check network connectivity

2. **Out of Memory**
   - Reduce thread count
   - Increase heap size: `-Xmx2g`

3. **Element Not Found**
   - Add explicit waits
   - Check for race conditions

4. **Port Already in Use**
   - WebDriver uses different ports per instance
   - Check for hanging browser processes

### Debug Mode
Enable detailed logging:
```bash
mvn test -DsuiteXmlFile=suites/orangehrm-parallel-tests.xml -Dlog.level=DEBUG
```

## Example Scenarios

### Scenario 1: Quick Smoke Test
Run only smoke tests in parallel:
```bash
./run-parallel-tests.sh parallel-tags chrome true 5
```

### Scenario 2: Full Regression
Run all tests with maximum parallelism:
```bash
./run-parallel-tests.sh parallel chrome true 8
```

### Scenario 3: Cross-Browser Testing
Run tests on multiple browsers:
```bash
./run-parallel-tests.sh parallel-advanced chrome true 2
```

## Performance Comparison

| Execution Mode | Thread Count | Typical Time | Notes |
|----------------|--------------|--------------|-------|
| Sequential     | 1            | 10 min       | Baseline |
| Parallel       | 3            | 4 min        | 60% faster |
| Parallel       | 5            | 3 min        | 70% faster |
| Parallel       | 8            | 2.5 min      | Diminishing returns |

## Conclusion

Parallel execution can significantly reduce test execution time. Start with 3-5 threads and adjust based on your system resources and test characteristics. The framework handles thread safety and browser management automatically.