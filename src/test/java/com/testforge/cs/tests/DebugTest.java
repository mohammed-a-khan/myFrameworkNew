package com.testforge.cs.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.driver.CSWebDriverManager;
import org.testng.annotations.Test;
import org.testng.Assert;

public class DebugTest extends CSBaseTest {
    
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    @Test
    public void testDriverInitialization() {
        logger.info("Testing driver initialization");
        
        // Check if driver is initialized
        Assert.assertNotNull(driver, "WebDriver should not be null");
        logger.info("Driver initialized: {}", driver);
        
        // Check CSWebDriverManager
        Assert.assertNotNull(CSWebDriverManager.getDriver(), "CSWebDriverManager driver should not be null");
        logger.info("CSWebDriverManager driver: {}", CSWebDriverManager.getDriver());
        
        // Check CSDriver
        CSDriver csDriver = new CSDriver(driver);
        Assert.assertNotNull(csDriver, "CSDriver should not be null");
        
        // Navigate to a page
        String url = config.getProperty("cs.orangehrm.url", "https://opensource-demo.orangehrmlive.com");
        logger.info("Navigating to: {}", url);
        driver.get(url);
        
        // Verify navigation
        String currentUrl = driver.getCurrentUrl();
        logger.info("Current URL: {}", currentUrl);
        Assert.assertTrue(currentUrl.contains("orangehrmlive.com"), "Should be on OrangeHRM site");
        
        logger.info("Driver initialization test passed");
    }
}