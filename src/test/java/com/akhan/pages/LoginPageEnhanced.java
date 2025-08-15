package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.waits.CSWaitUtils;

@CSPage(name = "Akhan Login Page Enhanced", url = "https://akhan-ui-sit.myshare.net/")
public class LoginPageEnhanced extends CSBasePage {
    
    @CSLocator(locatorKey = "akhan.login.username")
    private CSElement usernameField;
    
    @CSLocator(locatorKey = "akhan.login.password")
    private CSElement passwordField;
    
    @CSLocator(locatorKey = "akhan.login.button")
    private CSElement logOnButton;
    
    /**
     * Navigate to login page with comprehensive reporting
     */
    public void navigateTo() {
        CSReportManager.info("Navigating to Akhan Login Page");
        
        String url = config.getProperty("cs.akhan.url", "https://akhan-ui-sit.myshare.net/");
        CSReportManager.info("Target URL: " + url);
        CSReportManager.info("Environment: " + config.getProperty("cs.akhan.environment", "SIT"));
        
        // Navigate with automatic reporting from base class
        super.navigateTo(url);
        
        // Verify login page loaded
        CSReportManager.info("Verifying login page loaded correctly");
        
        verifyLoginPageElements();
        
        CSReportManager.pass("Login page loaded successfully");
    }
    
    /**
     * Login with detailed step reporting
     */
    public void login(String username, String password) {
        CSReportManager.info("Starting login process for user: " + username);
        
        // Report login attempt details
        CSReportManager.info("Username: " + username);
        CSReportManager.info("Password Length: " + password.length());
        CSReportManager.info("Browser: " + config.getProperty("browser.name", "chrome"));
        
        // Perform login steps
        enterUsername(username);
        enterPassword(password);
        clickLogOn();
        
        // Wait for navigation
        CSReportManager.info("Waiting for login process to complete");
        CSWaitUtils.waitForSeconds(config.getIntProperty("cs.wait.medium", 2000) / 1000);
        
        CSReportManager.pass("Login process completed");
    }
    
    /**
     * Enter username with detailed reporting
     */
    public void enterUsername(String username) {
        CSReportManager.info("Entering username: " + username);
        
        // Pre-action verification
        CSReportManager.info("Verifying username field is ready");
        
        boolean fieldDisplayed = usernameField.isDisplayed();
        boolean fieldEnabled = usernameField.isEnabled();
        String currentValue = usernameField.getAttribute("value");
        
        CSReportManager.info(String.format("Username field - Displayed: %s, Enabled: %s, Current value: '%s'", 
            fieldDisplayed, fieldEnabled, currentValue));
        
        // Clear and type username
        usernameField.clearAndType(username);
        
        // Post-action verification
        String enteredValue = usernameField.getAttribute("value");
        if (username.equals(enteredValue)) {
            CSReportManager.pass("Username entered correctly: " + username);
        } else {
            CSReportManager.fail("Username not entered correctly. Expected: " + username + ", Actual: " + enteredValue);
        }
    }
    
    /**
     * Enter password with security considerations
     */
    public void enterPassword(String password) {
        CSReportManager.info("Entering password (masked)");
        
        // Pre-action verification
        CSReportManager.info("Verifying password field is ready");
        
        CSReportManager.info("Password field type: " + passwordField.getAttribute("type"));
        
        // Enter password (masked in reports)
        passwordField.clearAndType(password);
        
        // Verify password field has value (but don't report actual value)
        if (!passwordField.getAttribute("value").isEmpty()) {
            CSReportManager.pass("Password entered successfully");
        } else {
            CSReportManager.fail("Password field is empty after entering");
        }
    }
    
    /**
     * Click Log On button with comprehensive reporting
     */
    public void clickLogOn() {
        CSReportManager.info("Clicking Log On button");
        
        // Pre-click verification
        CSReportManager.info("Verifying button is ready to click");
        
        // Check button state
        boolean buttonDisplayed = logOnButton.isDisplayed();
        boolean buttonEnabled = logOnButton.isEnabled();
        String buttonText = logOnButton.getText();
        
        CSReportManager.info(String.format("Log On button - Displayed: %s, Enabled: %s, Text: '%s'", 
            buttonDisplayed, buttonEnabled, buttonText));
        
        // Capture screenshot before click
        captureScreenshot("before_login_click");
        
        // Click button
        logOnButton.click();
        
        // Report post-click state
        CSReportManager.info("Login form submitted, waiting for response");
    }
    
    /**
     * Verify all login page elements are present
     */
    private void verifyLoginPageElements() {
        CSReportManager.info("Verifying login page elements");
        
        // Check username field
        if (usernameField.isDisplayed()) {
            CSReportManager.pass("Username field is present");
        } else {
            CSReportManager.fail("Username field is not displayed");
        }
        
        // Check password field
        if (passwordField.isDisplayed()) {
            CSReportManager.pass("Password field is present");
        } else {
            CSReportManager.fail("Password field is not displayed");
        }
        
        // Check login button
        if (logOnButton.isDisplayed()) {
            CSReportManager.pass("Log On button is present");
        } else {
            CSReportManager.fail("Log On button is not displayed");
        }
        
        // Check page title
        String pageTitle = driver.getTitle();
        CSReportManager.info("Page Title: " + pageTitle);
        
        // Check for any error messages
        checkForLoginErrors();
    }
    
    /**
     * Check for login error messages
     */
    private void checkForLoginErrors() {
        try {
            CSElement errorElement = findElement(
                "xpath://div[contains(@class,'error')] | //span[contains(@class,'error')]", 
                "error message");
            
            if (errorElement != null && errorElement.isDisplayed()) {
                String errorText = errorElement.getText();
                CSReportManager.warn("Login error detected: " + errorText);
            }
        } catch (Exception e) {
            // No error message found - this is expected for initial page load
            CSReportManager.info("No login errors detected");
        }
    }
    
    /**
     * Verify login page is displayed
     */
    public boolean isLoginPageDisplayed() {
        CSReportManager.info("Checking if login page is displayed");
        
        boolean allElementsDisplayed = usernameField.isDisplayed() && 
                                      passwordField.isDisplayed() && 
                                      logOnButton.isDisplayed();
        
        CSReportManager.info("Login page displayed: " + allElementsDisplayed);
        
        return allElementsDisplayed;
    }
}