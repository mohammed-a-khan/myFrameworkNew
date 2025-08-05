package com.orangehrm.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;

@CSFeature(name = "OrangeHRM Login", tags = {"@login"})
public class LoginStepsNew extends CSStepDefinitions {
    
    private LoginPageNew loginPage;
    private DashboardPageNew dashboardPage;
    
    @CSStep(value = "^I am on the OrangeHRM login page$")
    public void iAmOnTheLoginPage() {
        logger.info("Navigating to OrangeHRM login page");
        loginPage = getPage(LoginPageNew.class);
        loginPage.navigateTo();
        loginPage.assertLoginPageDisplayed();
    }
    
    @CSStep(value = "^I enter username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void iEnterUsernameAndPassword(String username, String password) {
        logger.info("Entering credentials - Username: {}", username);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }
    
    @CSStep(value = "^I click the login button$")
    public void iClickLoginButton() {
        logger.info("Clicking login button");
        loginPage.clickLogin();
        // Wait for page transition
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.warn("Sleep interrupted", e);
        }
    }
    
    @CSStep(value = "^I should be redirected to the dashboard$")
    public void iShouldBeRedirectedToDashboard() {
        logger.info("Verifying dashboard redirect");
        dashboardPage = getPage(DashboardPageNew.class);
        dashboardPage.assertOnDashboard();
    }
    
    @CSStep(value = "^I should see \"([^\"]*)\" as page title$")
    public void iShouldSeePageTitle(String expectedTitle) {
        logger.info("Verifying page title: {}", expectedTitle);
        String actualTitle = dashboardPage.getHeaderTitle();
        assertEquals(actualTitle, expectedTitle, "Page title mismatch");
    }
    
    @CSStep(value = "^I should see the user dropdown with text \"([^\"]*)\"$")
    public void iShouldSeeUserDropdown(String expectedUser) {
        logger.info("Verifying logged in user: {}", expectedUser);
        dashboardPage.assertUserLoggedIn(expectedUser);
    }
    
    @CSStep(value = "^I should see an error message \"([^\"]*)\"$")
    public void iShouldSeeErrorMessage(String expectedMessage) {
        logger.info("Verifying error message: {}", expectedMessage);
        loginPage.assertErrorMessage(expectedMessage);
    }
    
    @CSStep(value = "^I should remain on the login page$")
    public void iShouldRemainOnLoginPage() {
        logger.info("Verifying user remains on login page");
        loginPage.assertLoginPageDisplayed();
    }
}