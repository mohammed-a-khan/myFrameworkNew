package com.testforge.examples;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.annotations.CSTest;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.elements.CSElement;
import org.testng.annotations.Test;

/**
 * Example demonstrating the simplified reporting in CS TestForge Framework
 * Every action is automatically logged using the static methods in CSReportManager
 */
public class SimpleReportingExample extends CSBaseTest {
    
    @Test
    @CSTest(name = "Simple Reporting Demo", description = "Demonstrates automatic action reporting")
    public void testSimpleReporting() {
        // User can log their own messages
        CSReportManager.info("Starting simple reporting test");
        CSReportManager.pass("Test setup completed successfully");
        
        // Navigate to a page - automatically reports the action
        CSDriver csDriver = getCSDriver();
        csDriver.navigateTo("https://www.google.com");
        
        // Find and interact with elements - each action is automatically reported
        CSElement searchBox = csDriver.findElement("name:q", "Google search box");
        searchBox.clearAndType("CS TestForge Framework");
        
        // User can add custom validation messages
        if (searchBox.getAttribute("value").contains("TestForge")) {
            CSReportManager.pass("Search text entered correctly");
        } else {
            CSReportManager.fail("Search text not entered correctly");
        }
        
        // Example of warning message
        CSReportManager.warn("This is just a demo - not searching");
        
        // More examples of automatic reporting
        boolean isDisplayed = searchBox.isDisplayed();
        boolean isEnabled = searchBox.isEnabled();
        
        // User can log test results
        CSReportManager.info("Test completed - search box displayed: " + isDisplayed + ", enabled: " + isEnabled);
        CSReportManager.pass("Simple reporting test completed successfully");
    }
    
    @Test
    @CSTest(name = "Page Object Reporting Demo", description = "Shows reporting in page objects")
    public void testPageObjectReporting() {
        CSReportManager.info("Testing page object reporting");
        
        // Create and use a page object
        GoogleSearchPage searchPage = new GoogleSearchPage();
        searchPage.searchFor("Selenium WebDriver");
        
        CSReportManager.pass("Page object test completed");
    }
}

/**
 * Simple page object demonstrating automatic reporting
 */
@CSPage(name = "Google Search Page", url = "https://www.google.com")
class GoogleSearchPage extends CSBasePage {
    
    private CSElement searchBox = findElement("name:q", "Search input field");
    private CSElement searchButton = findElement("name:btnK", "Search button");
    
    public void searchFor(String text) {
        CSReportManager.info("Performing search for: " + text);
        
        // Navigate to page - automatically reported
        navigateTo();
        
        // Wait for page load - automatically reported
        waitForPageLoad();
        
        // Type in search box - automatically reported
        searchBox.clearAndType(text);
        
        // Check if button is clickable - automatically reported
        if (searchButton.isDisplayed() && searchButton.isEnabled()) {
            CSReportManager.pass("Search button is ready to click");
        }
        
        CSReportManager.info("Search setup completed for: " + text);
    }
}