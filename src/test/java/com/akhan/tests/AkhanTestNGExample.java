package com.akhan.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.annotations.CSTest;
import com.testforge.cs.waits.CSWaitUtils;
import com.akhan.pages.LoginPage;
import com.akhan.pages.HomePage;
import com.akhan.pages.ESSSeriesPage;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.Assert;
import java.lang.reflect.Method;

/**
 * Example TestNG test class demonstrating CS TestForge features without BDD
 */
public class AkhanTestNGExample extends CSBaseTest {
    
    private LoginPage loginPage;
    private HomePage homePage;
    private ESSSeriesPage essSeriesPage;
    
    @BeforeMethod(alwaysRun = true)
    public void setupPages(Method method) {
        CSReportManager.info("Starting test: " + method.getName());
        CSReportManager.info("Test Description: " + method.getAnnotation(Test.class).description());
    }
    
    @Test(description = "Verify successful login to Akhan application")
    @CSTest(tags = {"@smoke", "@login"}, priority = CSTest.Priority.HIGH)
    public void testSuccessfulLogin() {
        // Navigate to login page
        CSReportManager.info("Step 1: Navigate to Akhan application");
        loginPage = new LoginPage();
        loginPage.navigateTo();
        loginPage.assertLoginPageDisplayed();
        
        // Perform login
        CSReportManager.info("Step 2: Enter credentials and login");
        String username = config.getProperty("cs.akhan.user.default", "testuser");
        String password = config.getProperty("cs.akhan.password.default", "testpass");
        
        loginPage.login(username, password);
        
        // Verify login success
        CSReportManager.info("Step 3: Verify successful login");
        homePage = new HomePage();
        Assert.assertTrue(homePage.isHomeHeaderDisplayed(), "Home header should be displayed after login");
        
        String welcomeUser = homePage.getWelcomeUserName();
        Assert.assertTrue(welcomeUser.contains(username), "Welcome message should contain username");
        
        CSReportManager.pass("Login successful for user: " + username);
        takeScreenshot("login_success");
    }
    
    @Test(description = "Verify all navigation menu items are displayed")
    @CSTest(tags = {"@smoke", "@navigation"})
    public void testNavigationMenuItems() {
        // Login first
        performQuickLogin();
        
        // Define expected menu items
        String[] expectedMenuItems = {
            "Home", "ESSS/Series", "Reference Interests", 
            "Interest History", "External Interests", 
            "System Admin", "Version Information", "File Upload"
        };
        
        CSReportManager.info("=== Navigation Menu Verification ===");
        
        // Verify each menu item
        for (String menuItem : expectedMenuItems) {
            CSReportManager.info("Verifying menu item: " + menuItem);
            boolean isDisplayed = homePage.isMenuItemDisplayed(menuItem);
            
            if (isDisplayed) {
                CSReportManager.pass(menuItem + " is displayed");
            } else {
                CSReportManager.fail(menuItem + " is NOT displayed");
            }
            
            Assert.assertTrue(isDisplayed, "Menu item should be displayed: " + menuItem);
        }
        
        takeScreenshot("navigation_menu");
    }
    
    @Test(description = "Navigate to each module and verify page headers",
          dataProvider = "moduleNavigationData")
    @CSTest(tags = {"@regression", "@navigation"})
    public void testModuleNavigation(String menuItem, String expectedHeader) {
        // Login first
        performQuickLogin();
        
        CSReportManager.info("=== Module Navigation: " + menuItem + " ===");
        
        // Click menu item
        CSReportManager.info("Click on " + menuItem);
        homePage.clickMenuItem(menuItem);
        CSWaitUtils.waitForSeconds(config.getIntProperty("cs.wait.medium", 2000) / 1000);
        
        // Verify page header
        String xpath = "//h1[text()='" + expectedHeader + "']";
        boolean headerDisplayed = isElementDisplayed(xpath);
        
        if (headerDisplayed) {
            CSReportManager.pass("Page Header '" + expectedHeader + "' is displayed");
        } else {
            CSReportManager.fail("Page Header '" + expectedHeader + "' not found");
        }
        
        Assert.assertTrue(headerDisplayed, "Page header should be displayed: " + expectedHeader);
        takeScreenshot(menuItem.toLowerCase().replace(" ", "_") + "_page");
    }
    
    @Test(description = "Perform ESSS search and verify results")
    @CSTest(tags = {"@regression", "@search"}, priority = CSTest.Priority.HIGH)
    public void testESSSearch() {
        // Login and navigate to ESSS/Series
        performQuickLogin();
        
        CSReportManager.info("Starting ESSS Search Test");
        String searchKey = config.getProperty("cs.test.esss.key", "MESA 2001-5");
        CSReportManager.info("Searching for ESSS with key: " + searchKey);
        
        // Navigate to ESSS/Series
        homePage.clickMenuItem("ESSS/Series");
        essSeriesPage = new ESSSeriesPage();
        Assert.assertTrue(essSeriesPage.isPageHeaderDisplayed(), "ESSS/Series page should be displayed");
        
        // Select Type = ESSS
        CSReportManager.info("Step 1: Select Type: ESSS");
        essSeriesPage.clickTypeDropdown();
        essSeriesPage.selectTypeOption("ESSS");
        Assert.assertEquals(essSeriesPage.getSelectedTypeText(), "ESSS", "ESSS should be selected");
        
        // Select Attribute = Key
        CSReportManager.info("Step 2: Select Attribute: Key");
        essSeriesPage.clickAttributeDropdown();
        essSeriesPage.selectAttributeOption("Key");
        Assert.assertEquals(essSeriesPage.getSelectedAttributeText(), "Key", "Key should be selected");
        
        // Enter search value
        CSReportManager.info("Step 3: Enter search value: " + searchKey);
        essSeriesPage.enterSearchValue("Key", searchKey);
        takeScreenshot("esss_search_input");
        
        // Execute search
        CSReportManager.info("Step 4: Execute search");
        essSeriesPage.clickSearch();
        CSWaitUtils.waitForSeconds(config.getIntProperty("cs.wait.long", 5000) / 1000);
        
        // Verify results
        CSReportManager.info("Step 5: Verify search results");
        int rowCount = essSeriesPage.getTableRowCount();
        CSReportManager.info("Found " + rowCount + " results");
        
        boolean found = false;
        for (int i = 1; i <= rowCount; i++) {
            String type = essSeriesPage.getCellText(i, 2);
            if ("ESSS".equals(type)) {
                String key = essSeriesPage.getSpanTextInCell(i, 4);
                if (searchKey.equals(key)) {
                    found = true;
                    CSReportManager.pass("Found ESSS with key '" + searchKey + "' at row " + i);
                    break;
                }
            }
        }
        
        Assert.assertTrue(found, "ESSS with key '" + searchKey + "' should be found in results");
        takeScreenshot("esss_search_results");
        
        CSReportManager.pass("ESSS Search Test completed successfully");
    }
    
    @Test(description = "Test with performance metrics")
    @CSTest(tags = {"@performance"})
    public void testLoginPerformance() {
        CSReportManager.info("=== Performance Test - Login ===");
        
        long startTime = System.currentTimeMillis();
        
        // Navigate to login
        loginPage = new LoginPage();
        loginPage.navigateTo();
        
        long navigationTime = System.currentTimeMillis() - startTime;
        CSReportManager.info("Navigation to login page: " + navigationTime + "ms");
        
        // Perform login
        long loginStartTime = System.currentTimeMillis();
        loginPage.login(config.getProperty("cs.akhan.user.default", "testuser"), 
                       config.getProperty("cs.akhan.password.default", "testpass"));
        
        long loginTime = System.currentTimeMillis() - loginStartTime;
        CSReportManager.info("Login execution: " + loginTime + "ms");
        
        // Total time
        long totalTime = System.currentTimeMillis() - startTime;
        CSReportManager.info("Total operation: " + totalTime + "ms");
        
        // Performance assertions
        int pageLoadThreshold = config.getIntProperty("cs.akhan.performance.pageload", 2000);
        int totalThreshold = config.getIntProperty("cs.akhan.performance.total", 5000);
        
        if (navigationTime <= pageLoadThreshold) {
            CSReportManager.pass("Page load within threshold: " + 
                navigationTime + "ms <= " + pageLoadThreshold + "ms");
        } else {
            CSReportManager.warn("Page load exceeds threshold: " + 
                navigationTime + "ms > " + pageLoadThreshold + "ms");
        }
        
        Assert.assertTrue(totalTime <= totalThreshold, 
            "Total time should be within " + totalThreshold + "ms");
    }
    
    // ================== DATA PROVIDERS ==================
    
    @DataProvider(name = "moduleNavigationData")
    public Object[][] getModuleNavigationData() {
        return new Object[][] {
            {"ESSS/Series", "ESSSs/Series"},
            {"Reference Interests", "Reference Interests"},
            {"Interest History", "Interest History"},
            {"External Interests", "External Interests"},
            {"System Admin", "System Admin"},
            {"Version Information", "Version Information"}
        };
    }
    
    // ================== HELPER METHODS ==================
    
    private void performQuickLogin() {
        loginPage = new LoginPage();
        loginPage.navigateTo();
        loginPage.login(config.getProperty("cs.akhan.user.default", "testuser"), 
                       config.getProperty("cs.akhan.password.default", "testpass"));
        
        homePage = new HomePage();
        CSWaitUtils.waitForElementVisible(driver, org.openqa.selenium.By.xpath("//h1[text()='Home']"), config.getIntProperty("cs.wait.long", 5000) * 2 / 1000);
    }
    
    private boolean isElementDisplayed(String xpath) {
        try {
            return driver.findElement(org.openqa.selenium.By.xpath(xpath)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}