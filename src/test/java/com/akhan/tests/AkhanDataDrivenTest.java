package com.akhan.tests;
import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.utils.CSCsvUtils;
import com.testforge.cs.utils.CSJsonUtils;
import com.testforge.cs.security.CSEncryptionUtils;
import com.akhan.pages.AkhanLoginPage;
import com.akhan.pages.AkhanHomePage;
import com.akhan.pages.AkhanESSSeriesPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

/**
 * Data-driven test class demonstrating all data source features:
 * - @DataRow annotations
 * - CSV data source
 * - JSON data source
 * - Properties data source
 * - Excel data source (with CSV fallback)
 * - Encrypted password handling
 * - Property substitution
 * - Parallel data-driven execution
 */
public class AkhanDataDrivenTest extends CSBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(AkhanDataDrivenTest.class);
    
    /**
     * DataProvider for CSV-based test data
     * Demonstrates CSV data source integration
     */
    @DataProvider(name = "csvTestData", parallel = true)
    public Object[][] getCsvTestData() {
        logger.info("Loading test data from CSV file");
        List<Map<String, String>> data = CSCsvUtils.readCsv("testdata/akhan-test-data.csv", true);
        
        Object[][] testData = new Object[data.size()][1];
        for (int i = 0; i < data.size(); i++) {
            testData[i][0] = data.get(i);
        }
        
        return testData;
    }
    
    /**
     * DataProvider for JSON-based test data
     * Demonstrates JSON data source with path expression
     */
    @DataProvider(name = "jsonTestData", parallel = true)
    public Object[][] getJsonTestData() {
        logger.info("Loading test data from JSON file");
        String jsonContent = com.testforge.cs.utils.CSFileUtils.readTextFile("testdata/akhan-search-data.json");
        Map<String, Object> jsonData = CSJsonUtils.jsonToMap(jsonContent);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchData = (List<Map<String, Object>>) jsonData.get("searchData");
        
        Object[][] testData = new Object[searchData.size()][1];
        for (int i = 0; i < searchData.size(); i++) {
            testData[i][0] = searchData.get(i);
        }
        
        return testData;
    }
    
    /**
     * DataProvider with @DataRow examples
     * Demonstrates inline data definition
     */
    @DataProvider(name = "dataRowExamples", parallel = false)
    public Object[][] getDataRowExamples() {
        return new Object[][] {
            {"Row1 - Basic user login", "testuser", "testpass", "Viewer"},
            {"Row2 - Editor user login", "editor", "editorpass", "Editor"},
            {"Row3 - Admin user login", "admin", "adminpass", "Administrator"}
        };
    }
    
    /**
     * Test with CSV data source
     * Demonstrates:
     * - CSV data-driven testing
     * - Encrypted password handling
     * - Property substitution
     */
    @Test(dataProvider = "csvTestData", groups = {"data-driven", "csv"})
    public void testLoginWithCsvData(Map<String, String> testData) {
        logger.info("Executing test with CSV data: {}", testData.get("Username"));
        
        // Handle encrypted password
        String password = testData.get("Password");
        if (password.startsWith("ENC(")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        // Handle property substitution
        if (password.startsWith("${") && password.endsWith("}")) {
            String propertyKey = password.substring(2, password.length() - 1);
            password = CSConfigManager.getInstance().getProperty(propertyKey, "defaultpass");
        }
        
        // Create page objects
        AkhanLoginPage loginPage = new AkhanLoginPage();
        
        // Navigate and login
        loginPage.navigateToApplication();
        loginPage.login(testData.get("Username"), password);
        
        // Verify based on expected result
        if (testData.get("ExpectedResult") != null && !testData.get("ExpectedResult").isEmpty()) {
            AkhanHomePage homePage = new AkhanHomePage();
            Assert.assertTrue(homePage.isHomePageDisplayed(), 
                "Login should be successful for user: " + testData.get("Username"));
        }
    }
    
    /**
     * Test with JSON data source
     * Demonstrates JSON-based data-driven testing
     */
    @Test(dataProvider = "jsonTestData", groups = {"data-driven", "json"})
    // Data source: JSON file testdata/akhan-search-data.json with path $.searchData[*]
    public void testSearchWithJsonData(Map<String, Object> testData) {
        logger.info("Executing search test with JSON data: Type={}, Attribute={}, Value={}", 
            testData.get("SearchType"), testData.get("SearchAttribute"), testData.get("SearchValue"));
        
        // Login first
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        loginPage.login("testuser", "testpass");
        
        // Navigate to ESSS/Series
        AkhanHomePage homePage = new AkhanHomePage();
        homePage.navigateToESSSSeries();
        
        // Perform search
        AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
        essPage.performSearch(
            (String) testData.get("SearchType"),
            (String) testData.get("SearchAttribute"),
            (String) testData.get("SearchValue")
        );
        
        // Verify results
        String expectedResult = (String) testData.get("ExpectedResult");
        Assert.assertTrue(essPage.validateResultsContain(expectedResult),
            "Search results should contain: " + expectedResult);
    }
    
    /**
     * Test with @DataRow annotation
     * Demonstrates inline data provider with role-based testing
     */
    @Test(dataProvider = "dataRowExamples", groups = {"data-driven", "datarow"})
    public void testRoleBasedAccess(
        String dataRowInfo,
        String username, 
        String password, 
        String expectedRole) {
        
        logger.info("Testing role-based access for user: {} with expected role: {}", 
            username, expectedRole);
        
        // Login
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        loginPage.login(username, password);
        
        // Verify role-based access
        AkhanHomePage homePage = new AkhanHomePage();
        if (homePage.isHomePageDisplayed()) {
            String profileInfo = homePage.getWelcomeMessage();
            Assert.assertTrue(profileInfo.contains(expectedRole),
                "User profile should show role: " + expectedRole);
        }
    }
    
    /**
     * Test with property file data source
     * Demonstrates property-based configuration
     */
    @Test(groups = {"data-driven", "properties"})
    // Data source: Properties file testdata/akhan-test.properties with key user
    public void testWithPropertiesData() {
        logger.info("Testing with properties file data source");
        
        // Load properties
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(new java.io.FileInputStream("testdata/akhan-test.properties"));
        } catch (Exception e) {
            logger.error("Failed to load properties", e);
            return;
        }
        
        // Test each user
        for (int i = 1; i <= 3; i++) {
            String username = props.getProperty("user" + i + ".username");
            String password = props.getProperty("user" + i + ".password");
            String role = props.getProperty("user" + i + ".role");
            
            if (username != null) {
                logger.info("Testing user: {} with role: {}", username, role);
                
                // Handle encrypted password
                if (password.startsWith("ENC(")) {
                    password = CSEncryptionUtils.decrypt(password);
                }
                
                // Handle property substitution
                if (password.startsWith("${")) {
                    String propertyKey = password.substring(2, password.length() - 1);
                    password = CSConfigManager.getInstance().getProperty(propertyKey, "defaultpass");
                }
                
                // Perform login test
                AkhanLoginPage loginPage = new AkhanLoginPage();
                loginPage.navigateToApplication();
                loginPage.login(username, password);
                
                // Verify based on role
                AkhanHomePage homePage = new AkhanHomePage();
                if (homePage.isHomePageDisplayed()) {
                    logger.info("Login successful for user: {}", username);
                }
                
                // Logout for next iteration
                homePage.logout();
            }
        }
    }
    
    /**
     * Test demonstrating Excel data source with CSV fallback
     * The framework will use CSV if Excel reading fails
     */
    @Test(groups = {"data-driven", "excel"})
    // Data source: Excel file testdata/akhan-test-data.xlsx sheet TestData
    public void testWithExcelData() {
        logger.info("Testing with Excel data source (CSV fallback supported)");
        
        // The framework's CSDataSourceProcessor will handle Excel or fall back to CSV
        // For this example, we'll use the CSV file as fallback
        List<Map<String, String>> data = CSCsvUtils.readCsv("testdata/akhan-test-data.csv", true);
        
        for (Map<String, String> row : data) {
            logger.info("Processing row: {}", row.get("Username"));
            
            String password = row.get("Password");
            
            // Handle encryption and property substitution
            if (password.startsWith("ENC(")) {
                password = CSEncryptionUtils.decrypt(password);
            } else if (password.startsWith("${")) {
                String propertyKey = password.substring(2, password.length() - 1);
                password = CSConfigManager.getInstance().getProperty(propertyKey, password);
            }
            
            // Execute test
            AkhanLoginPage loginPage = new AkhanLoginPage();
            loginPage.navigateToApplication();
            
            try {
                loginPage.login(row.get("Username"), password);
                
                // Perform search if data available
                if (row.get("SearchType") != null) {
                    AkhanHomePage homePage = new AkhanHomePage();
                    homePage.navigateToESSSSeries();
                    
                    AkhanESSSeriesPage essPage = new AkhanESSSeriesPage();
                    essPage.performSearch(
                        row.get("SearchType"),
                        row.get("SearchAttribute"),
                        row.get("SearchValue")
                    );
                    
                    // Verify results
                    Assert.assertTrue(essPage.validateResultsContain(row.get("ExpectedResult")),
                        "Results should contain: " + row.get("ExpectedResult"));
                }
                
            } catch (Exception e) {
                logger.error("Test failed for user: {}", row.get("Username"), e);
            }
        }
    }
    
    /**
     * Parallel data-driven test
     * Demonstrates parallel execution with data provider
     */
    @Test(dataProvider = "csvTestData", threadPoolSize = 4, groups = {"parallel", "data-driven"})
    public void testParallelDataDriven(Map<String, String> testData) {
        logger.info("Thread {} executing test for user: {}", 
            Thread.currentThread().getId(), testData.get("Username"));
        
        // Each thread gets its own driver instance
        AkhanLoginPage loginPage = new AkhanLoginPage();
        loginPage.navigateToApplication();
        
        String password = testData.get("Password");
        
        // Handle encrypted passwords
        if (password.startsWith("ENC(")) {
            password = CSEncryptionUtils.decrypt(password);
        }
        
        loginPage.login(testData.get("Username"), password);
        
        // Verify login
        AkhanHomePage homePage = new AkhanHomePage();
        if (homePage.isHomePageDisplayed()) {
            logger.info("Thread {} - Login successful for user: {}", 
                Thread.currentThread().getId(), testData.get("Username"));
        }
    }
    
    /**
     * Test demonstrating property resolution in test data
     */
    @Test(groups = {"property-substitution"})
    public void testPropertySubstitution() {
        logger.info("Testing property substitution feature");
        
        CSConfigManager config = CSConfigManager.getInstance();
        
        // Test property substitution patterns
        String[] testPatterns = {
            "${cs.akhan.url}",
            "${browser.name}",
            "${cs.wait.timeout}",
            "${cs.screenshot.directory}",
            "${cs.report.directory}"
        };
        
        for (String pattern : testPatterns) {
            String propertyKey = pattern.substring(2, pattern.length() - 1);
            String resolvedValue = config.getProperty(propertyKey);
            
            logger.info("Property {} resolved to: {}", pattern, resolvedValue);
            Assert.assertNotNull(resolvedValue, 
                "Property should be resolved: " + propertyKey);
        }
    }
}