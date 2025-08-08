package com.akhan.pages;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;

@CSPage(name = "Akhan Home Page")
public class HomePage extends CSBasePage {
    
    @CSLocator(xpath = "//h1[text()='Home']")
    private CSElement homeHeader;
    
    @CSLocator(xpath = "//p[text()='Welcome, ']/strong")
    private CSElement welcomeUserName;
    
    public boolean isHomeHeaderDisplayed() {
        return homeHeader.isDisplayed();
    }
    
    public String getWelcomeUserName() {
        return welcomeUserName.getText();
    }
    
    public void clickMenuItem(String menuItemName) {
        String xpath;
        if ("System Admin".equals(menuItemName)) {
            xpath = "//span[text()='System Admin']";
        } else {
            xpath = "//div[@id='abcdNavigatorBody']//a[text()='" + menuItemName + "']";
        }
        CSElement menuItem = findElement("xpath:" + xpath, menuItemName + " menu item");
        menuItem.click();
    }
    
    public boolean isMenuItemDisplayed(String menuItemName) {
        String xpath;
        if ("System Admin".equals(menuItemName)) {
            xpath = "//span[text()='System Admin']";
        } else {
            xpath = "//div[@id='abcdNavigatorBody']//a[text()='" + menuItemName + "']";
        }
        CSElement menuItem = findElement("xpath:" + xpath, menuItemName + " menu item");
        return menuItem.isDisplayed();
    }
}