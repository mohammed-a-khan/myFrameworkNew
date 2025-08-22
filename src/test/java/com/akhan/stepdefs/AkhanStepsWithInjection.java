package com.akhan.stepdefs;

import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.annotations.CSPageInjection;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.security.CSEncryptionUtils;

/**
 * Akhan step definitions using @CSPageInjection annotation
 * Demonstrates automatic page object initialization with zero boilerplate code
 */
@CSFeature(name = "Akhan Steps with Page Injection", tags = {"@akhan", "@injection"})
public class AkhanStepsWithInjection extends CSStepDefinitions {
    
    private final CSConfigManager config = CSConfigManager.getInstance();
    private long startTime;
    
    // ================== PAGE OBJECTS WITH AUTO-INJECTION ==================
    // These page objects are automatically initialized when first accessed
    // No need for getPage() calls or lazy initialization patterns
    
    @CSPageInjection
    private AkhanLoginPage loginPage;
    
    @CSPageInjection
    private AkhanHomePage homePage;
    
    @CSPageInjection
    private AkhanESSSeriesPage esssPage;
    
    // ================== LOGIN STEPS ==================
    
    @CSStep(description = "I am on the Akhan application")
    public void navigateToAkhan() {
        loginPage.navigateToApplication();  // Page is auto-initialized on first access
        CSReportManager.info("Navigated to: " + config.getString("cs.akhan.url"));
    }
    
    @CSStep(description = "I enter username {username}")
    public void enterUsername(String username) {
        loginPage.enterUsername(username);  // Clean and simple
        CSReportManager.info("Username entered: " + username);
    }
    
    @CSStep(description = "I enter password from encrypted source")
    public void enterEncryptedPassword() {
        String encryptedPassword = config.getString("cs.akhan.password.default");
        String password = CSEncryptionUtils.decrypt(encryptedPassword);
        loginPage.enterPassword(password);
        CSReportManager.info("Encrypted password decrypted and entered");
    }
    
    @CSStep(description = "I click the login button")
    public void clickLogin() {
        loginPage.clickLoginButton();
        CSReportManager.info("Login button clicked");
    }
    
    @CSStep(description = "I login with username {username} and encrypted password")
    public void loginWithEncrypted(String username) {
        enterUsername(username);
        enterEncryptedPassword();
        clickLogin();
    }
    
    // ================== NAVIGATION STEPS ==================
    
    @CSStep(description = "I am logged in")
    public void ensureLoggedIn() {
        homePage.assertHomePageReady();  // Auto-initialized home page
        CSReportManager.info("User is logged in and on home page");
    }
    
    @CSStep(description = "I navigate to {menuItem}")
    public void navigateToMenuItem(String menuItem) {
        homePage.clickMenuItem(menuItem);
        CSReportManager.info("Navigated to menu item: " + menuItem);
    }
    
    @CSStep(description = "I am on the ESSS page")
    public void navigateToESSS() {
        navigateToMenuItem("ESSS/Series");
        CSReportManager.info("Navigated to ESSS/Series page");
    }
    
    // ================== SEARCH STEPS ==================
    
    @CSStep(description = "I select {searchType} from search type dropdown")
    public void selectSearchType(String searchType) {
        esssPage.selectType(searchType);  // Auto-initialized ESSS page
        CSReportManager.info("Selected search type: " + searchType);
    }
    
    @CSStep(description = "I select {attribute} from attribute dropdown")
    public void selectAttribute(String attribute) {
        esssPage.selectAttribute(attribute);
        CSReportManager.info("Selected attribute: " + attribute);
    }
    
    @CSStep(description = "I click search")
    public void clickSearch() {
        startTime = System.currentTimeMillis();
        esssPage.clickSearch();
        CSReportManager.info("Search button clicked");
    }
    
    @CSStep(description = "I perform search with type {searchType} and attribute {attribute}")
    public void performSearch(String searchType, String attribute) {
        selectSearchType(searchType);
        selectAttribute(attribute);
        clickSearch();
    }
    
    // ================== VALIDATION STEPS ==================
    
    @CSStep(description = "I should see {expectedResult}")
    public void verifyExpectedResult(String expectedResult) {
        if ("search results".equals(expectedResult) || "results".equals(expectedResult)) {
            assertTrue(esssPage.hasSearchResults(), "Should have search results");
            CSReportManager.info("Search results are displayed");
        } else if ("no results".equals(expectedResult)) {
            assertFalse(esssPage.hasSearchResults(), "Should have no results");
            CSReportManager.info("No search results displayed");
        } else {
            throw new IllegalArgumentException("Unknown expected result: " + expectedResult);
        }
    }
    
    @CSStep(description = "I should see search results")
    public void verifySearchResults() {
        assertTrue(esssPage.hasSearchResults(), "Search results should be displayed");
        long searchTime = System.currentTimeMillis() - startTime;
        CSReportManager.info("Search results displayed in " + searchTime + "ms");
    }
    
    @CSStep(description = "I should be on the home page")
    public void verifyHomePage() {
        homePage.assertHomePageReady();
        CSReportManager.info("User is on home page");
    }
    
    @CSStep(description = "I should be on the ESSS page")
    public void verifyESSPage() {
        assertTrue(esssPage.verifyPageHeader("ESSS/Series"), "Should be on ESSS/Series page");
        CSReportManager.info("User is on ESSS page");
    }
    
    @CSStep(description = "I should be on the login page")
    public void verifyLoginPage() {
        loginPage.assertLoginPageReady();
        CSReportManager.info("User is on login page");
    }
    
    @CSStep(description = "I logout")
    public void logout() {
        homePage.logout();
        CSReportManager.info("User logged out");
    }
    
    @CSStep(description = "I take a screenshot named {name}")
    public void takeScreenshot(String name) {
        byte[] screenshot = getDriver().takeScreenshot();
        if (screenshot != null) {
            CSReportManager.info("Screenshot captured: " + name);
        } else {
            CSReportManager.warn("Failed to capture screenshot: " + name);
        }
    }
}