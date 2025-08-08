package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import org.openqa.selenium.WebElement;
import java.util.List;

@CSPage(name = "ESSS/Series Page")
public class ESSSeriesPage extends CSBasePage {
    
    @CSLocator(xpath = "//h1[text()='ESSSs/Series']")
    private CSElement pageHeader;
    
    @CSLocator(xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchType']")
    private CSElement typeDropdown;
    
    @CSLocator(xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchType']/span[@class='abcd-button__label']")
    private CSElement typeDropdownSelectedText;
    
    @CSLocator(xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchAttributes']")
    private CSElement attributeDropdown;
    
    @CSLocator(xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchAttributes']/span[@class='abcd-button__label']")
    private CSElement attributeDropdownSelectedText;
    
    @CSLocator(xpath = "//span[text()='Search']/parent::button[@type='submit']")
    private CSElement searchButton;
    
    @CSLocator(xpath = "//table")
    private CSElement resultsTable;
    
    public boolean isPageHeaderDisplayed() {
        return pageHeader.isDisplayed();
    }
    
    public void clickTypeDropdown() {
        typeDropdown.click();
    }
    
    public String getSelectedTypeText() {
        return typeDropdownSelectedText.getText();
    }
    
    public void selectTypeOption(String option) {
        String xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchType']/parent::div//div[@class='abcd-balloon__content']/span[text()='" + option + "']/ancestor::li[position()=1]";
        CSElement optionElement = findElement("xpath:" + xpath, option + " type option");
        optionElement.click();
    }
    
    public boolean isTypeOptionDisplayed(String option) {
        String xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchType']/parent::div//div[@class='abcd-balloon__content']/span[text()='" + option + "']/ancestor::li[position()=1]";
        CSElement optionElement = findElement("xpath:" + xpath, option + " type option");
        return optionElement.isDisplayed();
    }
    
    public void clickAttributeDropdown() {
        attributeDropdown.click();
    }
    
    public String getSelectedAttributeText() {
        return attributeDropdownSelectedText.getText();
    }
    
    public void selectAttributeOption(String option) {
        String xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchAttributes']/parent::div//div[@class='abcd-balloon__content']/span[text()='" + option + "']/ancestor::li[position()=1]";
        CSElement optionElement = findElement("xpath:" + xpath, option + " attribute option");
        optionElement.click();
    }
    
    public boolean isAttributeOptionDisplayed(String option) {
        String xpath = "//div[@class='abcd-select__wrapper']//button[@name='searchAttributes']/parent::div//div[@class='abcd-balloon__content']/span[text()='" + option + "']/ancestor::li[position()=1]";
        CSElement optionElement = findElement("xpath:" + xpath, option + " attribute option");
        return optionElement.isDisplayed();
    }
    
    public void enterSearchValue(String labelName, String value) {
        String xpath = "//div[text()='Search By']/parent::form//label[text()='" + labelName + "']/following::div[position()=1]//input";
        CSElement searchInput = findElement("xpath:" + xpath, labelName + " search input");
        searchInput.clearAndType(value);
    }
    
    public void clickSearch() {
        searchButton.click();
    }
    
    public int getTableRowCount() {
        List<WebElement> rows = findElements(org.openqa.selenium.By.xpath("//table//tbody/tr"));
        return rows.size();
    }
    
    public String getCellText(int row, int column) {
        String xpath = "//table//tbody/tr[position()=" + row + "]//td[position()=" + column + "]";
        CSElement cell = findElement("xpath:" + xpath, "cell at row " + row + " column " + column);
        return cell.getText();
    }
    
    public String getSpanTextInCell(int row, int column) {
        String xpath = "//table//tbody/tr[position()=" + row + "]//td[position()=" + column + "]//span";
        CSElement cellSpan = findElement("xpath:" + xpath, "span in cell at row " + row + " column " + column);
        return cellSpan.getText();
    }
}