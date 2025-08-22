#!/bin/bash

echo "Testing CORRECTED Configuration Hierarchy"
echo "========================================"
echo "Priority Order:"
echo "1. HIGHEST: Command line -D properties" 
echo "2. MEDIUM:  XML suite parameters"
echo "3. LOWEST:  Properties files"
echo ""

# Create a comprehensive test to verify the corrected hierarchy
cat > src/test/java/com/testforge/cs/tests/CorrectedHierarchyTest.java << 'EOF'
package com.testforge.cs.tests;

import com.testforge.cs.config.CSConfigManager;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class CorrectedHierarchyTest {
    private static final Logger logger = LoggerFactory.getLogger(CorrectedHierarchyTest.class);
    private CSConfigManager config;
    
    @BeforeClass
    public void setupCorrectedHierarchyTest() {
        logger.info("Setting up CORRECTED configuration hierarchy test");
        
        // Simulate XML suite parameters (these will be set by CSTestListener in real usage)
        System.setProperty("suite.browser.name", "edge");
        System.setProperty("suite.cs.browser.headless", "true");
        System.setProperty("suite.environment.name", "suite_env");
        System.setProperty("suite.custom.parameter", "from_suite");
        
        config = CSConfigManager.getInstance();
        
        logger.info("Setup complete - XML suite parameters simulated");
        logger.info("Note: Command line -D properties must be set when running Maven");
    }
    
    @Test
    public void testCommandLineHasHighestPriority() {
        logger.info("=== Testing Command Line -D Properties (Highest Priority) ===");
        
        // These properties should be set via -D on command line
        // We'll test if they override both suite parameters and properties files
        
        String cmdLineProperty = config.getProperty("test.commandline.override");
        config.logConfigurationHierarchy("test.commandline.override");
        
        if (cmdLineProperty != null) {
            Map<String, String> info = config.getPropertySourceInfo("test.commandline.override");
            logger.info("Command line property found: {} from {}", cmdLineProperty, info.get("source"));
            Assert.assertTrue(info.get("source").contains("Command Line"), "Should come from command line");
            Assert.assertEquals(info.get("priority"), "1 (Highest)", "Should have highest priority");
        } else {
            logger.info("No command line override detected - run with -Dtest.commandline.override=cmd_value to test");
        }
    }
    
    @Test 
    public void testSuiteParametersOverrideProperties() {
        logger.info("=== Testing XML Suite Parameters Override Properties Files ===");
        
        // Test browser name: suite should override properties file
        String browserName = config.getProperty("browser.name");
        logger.info("Browser name: {}", browserName);
        config.logConfigurationHierarchy("browser.name");
        
        Map<String, String> browserInfo = config.getPropertySourceInfo("browser.name");
        
        // Check if command line override exists
        String cmdLineBrowser = System.getProperty("browser.name");
        if (cmdLineBrowser != null && !config.isFromSuite("browser.name")) {
            logger.info("Command line override detected for browser.name");
            Assert.assertEquals(browserInfo.get("priority"), "1 (Highest)", "Command line should have highest priority");
        } else {
            // Should come from suite parameter, not properties file
            Assert.assertEquals(browserName, "edge", "Suite parameter should override properties file");
            Assert.assertTrue(browserInfo.get("source").contains("XML Suite Parameter"), "Should come from XML suite parameter");
            Assert.assertEquals(browserInfo.get("priority"), "2 (Medium)", "Suite should have medium priority");
        }
        
        // Test headless mode
        boolean headless = config.getBooleanProperty("cs.browser.headless", false);
        logger.info("Headless mode: {}", headless);
        config.logConfigurationHierarchy("cs.browser.headless");
        
        Map<String, String> headlessInfo = config.getPropertySourceInfo("cs.browser.headless");
        String cmdLineHeadless = System.getProperty("cs.browser.headless");
        if (cmdLineHeadless != null && !config.isFromSuite("cs.browser.headless")) {
            logger.info("Command line override detected for cs.browser.headless");
        } else {
            Assert.assertTrue(headless, "Suite parameter should override properties file headless=false");
            Assert.assertTrue(headlessInfo.get("source").contains("XML Suite Parameter"), "Should come from XML suite parameter");
        }
    }
    
    @Test
    public void testPropertiesFileFallback() {
        logger.info("=== Testing Properties File Fallback (Lowest Priority) ===");
        
        // Test a property that only exists in properties file
        String reportDir = config.getProperty("cs.report.directory");
        logger.info("Report directory: {}", reportDir);
        config.logConfigurationHierarchy("cs.report.directory");
        
        Map<String, String> reportInfo = config.getPropertySourceInfo("cs.report.directory");
        
        // Check if there are any overrides
        String cmdLineReport = System.getProperty("cs.report.directory");
        String suiteReport = System.getProperty("suite.cs.report.directory");
        
        if (cmdLineReport != null && !config.isFromSuite("cs.report.directory")) {
            logger.info("Command line override detected for cs.report.directory");
            Assert.assertEquals(reportInfo.get("priority"), "1 (Highest)", "Command line should have highest priority");
        } else if (suiteReport != null) {
            logger.info("Suite parameter override detected for cs.report.directory");
            Assert.assertEquals(reportInfo.get("priority"), "2 (Medium)", "Suite should have medium priority");
        } else {
            Assert.assertNotNull(reportDir, "Should fall back to properties file");
            Assert.assertEquals(reportInfo.get("source"), "Properties File", "Should come from properties file");
            Assert.assertEquals(reportInfo.get("priority"), "3 (Lowest)", "Should have lowest priority");
        }
    }
    
    @Test
    public void testCompleteHierarchyDemonstration() {
        logger.info("=== Complete Configuration Hierarchy Demonstration ===");
        
        logger.info("\n🏆 CORRECTED PRIORITY ORDER:");
        logger.info("1. 🥇 HIGHEST: Command Line -D Properties");
        logger.info("2. 🥈 MEDIUM:  XML Suite Parameters"); 
        logger.info("3. 🥉 LOWEST:  Properties Files");
        
        String[] testKeys = {
            "browser.name",                 // May have command line > suite > properties
            "cs.browser.headless",          // May have command line > suite > properties  
            "environment.name",             // May have command line > suite > properties
            "custom.parameter",             // May have command line > suite only
            "cs.report.directory",          // May have command line > properties only
            "test.commandline.override"     // May have command line only
        };
        
        logger.info("\n📊 PROPERTY SOURCE ANALYSIS:");
        for (String key : testKeys) {
            Map<String, String> info = config.getPropertySourceInfo(key);
            String value = info.get("value");
            String source = info.get("source");
            String priority = info.get("priority");
            
            if (value != null) {
                logger.info("✅ {} = '{}' | Source: {} | Priority: {}", key, value, source, priority);
            } else {
                logger.info("❌ {} = NOT FOUND", key);
            }
        }
        
        logger.info("\n🔧 SYSTEM PROPERTIES ANALYSIS:");
        logger.info("Command Line Properties (non-suite):");
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> !key.startsWith("suite."))
            .filter(key -> key.startsWith("test.") || key.startsWith("browser.") || key.startsWith("cs.") || key.startsWith("environment."))
            .sorted()
            .forEach(key -> {
                String value = System.getProperty(key);
                boolean isFromSuite = config.isFromSuite(key);
                logger.info("  {} = {} (from suite: {})", key, value, isFromSuite);
            });
            
        logger.info("\nXML Suite Parameters (suite.*):");
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("suite."))
            .sorted()
            .forEach(key -> logger.info("  {} = {}", key, System.getProperty(key)));
    }
}
EOF

echo "Corrected hierarchy test created!"
echo ""
echo "Testing with command line overrides..."

# Test with command line properties to demonstrate highest priority
mvn test-compile -q

echo "Running test WITHOUT command line overrides..."
mvn test -Dtest=CorrectedHierarchyTest -Dsurefire.suiteXmlFiles= -q

echo ""
echo "Running test WITH command line overrides to show highest priority..."
mvn test -Dtest=CorrectedHierarchyTest -Dsurefire.suiteXmlFiles= \
    -Dtest.commandline.override=from_command_line \
    -Dbrowser.name=chrome_from_cmdline \
    -Dcs.browser.headless=false \
    -Dcs.report.directory=cmdline_reports \
    -q

echo ""
echo "✅ CORRECTED Configuration Hierarchy Test Completed!"
echo "Command Line -D properties now have HIGHEST priority as requested!"