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
            Thread.sleep(config.getIntProperty("cs.wait.short", 1000) / 2);
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
            // Wait a bit for page to load after login
            Thread.sleep(config.getIntProperty("cs.wait.medium", 2000));
            
            // Check if URL contains dashboard
            String currentUrl = getDriver().getCurrentUrl();
            logger.info("Current URL: {}", currentUrl);
            
            // For OrangeHRM, after successful login, URL should contain /dashboard
            if (currentUrl.contains("/dashboard")) {
                logger.info("Dashboard URL detected");
                return true;
            }
            
            // Also try to check if header element exists
            try {
                boolean headerVisible = headerTitle.isDisplayed(config.getIntProperty("cs.wait.long", 5000) / 1000);
                logger.info("Dashboard header visible: {}", headerVisible);
                return headerVisible;
            } catch (Exception ex) {
                logger.debug("Header not found: {}", ex.getMessage());
            }
            
            return false;
        } catch (Exception e) {
            logger.debug("Dashboard not displayed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isDashboardUrl() {
        String currentUrl = getDriver().getCurrentUrl();
        return currentUrl.contains("/dashboard/index") || currentUrl.contains("/dashboard");
    }
    
    public String getHeaderTitle() {
        try {
            return headerTitle.getText();
        } catch (Exception e) {
            return "";
        }
    }
    
    public String getHeaderText() {
        return getHeaderTitle();
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