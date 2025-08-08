package com.akhan.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.annotations.CSDataRow;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.bdd.CSScenarioRunner;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.waits.CSWaitUtils;
import com.akhan.pages.LoginPage;
import com.akhan.pages.HomePage;
import com.akhan.pages.ESSSeriesPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@CSFeature(name = "Akhan Comprehensive Steps", 
          tags = {"@akhan", "@comprehensive"},
          description = "Comprehensive step definitions demonstrating all framework features")
public class AkhanComprehensiveSteps extends CSStepDefinitions {
    
    private LoginPage loginPage;
    private HomePage homePage;
    private ESSSeriesPage essSeriesPage;
    private String esssKeyFromTestData;
    private long performanceStartTime;
    
    // ================== ENHANCED LOGIN STEPS ==================
    
    @CSStep(description = "I wait for page to load")
    public void waitForPageLoad() {
        CSWaitUtils.waitForPageLoad(getDriver().getWebDriver(), 30);
        CSReportManager.pass("Page load completed");
    }
    
    @CSStep(description = "I wait for {seconds} seconds")
    public void waitForSeconds(int seconds) {
        CSReportManager.info("Waiting for " + seconds + " seconds");
        CSWaitUtils.waitForSeconds(seconds);
    }
    
    @CSStep(description = "I wait for search results")
    public void waitForSearchResults() {
        CSReportManager.info("Waiting for search results to load");
        CSWaitUtils.waitForElementVisible(getDriver().getWebDriver(), org.openqa.selenium.By.xpath("//table//tbody/tr"), 10);
        CSReportManager.pass("Search results loaded");
    }
    
    // ================== PERFORMANCE TRACKING ==================
    
    @CSStep(description = "I record the start time")
    public void recordStartTime() {
        performanceStartTime = System.currentTimeMillis();
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            runner.storeInContext("startTime", performanceStartTime);
        }
        CSReportManager.info("⏱️ Performance tracking started");
    }
    
    @CSStep(description = "I navigate to ESSS/Series module with performance tracking")
    public void navigateToESSWithPerformance() {
        long startTime = System.currentTimeMillis();
        
        homePage = getPage(HomePage.class);
        homePage.clickMenuItem("ESSS/Series");
        
        CSWaitUtils.waitForElementVisible(getDriver().getWebDriver(), org.openqa.selenium.By.xpath("//h1[text()='ESSSs/Series']"), 10);
        
        long loadTime = System.currentTimeMillis() - startTime;
        CSReportManager.info("ESSS/Series page load time: " + loadTime + "ms");
    }
    
    @CSStep(description = "I perform ESSS search with performance metrics")
    public void performESSSearchWithMetrics() {
        long searchStartTime = System.currentTimeMillis();
        
        essSeriesPage = getPage(ESSSeriesPage.class);
        
        // Type selection
        essSeriesPage.clickTypeDropdown();
        essSeriesPage.selectTypeOption("ESSS");
        
        // Attribute selection
        essSeriesPage.clickAttributeDropdown();
        essSeriesPage.selectAttributeOption("Key");
        
        // Enter search value
        essSeriesPage.enterSearchValue("Key", "MESA 2001-5");
        
        // Execute search
        essSeriesPage.clickSearch();
        waitForSearchResults();
        
        long searchTime = System.currentTimeMillis() - searchStartTime;
        CSReportManager.info("ESSS search execution time: " + searchTime + "ms");
    }
    
    @CSStep(description = "I verify search performance is within limits")
    public void verifySearchPerformance() {
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            Long startTime = runner.getFromContext("startTime");
            if (startTime != null) {
                long totalTime = System.currentTimeMillis() - startTime;
                
                if (totalTime < 5000) {
                    CSReportManager.pass("Performance within acceptable limits: " + totalTime + "ms");
                } else {
                    CSReportManager.warn("Performance exceeds target: " + totalTime + "ms (target: 5000ms)");
                }
            }
        }
    }
    
    // ================== DROPDOWN VERIFICATION ==================
    
    @CSStep(description = "I verify Type dropdown has {count} options")
    public void verifyTypeDropdownCount(int expectedCount) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        
        String[] typeOptions = {"ESSS", "Series", "Reference Interest", "Fallback Interest", 
                               "Product Group", "Business Line", "Benchmark", "Administrator", "CDI Name"};
        
        int actualCount = 0;
        for (String option : typeOptions) {
            if (essSeriesPage.isTypeOptionDisplayed(option)) {
                actualCount++;
            }
        }
        
        assertEquals(actualCount, expectedCount, 
            "Type dropdown should have " + expectedCount + " options");
        if (actualCount == expectedCount) {
            CSReportManager.pass("Type dropdown options count - Expected: " + expectedCount + ", Actual: " + actualCount);
        } else {
            CSReportManager.fail("Type dropdown options count - Expected: " + expectedCount + ", Actual: " + actualCount);
        }
    }
    
    @CSStep(description = "I verify Attribute dropdown options for {searchType}")
    public void verifyAttributeOptionsForType(String searchType) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        
        Map<String, String[]> attributeOptionsMap = new HashMap<>();
        attributeOptionsMap.put("ESSS", new String[]{"Key", "Name", "ID"});
        attributeOptionsMap.put("Series", new String[]{"Key", "Name", "ID"});
        attributeOptionsMap.put("Reference Interest", new String[]{"Name", "Type"});
        
        String[] expectedOptions = attributeOptionsMap.get(searchType);
        if (expectedOptions != null) {
            for (String option : expectedOptions) {
                assertTrue(essSeriesPage.isAttributeOptionDisplayed(option),
                    "Attribute option '" + option + "' should be displayed for " + searchType);
            }
            CSReportManager.pass("All attribute options verified for " + searchType);
        }
    }
    
    // ================== SEARCH FUNCTIONALITY ==================
    
    @CSStep(description = "I enter {searchValue} in search field")
    public void enterSearchValue(String searchValue) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        String selectedAttribute = essSeriesPage.getSelectedAttributeText();
        
        CSReportManager.info("Search Attribute: " + selectedAttribute);
        CSReportManager.info("Search Value: " + searchValue);
        
        essSeriesPage.enterSearchValue(selectedAttribute, searchValue);
    }
    
    @CSStep(description = "I log the search results summary")
    public void logSearchResultsSummary() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        int rowCount = essSeriesPage.getTableRowCount();
        
        CSReportManager.info("=== Search Results Summary ===");
        CSReportManager.info("Total Results: " + rowCount);
        
        // Log first few results
        for (int i = 1; i <= Math.min(3, rowCount); i++) {
            String type = essSeriesPage.getCellText(i, 2);
            String key = essSeriesPage.getSpanTextInCell(i, 4);
            CSReportManager.info("Result " + i + ": " + type + " - " + key);
        }
    }
    
    @CSStep(description = "I verify search results contain {expectedValue}")
    public void verifySearchResultsContain(String expectedValue) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        int rowCount = essSeriesPage.getTableRowCount();
        boolean found = false;
        
        for (int i = 1; i <= rowCount; i++) {
            String keyText = essSeriesPage.getSpanTextInCell(i, 4);
            if (expectedValue.equals(keyText)) {
                found = true;
                CSReportManager.pass("Found expected value at row " + i);
                break;
            }
        }
        
        assertTrue(found, "Search results should contain: " + expectedValue);
    }
    
    // ================== ERROR HANDLING ==================
    
    @CSStep(description = "I should see login error")
    public void verifyLoginError() {
        // Check if still on login page
        loginPage = getPage(LoginPage.class);
        assertTrue(loginPage.isLoginPageDisplayed(), "Should still be on login page after failed login");
        
        // Look for error message
        CSElement errorElement = findElement("xpath://div[contains(@class,'error')] | //span[contains(@class,'error')]", 
                                           "login error message");
        if (errorElement != null && errorElement.isDisplayed()) {
            String errorText = errorElement.getText();
            CSReportManager.info("Error message: " + errorText);
        }
    }
    
    @CSStep(description = "I perform logout")
    public void performLogout() {
        CSReportManager.info("Performing logout");
        
        // Implementation depends on how logout works in the application
        // Example: Click user menu then logout
        CSElement userMenu = findElement("xpath://div[@class='user-menu']", "user menu");
        if (userMenu != null && userMenu.isDisplayed()) {
            userMenu.click();
            CSElement logoutLink = findElement("xpath://a[text()='Logout']", "logout link");
            if (logoutLink != null) {
                logoutLink.click();
            }
        }
    }
    
    @CSStep(description = "I should be on login page")
    public void verifyOnLoginPage() {
        loginPage = getPage(LoginPage.class);
        assertTrue(loginPage.isLoginPageDisplayed(), "Should be on login page");
        CSReportManager.pass("Returned to login page");
    }
    
    // ================== DATA-DRIVEN SUPPORT ==================
    
    @CSStep(description = "I use test data from current row")
    public void useTestDataFromRow(@CSDataRow Map<String, String> dataRow) {
        CSReportManager.info("=== Using Test Data ===");
        for (Map.Entry<String, String> entry : dataRow.entrySet()) {
            CSReportManager.info(entry.getKey() + ": " + entry.getValue());
        }
        
        // Store data for later use
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            runner.storeInContext("currentTestData", dataRow);
        }
    }
    
    // ================== UTILITY METHODS ==================
    
    // Helper method removed as it's not used
}