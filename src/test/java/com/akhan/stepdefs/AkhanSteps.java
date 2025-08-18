package com.akhan.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSDataRow;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.security.CSEncryptionUtils;
import com.testforge.cs.reporting.CSReportManager;
import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import java.util.Map;
import java.util.HashMap;

/**
 * Akhan Step Definitions - Demonstrates ALL Framework Features
 * 
 * Features Demonstrated:
 * - @CSStep annotation (works for all Gherkin keywords)
 * - @CSDataRow for accessing complete data rows in data-driven scenarios
 * - CSStepDefinitions base class with helper methods
 * - Configuration and encryption
 * - Performance tracking
 * - Validation utilities
 */
@CSFeature(name = "Akhan Steps", tags = {"@akhan"})
public class AkhanSteps extends CSStepDefinitions {
    
    private AkhanLoginPage loginPage;
    private AkhanHomePage homePage;
    private AkhanESSSeriesPage esssPage;
    private CSConfigManager config = CSConfigManager.getInstance();
    private long startTime;
    
    // ================== LOGIN STEPS ==================
    
    @CSStep(description = "I am on the Akhan application")
    public void navigateToAkhan() {
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.navigateToApplication();
        CSReportManager.info("Navigated to: " + config.getString("cs.akhan.url"));
    }
    
    @CSStep(description = "I enter username {username}")
    public void enterUsername(String username) {
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.enterUsername(username);
        CSReportManager.info("Username entered: " + username);
    }
    
    @CSStep(description = "I enter password from encrypted source")
    public void enterEncryptedPassword() {
        // Demonstrates encryption feature
        String encryptedPassword = config.getString("cs.akhan.password.default");
        String password = CSEncryptionUtils.decrypt(encryptedPassword);
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.enterPassword(password);
        CSReportManager.info("Encrypted password decrypted and entered");
    }
    
    @CSStep(description = "I click the login button")
    public void clickLogin() {
        startTime = System.currentTimeMillis();
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.clickLoginButton();
        long loginTime = System.currentTimeMillis() - startTime;
        
        // Log performance metrics
        CSReportManager.getInstance().addPerformanceMetric("login_time", loginTime);
        CSReportManager.info("Login completed in " + loginTime + "ms");
    }
    
    @CSStep(description = "I login with username {username} and encrypted password")
    public void loginWithEncrypted(String username, @CSDataRow Map<String, String> dataRow) {
        // Demonstrates @CSDataRow usage - access complete data row
        loginPage = getPage(AkhanLoginPage.class);
        
        // Get password from data row if available, otherwise use config
        String password;
        if (dataRow != null && dataRow.containsKey("password")) {
            password = dataRow.get("password");
            if (CSEncryptionUtils.isEncrypted(password)) {
                password = CSEncryptionUtils.decrypt(password);
            }
            CSReportManager.info("Using password from data row");
        } else {
            String encryptedPassword = config.getString("cs.akhan.password.default");
            password = CSEncryptionUtils.decrypt(encryptedPassword);
            CSReportManager.info("Using password from config");
        }
        
        loginPage.login(username, password);
        
        // Log additional data from row if available
        if (dataRow != null) {
            CSReportManager.info("Data row contains " + dataRow.size() + " fields");
            if (dataRow.containsKey("role")) {
                CSReportManager.info("User role: " + dataRow.get("role"));
            }
        }
    }
    
    @CSStep(description = "I am logged in")
    public void ensureLoggedIn() {
        homePage = getPage(AkhanHomePage.class);
        if (!homePage.isHomePageDisplayed()) {
            loginPage = getPage(AkhanLoginPage.class);
            String username = config.getString("cs.akhan.user.default");
            String encryptedPassword = config.getString("cs.akhan.password.default");
            String password = CSEncryptionUtils.decrypt(encryptedPassword);
            loginPage.login(username, password);
        }
        CSReportManager.info("User is logged in");
    }
    
    // ================== NAVIGATION STEPS ==================
    
    @CSStep(description = "I navigate to {menuItem}")
    public void navigateToMenuItem(String menuItem, @CSDataRow(includeMetadata = true) Map<String, String> fullDataRow) {
        // Demonstrates @CSDataRow with metadata
        homePage = getPage(AkhanHomePage.class);
        
        startTime = System.currentTimeMillis();
        homePage.clickMenuItem(menuItem);
        long navTime = System.currentTimeMillis() - startTime;
        
        // Performance check
        if (navTime > 3000) {
            CSReportManager.warn("Navigation took longer than 3000ms: " + navTime + "ms");
        }
        
        // Log metadata if available
        if (fullDataRow != null) {
            String dataSource = fullDataRow.get("dataSourceType");
            String sourceFile = fullDataRow.get("dataSourceFile");
            if (dataSource != null) {
                CSReportManager.info("Data source: " + dataSource);
            }
            if (sourceFile != null) {
                CSReportManager.info("Source file: " + sourceFile);
            }
        }
        
        CSReportManager.info("Navigated to " + menuItem + " in " + navTime + "ms");
    }
    
    @CSStep(description = "I navigate through all menu items")
    public void navigateThroughAllMenus() {
        homePage = getPage(AkhanHomePage.class);
        
        // Get menu items from config or use defaults
        String menuItemsConfig = config.getString("akhan.menu.items", 
            "Reference Interests,Interest History,External Interests");
        String[] menus = menuItemsConfig.split(",");
        
        for (String menu : menus) {
            homePage.clickMenuItem(menu.trim());
            CSReportManager.info("Navigated to: " + menu);
        }
        homePage.clickMenuItem("Home");
    }
    
    // ================== ESSS/SERIES SEARCH STEPS ==================
    
    @CSStep(description = "I am on the ESSS page")
    public void navigateToESSS() {
        homePage = getPage(AkhanHomePage.class);
        homePage.clickMenuItem("ESSS/Series");
        esssPage = getPage(AkhanESSSeriesPage.class);
        
        // Validate page loaded
        assertNotNull(esssPage, "ESSS page object should not be null");
        assertTrue(esssPage.verifyPageHeader("ESSS"), "Should be on ESSS page");
    }
    
    @CSStep(description = "I select {searchType} from search type dropdown")
    public void selectSearchType(String searchType, @CSDataRow Map<String, String> dataRow) {
        // Access complete data row for additional context
        esssPage = getPage(AkhanESSSeriesPage.class);
        esssPage.selectType(searchType);
        
        if (dataRow != null && dataRow.containsKey("description")) {
            CSReportManager.info("Test: " + dataRow.get("description"));
        }
        CSReportManager.info("Selected search type: " + searchType);
    }
    
    @CSStep(description = "I select {attribute} from attribute dropdown")
    public void selectAttribute(String attribute) {
        esssPage = getPage(AkhanESSSeriesPage.class);
        esssPage.selectAttribute(attribute);
        CSReportManager.info("Selected attribute: " + attribute);
    }
    
    @CSStep(description = "I click search")
    public void clickSearch() {
        esssPage = getPage(AkhanESSSeriesPage.class);
        
        startTime = System.currentTimeMillis();
        esssPage.clickSearch();
        long searchTime = System.currentTimeMillis() - startTime;
        
        // Performance tracking
        CSReportManager.getInstance().addPerformanceMetric("search_time", searchTime);
        
        // Performance check against configured threshold
        long threshold = config.getLong("cs.akhan.performance.search", 2000);
        if (searchTime > threshold) {
            CSReportManager.warn("Search exceeded threshold: " + searchTime + "ms > " + threshold + "ms");
        }
        
        CSReportManager.info("Search executed in " + searchTime + "ms");
    }
    
    @CSStep(description = "I perform search with type {searchType} and attribute {attribute}")
    public void performSearch(String searchType, String attribute, @CSDataRow Map<String, String> dataRow) {
        // Demonstrates using both parameters and data row
        esssPage = getPage(AkhanESSSeriesPage.class);
        
        // Log complete data row if available
        if (dataRow != null) {
            CSReportManager.info("Executing search from data row with " + dataRow.size() + " fields");
            
            // Override parameters if data row has different values
            if (dataRow.containsKey("searchType")) {
                searchType = dataRow.get("searchType");
            }
            if (dataRow.containsKey("attribute")) {
                attribute = dataRow.get("attribute");
            }
        }
        
        esssPage.performSearch(searchType, attribute);
        CSReportManager.info("Search performed: " + searchType + "/" + attribute);
    }
    
    @CSStep(description = "I should see {expectedResult}")
    public void verifyExpectedResult(String expectedResult, @CSDataRow Map<String, String> dataRow) {
        // Demonstrates validation based on expected result and data row
        esssPage = getPage(AkhanESSSeriesPage.class);
        
        // Check if we have additional validation criteria in data row
        if (dataRow != null) {
            String minCount = dataRow.get("minResultCount");
            String maxCount = dataRow.get("maxResultCount");
            
            if (minCount != null || maxCount != null) {
                int actualCount = esssPage.getResultRowCount();
                
                if (minCount != null) {
                    int min = Integer.parseInt(minCount);
                    assertTrue(actualCount > min, "Result count should be > " + min);
                }
                if (maxCount != null) {
                    int max = Integer.parseInt(maxCount);
                    assertTrue(actualCount < max, "Result count should be < " + max);
                }
            }
        }
        
        // Standard validation
        if (expectedResult.contains("results")) {
            assertTrue(esssPage.hasSearchResults(), "Should have search results");
            int count = esssPage.getResultRowCount();
            CSReportManager.pass("Found " + count + " results");
        } else {
            assertFalse(esssPage.hasSearchResults(), "Should have no results");
            CSReportManager.pass("No results as expected");
        }
    }
    
    @CSStep(description = "I should see search results")
    public void verifySearchResults() {
        esssPage = getPage(AkhanESSSeriesPage.class);
        
        // Validation
        assertNotNull(esssPage, "ESSS page should not be null");
        assertTrue(esssPage.hasSearchResults(), "Should have search results");
        
        int count = esssPage.getResultRowCount();
        assertTrue(count > 0, "Result count should be greater than 0");
        
        CSReportManager.pass("Search results found: " + count);
    }
    
    // ================== VALIDATION STEPS ==================
    
    @CSStep(description = "I should be on the home page")
    public void verifyHomePage() {
        homePage = getPage(AkhanHomePage.class);
        
        // Multiple validations
        assertNotNull(homePage, "Home page should not be null");
        assertTrue(homePage.isHomePageDisplayed(), "Should be on home page");
        
        // Capture screenshot for report
        captureScreenshot("home-page-verified");
        CSReportManager.pass("Home page verified");
    }
    
    @CSStep(description = "I should be on the ESSS page")
    public void verifyESSPage() {
        esssPage = getPage(AkhanESSSeriesPage.class);
        assertTrue(esssPage.verifyPageHeader("ESSS"), "Should be on ESSS page");
        CSReportManager.pass("ESSS page verified");
    }
    
    @CSStep(description = "I should be on the login page")
    public void verifyLoginPage() {
        loginPage = getPage(AkhanLoginPage.class);
        assertTrue(loginPage.isLoginPageDisplayed(), "Should be on login page");
        CSReportManager.pass("Login page verified");
    }
    
    // ================== UTILITY STEPS ==================
    
    @CSStep(description = "I take a screenshot named {name}")
    public void takeScreenshot(String name) {
        // Inherited from CSStepDefinitions
        captureScreenshot(name);
        CSReportManager.info("Screenshot: " + name);
    }
    
    @CSStep(description = "I logout")
    public void logout() {
        homePage = getPage(AkhanHomePage.class);
        homePage.logout();
        
        // Validate logout successful
        loginPage = getPage(AkhanLoginPage.class);
        assertTrue(loginPage.isLoginPageDisplayed(), "User should be logged out");
        
        CSReportManager.info("Logged out successfully");
    }
}