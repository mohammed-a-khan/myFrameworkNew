# Parallel Execution Guide

This guide explains how to configure parallel execution in the CS TestForge Framework for data-driven tests.

## Overview

The framework supports parallel execution at multiple levels using TestNG's parallel execution capabilities. With proper configuration, you can run tests across multiple threads to reduce execution time significantly.

## Configuration Options

### 1. TestNG Suite XML Configuration

The primary way to configure parallel execution is through your TestNG suite XML file.

#### Basic Configuration

```xml
<suite name="Your Suite Name" parallel="methods" thread-count="3" data-provider-thread-count="3">
    <test name="Your Test">
        <parameter name="featuresPath" value="features/your-feature.feature"/>
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

#### Key Attributes

- **`parallel`** - Controls what runs in parallel:
  - `"methods"` - Run test methods in parallel (recommended for data-driven tests)
  - `"tests"` - Run `<test>` tags in parallel
  - `"classes"` - Run test classes in parallel
  - `"instances"` - Run test class instances in parallel
  - `"none"` - Disable parallel execution (sequential mode)

- **`thread-count`** - Number of threads for parallel execution (default: 5)
  
- **`data-provider-thread-count`** - Number of threads for data provider parallel execution

### 2. Maven Command Line Configuration

You can override suite settings from the command line without modifying XML files.

#### Set Thread Count
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Dthreadcount=5
```

#### Set Data Provider Thread Count
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Ddataproviderthreadcount=5
```

#### Force Sequential Execution
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/your-suite.xml -Dparallel=none
```

#### Combined Example
```bash
mvn test -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml -Dthreadcount=10 -Ddataproviderthreadcount=10
```

### 3. Maven POM Configuration

The framework's `pom.xml` includes default settings:

```xml
<configuration>
    <properties>
        <property>
            <name>dataproviderthreadcount</name>
            <value>3</value>
        </property>
    </properties>
</configuration>
```

This can be overridden via command line: `mvn test -Ddataproviderthreadcount=10`

## Examples

### Example 1: Sequential Execution Suite

Create `testng-sequential.xml`:
```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Sequential Test Suite">
    
    <test name="Login Tests Sequential">
        <parameter name="featuresPath" value="features/login-data-driven-examples.feature"/>
        <parameter name="tags" value="@data-driven"/>
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
    
</suite>
```

Run with: `mvn test -Dsurefire.suiteXmlFiles=suites/testng-sequential.xml`

### Example 2: High Parallelism Suite

Create `testng-parallel-high.xml`:
```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="High Parallel Test Suite" parallel="methods" thread-count="10" data-provider-thread-count="10">
    
    <test name="Login Tests Parallel">
        <parameter name="featuresPath" value="features/login-data-driven-examples.feature"/>
        <parameter name="tags" value="@data-driven"/>
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
    
</suite>
```

Run with: `mvn test -Dsurefire.suiteXmlFiles=suites/testng-parallel-high.xml`

### Example 3: Moderate Parallelism (Default)

The existing `testng-data-driven-examples.xml` uses 3 threads:
```xml
<suite name="Data Driven Tests" parallel="methods" thread-count="3" data-provider-thread-count="3">
```

## Browser Management in Parallel Execution

When running tests in parallel, the framework automatically manages browser instances:

- **One browser per thread** - Each thread gets its own browser instance
- **Browser reuse** - The same browser is reused for all tests on a thread
- **Automatic cleanup** - Browsers are closed when all tests complete

### How It Works

1. First test on a thread creates a new browser
2. Subsequent tests on the same thread reuse the existing browser
3. Browser state is cleared between tests (cookies, navigation)
4. All browsers are closed after test suite completion

## Performance Considerations

### Choosing Thread Count

- **CPU Cores**: Don't exceed 2x your CPU core count
- **Memory**: Each browser instance uses ~200-500MB RAM
- **Test Type**: UI tests benefit from 3-5 threads; API tests can use more

### Recommended Settings

- **Local Development**: 2-4 threads
- **CI/CD Pipeline**: 4-8 threads (depending on agent resources)
- **Dedicated Test Server**: 8-16 threads

## Troubleshooting

### Issue: Tests fail with parallel execution but pass sequentially

**Solution**: Ensure your tests are thread-safe:
- No static shared state
- Proper test data isolation
- Thread-safe page objects

### Issue: Browser windows pile up

**Solution**: This has been fixed in the framework. Browsers are now reused per thread.

### Issue: Out of memory errors

**Solution**: Reduce thread count or increase JVM heap:
```bash
mvn test -Xmx4g -Dsurefire.suiteXmlFiles=suites/your-suite.xml
```

## Quick Reference

| Execution Type | Suite Configuration | Command Line Override |
|----------------|-------------------|----------------------|
| Sequential | `parallel="none"` or omit `parallel` | `-Dparallel=none` |
| 3 Threads | `parallel="methods" thread-count="3"` | `-Dthreadcount=3` |
| 5 Threads | `parallel="methods" thread-count="5"` | `-Dthreadcount=5` |
| 10 Threads | `parallel="methods" thread-count="10"` | `-Dthreadcount=10` |

## Best Practices

1. **Start Small**: Begin with 2-3 threads and increase gradually
2. **Monitor Resources**: Watch CPU and memory usage
3. **Test Stability**: Ensure tests pass consistently before increasing parallelism
4. **Environment Specific**: Create different suite files for different environments
5. **Data Independence**: Ensure test data doesn't conflict between parallel tests

## Example Commands

```bash
# Run with default settings (3 threads)
mvn test -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml

# Run with 5 threads
mvn test -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml -Dthreadcount=5 -Ddataproviderthreadcount=5

# Run sequentially
mvn test -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml -Dparallel=none

# Run with high memory allocation
mvn test -Xmx4g -Dsurefire.suiteXmlFiles=suites/testng-data-driven-examples.xml -Dthreadcount=8
```