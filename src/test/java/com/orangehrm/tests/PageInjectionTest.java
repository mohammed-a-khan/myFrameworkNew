package com.orangehrm.tests;

import com.testforge.cs.annotations.CSPageInjection;
import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.injection.CSSmartPageInjector;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Test to verify @CSPageInjection functionality
 */
public class PageInjectionTest extends CSBaseTest {
    
    @CSPageInjection
    private LoginPageNew loginPage;
    
    @CSPageInjection
    private DashboardPageNew dashboardPage;
    
    @Test(description = "Test @CSPageInjection with OrangeHRM login")
    public void testPageInjectionWithLogin() {
        logger.info("Testing @CSPageInjection functionality");
        
        // Manually inject pages to test the mechanism
        logger.info("Manually injecting @CSPageInjection pages");
        CSSmartPageInjector.injectPages(this);
        
        // Navigate to login page using injected page
        loginPage.navigateTo();
        
        // Verify login page is displayed
        Assert.assertTrue(loginPage.isDisplayed(), "Login page should be displayed");
        
        // Get credentials from config
        String username = config.getProperty("cs.orangehrm.username", "Admin");
        String password = config.getProperty("cs.orangehrm.password", "admin123");
        
        // Perform login
        logger.info("Logging in with username: {}", username);
        loginPage.login(username, password);
        
        // Wait for dashboard
        try {
            Thread.sleep(2000); // Simple wait for demo
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Verify dashboard is displayed
        Assert.assertTrue(dashboardPage.isDisplayed(), "Dashboard should be displayed after login");
        
        logger.info("✅ @CSPageInjection test completed successfully!");
    }
    
}