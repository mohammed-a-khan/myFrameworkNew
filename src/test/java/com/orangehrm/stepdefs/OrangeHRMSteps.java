package com.orangehrm.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSDataRow;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;
import java.util.List;
import java.util.Map;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.bdd.CSScenarioRunner;

/**
 * Comprehensive step definitions for OrangeHRM application
 * Demonstrates all CS TestForge features:
 * - Clean placeholder syntax with @CSPageInjection
 * - Data-driven testing
 * - Data row access
 * - Data tables
 * - No step type specification needed
 */
@CSFeature(name = "OrangeHRM Steps", tags = {"@orangehrm", "@all"})
public class OrangeHRMSteps extends CSStepDefinitions {
    
    // ULTIMATE SOLUTION: Automatic Page Object Injection
    // Just declare page object fields - framework handles EVERYTHING automatically!
    // - Thread-safe instances per thread (parallel execution safe)
    // - Lazy initialization (created only when used)  
    // - Zero boilerplate code (no ThreadLocal, no getPage() calls)
    // - Works with unlimited page objects
    // - Users simply declare and use - framework does all the heavy lifting!
    
    private LoginPageNew loginPage;        // Auto-injected before each step
    private DashboardPageNew dashboardPage; // Auto-injected before each step
    
    // ================== NAVIGATION STEPS ==================
    
    @CSStep(description = "I am on the OrangeHRM application")
    public void navigateToApplication() {
        logger.info("Navigating to OrangeHRM application");
        loginPage.navigateTo();

    }
    
    @CSStep(description = "I am on the login page")
    public void navigateToLoginPage() {
        logger.info("Navigating to login page");
        loginPage.navigateTo();
        
        // Wait a bit more to ensure page is fully rendered
        try {
            Thread.sleep(2000); // Give page time to render
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Log current URL to verify navigation worked
        String currentUrl = getDriver().getCurrentUrl();
        logger.info("Current URL after navigation: {}", currentUrl);
        
        // SOFT FAIL - this will capture login page screenshot  
        CSReportManager.fail("SOFT FAIL: Login page loaded - URL: " + currentUrl);
    }
    
    @CSStep(description = "I am on the OrangeHRM login page")
    public void navigateToOrangeHRMLoginPage() {
        logger.info("Navigating to OrangeHRM login page");
        loginPage.navigateTo();
    }
    
    @CSStep(description = "I am navigating to the {pageName} page")
    public void navigateToPage(String pageName) {
        logger.info("Navigating to {} page", pageName);
        switch (pageName.toLowerCase()) {
            case "login":
            case "orangehrm login":
                loginPage.navigateTo();
                break;
            case "dashboard":
                assertTrue(dashboardPage.isDisplayed(), "Not on dashboard page");
                break;
            default:
                logger.warn("Unknown page: {}", pageName);
        }
    }
    
    // ================== LOGIN STEPS ==================
    
    @CSStep(description = "I enter username {username} and password {password}")
    public void enterCredentials(String username, String password) {
        logger.info("Entering credentials - Username: {}", username);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }
    
    @CSStep(description = "I login with username {username} and password {password}")
    public void loginWithCredentials(String username, String password) {
        logger.info("Logging in with username: {} and password: {}", username, password);
        System.out.println("DEBUG: loginWithCredentials called with username=" + username + ", password=" + password);
        loginPage.login(username, password);
    }
    
    @CSStep(description = "I click the login button")
    public void clickLoginButton() {
        logger.info("Clicking login button");
        loginPage.clickLogin();
    }
    
    
    @CSStep(description = "I am logged in as {username}")
    public void loginAsUser(String username) {
        logger.info("Logging in as user: {}", username);
        loginPage.navigateTo();
        
        // Default password mapping
        String password = getPasswordForUser(username);
        loginPage.login(username, password);
        
        dashboardPage.assertOnDashboard();
    }
    
    // ================== VERIFICATION STEPS ==================
    
    @CSStep(description = "I should see the dashboard")
    public void verifyDashboard() {
        logger.info("Verifying dashboard is displayed");
        CSReportManager.addAction("verify", "Verify dashboard is displayed");
        assertTrue(dashboardPage.isDisplayed(), "Dashboard not displayed");
    }
    
    @CSStep(description = "I should see an error message {errorMessage}")
    public void verifyErrorMessage(String errorMessage) {
        logger.info("Verifying error message: {}", errorMessage);
        CSReportManager.addAction("verify", "Verify error message", "error message", errorMessage);
        String actualMessage = loginPage.getErrorMessage();
        assertEquals(actualMessage, errorMessage, "Error message mismatch");
    }
    
    @CSStep(description = "I should be on the {pageName} page")
    public void verifyOnPage(String pageName) {
        logger.info("Verifying on {} page", pageName);
        String currentUrl = getDriver().getCurrentUrl();
        assertTrue(currentUrl.contains(pageName.toLowerCase()), 
            "Not on " + pageName + " page. Current URL: " + currentUrl);
    }
    
    @CSStep(description = "I should see {elementName} element")
    public void verifyElementVisible(String elementName) {
        logger.info("Verifying {} element is visible", elementName);
        // Generic element verification
        CSElement element = findElement("id:" + elementName, elementName);
        assertTrue(element != null && element.isDisplayed(), 
            elementName + " element not found");
    }
    
    // ================== DATA ROW EXAMPLES ==================
    
    @CSStep(description = "I login with test data")
    public void loginWithTestData(@CSDataRow Map<String, String> dataRow) {
        logger.info("Logging in with test data: {}", dataRow);
        String username = dataRow.get("username");
        String password = dataRow.get("password");
        
        loginPage.login(username, password);
    }
    
    @CSStep(description = "I verify all user data")
    public void verifyAllUserData(@CSDataRow Map<String, String> dataRow) {
        logger.info("Verifying user data with {} fields", dataRow.size());
        
        // Log all data fields
        for (Map.Entry<String, String> entry : dataRow.entrySet()) {
            logger.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        
        // Example verification
        if (dataRow.containsKey("expectedUser")) {
            String expectedUser = dataRow.get("expectedUser");
            String actualUser = dashboardPage.getUserName();
            assertEquals(actualUser, expectedUser, "User mismatch");
        }
    }
    
    @CSStep(description = "I verify login with complete data set")
    public void verifyLoginWithDataSet(@CSDataRow(includeMetadata = true) Map<String, String> fullDataRow) {
        logger.info("Full data row with metadata:");
        
        // Log metadata
        String dataSource = fullDataRow.get("dataSourceType");
        String sourceFile = fullDataRow.get("dataSourceFile");
        logger.info("Data source: {} from file: {}", dataSource, sourceFile);
        
        // Perform login
        String username = fullDataRow.get("username");
        String password = fullDataRow.get("password");
        loginPage.login(username, password);
        
        // Verify based on expected result
        if (fullDataRow.containsKey("expectedResult")) {
            String expectedResult = fullDataRow.get("expectedResult");
            if ("success".equals(expectedResult)) {
                verifyDashboard();
            } else if (fullDataRow.containsKey("errorMessage")) {
                verifyErrorMessage(fullDataRow.get("errorMessage"));
            }
        }
    }
    
    // ================== DATA TABLE EXAMPLES ==================
    
    @CSStep(description = "I enter the following credentials:")
    public void enterCredentialsFromTable(List<Map<String, String>> dataTable) {
        logger.info("Entering credentials from data table");
        for (Map<String, String> row : dataTable) {
            String field = row.get("field");
            String value = row.get("value");
            logger.info("Setting {} to {}", field, value);
            
            switch (field.toLowerCase()) {
                case "username":
                    loginPage.enterUsername(value);
                    break;
                case "password":
                    loginPage.enterPassword(value);
                    break;
                default:
                    logger.warn("Unknown field: {}", field);
            }
        }
    }
    
    @CSStep(description = "I verify the following users can login:")
    public void verifyMultipleUsersLogin(List<Map<String, String>> users) {
        logger.info("Verifying {} users can login", users.size());
        
        for (Map<String, String> user : users) {
            String username = user.get("username");
            String password = user.get("password");
            String expectedResult = user.get("expectedResult");
            
            logger.info("Testing login for user: {}", username);
            loginPage.navigateTo();
            loginPage.login(username, password);
            
            if ("success".equals(expectedResult)) {
                assertTrue(dashboardPage.isDisplayed(), 
                    "Login failed for user: " + username);
                dashboardPage.logout();
            } else {
                String errorMessage = loginPage.getErrorMessage();
                assertNotNull(errorMessage, "Expected error for user: " + username);
            }
        }
    }
    
    // ================== UTILITY STEPS ==================
    
    @CSStep(description = "I take a screenshot {screenshotName}")
    public void takeScreenshot(String screenshotName) {
        logger.info("Taking screenshot: {}", screenshotName);
        CSReportManager.addAction("screenshot", "Capture screenshot", screenshotName);
        captureScreenshot(screenshotName);
    }
    
    @CSStep(description = "I wait for {seconds} seconds")
    public void waitForSeconds(int seconds) {
        logger.info("Waiting for {} seconds", seconds);
        CSReportManager.addAction("wait", "Wait for " + seconds + " seconds");
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @CSStep(description = "I refresh the page")
    public void refreshPage() {
        logger.info("Refreshing the page");
        getDriver().refresh();
    }
    
    @CSStep(description = "I clear browser cache")
    public void clearBrowserCache() {
        logger.info("Clearing browser cache");
        getDriver().deleteAllCookies();
    }
    
    @CSStep(description = "I maximize the browser window")
    public void maximizeBrowser() {
        logger.info("Maximizing browser window");
        getDriver().maximize();
    }
    
    @CSStep(description = "I log {message}")
    public void logMessage(String message) {
        logger.info("User message: {}", message);
        CSReportManager.addAction("log", "Log message", null, message);
        // Also log to report manager for visibility in HTML reports
        CSReportManager.getInstance().logInfo(message);
    }
    
    @CSStep(description = "I log pass {message}")
    public void logPass(String message) {
        logger.info("PASS: {}", message);
        CSReportManager.getInstance().logInfo("✅ PASS: " + message);
    }
    
    @CSStep(description = "I log fail {message}")
    public void logFail(String message) {
        logger.error("FAIL: {}", message);
        CSReportManager.getInstance().logError("❌ FAIL: " + message);
    }
    
    @CSStep(description = "I log warning {message}")
    public void logWarning(String message) {
        logger.warn("WARNING: {}", message);
        CSReportManager.getInstance().logWarning("⚠️ WARNING: " + message);
    }
    
    @CSStep(description = "I record the start time")
    public void recordStartTime() {
        long startTime = System.currentTimeMillis();
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            runner.storeInContext("startTime", startTime);
        }
        CSReportManager.getInstance().logInfo("⏱️ Start time recorded: " + startTime);
    }
    
    @CSStep(description = "I record the page load time")
    public void recordPageLoadTime() {
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            Long startTime = runner.getFromContext("startTime");
            if (startTime != null) {
                long loadTime = System.currentTimeMillis() - startTime;
                runner.storeInContext("pageLoadTime", loadTime);
                CSReportManager.getInstance().logInfo("⏱️ Page load time: " + loadTime + " ms");
            }
        }
    }
    
    @CSStep(description = "I record the total execution time")
    public void recordTotalTime() {
        CSScenarioRunner runner = CSScenarioRunner.getCurrentInstance();
        if (runner != null) {
            Long startTime = runner.getFromContext("startTime");
            if (startTime != null) {
                long totalTime = System.currentTimeMillis() - startTime;
                CSReportManager.getInstance().logInfo("⏱️ Total execution time: " + totalTime + " ms");
            }
        }
    }
    
    @CSStep(description = "I wait for dashboard to load")
    public void waitForDashboard() {
        assertTrue(dashboardPage.isDisplayed(), "Dashboard should be displayed");
    }
    
    @CSStep(description = "I verify login result is {expected}")
    public void verifyLoginResult(String expected) {
        if ("success".equals(expected)) {
            assertTrue(dashboardPage.isDisplayed(), "Dashboard should be displayed for successful login");
        } else if ("failure".equals(expected)) {
            assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed for failed login");
        }
    }
    
    @CSStep(description = "I should see the dashboard page")
    public void shouldSeeDashboardPage() {
        logger.info("Verifying dashboard page is displayed");
        assertTrue(dashboardPage.isDisplayed(), "Dashboard page should be displayed");
        CSReportManager.getInstance().logInfo("Dashboard page is displayed");
    }
    
    @CSStep(description = "the dashboard header should display {headerText}")
    public void dashboardHeaderShouldDisplay(String headerText) {
        logger.info("Verifying dashboard header displays: {}", headerText);
        String actualHeader = dashboardPage.getHeaderText();
        assertTrue(actualHeader.contains(headerText), 
            "Dashboard header should contain '" + headerText + "' but was '" + actualHeader + "'");
        CSReportManager.getInstance().logInfo("Dashboard header displays: " + headerText);
    }
    
    @CSStep(description = "I should see an error message containing {errorText}")
    public void shouldSeeErrorMessageContaining(String errorText) {
        logger.info("Verifying error message contains: {}", errorText);
        String actualMessage = loginPage.getErrorMessage();
        assertTrue(actualMessage.contains(errorText), 
            "Error message should contain '" + errorText + "' but was '" + actualMessage + "'");
        CSReportManager.getInstance().logInfo("Error message contains: " + errorText);
    }
    
    @CSStep(description = "I should remain on the login page")
    public void shouldRemainOnLoginPage() {
        logger.info("Verifying user remains on login page");
        assertTrue(loginPage.isDisplayed(), "Should remain on login page");
        assertTrue(getDriver().getCurrentUrl().contains("login"), 
            "URL should contain 'login' but was: " + getDriver().getCurrentUrl());
        CSReportManager.getInstance().logInfo("User remains on login page");
    }
    
    // ================== ACTION STEPS ==================
    
    @CSStep(description = "I click the {buttonName} button")
    public void clickButton(String buttonName) {
        logger.info("Clicking {} button", buttonName);
        switch (buttonName.toLowerCase()) {
            case "login":
                loginPage.clickLogin();
                break;
            case "logout":
                dashboardPage.logout();
                break;
            default:
                // Generic button click
                findElement("xpath://button[contains(text(),'" + buttonName + "')]", buttonName + " button").click();
        }
    }
    
    @CSStep(description = "I click on {linkText} link")
    public void clickLink(String linkText) {
        logger.info("Clicking on {} link", linkText);
        findElement("link:" + linkText, linkText + " link").click();
    }
    
    @CSStep(description = "I enter {value} in the {fieldName} field")
    public void enterValueInField(String value, String fieldName) {
        logger.info("Entering '{}' in {} field", value, fieldName);
        findElement("id:" + fieldName, fieldName + " field").type(value);
    }
    
    // ================== COMPLEX SCENARIOS ==================
    
    @CSStep(description = "I perform a complete login test")
    public void performCompleteLoginTest() {
        logger.info("Performing complete login test");
        
        // Using inherited helper methods to access data row
        Map<String, String> currentDataRow = getDataRow();
        if (!currentDataRow.isEmpty()) {
            String loginUsername = getDataValue("username");
            String loginPassword = getDataValue("password");
            
            loginPage.navigateTo();
            loginPage.login(loginUsername, loginPassword);
            
            if (hasDataKey("expectedResult")) {
                String expectedResult = getDataValue("expectedResult");
                if ("success".equals(expectedResult)) {
                    verifyDashboard();
                } else if (hasDataKey("errorMessage")) {
                    verifyErrorMessage(getDataValue("errorMessage"));
                }
            }
        }
    }
    
    @CSStep(description = "I log the current URL")
    public void logCurrentUrl() {
        String currentUrl = getDriver().getCurrentUrl();
        logger.info("Current URL: {}", currentUrl);
        CSReportManager.getInstance().logInfo("Current URL: " + currentUrl);
        System.out.println("DEBUG: CURRENT URL = " + currentUrl);
    }
    
    // ================== HELPER METHODS ==================
    
    private String getPasswordForUser(String username) {
        // Default password mapping
        switch (username.toLowerCase()) {
            case "admin":
                return "admin123";
            case "user":
                return "user123";
            case "test":
                return "test123";
            default:
                return "password123";
        }
    }
    
    // ================== MIXED PARAMETER TEST ==================
    
    @CSStep(description = "I navigate to {menuItem} with test data")
    public void navigateToMenuItemWithData(String menuItem, @CSDataRow(includeMetadata = true) Map<String, String> fullDataRow) {
        logger.info("Navigating to menu: {} with data: {}", menuItem, fullDataRow);
        logger.info("Menu item parameter: {}", menuItem);
        logger.info("Data row size: {}", fullDataRow.size());
        
        // This step tests the fix for mixed parameters (text + @CSDataRow)
        // Just log the information for now
        if (fullDataRow.containsKey("username")) {
            logger.info("Username from data row: {}", fullDataRow.get("username"));
        }
    }
    
    @CSStep(description = "I navigate to {menuItem}")
    public void navigateToMenuItem(String menuItem) {
        logger.info("Navigating to menu item: {}", menuItem);
        
        // Simple navigation without data row dependency
        // This version works for regular scenarios
        switch (menuItem.toLowerCase()) {
            case "admin":
                logger.info("Navigating to Admin menu");
                // Add actual navigation logic here if needed
                break;
            case "pim":
                logger.info("Navigating to PIM menu");
                break;
            default:
                logger.info("Navigating to {} menu", menuItem);
        }
    }
}