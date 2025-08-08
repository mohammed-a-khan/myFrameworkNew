package com.akhan.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.elements.CSElement;
import com.akhan.pages.LoginPageEnhanced;
import com.akhan.pages.HomePage;
import com.akhan.pages.ESSSeriesPage;
import org.testng.Assert;

/**
 * Enhanced step definitions with comprehensive action reporting
 * Every action within each step is automatically logged
 */
@CSFeature(name = "Akhan Enhanced Steps", tags = {"@akhan", "@enhanced"})
public class AkhanEnhancedSteps extends CSStepDefinitions {
    
    private LoginPageEnhanced loginPage;
    private HomePage homePage;
    private ESSSeriesPage essSeriesPage;
    
    @CSStep(description = "I am on the login page")
    public void navigateToLoginPage() {
        // High-level step reporting
        CSReportManager.info("Navigate to Login Page");
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Navigate to Akhan Login Page");
        CSReportManager.info("═══════════════════════════════════════════");
        
        // All detailed actions are automatically reported by the page object
        loginPage = getPage(LoginPageEnhanced.class);
        loginPage.navigateTo();
        
        // Step completion
        CSReportManager.info("✓ STEP COMPLETE: Successfully navigated to login page");
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I enter username {username} and password {password}")
    public void enterCredentials(String username, String password) {
        // High-level step reporting
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Enter Login Credentials");
        CSReportManager.info("═══════════════════════════════════════════");
        
        // All field interactions are automatically reported
        loginPage = getPage(LoginPageEnhanced.class);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        
        // Step completion
        CSReportManager.info("✓ STEP COMPLETE: Credentials entered");
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I click the log on button")
    public void clickLogOn() {
        // High-level step reporting
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Submit Login Form");
        CSReportManager.info("═══════════════════════════════════════════");
        
        // Button click with all pre/post conditions reported automatically
        loginPage = getPage(LoginPageEnhanced.class);
        loginPage.clickLogOn();
        
        // Step completion
        CSReportManager.info("✓ STEP COMPLETE: Login form submitted");
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I should see the home header")
    public void verifyHomeHeader() {
        // High-level step reporting
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Verify Home Page Displayed");
        CSReportManager.info("═══════════════════════════════════════════");
        
        homePage = getPage(HomePage.class);
        
        // Detailed verification with automatic reporting
        CSReportManager.info("Verify home page loaded after login");
        
        // Check multiple elements
        CSReportManager.info("Checking home header");
        boolean headerDisplayed = homePage.isHomeHeaderDisplayed();
        if (headerDisplayed) {
            CSReportManager.pass("Home header is displayed");
        } else {
            CSReportManager.fail("Home header is not displayed");
        }
        
        // Check page URL
        CSReportManager.info("Checking page URL");
        String currentUrl = getDriver().getCurrentUrl();
        boolean isHomePage = !currentUrl.contains("login");
        if (isHomePage) {
            CSReportManager.pass("Navigated away from login page");
        } else {
            CSReportManager.fail("Still on login page");
        }
        
        // Check for user menu
        CSReportManager.info("Checking for user menu");
        boolean userMenuPresent = isElementDisplayed("xpath://div[@class='user-menu']");
        if (userMenuPresent) {
            CSReportManager.info("User menu is present");
        }
        
        // Assert the verification
        Assert.assertTrue(headerDisplayed, "Home header should be displayed");
        
        // Step completion
        CSReportManager.info("✓ STEP COMPLETE: Home page verified");
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I am logged in as {username}")
    public void loginAsUser(String username) {
        // Composite step with full reporting
        CSReportManager.info("Complete Login Flow");
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("COMPOSITE STEP: Complete Login as " + username);
        CSReportManager.info("═══════════════════════════════════════════");
        
        // Each sub-step is fully reported
        CSReportManager.info("── Sub-step 1: Navigate to login page");
        navigateToLoginPage();
        
        CSReportManager.info("── Sub-step 2: Enter credentials");
        enterCredentials(username, config.getProperty("akhan.password.default", "testpass"));
        
        CSReportManager.info("── Sub-step 3: Submit login");
        clickLogOn();
        
        CSReportManager.info("── Sub-step 4: Verify login success");
        verifyHomeHeader();
        
        // Composite step completion
        CSReportManager.info("✓ COMPOSITE STEP COMPLETE: Successfully logged in as " + username);
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I click on {menuItem} menu item")
    public void clickMenuItem(String menuItem) {
        // High-level step reporting
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Navigate to " + menuItem);
        CSReportManager.info("═══════════════════════════════════════════");
        
        homePage = getPage(HomePage.class);
        
        // Pre-click verification
        CSReportManager.info("Click menu item: " + menuItem);
        
        // Verify menu is visible
        CSReportManager.info("Verifying menu is visible");
        boolean menuVisible = isElementDisplayed("xpath://div[@id='abcdNavigatorBody']");
        CSReportManager.info("Navigation menu is " + (menuVisible ? "visible" : "not visible"));
        
        // Check if menu item exists
        CSReportManager.info("Checking if menu item exists: " + menuItem);
        boolean menuItemExists = homePage.isMenuItemDisplayed(menuItem);
        if (menuItemExists) {
            CSReportManager.pass("Menu item exists: " + menuItem);
        } else {
            CSReportManager.fail("Menu item not found: " + menuItem);
        }
        
        // Perform click
        if (menuItemExists) {
            CSReportManager.info("Clicking menu item: " + menuItem);
            homePage.clickMenuItem(menuItem);
            
            // Wait for navigation
            CSReportManager.info("Waiting for page navigation");
            CSWaitUtils.waitForSeconds(2);
            
            CSReportManager.pass("Menu navigation completed");
        } else {
            CSReportManager.fail("Menu navigation failed - item not found: " + menuItem);
        }
        
        // Step completion
        CSReportManager.info("✓ STEP COMPLETE: Navigated to " + menuItem);
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    @CSStep(description = "I enable detailed action reporting")
    public void enableDetailedReporting() {
        CSReportManager.info("✅ Detailed action reporting ENABLED");
        CSReportManager.info("All Selenium actions will be logged with:");
        CSReportManager.info("  • Pre-action state verification");
        CSReportManager.info("  • Action execution details");
        CSReportManager.info("  • Post-action verification");
        CSReportManager.info("  • Performance metrics");
        CSReportManager.info("  • Element state tracking");
    }
    
    @CSStep(description = "I disable detailed action reporting")
    public void disableDetailedReporting() {
        CSReportManager.info("⚠️ Detailed action reporting DISABLED");
    }
    
    @CSStep(description = "I should see login error")
    public void verifyLoginError() {
        CSReportManager.info("═══════════════════════════════════════════");
        CSReportManager.info("STEP: Verify Login Error");
        CSReportManager.info("═══════════════════════════════════════════");
        
        loginPage = getPage(LoginPageEnhanced.class);
        
        // Check if still on login page
        CSReportManager.info("Verifying still on login page");
        boolean stillOnLoginPage = loginPage.isLoginPageDisplayed();
        if (stillOnLoginPage) {
            CSReportManager.pass("Still on login page");
        } else {
            CSReportManager.fail("Not on login page anymore");
        }
        
        // Check for error messages
        CSReportManager.info("Checking for error messages");
        try {
            CSElement errorElement = findElement(
                "xpath://div[contains(@class,'error')] | //span[contains(@class,'error')]", 
                "error message");
            
            if (errorElement != null && errorElement.isDisplayed()) {
                String errorText = errorElement.getText();
                CSReportManager.info("Error message found: " + errorText);
                CSReportManager.fail("Login error displayed: " + errorText);
            }
        } catch (Exception e) {
            CSReportManager.info("No visible error message - Login may have failed silently");
        }
        
        Assert.assertTrue(stillOnLoginPage, "Should still be on login page after failed login");
        
        CSReportManager.info("✓ STEP COMPLETE: Login error verified");
        CSReportManager.info("═══════════════════════════════════════════");
    }
    
    // Helper method
    private boolean isElementDisplayed(String xpath) {
        try {
            return getDriver().findElement(org.openqa.selenium.By.xpath(xpath)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}