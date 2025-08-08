package com.orangehrm.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;

@CSPage(name = "OrangeHRM Dashboard Page")
public class DashboardPageNew extends CSBasePage {
    
    // Using Object Repository
    @CSLocator(locatorKey = "dashboard.header.title")
    private CSElement headerTitle;
    
    @CSLocator(locatorKey = "dashboard.user.dropdown")
    private CSElement userDropdown;
    
    @CSLocator(locatorKey = "dashboard.user.dropdown.name") 
    private CSElement userDropdownName;
    
    @CSLocator(locatorKey = "dashboard.logout.link")
    private CSElement logoutLink;
    
    @CSLocator(locatorKey = "dashboard.main.menu")
    private CSElement mainMenu;
    
    @CSLocator(locatorKey = "nav.pim")
    private CSElement pimMenu;
    
    @CSLocator(locatorKey = "nav.admin")
    private CSElement adminMenu;
    
    // Page URL path
    private static final String DASHBOARD_PATH = "/web/index.php/dashboard/index";
    
    // Page Methods
    public String getUserName() {
        return userDropdownName.getText();
    }
    
    public void clickUserDropdown() {
        logger.info("Clicking user dropdown");
        userDropdown.click();
        // Wait for dropdown menu to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void logout() {
        logger.info("Logging out");
        clickUserDropdown();
        logoutLink.waitForClickable().click();
    }
    
    public void navigateToPIM() {
        pimMenu.click();
        waitForPageLoad();
    }
    
    public void navigateToAdmin() {
        adminMenu.click();
        waitForPageLoad();
    }
    
    public boolean isDisplayed() {
        try {
            // Try to find the header title with a shorter timeout
            return headerTitle.isDisplayed(3); // 3 second timeout instead of default
        } catch (Exception e) {
            logger.debug("Dashboard not displayed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isDashboardUrl() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("/dashboard/index") || currentUrl.contains("/dashboard");
    }
    
    public String getHeaderTitle() {
        try {
            return headerTitle.getText();
        } catch (Exception e) {
            return "";
        }
    }
    
    // Validation Methods
    public void assertDashboardDisplayed() {
        if (!isDisplayed()) {
            throw new AssertionError("Dashboard page is not displayed");
        }
        logger.info("Dashboard page is displayed");
    }
    
    public void assertUserLoggedIn(String expectedUsername) {
        String actualUsername = getUserName();
        if (!actualUsername.contains(expectedUsername)) {
            throw new AssertionError("Expected user: " + expectedUsername + ", but found: " + actualUsername);
        }
    }
    
    public void assertOnDashboard() {
        assertDashboardDisplayed();
    }
}