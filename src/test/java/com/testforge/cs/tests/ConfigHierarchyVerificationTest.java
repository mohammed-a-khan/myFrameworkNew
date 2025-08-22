package com.testforge.cs.tests;

import com.testforge.cs.config.CSConfigManager;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ConfigHierarchyVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigHierarchyVerificationTest.class);
    private CSConfigManager config;
    
    @BeforeClass
    public void setupHierarchyTest() {
        logger.info("Setting up configuration hierarchy test");
        
        // Simulate suite parameters by setting system properties
        System.setProperty("suite.browser.name", "firefox");
        System.setProperty("suite.cs.browser.headless", "true");
        System.setProperty("suite.test.parameter", "suite_override_value");
        System.setProperty("suite.environment.name", "test_env");
        
        // Also test regular system properties
        System.setProperty("system.property.test", "system_value");
        
        config = CSConfigManager.getInstance();
        
        logger.info("Hierarchy test setup complete");
    }
    
    @Test
    public void testSuiteParameterTakesPrecedence() {
        logger.info("=== Testing Suite Parameter Precedence ===");
        
        // Test 1: Suite parameter should override properties file for browser.name
        String browserName = config.getProperty("browser.name");
        logger.info("Browser name: {}", browserName);
        config.logConfigurationHierarchy("browser.name");
        
        Map<String, String> browserInfo = config.getPropertySourceInfo("browser.name");
        Assert.assertEquals(browserInfo.get("value"), "firefox", "Suite parameter should override properties file");
        Assert.assertTrue(browserInfo.get("source").contains("Suite Parameter"), "Source should indicate suite parameter");
        
        // Test 2: Suite parameter for headless mode
        boolean headless = config.getBooleanProperty("cs.browser.headless", false);
        logger.info("Headless mode: {}", headless);
        config.logConfigurationHierarchy("cs.browser.headless");
        
        Map<String, String> headlessInfo = config.getPropertySourceInfo("cs.browser.headless");
        Assert.assertEquals(headlessInfo.get("value"), "true", "Suite parameter should override properties file");
        Assert.assertTrue(headless, "Headless should be true from suite parameter");
        
        // Test 3: Custom suite parameter
        String testParam = config.getProperty("test.parameter");
        logger.info("Test parameter: {}", testParam);
        config.logConfigurationHierarchy("test.parameter");
        
        Map<String, String> testParamInfo = config.getPropertySourceInfo("test.parameter");
        Assert.assertEquals(testParamInfo.get("value"), "suite_override_value", "Suite parameter should be available");
        
        // Test 4: Environment name override
        String envName = config.getProperty("environment.name");
        logger.info("Environment name: {}", envName);
        config.logConfigurationHierarchy("environment.name");
        
        Map<String, String> envInfo = config.getPropertySourceInfo("environment.name");
        Assert.assertEquals(envInfo.get("value"), "test_env", "Suite parameter should override properties file");
    }
    
    @Test
    public void testSystemPropertyPrecedence() {
        logger.info("=== Testing System Property Precedence ===");
        
        String systemProp = config.getProperty("system.property.test");
        logger.info("System property test: {}", systemProp);
        config.logConfigurationHierarchy("system.property.test");
        
        Map<String, String> systemInfo = config.getPropertySourceInfo("system.property.test");
        Assert.assertEquals(systemInfo.get("value"), "system_value", "System property should be available");
        Assert.assertEquals(systemInfo.get("source"), "System Property", "Should come from system properties");
    }
    
    @Test
    public void testPropertiesFileFallback() {
        logger.info("=== Testing Properties File Fallback ===");
        
        // Test a property that only exists in properties file
        String reportDir = config.getProperty("cs.report.directory");
        logger.info("Report directory: {}", reportDir);
        config.logConfigurationHierarchy("cs.report.directory");
        
        Map<String, String> reportInfo = config.getPropertySourceInfo("cs.report.directory");
        Assert.assertNotNull(reportInfo.get("value"), "Properties file value should be available");
        Assert.assertEquals(reportInfo.get("source"), "Properties File", "Should come from properties file");
        Assert.assertEquals(reportInfo.get("priority"), "3 (Lowest)", "Should have lowest priority");
    }
    
    @Test
    public void testHierarchyPriorityOrder() {
        logger.info("=== Testing Complete Hierarchy Priority Order ===");
        
        logger.info("\n--- Configuration Hierarchy Summary ---");
        logger.info("1. HIGHEST PRIORITY: Suite Parameters (suite.*)");
        logger.info("2. MEDIUM PRIORITY: System Properties");
        logger.info("3. LOWEST PRIORITY: Properties Files");
        
        String[] testKeys = {
            "browser.name",           // Suite override
            "cs.browser.headless",    // Suite override
            "test.parameter",         // Suite parameter only
            "environment.name",       // Suite override
            "system.property.test",   // System property only
            "cs.report.directory"     // Properties file only
        };
        
        logger.info("\n--- Property Source Analysis ---");
        for (String key : testKeys) {
            Map<String, String> info = config.getPropertySourceInfo(key);
            logger.info("Key: {} | Value: {} | Source: {} | Priority: {}", 
                key, info.get("value"), info.get("source"), info.get("priority"));
        }
        
        logger.info("\n--- All Suite Parameters (suite.*) ---");
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("suite."))
            .sorted()
            .forEach(key -> logger.info("  {} = {}", key, System.getProperty(key)));
    }
}
