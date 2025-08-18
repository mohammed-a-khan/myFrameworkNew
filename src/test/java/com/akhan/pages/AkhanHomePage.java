package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.reporting.CSReportManager;
import org.testng.Assert;
import java.util.List;
import java.util.ArrayList;

/**
 * Akhan Application Home Page
 * Demonstrates proper framework usage:
 * - @CSPage annotation for page configuration
 * - @CSLocator annotations with object repository keys
 * - Multiple locator strategies (xpath, css, locatorKey)
 * - Alternative locators for self-healing
 * - CSElement collections for menu items
 */
@CSPage(
    name = "Akhan Home Page",
    url = "${cs.akhan.url}/home",
    title = "Akhan - Home",
    validateOnLoad = false
)
public class AkhanHomePage extends CSBasePage {
    
    // Page Header Elements
    @CSLocator(locatorKey = "akhan.home.page.header")
    private CSElement pageHeader;
    
    @CSLocator(locatorKey = "akhan.home.welcome.message")
    private CSElement welcomeMessage;
    
    @CSLocator(locatorKey = "akhan.home.user.profile.icon")
    private CSElement userProfileIcon;
    
    @CSLocator(locatorKey = "akhan.home.logout.button")
    private CSElement logoutButton;
    
    // Navigation Menu Items using Object Repository
    @CSLocator(locatorKey = "akhan.menu.home")
    private CSElement homeMenu;
    
    @CSLocator(locatorKey = "akhan.menu.esss.series")
    private CSElement esssSeriesMenu;
    
    @CSLocator(locatorKey = "akhan.menu.reference.interests")
    private CSElement referenceInterestsMenu;
    
    @CSLocator(locatorKey = "akhan.menu.interest.history")
    private CSElement interestHistoryMenu;
    
    @CSLocator(locatorKey = "akhan.menu.external.interests")
    private CSElement externalInterestsMenu;
    
    @CSLocator(locatorKey = "akhan.menu.system.admin")
    private CSElement systemAdminMenu;
    
    @CSLocator(locatorKey = "akhan.menu.version.info")
    private CSElement versionInfoMenu;
    
    @CSLocator(locatorKey = "akhan.menu.file.upload")
    private CSElement fileUploadMenu;
    
    // Using CSS selector with alternatives for menu container
    @CSLocator(
        css = "nav.main-navigation",
        alternativeLocators = {
            "akhan.navigation.container",
            "xpath://div[@class='navigation-menu']"
        },
        description = "Main navigation container",
        aiEnabled = true,
        aiDescription = "Navigation menu containing all menu items"
    )
    private CSElement navigationContainer;
    
    // Dynamic menu items collection
    @CSLocator(
        css = "nav.main-navigation a.menu-item",
        alternativeLocators = {"akhan.menu.all.items"},
        description = "All menu items",
        waitCondition = CSLocator.WaitCondition.PRESENT
    )
    private List<CSElement> allMenuItems;
    
    // Notification elements
    @CSLocator(locatorKey = "akhan.home.notification.icon")
    private CSElement notificationIcon;
    
    @CSLocator(locatorKey = "akhan.home.notification.count")
    private CSElement notificationCount;
    
    // Search functionality
    @CSLocator(locatorKey = "akhan.home.search.box")
    private CSElement searchBox;
    
    @CSLocator(locatorKey = "akhan.home.search.button")
    private CSElement searchButton;
    
    // Dashboard widgets
    @CSLocator(
        css = ".dashboard-widget",
        description = "Dashboard widgets",
        optional = true
    )
    private List<CSElement> dashboardWidgets;
    
    /**
     * Check if home page is displayed
     */
    public boolean isHomePageDisplayed() {
        CSReportManager.info("Verifying home page is displayed");
        
        try {
            boolean displayed = pageHeader.isDisplayed() && 
                              navigationContainer.isPresent();
            
            if (displayed) {
                CSReportManager.pass("Home page is displayed successfully");
            } else {
                CSReportManager.warn("Home page elements are not fully displayed");
            }
            
            return displayed;
        } catch (Exception e) {
            CSReportManager.fail("Error checking home page display: " + e.getMessage());
            logger.error("Error checking if home page is displayed", e);
            return false;
        }
    }
    
    /**
     * Get welcome message text
     */
    public String getWelcomeMessage() {
        if (welcomeMessage.isPresent()) {
            return welcomeMessage.getText();
        }
        return "";
    }
    
    /**
     * Get page header text
     */
    public String getPageHeaderText() {
        if (pageHeader.isPresent()) {
            return pageHeader.getText();
        }
        return "";
    }
    
    /**
     * Click on a menu item using dynamic element pattern
     * This uses the exact XPath: //div[@id='abcdNavigatorBody']//a[text()='<Menu_Item_Name>']
     */
    public void clickMenuItem(String menuName) {
        CSReportManager.info("Clicking menu item: " + menuName);
        logger.info("Clicking menu item: {}", menuName);
        
        try {
            // Use dynamic element pattern from object repository
            CSElement menuElement = findDynamicElement("akhan.menu.dynamic", menuName);
            menuElement.waitForClickable();
            menuElement.click();
            waitForPageLoad();
            CSReportManager.pass("Successfully clicked menu item: " + menuName);
        } catch (Exception e) {
            // Fallback to predefined menu elements
            CSElement menuElement = getMenuElement(menuName);
            if (menuElement != null) {
                try {
                    menuElement.waitForClickable();
                    menuElement.click();
                    waitForPageLoad();
                    CSReportManager.pass("Successfully clicked menu item: " + menuName);
                } catch (Exception ex) {
                    CSReportManager.fail("Failed to click menu item: " + menuName + " - " + ex.getMessage());
                    throw ex;
                }
            } else {
                CSReportManager.fail("Menu item not found: " + menuName);
                throw new RuntimeException("Menu item not found: " + menuName);
            }
        }
    }
    
    /**
     * Get menu element by name
     */
    private CSElement getMenuElement(String menuName) {
        switch (menuName.toLowerCase()) {
            case "home":
                return homeMenu;
            case "esss/series":
            case "esss series":
                return esssSeriesMenu;
            case "reference interests":
                return referenceInterestsMenu;
            case "interest history":
                return interestHistoryMenu;
            case "external interests":
                return externalInterestsMenu;
            case "system admin":
                return systemAdminMenu;
            case "version information":
                return versionInfoMenu;
            case "file upload":
                return fileUploadMenu;
            default:
                logger.warn("Unknown menu item: {}", menuName);
                return null;
        }
    }
    
    /**
     * Navigate to ESSS/Series module
     */
    public void navigateToESSSSeries() {
        CSReportManager.info("Navigating to ESSS/Series module");
        logger.info("Navigating to ESSS/Series module");
        
        try {
            esssSeriesMenu.waitForClickable();
            esssSeriesMenu.click();
            waitForPageLoad();
            CSReportManager.pass("Successfully navigated to ESSS/Series");
        } catch (Exception e) {
            CSReportManager.fail("Failed to navigate to ESSS/Series: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get all menu items text
     */
    public List<String> getAllMenuItems() {
        List<String> menuTexts = new ArrayList<>();
        
        // Add each menu item if present
        if (homeMenu.isPresent()) menuTexts.add("Home");
        if (esssSeriesMenu.isPresent()) menuTexts.add("ESSS/Series");
        if (referenceInterestsMenu.isPresent()) menuTexts.add("Reference Interests");
        if (interestHistoryMenu.isPresent()) menuTexts.add("Interest History");
        if (externalInterestsMenu.isPresent()) menuTexts.add("External Interests");
        if (systemAdminMenu.isPresent()) menuTexts.add("System Admin");
        if (versionInfoMenu.isPresent()) menuTexts.add("Version Information");
        if (fileUploadMenu.isPresent()) menuTexts.add("File Upload");
        
        return menuTexts;
    }
    
    /**
     * Verify menu items are present
     */
    public boolean verifyMenuItems(List<String> expectedItems) {
        CSReportManager.info("Verifying menu items are present");
        List<String> actualItems = getAllMenuItems();
        
        for (String expected : expectedItems) {
            boolean found = actualItems.stream()
                .anyMatch(item -> item.equalsIgnoreCase(expected));
            if (!found) {
                CSReportManager.warn("Menu item not found: " + expected);
                logger.error("Menu item not found: {}", expected);
                return false;
            }
        }
        
        CSReportManager.pass("All expected menu items are present");
        return true;
    }
    
    /**
     * Check if menu item is clickable
     */
    public boolean isMenuItemClickable(String menuName) {
        CSElement menuElement = getMenuElement(menuName);
        return menuElement != null && menuElement.isEnabled();
    }
    
    /**
     * Perform search
     */
    public void performSearch(String searchTerm) {
        CSReportManager.info("Performing search for: " + searchTerm);
        logger.info("Performing search for: {}", searchTerm);
        
        try {
            searchBox.clearAndType(searchTerm);
            searchButton.click();
            waitForPageLoad();
            CSReportManager.pass("Search performed successfully for: " + searchTerm);
        } catch (Exception e) {
            CSReportManager.fail("Search failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get notification count
     */
    public int getNotificationCount() {
        if (notificationCount.isPresent()) {
            String count = notificationCount.getText();
            try {
                return Integer.parseInt(count);
            } catch (NumberFormatException e) {
                logger.warn("Invalid notification count: {}", count);
            }
        }
        return 0;
    }
    
    /**
     * Click notification icon
     */
    public void clickNotifications() {
        notificationIcon.click();
        CSWaitUtils.waitForSeconds(1);
    }
    
    /**
     * Logout from application
     */
    public void logout() {
        CSReportManager.info("Logging out from application");
        logger.info("Logging out from application");
        
        try {
            if (userProfileIcon.isPresent()) {
                userProfileIcon.click();
                CSWaitUtils.waitForSeconds(1);
                CSReportManager.info("Clicked user profile icon");
            }
            
            logoutButton.waitForClickable();
            logoutButton.click();
            CSReportManager.pass("Successfully logged out from application");
        } catch (Exception e) {
            CSReportManager.fail("Failed to logout: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get dashboard widget count
     */
    public int getDashboardWidgetCount() {
        if (dashboardWidgets != null) {
            return dashboardWidgets.size();
        }
        return 0;
    }
    
    /**
     * Perform parallel operations for testing thread safety
     */
    public void performParallelOperations() {
        logger.info("Performing parallel operations on thread: {}", Thread.currentThread().getId());
        
        // Navigate through multiple menu items
        if (homeMenu.isPresent()) {
            logger.info("Thread {} - Home menu is present", Thread.currentThread().getId());
        }
        
        if (esssSeriesMenu.isPresent()) {
            logger.info("Thread {} - ESSS menu is present", Thread.currentThread().getId());
        }
        
        // Perform search
        String searchTerm = "Test-" + Thread.currentThread().getId();
        if (searchBox.isPresent()) {
            searchBox.clearAndType(searchTerm);
            logger.info("Thread {} - Entered search term: {}", Thread.currentThread().getId(), searchTerm);
        }
        
        CSWaitUtils.waitForSeconds(1);
    }
    
    /**
     * Validate home page is ready
     */
    public void assertHomePageReady() {
        Assert.assertTrue(isHomePageDisplayed(), "Home page is not fully loaded");
        Assert.assertTrue(navigationContainer.isPresent(), "Navigation menu should be present");
        logger.info("Home page is ready");
    }
    
    /**
     * Validate user is logged in
     */
    public void assertUserLoggedIn(String expectedUsername) {
        String welcomeText = getWelcomeMessage();
        Assert.assertTrue(welcomeText.contains(expectedUsername), 
            String.format("Welcome message should contain username '%s', but was '%s'", 
                expectedUsername, welcomeText));
    }
}