package com.orangehrm.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;

@CSFeature(name = "Common Steps", tags = {"@common"})
public class CommonSteps extends CSStepDefinitions {
    
    private LoginPageNew loginPage;
    private DashboardPageNew dashboardPage;
    
    @CSStep(value = "^I am on the OrangeHRM application$")
    public void iAmOnOrangeHRMApplication() {
        logger.info("Navigating to OrangeHRM application");
        loginPage = getPage(LoginPageNew.class);
        loginPage.navigateTo();
    }
    
    @CSStep(value = "^I am using the OrangeHRM application$")
    public void iAmUsingOrangeHRMApplication() {
        logger.info("Using OrangeHRM application");
        // This is a context step, verify we have access to the application
        String currentUrl = getDriver().getCurrentUrl();
        assertTrue(currentUrl.contains("orangehrmlive.com"), "Not on OrangeHRM application");
    }
    
    @CSStep(value = "^I am logged in as \"([^\"]*)\"$")
    public void iAmLoggedInAsUser(String username) {
        logger.info("Logging in as user: {}", username);
        loginPage = getPage(LoginPageNew.class);
        loginPage.navigateTo();
        
        // Default password mapping
        String password = getPasswordForUser(username);
        loginPage.login(username, password);
        
        dashboardPage = getPage(DashboardPageNew.class);
        dashboardPage.assertOnDashboard();
    }
    
    @CSStep(value = "^I should see success message \"([^\"]*)\"$")
    public void iShouldSeeSuccessMessage(String expectedMessage) {
        logger.info("Verifying success message: {}", expectedMessage);
        // Generic success message verification
        String actualMessage = findElement("css:.oxd-text--toast-message", "Success message").getText();
        assertEquals(actualMessage, expectedMessage, "Success message mismatch");
    }
    
    @CSStep(value = "^I should see error message \"([^\"]*)\"$")
    public void iShouldSeeErrorMessage(String expectedMessage) {
        logger.info("Verifying error message: {}", expectedMessage);
        // Generic error message verification
        String actualMessage = findElement("css:.oxd-alert-content--error", "Error message").getText();
        assertTrue(actualMessage.contains(expectedMessage), "Error message mismatch");
    }
    
    @CSStep(value = "^I click \"([^\"]*)\" button$")
    public void iClickButton(String buttonText) {
        logger.info("Clicking button: {}", buttonText);
        String xpath = String.format("//button[contains(text(),'%s')]", buttonText);
        findElement(xpath, buttonText + " button").click();
    }
    
    @CSStep(value = "^I click on \"([^\"]*)\" button$")
    public void iClickOnButton(String buttonText) {
        iClickButton(buttonText);
    }
    
    @CSStep(value = "^I take a screenshot \"([^\"]*)\"$")
    public void iTakeScreenshot(String screenshotName) {
        logger.info("Taking screenshot: {}", screenshotName);
        captureScreenshot(screenshotName);
    }
    
    @CSStep(value = "^I should be logged in successfully$")
    public void iShouldBeLoggedInSuccessfully() {
        logger.info("Verifying successful login");
        dashboardPage = getPage(DashboardPageNew.class);
        dashboardPage.assertOnDashboard();
    }
    
    @CSStep(value = "^I navigate to \"([^\"]*)\" > \"([^\"]*)\"$")
    public void iNavigateToSubMenu(String mainMenu, String subMenu) {
        logger.info("Navigating to {} > {}", mainMenu, subMenu);
        // Click main menu
        String mainMenuXpath = String.format("//span[text()='%s']", mainMenu);
        findElement(mainMenuXpath, mainMenu + " menu").click();
        
        // Wait a bit for submenu to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.warn("Sleep interrupted", e);
        }
        
        // Click submenu
        String subMenuXpath = String.format("//a[contains(text(),'%s')]", subMenu);
        findElement(subMenuXpath, subMenu + " submenu").click();
    }
    
    @CSStep(value = "^I navigate to \"([^\"]*)\" > \"([^\"]*)\" > \"([^\"]*)\"$")
    public void iNavigateToThreeLevelMenu(String level1, String level2, String level3) {
        logger.info("Navigating to {} > {} > {}", level1, level2, level3);
        // Navigate through three levels
        iNavigateToSubMenu(level1, level2);
        
        // Click third level
        String level3Xpath = String.format("//a[contains(text(),'%s')]", level3);
        findElement(level3Xpath, level3 + " menu item").click();
    }
    
    // Helper method to get password for a user
    private String getPasswordForUser(String username) {
        // In real implementation, this would come from config or test data
        switch (username.toLowerCase()) {
            case "admin":
                return "admin123";
            case "michael.brown":
            case "emma.wilson":
            case "john.smith":
            case "sarah.johnson":
                return "password123";
            default:
                return "admin123";
        }
    }
}