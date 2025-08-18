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
    
    // Dropdown Elements using Object Repository
    @CSLocator(locatorKey = "akhan.esss.type.dropdown")
    private CSElement typeDropdown;
    
    @CSLocator(locatorKey = "akhan.esss.attribute.dropdown")
    private CSElement attributeDropdown;
    
    // Search Elements
    @CSLocator(locatorKey = "akhan.esss.search.input")
    private CSElement searchInput;
    
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
     */
    public void selectType(String type) {
        CSReportManager.info("Selecting type: " + type);
        logger.info("Selecting type: {}", type);
        
        try {
            typeDropdown.waitForVisible();
            typeDropdown.selectByVisibleText(type);
            
            // Wait for attribute dropdown to update
            CSWaitUtils.waitForSeconds(1);
            CSReportManager.pass("Type selected: " + type);
        } catch (Exception e) {
            CSReportManager.fail("Failed to select type: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Select value from Attribute dropdown
     */
    public void selectAttribute(String attribute) {
        logger.info("Selecting attribute: {}", attribute);
        
        attributeDropdown.waitForVisible();
        attributeDropdown.selectByVisibleText(attribute);
    }
    
    /**
     * Enter search value
     */
    public void enterSearchValue(String value) {
        logger.info("Entering search value: {}", value);
        searchInput.waitForVisible();
        searchInput.clearAndType(value);
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
     * Demonstrates composite search action
     */
    public void performSearch(String type, String attribute, String searchValue) {
        CSReportManager.info(String.format("Performing search - Type: %s, Attribute: %s, Value: %s", type, attribute, searchValue));
        logger.info("Performing search - Type: {}, Attribute: {}, Value: {}", type, attribute, searchValue);
        
        selectType(type);
        selectAttribute(attribute);
        enterSearchValue(searchValue);
        clickSearch();
        
        captureScreenshot("search-results-" + type.toLowerCase());
        
        if (hasSearchResults()) {
            CSReportManager.pass("Search completed with " + getResultRowCount() + " results");
        } else {
            CSReportManager.warn("Search completed but no results found");
        }
    }
    
    /**
     * Get all Type dropdown options
     */
    public List<String> getTypeOptions() {
        logger.info("Getting Type dropdown options");
        typeDropdown.waitForVisible();
        List<String> options = typeDropdown.getDropdownOptions();
        logger.info("Found {} type options", options.size());
        return options;
    }
    
    /**
     * Get all Attribute dropdown options
     */
    public List<String> getAttributeOptions() {
        logger.info("Getting Attribute dropdown options");
        attributeDropdown.waitForVisible();
        List<String> options = attributeDropdown.getDropdownOptions();
        logger.info("Found {} attribute options", options.size());
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
            searchInput.clear();
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
    public Map<String, Object> performSearchWithMetrics(String type, String attribute, String value) {
        CSReportManager.info("Starting search with performance monitoring");
        Map<String, Object> metrics = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        performSearch(type, attribute, value);
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