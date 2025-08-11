# Parallel Execution Guide for CS TestForge Framework

## How to Run Tests in Parallel

### Quick Start

To run BDD scenarios in parallel, you need **both** of these attributes in your TestNG suite:

```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Your Suite Name" parallel="methods" data-provider-thread-count="3">
    <test name="Your Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

**Why both?** This is a TestNG quirk:
- `parallel="methods"` tells TestNG to run methods in parallel
- `data-provider-thread-count="3"` tells TestNG to run data-driven tests (like BDD scenarios) in parallel

Yes, it's awkward that TestNG requires two settings for what should be one thing, but that's how TestNG works.

### Understanding TestNG's Parallel Settings

For BDD scenarios (which use data providers), you typically need:

1. **`parallel="methods"`** - Enables parallel execution
2. **`data-provider-thread-count="3"`** - Sets how many threads for BDD scenarios

**Note**: The `thread-count` attribute doesn't affect data-driven tests. This is a TestNG design choice, not a framework limitation.

### Common Configurations

#### Run 3 scenarios in parallel:
```xml
<suite parallel="methods" data-provider-thread-count="3">
```

#### Run 5 scenarios in parallel:
```xml
<suite parallel="methods" data-provider-thread-count="5">
```

#### Sequential execution (default):
```xml
<suite> <!-- No parallel attributes -->
```

### Example Suite Configurations

#### Parallel BDD Scenarios (Simple - Recommended)
```xml
<suite name="BDD Suite" parallel="methods" thread-count="4">
    <parameter name="browser.name" value="chrome"/>
    <parameter name="browser.headless" value="true"/>
    
    <test name="BDD Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

#### Parallel BDD Scenarios (Explicit Control)
```xml
<suite name="BDD Suite" parallel="methods" thread-count="4" data-provider-thread-count="8">
    <!-- Use different thread counts for methods vs data providers -->
    <test name="BDD Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

#### Sequential Execution (Default)
```xml
<suite name="Sequential Suite">
    <!-- No parallel attributes = sequential execution -->
    <test name="Sequential Tests">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
```

### Running Tests

```bash
# Run with specific suite file
mvn test -DsuiteXmlFile=suites/parallel-suite.xml

# Run with default suite (testng.xml)
mvn test
```

### Best Practices

1. **Thread Count**: Start with 2-3 threads and increase based on your machine's capabilities
2. **Headless Mode**: Use `browser.headless=true` for better performance in parallel execution
3. **Independent Tests**: Ensure your tests are independent and don't share state
4. **Resource Management**: The framework handles WebDriver instances per thread automatically

### Troubleshooting

#### Tests Still Running Sequentially?

1. **Check your command**: Make sure you're running the correct suite file
   ```bash
   mvn test -DsuiteXmlFile=your-parallel-suite.xml
   ```

2. **Verify TestNG version**: Ensure you're using TestNG 7.x or higher

3. **Check for locks**: Some singleton implementations might force sequential execution

#### Browser Issues in Parallel?

- The framework automatically manages separate WebDriver instances per thread
- Each thread gets its own browser instance
- Browsers are automatically closed after each test

### Framework Features

The CS TestForge framework:
- ✅ Respects TestNG's parallel execution settings
- ✅ Manages thread-safe WebDriver instances automatically
- ✅ Provides thread-safe reporting
- ✅ Handles screenshots per thread
- ✅ Each thread gets its own isolated browser
- ❌ Does NOT require any framework property changes
- ❌ Does NOT require modifying application.properties

### Summary

**To run BDD scenarios in parallel:**
```xml
<suite name="Your Suite" parallel="methods" data-provider-thread-count="3">
```

**Yes, it's annoying** that TestNG requires `data-provider-thread-count` instead of just using `thread-count`, but that's a TestNG limitation, not a framework issue. The framework has been designed to work seamlessly with TestNG's parallel execution model.