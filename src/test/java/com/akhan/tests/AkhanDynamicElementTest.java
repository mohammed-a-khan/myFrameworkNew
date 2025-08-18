package com.akhan.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.reporting.CSReportManager;
import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class demonstrating dynamic element creation and CSReportManager logging
 * Shows how messages appear in generated reports with info, pass, warn, and fail levels
 */
public class AkhanDynamicElementTest extends CSBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(AkhanDynamicElementTest.class);
    
    /**
     * Test demonstrating dynamic element creation with report logging
     * All CSReportManager messages will appear in the HTML report
     */
    @Test(groups = {"dynamic", "demo"})
    public void testDynamicElementsWithReporting() {
        CSReportManager.info("=== Starting Dynamic Element Test ===");
        CSReportManager.info("This test demonstrates dynamic element creation and report logging");
        
        // Step 1: Login
        CSReportManager.info("STEP 1: Login to application");
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        loginPage.login("testuser", "testpass");
        
        // Check if login was successful
        AkhanHomePage homePage = new AkhanHomePage();
        if (homePage.isHomePageDisplayed()) {
            CSReportManager.pass("✓ Login successful - Home page is displayed");
        } else {
            CSReportManager.fail("✗ Login failed - Home page not displayed");
            Assert.fail("Login failed");
        }
        
        // Step 2: Dynamic menu navigation
        CSReportManager.info("STEP 2: Testing dynamic menu navigation");
        
        // Find menu item dynamically
        if (homePage.isDynamicElementPresent("dynamic.menu.item.xpath", "ESSS/Series")) {
            CSReportManager.pass("✓ Found ESSS/Series menu item dynamically");
            
            // Click the menu item
            homePage.clickDynamicElement("dynamic.menu.item.xpath", "ESSS/Series");
            CSReportManager.pass("✓ Clicked ESSS/Series menu dynamically");
        } else {
            CSReportManager.warn("⚠ ESSS/Series menu not found - skipping navigation");
        }
        
        // Step 3: Dynamic search operations
        CSReportManager.info("STEP 3: Testing dynamic search elements");
        AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
        
        // Demonstrate dynamic button finding
        try {
            essPage.demonstrateDynamicElements();
            CSReportManager.pass("✓ Dynamic element demonstration completed");
        } catch (Exception e) {
            CSReportManager.warn("⚠ Some dynamic elements not found: " + e.getMessage());
        }
        
        // Step 4: Performance validation with reporting
        CSReportManager.info("STEP 4: Search with performance monitoring");
        
        try {
            var metrics = essPage.performSearchWithMetrics("Type1", "Attribute1", "TestValue");
            
            long searchTime = (long) metrics.get("searchTime");
            int resultCount = (int) metrics.get("resultCount");
            
            if (searchTime < 3000) {
                CSReportManager.pass(String.format("✓ Search performance GOOD: %d ms", searchTime));
            } else if (searchTime < 5000) {
                CSReportManager.warn(String.format("⚠ Search performance SLOW: %d ms", searchTime));
            } else {
                CSReportManager.fail(String.format("✗ Search performance POOR: %d ms", searchTime));
            }
            
            CSReportManager.info(String.format("Search returned %d results", resultCount));
            
        } catch (Exception e) {
            CSReportManager.fail("✗ Search failed: " + e.getMessage());
        }
        
        // Step 5: Dynamic table operations
        CSReportManager.info("STEP 5: Testing dynamic table cell access");
        
        try {
            // Access table cell dynamically
            String cellValue = homePage.getDynamicElementText(
                "dynamic.table.cell.xpath", 
                "resultsTable", 1, 2
            );
            
            if (cellValue != null && !cellValue.isEmpty()) {
                CSReportManager.pass("✓ Retrieved table cell value: " + cellValue);
            } else {
                CSReportManager.warn("⚠ Table cell is empty");
            }
        } catch (Exception e) {
            CSReportManager.info("ℹ Table not present on current page");
        }
        
        // Step 6: Logout
        CSReportManager.info("STEP 6: Logout from application");
        homePage.logout();
        
        // Final summary
        CSReportManager.info("=== Test Execution Summary ===");
        CSReportManager.pass("✓ Dynamic element test completed successfully");
        CSReportManager.info("All CSReportManager messages appear in the HTML report");
        CSReportManager.info("Check the generated report to see info, pass, warn, and fail messages");
    }
    
    /**
     * Test demonstrating validation with detailed reporting
     */
    @Test(groups = {"validation", "reporting"})
    public void testValidationWithReporting() {
        CSReportManager.info("=== Starting Validation Test with Reporting ===");
        
        // Login
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        
        // Test invalid login
        CSReportManager.info("Testing invalid login scenario");
        loginPage.login("invaliduser", "wrongpass");
        
        if (loginPage.isErrorMessageDisplayed()) {
            String errorMsg = loginPage.getErrorMessage();
            CSReportManager.pass("✓ Error message displayed correctly: " + errorMsg);
        } else {
            CSReportManager.warn("⚠ No error message shown for invalid login");
        }
        
        // Test valid login
        CSReportManager.info("Testing valid login scenario");
        loginPage.login("testuser", "testpass");
        
        AkhanHomePage homePage = new AkhanHomePage();
        if (homePage.isHomePageDisplayed()) {
            CSReportManager.pass("✓ Valid login successful");
            
            // Validate search results
            homePage.navigateToESSSSeries();
            AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
            
            CSReportManager.info("Performing search validation");
            essPage.performSearch("Type1", "Attribute1", "Test");
            
            boolean valid = essPage.validateResultsWithReporting("Test", 1);
            
            if (valid) {
                CSReportManager.pass("✓ All validations passed");
            } else {
                CSReportManager.fail("✗ Validation failed");
            }
        } else {
            CSReportManager.fail("✗ Login failed - cannot proceed with validation");
        }
        
        CSReportManager.info("=== Validation Test Complete ===");
    }
}