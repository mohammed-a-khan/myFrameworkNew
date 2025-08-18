package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.config.CSConfigManager;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

/**
 * Demonstrates Dynamic Element Creation Using CSElement Class
 * 
 * This page shows how we can dynamically create elements:
 * 1. Using findElement() method from CSBasePage with parameterized locators
 * 2. Creating CSElement objects directly with dynamic locators
 * 3. Using object repository patterns with parameter substitution
 * 
 * The framework supports creating elements dynamically at runtime
 * when you don't know the exact locator values at compile time.
 */
@CSPage(
    name = "Akhan Dynamic Elements Page",
    url = "${cs.akhan.url}/dynamic",
    validateOnLoad = false
)
public class AkhanDynamicElementsPage extends CSBasePage {
    private static final Logger logger = LoggerFactory.getLogger(AkhanDynamicElementsPage.class);
    
    // Static elements defined with annotations
    @CSLocator(locatorKey = "akhan.dynamic.container")
    private CSElement container;
    
    @CSLocator(css = "div.product-list")
    private CSElement productList;
    
    /**
     * METHOD 1: Demonstrate using inherited dynamic methods from CSBasePage
     * The base class now provides findMenuItemByText() method
     */
    public void demonstrateMenuItemSearch(String menuText) {
        logger.info("Demonstrating dynamic menu item search: {}", menuText);
        
        // Use inherited method from CSBasePage
        CSElement menuItem = findMenuItemByText(menuText);
        if (menuItem.isPresent()) {
            logger.info("Found menu item: {}", menuText);
        }
    }
    
    /**
     * METHOD 2: Create dynamic table cell element
     * Example: Access specific cell in a table by row and column index
     */
    public CSElement getTableCell(String tableId, int row, int column) {
        logger.info("Creating dynamic element for table cell at row {} column {}", row, column);
        
        // Build dynamic XPath for table cell
        String cellXpath = String.format("//table[@id='%s']//tr[%d]//td[%d]", tableId, row, column);
        
        // Create CSElement with dynamic locator
        return findElement(cellXpath, String.format("Table cell [%d,%d]", row, column));
    }
    
    /**
     * METHOD 3: Create dynamic button by its label
     * Shows how to create elements for buttons with dynamic text
     */
    public CSElement findButtonByLabel(String label) {
        logger.info("Creating dynamic button element with label: {}", label);
        
        // Multiple strategies for finding button
        String xpathLocator = String.format("//button[contains(text(),'%s')]", label);
        // Note: CSS :contains is jQuery specific, using XPath instead
        
        // Try XPath first (more reliable for text content)
        return findElement(xpathLocator, "Button: " + label);
    }
    
    /**
     * METHOD 4: Demonstrate using inherited findInputByLabel from CSBasePage
     * The base class now provides this method with reporting
     */
    public void demonstrateInputByLabel(String labelText) {
        logger.info("Demonstrating input search by label: {}", labelText);
        
        // Use inherited method from CSBasePage
        CSElement input = findInputByLabel(labelText);
        if (input.isPresent()) {
            logger.info("Found input for label: {}", labelText);
        }
    }
    
    /**
     * METHOD 5: Create dynamic product card element
     * Example: Find product by its name in a list
     */
    public CSElement findProductCard(String productName) {
        logger.info("Creating dynamic element for product: {}", productName);
        
        // Build dynamic locator for product card
        String productXpath = String.format("//div[@class='product-card'][.//h3[contains(text(),'%s')]]", productName);
        
        return findElement(productXpath, "Product card: " + productName);
    }
    
    /**
     * METHOD 6: Create multiple dynamic elements
     * Example: Find all elements matching a pattern
     */
    public List<CSElement> findAllItemsWithClass(String className) {
        logger.info("Creating dynamic elements for all items with class: {}", className);
        
        List<CSElement> elements = new ArrayList<>();
        
        // Find all elements with the given class
        String cssSelector = "." + className;
        List<org.openqa.selenium.WebElement> webElements = findElements(By.cssSelector(cssSelector));
        
        // Convert to CSElements
        for (int i = 0; i < webElements.size(); i++) {
            // Create CSElement wrapper for each WebElement
            CSElement element = findElement(
                String.format("css:%s:nth-of-type(%d)", cssSelector, i + 1),
                String.format("%s element #%d", className, i + 1)
            );
            elements.add(element);
        }
        
        return elements;
    }
    
    /**
     * METHOD 7: Demonstrate using inherited findDropdownOption from CSBasePage
     * The base class now provides this method
     */
    public void demonstrateDropdownOption(String dropdownId, String optionText) {
        logger.info("Demonstrating dropdown option search: {}", optionText);
        
        // Use inherited method from CSBasePage
        CSElement option = findDropdownOption(dropdownId, optionText);
        if (option.isPresent()) {
            logger.info("Found option: {}", optionText);
        }
    }
    
    /**
     * METHOD 8: Create dynamic element with multiple parameters
     * Example: Find element by multiple attributes
     */
    public CSElement findElementByAttributes(String tagName, String attributeName, String attributeValue) {
        logger.info("Creating dynamic element: {} with {}='{}'", tagName, attributeName, attributeValue);
        
        // Build dynamic XPath with attributes
        String xpath = String.format("//%s[@%s='%s']", tagName, attributeName, attributeValue);
        
        return findElement(xpath, String.format("%s[%s='%s']", tagName, attributeName, attributeValue));
    }
    
    /**
     * METHOD 9: Demonstrate using inherited findElementInContainer from CSBasePage
     * The base class now provides this method
     */
    public void demonstrateElementInContainer(String containerId, String elementSelector) {
        logger.info("Demonstrating element search within container: {}", containerId);
        
        // Use inherited method from CSBasePage
        CSElement element = findElementInContainer(containerId, elementSelector);
        if (element.isPresent()) {
            logger.info("Found element in container: {}", containerId);
        }
    }
    
    /**
     * METHOD 10: Create dynamic element using data attributes
     * Example: Find element by data-* attributes commonly used in modern web apps
     */
    public CSElement findByDataAttribute(String dataAttribute, String value) {
        logger.info("Creating dynamic element with data-{}='{}'", dataAttribute, value);
        
        // Build CSS selector for data attribute
        String cssSelector = String.format("[data-%s='%s']", dataAttribute, value);
        
        return findElement("css:" + cssSelector, String.format("Element with data-%s='%s'", dataAttribute, value));
    }
    
    /**
     * ADVANCED: Create dynamic element with index-based selection
     * Example: Get nth element from a list
     */
    public CSElement getNthElement(String baseSelector, int index) {
        logger.info("Creating dynamic element for {}[{}]", baseSelector, index);
        
        // XPath uses 1-based indexing
        String indexedXpath = String.format("(%s)[%d]", baseSelector, index);
        
        return findElement("xpath:" + indexedXpath, String.format("%s at index %d", baseSelector, index));
    }
    
    /**
     * ADVANCED: Create dynamic element with partial text matching
     * Example: Find link containing partial text
     */
    public CSElement findLinkContainingText(String partialText) {
        logger.info("Creating dynamic link element containing: {}", partialText);
        
        // Use XPath for partial text matching
        String xpath = String.format("//a[contains(text(),'%s')]", partialText);
        
        return findElement(xpath, "Link containing: " + partialText);
    }
    
    /**
     * DEMONSTRATION: Using parameterized locators from repository
     * 
     * The base class now provides findDynamicElement() method that:
     * - Takes a pattern key from object-repository.properties
     * - Replaces {0}, {1}, {2} placeholders with runtime values
     * - Creates and returns a CSElement
     * 
     * Examples from repository:
     * dynamic.menu.item.xpath=//a[contains(text(),'{0}')]
     * dynamic.table.cell.xpath=//table[@id='{0}']//tr[{1}]//td[{2}]
     */
    public void demonstrateParameterizedLocators() {
        logger.info("Demonstrating parameterized locators from repository");
        
        // Example 1: Menu item with single parameter
        CSElement menuItem = findDynamicElement("dynamic.menu.item.xpath", "Home");
        logger.info("Found menu item: {}", menuItem.isPresent());
        
        // Example 2: Table cell with multiple parameters
        CSElement tableCell = findDynamicElement("dynamic.table.cell.xpath", "dataTable", 2, 3);
        logger.info("Found table cell: {}", tableCell.isPresent());
        
        // Example 3: Button by text
        CSElement button = findDynamicElement("dynamic.button.by.text", "Submit");
        logger.info("Found button: {}", button.isPresent());
    }
    
    /**
     * Example usage of repository pattern for menu navigation
     */
    public void clickDynamicMenuItem(String menuText) {
        // Use the inherited method from CSBasePage
        clickDynamicElement("dynamic.menu.item.xpath", menuText);
    }
    
    /**
     * Example usage of repository pattern for table cell access
     */
    public String getTableCellValue(String tableId, int row, int col) {
        // Use the inherited method from CSBasePage
        return getDynamicElementText("dynamic.table.cell.xpath", tableId, row, col);
    }
    
    /**
     * Demonstrate all dynamic element creation methods
     */
    public void demonstrateDynamicElements() {
        logger.info("=== Demonstrating Dynamic Element Creation ===");
        
        // 1. Menu item by text - using inherited method from CSBasePage
        CSElement homeMenu = findMenuItemByText("Home");
        if (homeMenu.isPresent()) {
            logger.info("Found Home menu item");
        }
        
        // 2. Table cell by coordinates - using custom method
        CSElement cell = getTableCell("dataTable", 2, 3);
        if (cell.isPresent()) {
            logger.info("Cell value: {}", cell.getText());
        }
        
        // 3. Button by text - using inherited method from CSBasePage
        CSElement submitBtn = findButtonByText("Submit");
        if (submitBtn.isPresent()) {
            logger.info("Found Submit button");
        }
        
        // 4. Input by label - using inherited method from CSBasePage
        CSElement emailInput = findInputByLabel("Email");
        if (emailInput.isPresent()) {
            emailInput.clearAndType("test@example.com");
        }
        
        // 5. Product card - using custom method
        CSElement product = findProductCard("Laptop");
        if (product.isPresent()) {
            logger.info("Found product: Laptop");
        }
        
        // 6. Multiple elements - using custom method
        List<CSElement> items = findAllItemsWithClass("list-item");
        logger.info("Found {} items with class 'list-item'", items.size());
        
        // 7. Using repository patterns - using inherited methods
        clickDynamicElement("dynamic.menu.item.xpath", "Settings");
        String cellValue = getDynamicElementText("dynamic.table.cell.xpath", "resultsTable", 1, 2);
        logger.info("Cell value from pattern: {}", cellValue);
        
        logger.info("=== Dynamic Element Creation Demo Complete ===");
    }
}