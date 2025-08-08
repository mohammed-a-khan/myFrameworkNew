package com.testforge.cs.bdd;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.reporting.CSReportManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * Test to verify scenario isolation when running multiple feature files
 */
public class CSScenarioIsolationTest {
    private static final Logger logger = LoggerFactory.getLogger(CSScenarioIsolationTest.class);
    private CSScenarioRunner scenarioRunner;
    private CSFeatureParser featureParser;
    
    @BeforeClass
    public void setup() {
        // Initialize config
        CSConfigManager.getInstance();
        CSReportManager.getInstance();
        
        scenarioRunner = new CSScenarioRunner();
        featureParser = new CSFeatureParser();
        
        // Register step definitions
        CSStepRegistry registry = CSStepRegistry.getInstance();
        registry.scanPackage("com.orangehrm.stepdefs");
    }
    
    @Test
    public void testScenarioIsolation() {
        logger.info("Testing scenario isolation between feature files");
        
        try {
            // Parse first feature file
            String featureFile1 = "features/orangehrm-simple-tests.feature";
            CSFeatureFile feature1 = featureParser.parseFeatureFile(featureFile1);
            
            // Parse second feature file  
            String featureFile2 = "features/orangehrm-comprehensive-tests.feature";
            CSFeatureFile feature2 = featureParser.parseFeatureFile(featureFile2);
            
            // Run a scenario from first feature
            if (!feature1.getScenarios().isEmpty()) {
                CSFeatureFile.Scenario scenario1 = feature1.getScenarios().get(0);
                logger.info("Running scenario from feature 1: {}", scenario1.getName());
                
                // Store feature context before running
                Map<String, Object> context1Before = new HashMap<>(scenarioRunner.getScenarioContext());
                
                try {
                    scenarioRunner.runScenarioFromFile(featureFile1, feature1, scenario1);
                } catch (Exception e) {
                    logger.warn("Scenario 1 failed (expected for this test): {}", e.getMessage());
                }
                
                // Get context after running scenario 1
                Map<String, Object> context1After = scenarioRunner.getScenarioContext();
                
                // Verify feature file context is set correctly
                Assert.assertEquals(context1After.get("feature_file"), featureFile1, 
                    "Feature file context should be set to feature 1");
                Assert.assertEquals(context1After.get("feature_name"), feature1.getName(),
                    "Feature name context should be set to feature 1");
            }
            
            // Run a scenario from second feature
            if (!feature2.getScenarios().isEmpty()) {
                CSFeatureFile.Scenario scenario2 = feature2.getScenarios().get(0);
                logger.info("Running scenario from feature 2: {}", scenario2.getName());
                
                try {
                    scenarioRunner.runScenarioFromFile(featureFile2, feature2, scenario2);
                } catch (Exception e) {
                    logger.warn("Scenario 2 failed (expected for this test): {}", e.getMessage());
                }
                
                // Get context after running scenario 2
                Map<String, Object> context2After = scenarioRunner.getScenarioContext();
                
                // Verify feature file context is updated correctly
                Assert.assertEquals(context2After.get("feature_file"), featureFile2, 
                    "Feature file context should be updated to feature 2");
                Assert.assertEquals(context2After.get("feature_name"), feature2.getName(),
                    "Feature name context should be updated to feature 2");
                
                // Verify no data from feature 1 is present
                Assert.assertNotEquals(context2After.get("feature_file"), featureFile1,
                    "Feature 1 context should not be present after running feature 2");
            }
            
            logger.info("Scenario isolation test completed successfully");
            
        } catch (Exception e) {
            logger.error("Scenario isolation test failed", e);
            throw new RuntimeException("Scenario isolation test failed", e);
        }
    }
    
    @Test
    public void testDataRowIsolation() {
        logger.info("Testing data row isolation between scenarios");
        
        try {
            // Create test scenarios with different data
            CSFeatureFile.Scenario scenario1 = new CSFeatureFile.Scenario();
            scenario1.setName("Test Scenario 1");
            Map<String, String> dataRow1 = new HashMap<>();
            dataRow1.put("username", "user1");
            dataRow1.put("password", "pass1");
            scenario1.setDataRow(dataRow1);
            
            CSFeatureFile.Scenario scenario2 = new CSFeatureFile.Scenario();
            scenario2.setName("Test Scenario 2");
            Map<String, String> dataRow2 = new HashMap<>();
            dataRow2.put("username", "user2");
            dataRow2.put("password", "pass2");
            scenario2.setDataRow(dataRow2);
            
            // Create dummy feature
            CSFeatureFile feature = new CSFeatureFile();
            feature.setName("Test Feature");
            
            // Run scenario 1
            try {
                scenarioRunner.runScenarioFromFile("test1.feature", feature, scenario1);
            } catch (Exception e) {
                // Expected to fail without proper setup
            }
            
            Map<String, Object> context1 = scenarioRunner.getScenarioContext();
            @SuppressWarnings("unchecked")
            Map<String, String> contextDataRow1 = (Map<String, String>) context1.get("dataRow");
            
            // Run scenario 2
            try {
                scenarioRunner.runScenarioFromFile("test2.feature", feature, scenario2);
            } catch (Exception e) {
                // Expected to fail without proper setup
            }
            
            Map<String, Object> context2 = scenarioRunner.getScenarioContext();
            @SuppressWarnings("unchecked")
            Map<String, String> contextDataRow2 = (Map<String, String>) context2.get("dataRow");
            
            // Verify data isolation
            Assert.assertNotNull(contextDataRow2, "Data row should be present in context");
            Assert.assertEquals(contextDataRow2.get("username"), "user2", 
                "Username should be from scenario 2");
            Assert.assertEquals(contextDataRow2.get("password"), "pass2",
                "Password should be from scenario 2");
            
            // Verify scenario 1 data is not present
            Assert.assertNotEquals(contextDataRow2.get("username"), "user1",
                "Username from scenario 1 should not be present");
            
            // Modify data in context and verify original is unchanged
            contextDataRow2.put("username", "modified");
            Assert.assertEquals(scenario2.getDataRow().get("username"), "user2",
                "Original scenario data should remain unchanged");
            
            logger.info("Data row isolation test completed successfully");
            
        } catch (Exception e) {
            logger.error("Data row isolation test failed", e);
            throw new RuntimeException("Data row isolation test failed", e);
        }
    }
}