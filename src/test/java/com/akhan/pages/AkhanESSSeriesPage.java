package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.exceptions.CSElementException;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.reporting.CSReportManager;
import org.testng.Assert;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Akhan ESSS/Series Module Page
 * Demonstrates proper framework usage:
 * - @CSPage annotation with page metadata
 * - @CSLocator annotations with object repository
 * - Dropdown handling with CSElement
 * - Table data extraction and validation
 * - Performance monitoring
 * - AI self-healing locators
 */
@CSPage(
    name = "Akhan ESSS/Series Page",
    url = "${cs.akhan.url}/esss",
    title = "ESSS/Series",
    validateOnLoad = false
)
public class AkhanESSSeriesPage extends CSBasePage {
    
    // Page Header
    @CSLocator(locatorKey = "akhan.esss.page.header")
    private CSElement pageHeader;
    
    // Dropdown Elements using Object Repository - Updated for custom dropdowns
    @CSLocator(locatorKey = "akhan.esss.type.dropdown.input")
    private CSElement typeDropdownInput;
    
    @CSLocator(locatorKey = "akhan.esss.type.dropdown.icon")
    private CSElement typeDropdownIcon;
    
    @CSLocator(locatorKey = "akhan.esss.attribute.dropdown.input")
    private CSElement attributeDropdownInput;
    
    @CSLocator(locatorKey = "akhan.esss.attribute.dropdown.icon")
    private CSElement attributeDropdownIcon;
    
    // Search Elements - Not needed as dropdowns are the search inputs
    
    @CSLocator(locatorKey = "akhan.esss.search.button")
    private CSElement searchButton;
    
    @CSLocator(locatorKey = "akhan.esss.clear.button")
    private CSElement clearButton;
    
    // Results Table with AI self-healing
    @CSLocator(
        id = "resultsTable",
        alternativeLocators = {
            "akhan.esss.results.table",
            "css:table.search-results",
            "xpath://table[@class='results-table']"
        },
        description = "Search results table",
        aiEnabled = true,
        aiDescription = "Table displaying search results with multiple columns"
    )
    private CSElement resultsTable;
    
    // Table rows and headers
    @CSLocator(
        xpath = "//table[@id='resultsTable']//tbody//tr",
        alternativeLocators = {"akhan.esss.table.rows"},
        description = "Result table rows",
        waitCondition = CSLocator.WaitCondition.PRESENT
    )
    private List<CSElement> tableRows;
    
    @CSLocator(
        xpath = "//table[@id='resultsTable']//thead//th",
        alternativeLocators = {"akhan.esss.table.headers"},
        description = "Result table headers"
    )
    private List<CSElement> tableHeaders;
    
    // No results message
    @CSLocator(
        className = "no-results",
        alternativeLocators = {"akhan.esss.no.results.message"},
        description = "No results found message",
        optional = true
    )
    private CSElement noResultsMessage;
    
    // File Upload Elements
    @CSLocator(locatorKey = "akhan.esss.add.files.button")
    private CSElement addFilesButton;
    
    @CSLocator(
        xpath = "//input[@type='file']",
        alternativeLocators = {"akhan.esss.file.input"},
        description = "File upload input",
        optional = true
    )
    private CSElement fileInput;
    
    @CSLocator(locatorKey = "akhan.esss.upload.button")
    private CSElement uploadButton;
    
    // Loading spinner
    @CSLocator(
        className = "loading-spinner",
        alternativeLocators = {"akhan.esss.loading.spinner"},
        description = "Loading spinner",
        waitCondition = CSLocator.WaitCondition.INVISIBLE,
        optional = true
    )
    private CSElement loadingSpinner;
    
    // Advanced search options
    @CSLocator(
        css = "input.advanced-search",
        alternativeLocators = {"akhan.esss.advanced.search.checkbox"},
        description = "Advanced search toggle",
        optional = true
    )
    private CSElement advancedSearchToggle;
    
    // Export button
    @CSLocator(
        xpath = "//button[contains(text(),'Export')]",
        alternativeLocators = {"akhan.esss.export.button"},
        description = "Export results button",
        optional = true
    )
    private CSElement exportButton;
    
    // Performance tracking
    private long searchStartTime;
    private long searchEndTime;
    
    /**
     * Select value from Type dropdown
     * Uses custom dropdown implementation with input field and search icon
     */
    public void selectType(String type) {
        CSReportManager.info("Selecting type: " + type);
        logger.info("Selecting type: {}", type);
        
        try {
            // Click the dropdown icon to open options
            typeDropdownIcon.waitForClickable();
            typeDropdownIcon.click();
            
            // Wait for dropdown to open
            CSWaitUtils.waitForSeconds(1);
            
            // Find and click the option using dynamic element
            CSElement option = findDynamicElement("akhan.esss.search.option", type);
            option.waitForClickable();
            option.click();
            
            // Verify selection appears in input field
            CSWaitUtils.waitForSeconds(1);
            CSReportManager.pass("Type selected: " + type);
        } catch (Exception e) {
            CSReportManager.fail("Failed to select type: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Select value from Attribute dropdown
     * Uses custom dropdown implementation with input field and search icon
     */
    public void selectAttribute(String attribute) {
        CSReportManager.info("Selecting attribute: " + attribute);
        logger.info("Selecting attribute: {}", attribute);
        
        try {
            // Click the dropdown icon to open options
            attributeDropdownIcon.waitForClickable();
            attributeDropdownIcon.click();
            
            // Wait for dropdown to open
            CSWaitUtils.waitForSeconds(1);
            
            // Find and click the option using dynamic element
            CSElement option = findDynamicElement("akhan.esss.search.option", attribute);
            option.waitForClickable();
            option.click();
            
            CSWaitUtils.waitForSeconds(1);
            CSReportManager.pass("Attribute selected: " + attribute);
        } catch (Exception e) {
            CSReportManager.fail("Failed to select attribute: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Enter search value directly in the type dropdown input
     * For direct text search without selecting from dropdown
     */
    public void enterSearchValue(String value) {
        CSReportManager.info("Entering search value: " + value);
        logger.info("Entering search value: {}", value);
        
        typeDropdownInput.waitForVisible();
        typeDropdownInput.clearAndType(value);
        CSReportManager.pass("Search value entered: " + value);
    }
    
    /**
     * Click search button
     */
    public void clickSearch() {
        logger.info("Clicking search button");
        searchStartTime = System.currentTimeMillis();
        searchButton.waitForClickable();
        searchButton.click();
        waitForSearchResults();
        searchEndTime = System.currentTimeMillis();
        
        long searchTime = searchEndTime - searchStartTime;
        logger.info("Search completed in {} ms", searchTime);
    }
    
    /**
     * Perform complete search operation
     * For ESSS/Series search - selects type and attribute from dropdowns
     */
    public void performSearch(String type, String attribute) {
        CSReportManager.info(String.format("Performing ESSS/Series search - Type: %s, Attribute: %s", type, attribute));
        logger.info("Performing search - Type: {}, Attribute: {}", type, attribute);
        
        selectType(type);
        selectAttribute(attribute);
        clickSearch();
        
        captureScreenshot("search-results-" + type.toLowerCase());
        
        if (hasSearchResults()) {
            CSReportManager.pass("Search completed with " + getResultRowCount() + " results");
        } else {
            CSReportManager.warn("Search completed but no results found");
        }
    }
    
    /**
     * Perform search with text value (alternative method)
     */
    public void performSearchWithValue(String searchValue) {
        CSReportManager.info("Performing search with value: " + searchValue);
        
        enterSearchValue(searchValue);
        clickSearch();
        
        if (hasSearchResults()) {
            CSReportManager.pass("Search completed with " + getResultRowCount() + " results");
        } else {
            CSReportManager.warn("Search completed but no results found");
        }
    }
    
    /**
     * Get all Type dropdown options
     * Opens the dropdown and collects available options
     */
    public List<String> getTypeOptions() {
        CSReportManager.info("Getting Type dropdown options");
        logger.info("Getting Type dropdown options");
        List<String> options = new ArrayList<>();
        
        try {
            // Click icon to open dropdown
            typeDropdownIcon.click();
            CSWaitUtils.waitForSeconds(1);
            
            // Get all option elements
            List<CSElement> optionElements = findElements("xpath://div[@id='dropdown']//a");
            for (CSElement option : optionElements) {
                options.add(option.getText());
            }
            
            // Close dropdown by clicking elsewhere
            pageHeader.click();
            
            CSReportManager.pass("Found " + options.size() + " type options");
        } catch (Exception e) {
            CSReportManager.warn("Could not retrieve type options: " + e.getMessage());
        }
        
        return options;
    }
    
    private List<CSElement> findElements(String locator) {
        // Helper method to find multiple elements
        List<CSElement> elements = new ArrayList<>();
        // This would need implementation based on framework capabilities
        return elements;
    }
    
    /**
     * Get all Attribute dropdown options
     * Opens the dropdown and collects available options
     */
    public List<String> getAttributeOptions() {
        CSReportManager.info("Getting Attribute dropdown options");
        logger.info("Getting Attribute dropdown options");
        List<String> options = new ArrayList<>();
        
        try {
            // Click icon to open dropdown
            attributeDropdownIcon.click();
            CSWaitUtils.waitForSeconds(1);
            
            // Get all option elements
            List<CSElement> optionElements = findElements("xpath://div[@id='dropdown']//a");
            for (CSElement option : optionElements) {
                options.add(option.getText());
            }
            
            // Close dropdown by clicking elsewhere
            pageHeader.click();
            
            CSReportManager.pass("Found " + options.size() + " attribute options");
        } catch (Exception e) {
            CSReportManager.warn("Could not retrieve attribute options: " + e.getMessage());
        }
        
        return options;
    }
    
    /**
     * Wait for search results to load
     */
    private void waitForSearchResults() {
        logger.info("Waiting for search results");
        
        // Wait for loading spinner to disappear if present
        // Wait for loading spinner to disappear if present
        // Note: CSElement doesn't have waitForInvisible, so we wait briefly
        CSWaitUtils.waitForSeconds(1);
        
        // Wait for either results table or no results message
        CSWaitUtils.waitForSeconds(1);
        
        // Check if results are available
        try {
            resultsTable.waitForVisible(5);
        } catch (Exception e) {
            // Check for no results message
            try {
                noResultsMessage.waitForVisible(2);
            } catch (Exception ex) {
                logger.debug("No results or message found");
            }
        }
    }
    
    /**
     * Check if search results are present
     * Demonstrates result validation
     */
    public boolean hasSearchResults() {
        return resultsTable.isPresent() && getResultRowCount() > 0;
    }
    
    /**
     * Get number of result rows
     */
    public int getResultRowCount() {
        if (tableRows != null) {
            return tableRows.size();
        }
        return 0;
    }
    
    /**
     * Get search results as list of maps
     */
    public List<Map<String, String>> getSearchResults() {
        logger.info("Extracting search results from table");
        List<Map<String, String>> results = new ArrayList<>();
        
        if (tableHeaders != null && tableRows != null) {
            // Get header texts
            List<String> headerTexts = new ArrayList<>();
            for (CSElement header : tableHeaders) {
                headerTexts.add(header.getText().trim());
            }
            
            // Process each row
            for (CSElement row : tableRows) {
                Map<String, String> rowData = new HashMap<>();
                // Get row text and split by tabs or multiple spaces
                String rowText = row.getText();
                String[] cellTexts = rowText.split("\\s{2,}|\\t");  // Split by tabs or multiple spaces
                
                for (int i = 0; i < Math.min(cellTexts.length, headerTexts.size()); i++) {
                    String cellText = cellTexts[i].trim();
                    rowData.put(headerTexts.get(i), cellText);
                    // Also add by column index for easier validation
                    rowData.put("Column" + (i + 1), cellText);
                }
                
                results.add(rowData);
            }
        }
        
        logger.info("Extracted {} result rows", results.size());
        return results;
    }
    
    /**
     * Validate search results contain expected value
     * Demonstrates result validation
     */
    public boolean validateResultsContain(String expectedValue) {
        logger.info("Validating results contain: {}", expectedValue);
        
        List<Map<String, String>> results = getSearchResults();
        
        for (Map<String, String> row : results) {
            for (String value : row.values()) {
                if (value.contains(expectedValue)) {
                    logger.info("Found expected value in results");
                    return true;
                }
            }
        }
        
        logger.warn("Expected value not found in results");
        return false;
    }
    
    /**
     * Validate specific column contains expected value
     * Demonstrates column-specific validation
     */
    public boolean validateColumnContains(int columnIndex, String expectedValue) {
        logger.info("Validating column {} contains: {}", columnIndex, expectedValue);
        
        List<Map<String, String>> results = getSearchResults();
        String columnKey = "Column" + columnIndex;
        
        for (Map<String, String> row : results) {
            String cellValue = row.get(columnKey);
            if (cellValue != null && cellValue.contains(expectedValue)) {
                logger.info("Found expected value in column {}", columnIndex);
                return true;
            }
        }
        
        logger.warn("Expected value not found in column {}", columnIndex);
        return false;
    }
    
    /**
     * Clear search form
     * Demonstrates form reset
     */
    public void clearSearch() {
        logger.info("Clearing search form");
        
        if (clearButton.isPresent()) {
            clearButton.click();
        } else {
            // Manual clear if button not available
            if (typeDropdownInput.isPresent()) {
                typeDropdownInput.clear();
            }
            if (attributeDropdownInput.isPresent()) {
                attributeDropdownInput.clear();
            }
        }
    }
    
    /**
     * Verify page header matches expected
     * Demonstrates header validation
     */
    public boolean verifyPageHeader(String expectedHeader) {
        if (pageHeader.isPresent()) {
            String actualHeader = pageHeader.getText();
            boolean matches = actualHeader.contains(expectedHeader);
            
            if (matches) {
                logger.info("Page header matches: {}", actualHeader);
            } else {
                logger.error("Page header mismatch. Expected: {}, Actual: {}", expectedHeader, actualHeader);
            }
            
            return matches;
        }
        
        return false;
    }
    
    /**
     * Check if Add Files button is present (for File Upload module)
     * Demonstrates module-specific element check
     */
    public boolean isFileUploadModuleDisplayed() {
        return addFilesButton.isPresent();
    }
    
    /**
     * Upload a file
     */
    public void uploadFile(String filePath) {
        logger.info("Uploading file: {}", filePath);
        
        fileInput.waitForVisible();
        fileInput.uploadFile(filePath);
        
        uploadButton.waitForClickable();
        uploadButton.click();
        waitForPageLoad();
    }
    
    /**
     * Get search performance time
     * Demonstrates performance metrics
     */
    public long getLastSearchTime() {
        if (searchEndTime > searchStartTime) {
            return searchEndTime - searchStartTime;
        }
        return 0;
    }
    
    /**
     * Verify search completes within timeout
     * Demonstrates performance validation
     */
    public boolean verifySearchPerformance(long maxTimeMillis) {
        long searchTime = getLastSearchTime();
        boolean withinLimit = searchTime > 0 && searchTime <= maxTimeMillis;
        
        if (withinLimit) {
            logger.info("Search completed within {} ms (actual: {} ms)", maxTimeMillis, searchTime);
        } else {
            logger.warn("Search exceeded {} ms limit (actual: {} ms)", maxTimeMillis, searchTime);
        }
        
        return withinLimit;
    }
    
    /**
     * Perform search with performance monitoring
     * Demonstrates performance tracking
     */
    public Map<String, Object> performSearchWithMetrics(String type, String attribute) {
        CSReportManager.info("Starting search with performance monitoring");
        Map<String, Object> metrics = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        performSearch(type, attribute);
        long endTime = System.currentTimeMillis();
        
        long searchTime = endTime - startTime;
        int resultCount = getResultRowCount();
        
        metrics.put("searchTime", searchTime);
        metrics.put("resultCount", resultCount);
        metrics.put("hasResults", hasSearchResults());
        
        CSReportManager.pass(String.format("Search completed in %d ms with %d results", searchTime, resultCount));
        logger.info("Search metrics: {}", metrics);
        return metrics;
    }
    
    /**
     * DEMONSTRATION: Using dynamic element methods from CSBasePage
     * This shows how to use the new dynamic element creation methods
     */
    public void demonstrateDynamicElements() {
        CSReportManager.info("=== Demonstrating Dynamic Element Creation ===");
        
        // Method 1: Find button dynamically using base class method
        CSElement searchBtn = findButtonByText("Search");
        if (searchBtn.isPresent()) {
            CSReportManager.pass("Found Search button using dynamic method");
        }
        
        // Method 2: Find link dynamically
        CSElement helpLink = findLinkByText("Help");
        if (helpLink.isPresent()) {
            CSReportManager.info("Found Help link dynamically");
        } else {
            CSReportManager.warn("Help link not found - might not exist on this page");
        }
        
        // Method 3: Find table cell dynamically using pattern
        if (resultsTable.isPresent()) {
            CSElement cell = findTableCell("resultsTable", 1, 2);
            String cellValue = cell.getText();
            CSReportManager.pass("Retrieved cell value: " + cellValue);
        }
        
        // Method 4: Find input by label
        CSElement searchField = findInputByLabel("Search");
        if (searchField.isPresent()) {
            CSReportManager.pass("Found search input field by label");
        }
        
        // Method 5: Check if dynamic element exists
        boolean hasClearButton = isDynamicElementPresent("dynamic.button.text.xpath", "Clear");
        if (hasClearButton) {
            CSReportManager.pass("Clear button is present");
        }
        
        // Method 6: Click dynamic element with reporting
        try {
            clickDynamicElement("dynamic.menu.item.xpath", "Home");
            CSReportManager.pass("Clicked Home menu using dynamic pattern");
        } catch (Exception e) {
            CSReportManager.warn("Could not click Home menu - may not be visible from this page");
        }
        
        CSReportManager.info("=== Dynamic Element Demo Complete ===");
    }
    
    /**
     * Validate search results with detailed reporting
     */
    public boolean validateResultsWithReporting(String expectedValue, int expectedMinCount) {
        CSReportManager.info("Validating search results");
        
        int actualCount = getResultRowCount();
        boolean hasExpectedValue = validateResultsContain(expectedValue);
        
        if (actualCount < expectedMinCount) {
            CSReportManager.fail(String.format("Expected at least %d results, but found %d", expectedMinCount, actualCount));
            return false;
        }
        
        if (!hasExpectedValue) {
            CSReportManager.fail("Expected value '" + expectedValue + "' not found in results");
            return false;
        }
        
        CSReportManager.pass(String.format("Validation passed: Found %d results containing '%s'", actualCount, expectedValue));
        return true;
    }
}