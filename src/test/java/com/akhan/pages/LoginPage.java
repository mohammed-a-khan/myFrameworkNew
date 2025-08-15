package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.waits.CSWaitUtils;

@CSPage(name = "Akhan Login Page", url = "https://akhan-ui-sit.myshare.net/")
public class LoginPage extends CSBasePage {
    
    // Using object repository with locator keys
    @CSLocator(locatorKey = "akhan.login.username", 
               alternativeLocators = {"name:login", "id:login"},
               description = "Username input field")
    private CSElement usernameField;
    
    @CSLocator(locatorKey = "akhan.login.password",
               alternativeLocators = {"name:passwd", "id:passwd"},
               description = "Password input field")
    private CSElement passwordField;
    
    @CSLocator(locatorKey = "akhan.login.button",
               description = "Log On button")
    private CSElement logOnButton;
    
    public void navigateTo() {
        CSReportManager.info("Navigate to Akhan login page");
        String url = config.getProperty("cs.akhan.url", "https://akhan-ui-sit.myshare.net/");
        navigateTo(url);
        waitForPageLoad();
        CSReportManager.pass("Login page loaded");
    }
    
    public void enterUsername(String username) {
        logger.info("Entering username: {}", username);
        CSReportManager.info("Step 1: Enter username: " + username);
        
        usernameField.waitForVisible(config.getIntProperty("cs.wait.long", 5000) / 500);
        usernameField.highlight(); // Visual highlight
        usernameField.clearAndType(username);
        
        // Capture screenshot after entering username
        captureScreenshot("username_entered");
    }
    
    public void enterPassword(String password) {
        logger.info("Entering password");
        CSReportManager.info("Step 2: Enter password: ***");
        
        passwordField.waitForClickable();
        passwordField.highlight();
        passwordField.clearAndType(password);
    }
    
    public void clickLogOn() {
        CSReportManager.info("Step 3: Click Log On button");
        
        logOnButton.waitForClickable(config.getIntProperty("cs.wait.long", 5000) / 500);
        logOnButton.highlight();
        captureScreenshot("before_login");
        logOnButton.click();
        
        // Wait for page transition
        CSWaitUtils.waitForSeconds(config.getIntProperty("cs.wait.medium", 2000) / 1000);
    }
    
    public void login(String username, String password) {
        CSReportManager.info("=== Login Process ===");
        long startTime = System.currentTimeMillis();
        
        enterUsername(username);
        enterPassword(password);
        clickLogOn();
        
        long loginTime = System.currentTimeMillis() - startTime;
        CSReportManager.info("Login completion time: " + loginTime + "ms");
    }
    
    // Validation methods
    public boolean isLoginPageDisplayed() {
        return usernameField.isDisplayed() && 
               passwordField.isDisplayed() && 
               logOnButton.isDisplayed();
    }
    
    public void assertLoginPageDisplayed() {
        if (isLoginPageDisplayed()) {
            CSReportManager.pass("Login page is displayed correctly");
        } else {
            CSReportManager.fail("Login page elements are missing");
            throw new AssertionError("Login page not properly displayed");
        }
    }
}