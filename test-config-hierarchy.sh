#!/bin/bash

echo "Testing Enhanced Configuration Hierarchy"
echo "========================================"

# Create a test suite with parameters to test the hierarchy
cat > suites/test-config-hierarchy-suite.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<suite name="ConfigHierarchyTestSuite">
    <parameter name="browser.name" value="firefox"/>
    <parameter name="cs.browser.headless" value="true"/>
    <parameter name="test.parameter" value="suite_value"/>
    <parameter name="environment.name" value="suite_env"/>
    
    <test name="ConfigTest">
        <classes>
            <class name="com.testforge.cs.tests.ConfigHierarchyTest"/>
        </classes>
    </test>
</suite>
EOF

# Create a simple test to verify configuration hierarchy
mkdir -p src/test/java/com/testforge/cs/tests
cat > src/test/java/com/testforge/cs/tests/ConfigHierarchyTest.java << 'EOF'
package com.testforge.cs.tests;

import com.testforge.cs.config.CSConfigManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ConfigHierarchyTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigHierarchyTest.class);
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    @Test
    public void testSuiteParameterOverride() {
        logger.info("Testing suite parameter override functionality");
        
        // Test 1: Suite parameter should override properties file
        String browserName = config.getProperty("browser.name");
        logger.info("Browser name from config: {}", browserName);
        config.logConfigurationHierarchy("browser.name");
        
        // Test 2: Boolean suite parameter
        boolean headless = config.getBooleanProperty("cs.browser.headless", false);
        logger.info("Headless mode: {}", headless);
        config.logConfigurationHierarchy("cs.browser.headless");
        
        // Test 3: Custom test parameter
        String testParam = config.getProperty("test.parameter");
        logger.info("Test parameter: {}", testParam);
        config.logConfigurationHierarchy("test.parameter");
        
        // Test 4: Environment name
        String envName = config.getProperty("environment.name");
        logger.info("Environment name: {}", envName);
        config.logConfigurationHierarchy("environment.name");
        
        // Verify that suite parameters are being used
        Map<String, String> browserInfo = config.getPropertySourceInfo("browser.name");
        logger.info("Browser name source info: {}", browserInfo);
        
        // Log all system properties starting with "suite."
        logger.info("All suite parameters:");
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("suite."))
            .forEach(key -> logger.info("  {} = {}", key, System.getProperty(key)));
    }
    
    @Test
    public void testConfigurationSourceInfo() {
        logger.info("Testing configuration source information");
        
        // Test source info for various keys
        String[] testKeys = {"browser.name", "cs.browser.headless", "test.parameter", "nonexistent.key"};
        
        for (String key : testKeys) {
            Map<String, String> info = config.getPropertySourceInfo(key);
            logger.info("Key '{}': Value='{}', Source='{}', Priority='{}'", 
                key, info.get("value"), info.get("source"), info.get("priority"));
        }
    }
}
EOF

echo "Test files created successfully!"
echo ""
echo "Running configuration hierarchy test..."

# Compile the test
mvn test-compile -q

# Run the specific test
mvn test -Dtest=ConfigHierarchyTest -Dsurefire.suiteXmlFiles=suites/test-config-hierarchy-suite.xml

echo ""
echo "Configuration hierarchy test completed!"
echo "Check the logs above to see the hierarchy in action."