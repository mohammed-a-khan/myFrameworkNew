package com.orangehrm.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import org.testng.Assert;

@CSPage(name = "OrangeHRM Login Page")
public class LoginPageNew extends CSBasePage {
    
    // Using Object Repository
    @CSLocator(locatorKey = "login.username.field")
    private CSElement usernameField;
    
    @CSLocator(locatorKey = "login.password.field")
    private CSElement passwordField;
    
    @CSLocator(locatorKey = "login.submit.button")
    private CSElement loginButton;
    
    @CSLocator(locatorKey = "login.error.message")
    private CSElement errorMessage;
    
    @CSLocator(locatorKey = "login.page.logo")
    private CSElement loginLogo;
    
    // Page URL path
    private static final String LOGIN_PATH = "/web/index.php/auth/login";
    
    // Page Methods
    public void navigateTo() {
        String baseUrl = config.getProperty("app.base.url");
        navigateTo(baseUrl + LOGIN_PATH);
        waitForPageLoad();
    }
    
    public void enterUsername(String username) {
        logger.info("Entering username: {}", username);
        usernameField.waitForVisible();
        usernameField.clearAndType(username);
    }
    
    public void enterPassword(String password) {
        logger.info("Entering password");
        passwordField.clearAndType(password);
    }
    
    public void clickLogin() {
        logger.info("Clicking login button");
        loginButton.click();
    }
    
    public void login(String username, String password) {
        System.out.println("DEBUG: LoginPageNew.login called with username=" + username + ", password=" + password);
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }
    
    public boolean isErrorMessageDisplayed() {
        try {
            // Use a shorter timeout for error message
            errorMessage.waitForVisible(3);
            return true;
        } catch (Exception e) {
            logger.warn("Error message not displayed: {}", e.getMessage());
            return false;
        }
    }
    
    public String getErrorMessage() {
        return errorMessage.getText();
    }
    
    public boolean isDisplayed() {
        return loginLogo.isDisplayed() && 
               usernameField.isDisplayed() && 
               passwordField.isDisplayed();
    }
    
    // Validation Methods
    public void assertLoginPageDisplayed() {
        Assert.assertTrue(isDisplayed(), "Login page is not displayed");
        logger.info("Login page is displayed");
    }
    
    public void assertErrorMessage(String expectedMessage) {
        Assert.assertTrue(isErrorMessageDisplayed(), "Error message not displayed");
        String actualMessage = getErrorMessage();
        Assert.assertTrue(actualMessage.contains(expectedMessage), 
            "Error message mismatch. Expected: " + expectedMessage + ", Actual: " + actualMessage);
    }
}