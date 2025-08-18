package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.reporting.CSReportManager;
import org.testng.Assert;

/**
 * Akhan Application Login Page
 * Demonstrates proper framework usage:
 * - @CSPage annotation with page configuration
 * - @CSLocator annotations with object repository
 * - CSElement with smart retry logic
 * - CSBasePage inheritance
 */
@CSPage(
    name = "Akhan Login Page",
    url = "${cs.akhan.url}/login",
    validateOnLoad = false,
    autoNavigate = false
)
public class AkhanLoginPage extends CSBasePage {
    
    // Using Object Repository with @CSLocator
    @CSLocator(locatorKey = "akhan.login.username.field")
    private CSElement usernameField;
    
    @CSLocator(locatorKey = "akhan.login.password.field")
    private CSElement passwordField;
    
    @CSLocator(locatorKey = "akhan.login.submit.button")
    private CSElement loginButton;
    
    @CSLocator(locatorKey = "akhan.login.error.message")
    private CSElement errorMessage;
    
    @CSLocator(locatorKey = "akhan.login.page.logo")
    private CSElement loginLogo;
    
    @CSLocator(locatorKey = "akhan.login.forgot.password.link")
    private CSElement forgotPasswordLink;
    
    @CSLocator(locatorKey = "akhan.login.remember.me.checkbox")
    private CSElement rememberMeCheckbox;
    
    // Using alternative locator strategies with AI self-healing
    @CSLocator(
        css = "div.loading-spinner",
        description = "Loading spinner",
        waitCondition = CSLocator.WaitCondition.INVISIBLE,
        optional = true
    )
    private CSElement loadingSpinner;
    
    // Dynamic element that appears after certain actions
    @CSLocator(
        xpath = "//div[@class='session-expired-message']",
        description = "Session expired message",
        optional = true,
        waitTime = 3
    )
    private CSElement sessionExpiredMessage;
    
    /**
     * Navigate to the application login page
     */
    public void navigateToApplication() {
        String akhanUrl = config.getProperty("cs.akhan.url");
        CSReportManager.info("Navigating to Akhan application: " + akhanUrl);
        logger.info("Navigating to Akhan application: {}", akhanUrl);
        
        try {
            navigateTo(akhanUrl);
            waitForPageLoad();
            CSReportManager.pass("Successfully navigated to Akhan application");
        } catch (Exception e) {
            CSReportManager.fail("Failed to navigate to application: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Enter username in the login field
     */
    public void enterUsername(String username) {
        CSReportManager.info("Entering username: " + username);
        logger.info("Entering username: {}", username);
        
        try {
            usernameField.waitForVisible();
            usernameField.clearAndType(username);
            CSReportManager.pass("Username entered successfully");
        } catch (Exception e) {
            CSReportManager.fail("Failed to enter username: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Enter password in the password field
     */
    public void enterPassword(String password) {
        CSReportManager.info("Entering password (masked for security)");
        logger.info("Entering password");
        
        try {
            passwordField.waitForVisible();
            passwordField.clearAndType(password);
            CSReportManager.pass("Password entered successfully");
        } catch (Exception e) {
            CSReportManager.fail("Failed to enter password: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Click the login button
     */
    public void clickLoginButton() {
        CSReportManager.info("Clicking login button");
        logger.info("Clicking login button");
        
        try {
            loginButton.waitForClickable();
            loginButton.click();
            CSReportManager.pass("Login button clicked successfully");
            
            // Wait for either success (page load) or error message
            CSWaitUtils.waitForSeconds(1);
        } catch (Exception e) {
            CSReportManager.fail("Failed to click login button: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Complete login flow
     */
    public void login(String username, String password) {
        CSReportManager.info("Starting login process for user: " + username);
        logger.info("Performing login for user: {}", username);
        
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
        
        // Check if login was successful
        if (isErrorMessageDisplayed()) {
            String error = getErrorMessage();
            CSReportManager.warn("Login failed with error: " + error);
        } else {
            CSReportManager.pass("Login completed successfully for user: " + username);
        }
    }
    
    /**
     * Check if login page is displayed
     */
    public boolean isLoginPageDisplayed() {
        try {
            return loginLogo.isDisplayed() && 
                   usernameField.isPresent() && 
                   passwordField.isPresent() &&
                   loginButton.isPresent();
        } catch (Exception e) {
            logger.error("Error checking if login page is displayed", e);
            return false;
        }
    }
    
    /**
     * Get error message text
     */
    public String getErrorMessage() {
        try {
            errorMessage.waitForVisible(3);
            return errorMessage.getText();
        } catch (Exception e) {
            logger.warn("Error message not found");
        }
        return null;
    }
    
    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return errorMessage.isDisplayed();
    }
    
    /**
     * Click forgot password link
     */
    public void clickForgotPassword() {
        logger.info("Clicking forgot password link");
        forgotPasswordLink.click();
    }
    
    /**
     * Check remember me checkbox
     */
    public void checkRememberMe() {
        if (!rememberMeCheckbox.isSelected()) {
            rememberMeCheckbox.click();
        }
    }
    
    /**
     * Wait for dynamic element (demonstrates retry mechanism)
     */
    public void waitForDynamicElement() {
        logger.info("Waiting for dynamic element to appear");
        
        // This will use CSElement's built-in retry mechanism
        try {
            sessionExpiredMessage.waitForVisible(5);
            logger.info("Session expired message appeared");
            // Handle session expiry
            clickLoginButton(); // Re-login
        } catch (Exception e) {
            logger.debug("No session expired message found");
        }
    }
    
    /**
     * Validate login page is ready
     */
    public void assertLoginPageReady() {
        Assert.assertTrue(isLoginPageDisplayed(), "Login page is not fully loaded");
        logger.info("Login page is ready");
    }
    
    /**
     * Validate error message contains expected text
     */
    public void assertErrorMessageContains(String expectedText) {
        String actualMessage = getErrorMessage();
        Assert.assertNotNull(actualMessage, "Error message should be displayed");
        Assert.assertTrue(actualMessage.contains(expectedText), 
            String.format("Error message should contain '%s', but was '%s'", expectedText, actualMessage));
    }
}