package com.orangehrm.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import org.testng.Assert;

@CSPage(name = "OrangeHRM Dashboard Page")
public class DashboardPageNew extends CSBasePage {
    
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
    
    // Navigation elements
    @CSLocator(locatorKey = "nav.pim")
    private CSElement pimMenu;
    
    @CSLocator(locatorKey = "nav.admin")
    private CSElement adminMenu;
    
    public String getHeaderTitle() {
        return headerTitle.getText();
    }
    
    public String getUserName() {
        return userDropdownName.getText();
    }
    
    public void clickUserDropdown() {
        userDropdown.click();
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
        return headerTitle.isDisplayed() && 
               userDropdown.isDisplayed() &&
               mainMenu.isDisplayed();
    }
    
    public void assertOnDashboard() {
        Assert.assertTrue(isDisplayed(), "Dashboard is not displayed");
        String title = getHeaderTitle();
        Assert.assertEquals(title, "Dashboard", "Not on Dashboard page");
        logger.info("Successfully on Dashboard page");
    }
    
    public void assertUserLoggedIn(String expectedUser) {
        Assert.assertTrue(isDisplayed(), "Dashboard not displayed");
        String actualUser = getUserName();
        Assert.assertEquals(actualUser, expectedUser, 
            "Logged in user mismatch. Expected: " + expectedUser + ", Actual: " + actualUser);
    }
}