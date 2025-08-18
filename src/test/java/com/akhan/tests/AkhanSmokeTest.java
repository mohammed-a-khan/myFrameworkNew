package com.akhan.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.annotations.CSTest;
import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Smoke Test for Akhan Application
 * Demonstrates basic framework usage with page objects
 */
public class AkhanSmokeTest extends CSBaseTest {
    
    @Test(description = "Verify login functionality")
    @CSTest(name = "Akhan Login Test", enabled = true)
    public void testLoginFunctionality() {
        logger.info("Starting Akhan login test");
        
        // Navigate to application
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        
        // Verify login page is displayed
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should be displayed");
        
        // Perform login
        String username = config.getProperty("cs.akhan.user.default", "testuser");
        String password = config.getProperty("cs.akhan.password.default", "testpass");
        loginPage.login(username, password);
        
        // Verify successful login
        AkhanHomePage homePage = new AkhanHomePage();
        Assert.assertTrue(homePage.isHomePageDisplayed(), "Home page should be displayed after login");
        
        logger.info("Login test completed successfully");
    }
    
    @Test(description = "Verify navigation to ESSS module")
    @CSTest(name = "Akhan ESSS Navigation Test", enabled = true)
    public void testESSSNavigation() {
        logger.info("Starting ESSS navigation test");
        
        // Login first
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        
        String username = config.getProperty("cs.akhan.user.default", "testuser");
        String password = config.getProperty("cs.akhan.password.default", "testpass");
        loginPage.login(username, password);
        
        // Navigate to ESSS module
        AkhanHomePage homePage = new AkhanHomePage();
        homePage.navigateToESSSSeries();
        
        // Verify ESSS page is displayed
        AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
        Assert.assertTrue(essPage.verifyPageHeader("ESSS"), "ESSS page should be displayed");
        
        logger.info("ESSS navigation test completed successfully");
    }
    
    @Test(description = "Verify search functionality")
    @CSTest(name = "Akhan Search Test", enabled = true)
    public void testSearchFunctionality() {
        logger.info("Starting search functionality test");
        
        // Login and navigate to ESSS
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        
        String username = config.getProperty("cs.akhan.user.default", "testuser");
        String password = config.getProperty("cs.akhan.password.default", "testpass");
        loginPage.login(username, password);
        
        AkhanHomePage homePage = new AkhanHomePage();
        homePage.navigateToESSSSeries();
        
        // Perform search
        AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
        essPage.performSearch("ESSS", "Key", "MESA 2001-5");
        
        // Verify search results
        Assert.assertTrue(essPage.hasSearchResults(), "Search should return results");
        Assert.assertTrue(essPage.validateResultsContain("MESA 2001-5"), "Results should contain search term");
        
        logger.info("Search functionality test completed successfully");
    }
}