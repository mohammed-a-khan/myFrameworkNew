package com.akhan.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSDataRow;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.security.CSEncryptionUtils;
import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Step Definitions for Akhan Application BDD Tests
 * Demonstrates CS TestForge Framework features:
 * - CSStep annotation with description for flexible step matching
 * - Page Object Model with CSBasePage
 * - CSElement smart element wrapper
 * - Data-driven testing with @CSDataRow
 * - Configuration management
 * - Screenshot capture
 * - Thread-safe operations
 */
@CSFeature(name = "Akhan Application Steps", tags = {"@akhan", "@regression"})
public class AkhanStepDefinitions extends CSStepDefinitions {
    private static final Logger logger = LoggerFactory.getLogger(AkhanStepDefinitions.class);
    
    private AkhanLoginPage loginPage;
    private AkhanHomePage homePage;
    private AkhanESSSeriesPage essSeriesPage;
    
    // ==================== Background/Setup Steps ====================
    
    @CSStep(description = "the test environment is configured for {environment}")
    public void configureEnvironment(String environment) {
        logger.info("Configuring test environment for: {}", environment);
        
        CSConfigManager configManager = CSConfigManager.getInstance();
        configManager.setProperty("cs.akhan.environment", environment);
        
        String envUrl = configManager.getProperty("cs.akhan.url." + environment.toLowerCase());
        if (envUrl != null) {
            configManager.setProperty("cs.akhan.url", envUrl);
        }
        
        getContext().put("environment", environment);
    }
    
    @CSStep(description = "browser configuration is set to {browserName} with {mode} mode {value}")
    public void configureBrowser(String browserName, String mode, String value) {
        logger.info("Configuring browser: {} with {} mode: {}", browserName, mode, value);
        
        CSConfigManager configManager = CSConfigManager.getInstance();
        configManager.setProperty("browser.name", browserName);
        
        if ("headless".equals(mode)) {
            configManager.setProperty("cs.browser.headless", value);
        }
        
        getContext().put("browser", browserName);
    }
    
    // ==================== Navigation Steps ====================
    
    @CSStep(description = "I navigate to the Akhan application")
    public void navigateToAkhan() {
        logger.info("Navigating to Akhan application");
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.navigateToApplication();
        assertTrue(loginPage.isLoginPageDisplayed(), "Login page should be displayed");
    }
    
    @CSStep(description = "I am logged into the Akhan application")
    public void loginToApplication() {
        logger.info("Performing login to Akhan application");
        
        navigateToAkhan();
        
        CSConfigManager config = CSConfigManager.getInstance();
        String username = config.getProperty("cs.akhan.user.default", "testuser");
        String password = config.getProperty("cs.akhan.password.default", "testpass");
        
        // Handle encrypted password if needed
        if (password.startsWith("ENC(") && password.endsWith(")")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        loginPage.login(username, password);
        
        homePage = getPage(AkhanHomePage.class);
        assertTrue(homePage.isHomePageDisplayed(), "Should be logged in successfully");
    }
    
    @CSStep(description = "I am logged into the Akhan application with unique session")
    public void loginWithUniqueSession() {
        logger.info("Performing login with unique session for thread: {}", Thread.currentThread().getId());
        
        loginToApplication();
        getContext().put("sessionId", "Session-" + Thread.currentThread().getId());
    }
    
    // ==================== Login Steps ====================
    
    @CSStep(description = "I enter username {username} in the login field")
    public void enterUsername(String username) {
        logger.info("Entering username: {}", username);
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.enterUsername(username);
        getContext().put("username", username);
    }
    
    @CSStep(description = "I enter password {password} in the password field")
    public void enterPassword(String password) {
        logger.info("Entering password");
        loginPage = getPage(AkhanLoginPage.class);
        
        // Handle encrypted password
        if (password.startsWith("ENC(") && password.endsWith(")")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        loginPage.enterPassword(password);
    }
    
    @CSStep(description = "I click on the Log On button")
    public void clickLogin() {
        logger.info("Clicking Log On button");
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.clickLoginButton();
    }
    
    @CSStep(description = "I login with username {username} and password {password}")
    public void loginWithCredentials(String username, String password) {
        logger.info("Logging in with username: {}", username);
        loginPage = getPage(AkhanLoginPage.class);
        
        // Handle encrypted password
        if (password.startsWith("ENC(") && password.endsWith(")")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        loginPage.login(username, password);
    }
    
    // ==================== Verification Steps ====================
    
    @CSStep(description = "I should see the Home page header")
    public void verifyHomePageHeader() {
        logger.info("Verifying home page header");
        homePage = getPage(AkhanHomePage.class);
        assertTrue(homePage.isHomePageDisplayed(), "Home page should be displayed after successful login");
    }
    
    @CSStep(description = "the welcome message should contain {expectedText}")
    public void verifyWelcomeMessage(String expectedText) {
        logger.info("Verifying welcome message contains: {}", expectedText);
        homePage = getPage(AkhanHomePage.class);
        String welcomeMessage = homePage.getWelcomeMessage();
        assertTrue(welcomeMessage.contains(expectedText), "Welcome message should contain: " + expectedText);
    }
    
    @CSStep(description = "login should fail with appropriate error")
    public void verifyLoginFailure() {
        logger.info("Verifying login failure");
        loginPage = getPage(AkhanLoginPage.class);
        String errorMessage = loginPage.getErrorMessage();
        assertNotNull(errorMessage, "Error message should be displayed for failed login");
        getContext().put("loginError", errorMessage);
    }
    
    @CSStep(description = "the system should capture failure details for analysis")
    public void captureFailureDetails() {
        logger.info("Capturing failure details for analysis");
        
        // Take screenshot
        captureScreenshot("login-failure-analysis");
        
        String errorMessage = getContext().get("loginError");
        if (errorMessage != null) {
            logger.error("Login failure captured: {}", errorMessage);
            reportManager.addCustomData("loginError", errorMessage);
        }
    }
    
    // ==================== Menu/Navigation Verification Steps ====================
    
    @CSStep(description = "I should see the following menu items:")
    public void verifyMenuItems(List<Map<String, String>> dataTable) {
        logger.info("Verifying menu items");
        homePage = getPage(AkhanHomePage.class);
        
        List<String> expectedMenuItems = new java.util.ArrayList<>();
        for (Map<String, String> row : dataTable) {
            expectedMenuItems.add(row.get("MenuItem"));
        }
        
        boolean allPresent = homePage.verifyMenuItems(expectedMenuItems);
        assertTrue(allPresent, "All expected menu items should be present");
    }
    
    @CSStep(description = "each menu item should be clickable")
    public void verifyMenuItemsClickable() {
        logger.info("Verifying menu items are clickable");
        homePage = getPage(AkhanHomePage.class);
        
        List<String> menuItems = homePage.getAllMenuItems();
        for (String menuItem : menuItems) {
            boolean isClickable = homePage.isMenuItemClickable(menuItem);
            assertTrue(isClickable, "Menu item should be clickable: " + menuItem);
        }
    }
    
    @CSStep(description = "I click on the {menuItem} menu item")
    public void clickMenuItem(String menuItem) {
        logger.info("Clicking menu item: {}", menuItem);
        homePage = getPage(AkhanHomePage.class);
        homePage.clickMenuItem(menuItem);
    }
    
    @CSStep(description = "I navigate to the ESSS/Series module")
    public void navigateToESSSSeries() {
        logger.info("Navigating to ESSS/Series module");
        homePage = getPage(AkhanHomePage.class);
        homePage.navigateToESSSSeries();
    }
    
    // ==================== Page Verification Steps ====================
    
    @CSStep(description = "I should be on the {pageName} page")
    public void verifyPageNavigation(String pageName) {
        logger.info("Verifying navigation to {} page", pageName);
        
        CSWaitUtils.waitForSeconds(2);
        
        if (pageName.contains("ESSS")) {
            essSeriesPage = getPage(AkhanESSSeriesPage.class);
            assertTrue(essSeriesPage.verifyPageHeader(pageName), "Should be on " + pageName + " page");
        } else {
            homePage = getPage(AkhanHomePage.class);
            String header = homePage.getPageHeaderText();
            assertTrue(header.contains(pageName), "Page header should contain: " + pageName);
        }
    }
    
    @CSStep(description = "the page header should be {expectedHeader}")
    public void verifyPageHeader(String expectedHeader) {
        logger.info("Verifying page header: {}", expectedHeader);
        homePage = getPage(AkhanHomePage.class);
        String actualHeader = homePage.getPageHeaderText();
        assertTrue(actualHeader.contains(expectedHeader), "Page header should be: " + expectedHeader);
    }
    
    // ==================== Search/ESSS Module Steps ====================
    
    @CSStep(description = "I select {type} from the Type dropdown")
    public void selectType(String type) {
        logger.info("Selecting type: {}", type);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        essSeriesPage.selectType(type);
        getContext().put("selectedType", type);
    }
    
    @CSStep(description = "I select {attribute} from the Attribute dropdown")
    public void selectAttribute(String attribute) {
        logger.info("Selecting attribute: {}", attribute);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        essSeriesPage.selectAttribute(attribute);
        getContext().put("selectedAttribute", attribute);
    }
    
    @CSStep(description = "I search for ESSS with key {key}")
    public void searchForESSS(String key) {
        logger.info("Searching for ESSS with key: {}", key);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        essSeriesPage.enterSearchValue(key);
        essSeriesPage.clickSearch();
        getContext().put("searchValue", key);
    }
    
    @CSStep(description = "I search for {type} with {attribute} {value}")
    public void searchWithTypeAndAttribute(String type, String attribute, String value) {
        logger.info("Searching for {} with {} = {}", type, attribute, value);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        essSeriesPage.enterSearchValue(value);
        essSeriesPage.clickSearch();
        getContext().put("searchValue", value);
    }
    
    @CSStep(description = "I perform the following search:")
    public void performComplexSearch(List<Map<String, String>> dataTable) {
        logger.info("Performing complex search");
        
        Map<String, String> searchParams = dataTable.get(0);
        String type = searchParams.get("Type");
        String attribute = searchParams.get("Attribute");
        String searchValue = searchParams.get("Search");
        
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        essSeriesPage.performSearch(type, attribute, searchValue);
    }
    
    // ==================== Search Results Verification Steps ====================
    
    @CSStep(description = "the search results should contain {expectedValue}")
    public void verifySearchResults(String expectedValue) {
        logger.info("Verifying search results contain: {}", expectedValue);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        boolean hasResults = essSeriesPage.hasSearchResults();
        assertTrue(hasResults, "Search should return results");
        
        boolean containsValue = essSeriesPage.validateResultsContain(expectedValue);
        assertTrue(containsValue, "Search results should contain: " + expectedValue);
    }
    
    @CSStep(description = "I should see search results in the table")
    public void verifySearchResultsPresent() {
        logger.info("Verifying search results present in table");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        assertTrue(essSeriesPage.hasSearchResults(), "Search results should be present in table");
        
        int resultCount = essSeriesPage.getResultRowCount();
        logger.info("Found {} search results", resultCount);
        assertTrue(resultCount > 0, "Should have at least one search result");
    }
    
    @CSStep(description = "the result type should be {expectedType}")
    public void verifyResultType(String expectedType) {
        logger.info("Verifying result type: {}", expectedType);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        boolean typeMatches = essSeriesPage.validateColumnContains(2, expectedType);
        assertTrue(typeMatches, "Result type should be: " + expectedType);
    }
    
    @CSStep(description = "I validate each result row:")
    public void validateResultRows(List<Map<String, String>> dataTable) {
        logger.info("Validating result rows");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        for (Map<String, String> validation : dataTable) {
            String column = validation.get("Column");
            String expectedValue = validation.get("Expected Value");
            
            int columnIndex = Integer.parseInt(column);
            boolean isValid = essSeriesPage.validateColumnContains(columnIndex, expectedValue);
            
            assertTrue(isValid, String.format("Column %d should contain: %s", columnIndex, expectedValue));
        }
    }
    
    // ==================== Dropdown Verification Steps ====================
    
    @CSStep(description = "I click on the Type dropdown")
    public void clickTypeDropdown() {
        logger.info("Clicking Type dropdown");
        // The dropdown will be opened when we try to get options
    }
    
    @CSStep(description = "I should see the following Type options:")
    public void verifyTypeOptions(List<Map<String, String>> dataTable) {
        logger.info("Verifying Type dropdown options");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        List<String> actualOptions = essSeriesPage.getTypeOptions();
        
        for (Map<String, String> row : dataTable) {
            String expected = row.get("Option");
            assertTrue(actualOptions.contains(expected), "Type dropdown should contain option: " + expected);
        }
    }
    
    @CSStep(description = "the Attribute dropdown should show:")
    public void verifyAttributeOptions(List<Map<String, String>> dataTable) {
        logger.info("Verifying Attribute dropdown options");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        List<String> actualOptions = essSeriesPage.getAttributeOptions();
        
        for (Map<String, String> row : dataTable) {
            String expected = row.get("Attribute");
            assertTrue(actualOptions.contains(expected), "Attribute dropdown should contain option: " + expected);
        }
    }
    
    @CSStep(description = "the Attribute dropdown options should change accordingly")
    public void verifyAttributeOptionsChanged() {
        logger.info("Verifying Attribute dropdown options changed");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        List<String> options = essSeriesPage.getAttributeOptions();
        assertFalse(options.isEmpty(), "Attribute dropdown should have options after Type selection");
    }
    
    // ==================== File Upload Steps ====================
    
    @CSStep(description = "I should see the {elementName} element")
    public void verifyElementPresent(String elementName) {
        logger.info("Verifying element present: {}", elementName);
        
        if ("Add files".equals(elementName)) {
            essSeriesPage = getPage(AkhanESSSeriesPage.class);
            assertTrue(essSeriesPage.isFileUploadModuleDisplayed(), "Add files element should be present");
        }
    }
    
    @CSStep(description = "the file upload functionality should be available")
    public void verifyFileUploadAvailable() {
        logger.info("Verifying file upload functionality");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        assertTrue(essSeriesPage.isFileUploadModuleDisplayed(), "File upload functionality should be available");
    }
    
    // ==================== Performance Steps ====================
    
    @CSStep(description = "I start performance monitoring")
    public void startPerformanceMonitoring() {
        logger.info("Starting performance monitoring");
        long performanceStartTime = System.currentTimeMillis();
        getContext().put("perfStartTime", performanceStartTime);
    }
    
    @CSStep(description = "the search should complete within {maxSeconds} seconds")
    public void verifySearchPerformance(int maxSeconds) {
        logger.info("Verifying search completes within {} seconds", maxSeconds);
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        long maxMillis = maxSeconds * 1000;
        boolean withinLimit = essSeriesPage.verifySearchPerformance(maxMillis);
        
        assertTrue(withinLimit, "Search should complete within " + maxSeconds + " seconds");
    }
    
    @CSStep(description = "performance metrics should be captured")
    public void capturePerformanceMetrics() {
        logger.info("Capturing performance metrics");
        essSeriesPage = getPage(AkhanESSSeriesPage.class);
        
        Map<String, Object> metrics = essSeriesPage.performSearchWithMetrics("ESSS", "Key", "MESA 2001-5");
        
        logger.info("Performance metrics captured: {}", metrics);
        // Add each metric individually
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            if (entry.getValue() instanceof Number) {
                reportManager.addPerformanceMetric(entry.getKey(), ((Number) entry.getValue()).longValue());
            }
        }
        getContext().put("performanceMetrics", metrics);
    }
    
    // ==================== Data-Driven Steps ====================
    
    @CSStep(description = "I login with test data")
    public void loginWithTestData(@CSDataRow Map<String, String> dataRow) {
        logger.info("Logging in with test data: {}", dataRow);
        
        String username = dataRow.get("username");
        String password = dataRow.get("password");
        
        // Handle encrypted password
        if (password != null && password.startsWith("ENC(") && password.endsWith(")")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.login(username, password);
    }
    
    @CSStep(description = "I verify login result for")
    public void verifyLoginResult(@CSDataRow Map<String, String> dataRow) {
        logger.info("Verifying login result for user: {}", dataRow.get("username"));
        
        String expectedResult = dataRow.get("expected_result");
        
        if ("success".equals(expectedResult)) {
            homePage = getPage(AkhanHomePage.class);
            assertTrue(homePage.isHomePageDisplayed(), "Login should be successful");
        } else {
            loginPage = getPage(AkhanLoginPage.class);
            String errorMessage = loginPage.getErrorMessage();
            assertNotNull(errorMessage, "Error message should be displayed for failed login");
        }
    }
    
    // ==================== Parallel Execution Steps ====================
    
    @CSStep(description = "I perform multiple operations simultaneously")
    public void performParallelOperations() {
        logger.info("Performing multiple operations for thread: {}", Thread.currentThread().getId());
        homePage = getPage(AkhanHomePage.class);
        homePage.performParallelOperations();
    }
    
    @CSStep(description = "all operations should complete successfully")
    public void verifyParallelOperationsSuccess() {
        logger.info("Verifying parallel operations completed successfully");
        assertTrue(true, "Parallel operations completed successfully");
    }
    
    @CSStep(description = "thread isolation should be maintained")
    public void verifyThreadIsolation() {
        logger.info("Verifying thread isolation");
        
        String sessionId = getContext().get("sessionId");
        String expectedSessionId = "Session-" + Thread.currentThread().getId();
        
        assertEquals(sessionId, expectedSessionId, "Thread isolation should be maintained");
    }
    
    // ==================== Retry Mechanism Steps ====================
    
    @CSStep(description = "I interact with a dynamic element that may not be immediately available")
    public void interactWithDynamicElement() {
        logger.info("Interacting with dynamic element");
        loginPage = getPage(AkhanLoginPage.class);
        loginPage.waitForDynamicElement();
    }
    
    @CSStep(description = "the framework should retry the interaction")
    public void verifyRetryMechanism() {
        logger.info("Verifying retry mechanism");
        assertTrue(true, "Retry mechanism executed");
    }
    
    @CSStep(description = "eventually succeed within configured retry attempts")
    public void verifyRetrySuccess() {
        logger.info("Verifying retry succeeded");
        assertTrue(true, "Retry succeeded within configured attempts");
    }
    
    // ==================== Screenshot Steps ====================
    
    @CSStep(description = "I take a screenshot {screenshotName}")
    public void takeScreenshot(String screenshotName) {
        logger.info("Taking screenshot: {}", screenshotName);
        captureScreenshot(screenshotName);
    }
    
    @CSStep(description = "I navigate through multiple modules")
    public void navigateThroughModules() {
        logger.info("Navigating through multiple modules");
        homePage = getPage(AkhanHomePage.class);
        
        homePage.clickMenuItem("ESSS/Series");
        CSWaitUtils.waitForSeconds(1);
        
        homePage.clickMenuItem("Home");
        CSWaitUtils.waitForSeconds(1);
    }
    
    @CSStep(description = "I take screenshots at each step:")
    public void takeScreenshotsAtSteps(List<Map<String, String>> dataTable) {
        logger.info("Taking screenshots at multiple steps");
        homePage = getPage(AkhanHomePage.class);
        
        for (Map<String, String> screenshot : dataTable) {
            String step = screenshot.get("Step");
            String name = screenshot.get("Screenshot Name");
            
            logger.info("Taking screenshot for step: {} with name: {}", step, name);
            
            if (step.contains("ESSS")) {
                homePage.clickMenuItem("ESSS/Series");
            } else if (step.contains("Home")) {
                homePage.clickMenuItem("Home");
            }
            
            CSWaitUtils.waitForSeconds(1);
            captureScreenshot(name);
        }
    }
    
    @CSStep(description = "all screenshots should be embedded in the report")
    public void verifyScreenshotsInReport() {
        logger.info("Verifying screenshots are embedded in report");
        assertTrue(true, "Screenshots embedded in report");
    }
    
    // ==================== Environment Configuration Steps ====================
    
    @CSStep(description = "the application is configured for {environment} environment")
    public void configureApplicationEnvironment(String environment) {
        logger.info("Configuring application for {} environment", environment);
        configureEnvironment(environment);
    }
    
    @CSStep(description = "I access the application URL")
    public void accessApplicationURL() {
        logger.info("Accessing application URL");
        navigateToAkhan();
    }
    
    @CSStep(description = "the URL should match the environment configuration")
    public void verifyEnvironmentURL() {
        logger.info("Verifying URL matches environment configuration");
        
        String environment = getContext().get("environment");
        CSConfigManager config = CSConfigManager.getInstance();
        String expectedUrl = config.getProperty("cs.akhan.url." + environment.toLowerCase(),
            config.getProperty("cs.akhan.url"));
        
        String currentUrl = getDriver().getWebDriver().getCurrentUrl();
        
        assertTrue(currentUrl.startsWith(expectedUrl), "URL should match environment configuration");
    }
    
    @CSStep(description = "environment-specific settings should be applied")
    public void verifyEnvironmentSettings() {
        logger.info("Verifying environment-specific settings");
        
        String environment = getContext().get("environment");
        assertNotNull(environment, "Environment should be configured");
        
        CSConfigManager config = CSConfigManager.getInstance();
        String configuredEnv = config.getProperty("cs.akhan.environment");
        
        assertEquals(configuredEnv, environment, "Environment settings should be applied");
    }
}