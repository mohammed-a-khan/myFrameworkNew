package com.testforge.cs.tests;

import com.testforge.cs.core.CSBaseTest;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;
import org.testng.annotations.Test;
import org.testng.Assert;

public class SimpleLoginTest extends CSBaseTest {
    
    @Test
    public void testSimpleLogin() {
        logger.info("Starting simple login test");
        
        // Navigate to login page
        LoginPageNew loginPage = new LoginPageNew();
        loginPage.navigateTo();
        
        // Wait a bit for page to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Current URL after navigation: {}", driver.getCurrentUrl());
        logger.info("Page title: {}", driver.getTitle());
        
        // Try multiple times to check if login page is displayed
        boolean isLoginPageDisplayed = false;
        for (int i = 0; i < 3; i++) {
            try {
                isLoginPageDisplayed = loginPage.isDisplayed();
                if (isLoginPageDisplayed) break;
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.warn("Attempt {} failed: {}", i+1, e.getMessage());
            }
        }
        
        Assert.assertTrue(isLoginPageDisplayed, "Login page should be displayed");
        logger.info("Login page is displayed");
        
        // Perform login
        loginPage.enterUsername("Admin");
        loginPage.enterPassword("admin123");
        loginPage.clickLogin();
        
        logger.info("Login submitted, waiting for dashboard");
        
        // Wait a bit for page to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Current URL after login: {}", driver.getCurrentUrl());
        
        // Check if we're on dashboard
        DashboardPageNew dashboardPage = new DashboardPageNew();
        boolean isDashboardDisplayed = dashboardPage.isDisplayed();
        
        logger.info("Dashboard displayed: {}", isDashboardDisplayed);
        
        Assert.assertTrue(isDashboardDisplayed, "Should be on dashboard after login");
        logger.info("Login test passed!");
    }
}