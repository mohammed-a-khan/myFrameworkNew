package com.akhan.stepdefs;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.annotations.CSFeature;
import com.testforge.cs.bdd.CSStepDefinitions;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.reporting.CSReportManager;
import com.akhan.pages.LoginPage;
import com.akhan.pages.HomePage;
import com.akhan.pages.ESSSeriesPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import java.io.File;
import java.util.List;
import java.util.Map;

@CSFeature(name = "Akhan Application Steps", tags = {"@akhan"})
public class AkhanSteps extends CSStepDefinitions {
    
    private static final CSConfigManager config = CSConfigManager.getInstance();
    private LoginPage loginPage;
    private HomePage homePage;
    private ESSSeriesPage essSeriesPage;
    private String esssKeyFromTestData;
    
    @CSStep(description = "I am on the login page")
    public void navigateToLoginPage() {
        loginPage = getPage(LoginPage.class);
        loginPage.navigateTo();
    }
    
    @CSStep(description = "I enter username {username} and password {password}")
    public void enterCredentials(String username, String password) {
        loginPage = getPage(LoginPage.class);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }
    
    @CSStep(description = "I click the log on button")
    public void clickLogOn() {
        loginPage = getPage(LoginPage.class);
        loginPage.clickLogOn();
    }
    
    @CSStep(description = "I should see the home header")
    public void verifyHomeHeader() {
        homePage = getPage(HomePage.class);
        assertTrue(homePage.isHomeHeaderDisplayed(), "Home header should be displayed");
    }
    
    @CSStep(description = "I should see welcome message for user {username}")
    public void verifyWelcomeMessage(String username) {
        homePage = getPage(HomePage.class);
        String welcomeUserName = homePage.getWelcomeUserName();
        assertTrue(welcomeUserName.contains(username), 
            "Welcome message should contain username: " + username + ", but found: " + welcomeUserName);
    }
    
    @CSStep(description = "I am logged in as {username}")
    public void loginAsUser(String username) {
        loginPage = getPage(LoginPage.class);
        loginPage.navigateTo();
        // Using default password for simplicity - in real test, get from config
        loginPage.login(username, config.getProperty("cs.akhan.password.default", "testpass"));
        homePage = getPage(HomePage.class);
        assertTrue(homePage.isHomeHeaderDisplayed(), "Should be logged in");
    }
    
    @CSStep(description = "I should see the following menu items:")
    public void verifyMenuItems(List<Map<String, String>> menuItems) {
        homePage = getPage(HomePage.class);
        for (Map<String, String> item : menuItems) {
            String menuItem = item.get("menuItem");
            assertTrue(homePage.isMenuItemDisplayed(menuItem), 
                "Menu item '" + menuItem + "' should be displayed");
        }
    }
    
    @CSStep(description = "I click on {menuItem} menu item")
    public void clickMenuItem(String menuItem) {
        homePage = getPage(HomePage.class);
        homePage.clickMenuItem(menuItem);
    }
    
    @CSStep(description = "I should see the {pageHeader} page header")
    public void verifyPageHeader(String pageHeader) {
        String xpath = "//h1[text()='" + pageHeader + "']";
        Assert.assertTrue(isElementDisplayed("xpath:" + xpath), 
            "Page header '" + pageHeader + "' should be displayed");
    }
    
    @CSStep(description = "I should see the {text} span element")
    public void verifySpanElement(String text) {
        String xpath = "//span[text()='" + text + "']";
        Assert.assertTrue(isElementDisplayed("xpath:" + xpath), 
            "Span element with text '" + text + "' should be displayed");
    }
    
    @CSStep(description = "I click on the Type dropdown")
    public void clickTypeDropdown() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        essSeriesPage.clickTypeDropdown();
    }
    
    @CSStep(description = "I should see the following Type options:")
    public void verifyTypeOptions(List<Map<String, String>> options) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        for (Map<String, String> item : options) {
            String option = item.get("option");
            assertTrue(essSeriesPage.isTypeOptionDisplayed(option), 
                "Type option '" + option + "' should be displayed");
        }
    }
    
    @CSStep(description = "I select {option} from Type dropdown")
    public void selectTypeOption(String option) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        essSeriesPage.selectTypeOption(option);
    }
    
    @CSStep(description = "I click on the Attribute dropdown")
    public void clickAttributeDropdown() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        essSeriesPage.clickAttributeDropdown();
    }
    
    @CSStep(description = "I should see the following Attribute options:")
    public void verifyAttributeOptions(List<Map<String, String>> options) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        for (Map<String, String> item : options) {
            String option = item.get("option");
            assertTrue(essSeriesPage.isAttributeOptionDisplayed(option), 
                "Attribute option '" + option + "' should be displayed");
        }
    }
    
    @CSStep(description = "I select {option} from Attribute dropdown")
    public void selectAttributeOption(String option) {
        essSeriesPage = getPage(ESSSeriesPage.class);
        essSeriesPage.selectAttributeOption(option);
    }
    
    @CSStep(description = "I enter ESSS key from test data")
    public void enterESSKeyFromTestData() {
        try {
            // Read test data from JSON file
            ObjectMapper mapper = new ObjectMapper();
            File testDataFile = new File("resources/testdata/akhan-test-data.json");
            JsonNode root = mapper.readTree(testDataFile);
            esssKeyFromTestData = root.path("esssSearch").path("esssKey").asText();
            
            // If not found in JSON, use config fallback
            if (esssKeyFromTestData == null || esssKeyFromTestData.isEmpty()) {
                esssKeyFromTestData = config.getProperty("cs.test.esss.key", "MESA 2001-5");
            }
            
            CSReportManager.getInstance().logInfo("Using ESSS key from test data: " + esssKeyFromTestData);
            
            essSeriesPage = getPage(ESSSeriesPage.class);
            String selectedAttribute = essSeriesPage.getSelectedAttributeText();
            essSeriesPage.enterSearchValue(selectedAttribute, esssKeyFromTestData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read test data", e);
        }
    }
    
    @CSStep(description = "I click the search button")
    public void clickSearchButton() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        essSeriesPage.clickSearch();
    }
    
    @CSStep(description = "I should see search results in the table")
    public void verifySearchResults() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        int rowCount = essSeriesPage.getTableRowCount();
        assertTrue(rowCount > 0, "Search results table should have at least one row");
        CSReportManager.getInstance().logInfo("Found " + rowCount + " rows in search results");
    }
    
    @CSStep(description = "I should find ESSS with the entered key in the results")
    public void verifyESSInResults() {
        essSeriesPage = getPage(ESSSeriesPage.class);
        int rowCount = essSeriesPage.getTableRowCount();
        boolean found = false;
        
        for (int i = 1; i <= rowCount; i++) {
            String cellText = essSeriesPage.getCellText(i, 2);
            if ("ESSS".equals(cellText)) {
                String keyText = essSeriesPage.getSpanTextInCell(i, 4);
                if (esssKeyFromTestData.equals(keyText)) {
                    found = true;
                    CSReportManager.getInstance().logInfo("Found ESSS with key '" + esssKeyFromTestData + "' at row " + i);
                    break;
                }
            }
        }
        
        Assert.assertTrue(found, "ESSS with key '" + esssKeyFromTestData + "' should be found in search results");
    }
    
    // Helper method
    private boolean isElementDisplayed(String locator) {
        try {
            org.openqa.selenium.By by = parseLocatorString(locator);
            return getDriver().findElement(by).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    private org.openqa.selenium.By parseLocatorString(String locatorStr) {
        if (locatorStr.startsWith("xpath:")) {
            return org.openqa.selenium.By.xpath(locatorStr.substring(6));
        } else if (locatorStr.startsWith("css:")) {
            return org.openqa.selenium.By.cssSelector(locatorStr.substring(4));
        } else if (locatorStr.startsWith("id:")) {
            return org.openqa.selenium.By.id(locatorStr.substring(3));
        } else if (locatorStr.startsWith("name:")) {
            return org.openqa.selenium.By.name(locatorStr.substring(5));
        } else {
            return org.openqa.selenium.By.xpath(locatorStr);
        }
    }
}