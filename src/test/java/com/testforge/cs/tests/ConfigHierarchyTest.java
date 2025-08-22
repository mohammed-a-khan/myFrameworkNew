package com.testforge.cs.tests;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.core.CSBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ConfigHierarchyTest extends CSBaseTest {
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
