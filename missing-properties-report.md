# Missing Properties Report

## Properties Currently Missing from Configuration

These properties are being requested by the framework but are not defined in your `application.properties` file. Here's what each one does and the recommended values:

### 1. Element Interaction Properties
```properties
# Maximum retry attempts for element interactions
cs.element.max.retries=3

# Delay between retry attempts (in milliseconds)
cs.element.retry.delay=500

# Whether to highlight elements during interaction (useful for debugging)
cs.element.highlight=false

# Take screenshot on every element action
cs.screenshot.on.action=false
```

**Significance:**
- `cs.element.max.retries`: How many times to retry finding/interacting with an element before failing
- `cs.element.retry.delay`: Wait time between retry attempts
- `cs.element.highlight`: Visually highlights elements being interacted with (useful for demos/debugging)
- `cs.screenshot.on.action`: Captures a screenshot for every element action (can slow down tests)

### 2. Screenshot Properties
```properties
# Take screenshot when test passes
screenshot.on.pass=false

# Take screenshot for all test results (overrides individual settings)
screenshot.on.all=false
```

**Significance:**
- `screenshot.on.pass`: Captures screenshots for passing tests (useful for documentation)
- `screenshot.on.all`: Captures screenshots regardless of test result

### 3. Execution Properties
```properties
# Execution mode (sequential or parallel)
execution.mode=parallel
```

**Significance:**
- `execution.mode`: Default execution mode when not specified in suite XML

### 4. Performance Properties (Often Missing)
```properties
# Page load strategy (normal, eager, none)
browser.page.load.strategy=normal

# Element visibility check timeout
cs.element.visibility.timeout=10

# Stale element retry count
cs.element.stale.retry.count=3
```

### 5. Reporting Properties (Often Missing)
```properties
# Include stack traces in reports
report.include.stacktrace=true

# Report generation timeout
report.generation.timeout=60000

# Archive old reports
report.archive.enabled=true
```

## Why These Warnings Appear

The warnings appear because:
1. The code uses `config.getProperty("property.name")` without providing a default value
2. The framework checks for these optional properties to enable/disable features
3. These are mostly optional enhancement properties, not critical for execution

## Impact of Missing Properties

- **No Critical Impact**: The framework uses sensible defaults when these properties are missing
- **Performance**: Some missing properties might cause slightly slower execution (e.g., no retry optimization)
- **Debugging**: Missing properties like `cs.element.highlight` means you won't see visual feedback during test execution
- **Reporting**: Less detailed reports without some optional properties

## Recommendation

To eliminate these warnings, you can:
1. Add all missing properties to your `application.properties` file with appropriate values
2. Or, modify the code to use default values when calling `getProperty()`
3. Or, simply ignore the warnings as they don't affect test execution

The warnings are informational and don't indicate any problems with your test execution.