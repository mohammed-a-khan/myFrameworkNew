package com.testforge.cs.elements;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.driver.CSWebDriverManager;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A custom List implementation for CSElements that populates on-demand
 * and handles wait conditions for list elements
 */
public class CSElementList implements List<CSElement> {
    private static final Logger logger = LoggerFactory.getLogger(CSElementList.class);
    
    private final CSLocator annotation;
    private final String fieldName;
    private final CSSelfHealingLocator selfHealingLocator;
    private List<CSElement> elements;
    private boolean initialized = false;
    
    public CSElementList(CSLocator annotation, String fieldName) {
        this.annotation = annotation;
        this.fieldName = fieldName;
        this.selfHealingLocator = CSSelfHealingLocator.getInstance();
        this.elements = new ArrayList<>();
    }
    
    /**
     * Initialize elements if not already done
     */
    private void ensureInitialized() {
        if (!initialized) {
            refresh();
        }
    }
    
    /**
     * Refresh the element list from the page
     */
    public void refresh() {
        WebDriver driver = CSWebDriverManager.getDriver();
        if (driver == null) {
            logger.error("WebDriver not available for CSElementList refresh");
            elements = new ArrayList<>();
            return;
        }
        
        try {
            // Handle wait condition if specified
            if (annotation.waitCondition() != CSLocator.WaitCondition.NONE) {
                handleWaitCondition(driver);
            }
            
            // Get elements from self-healing locator
            elements = selfHealingLocator.createElementList(driver, annotation, fieldName);
            initialized = true;
            
            logger.debug("CSElementList refreshed: {} elements found for field {}", elements.size(), fieldName);
            
        } catch (Exception e) {
            logger.error("Failed to refresh CSElementList for field: " + fieldName, e);
            elements = new ArrayList<>();
        }
    }
    
    /**
     * Handle wait conditions before populating list
     */
    private void handleWaitCondition(WebDriver driver) {
        // For lists, we primarily care about PRESENT condition
        // Other conditions like VISIBLE, CLICKABLE apply to individual elements
        switch (annotation.waitCondition()) {
            case PRESENT:
                // Wait for at least one element to be present
                waitForPresent(driver);
                break;
            case VISIBLE:
                // Wait for elements to be present first, visibility will be checked per element
                waitForPresent(driver);
                break;
            case CLICKABLE:
                // Wait for elements to be present first, clickability will be checked per element
                waitForPresent(driver);
                break;
            default:
                // No wait condition
                break;
        }
    }
    
    /**
     * Wait for at least one element matching the locator to be present
     */
    private void waitForPresent(WebDriver driver) {
        int timeout = annotation.waitTime() > 0 ? annotation.waitTime() : 10; // Default 10 seconds
        long endTime = System.currentTimeMillis() + (timeout * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            try {
                List<CSElement> tempElements = selfHealingLocator.createElementList(driver, annotation, fieldName);
                if (!tempElements.isEmpty()) {
                    logger.debug("Wait condition satisfied: {} elements found for {}", tempElements.size(), fieldName);
                    return; // At least one element found
                }
            } catch (Exception e) {
                logger.debug("Still waiting for elements: {}", e.getMessage());
            }
            
            try {
                Thread.sleep(500); // Check every 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.warn("Wait timeout exceeded for CSElementList field: {}", fieldName);
    }
    
    // List interface implementation
    
    @Override
    public int size() {
        ensureInitialized();
        return elements.size();
    }
    
    @Override
    public boolean isEmpty() {
        ensureInitialized();
        return elements.isEmpty();
    }
    
    @Override
    public boolean contains(Object o) {
        ensureInitialized();
        return elements.contains(o);
    }
    
    @Override
    public Iterator<CSElement> iterator() {
        ensureInitialized();
        return elements.iterator();
    }
    
    @Override
    public Object[] toArray() {
        ensureInitialized();
        return elements.toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        ensureInitialized();
        return elements.toArray(a);
    }
    
    @Override
    public boolean add(CSElement element) {
        ensureInitialized();
        return elements.add(element);
    }
    
    @Override
    public boolean remove(Object o) {
        ensureInitialized();
        return elements.remove(o);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        ensureInitialized();
        return elements.containsAll(c);
    }
    
    @Override
    public boolean addAll(Collection<? extends CSElement> c) {
        ensureInitialized();
        return elements.addAll(c);
    }
    
    @Override
    public boolean addAll(int index, Collection<? extends CSElement> c) {
        ensureInitialized();
        return elements.addAll(index, c);
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        ensureInitialized();
        return elements.removeAll(c);
    }
    
    @Override
    public boolean retainAll(Collection<?> c) {
        ensureInitialized();
        return elements.retainAll(c);
    }
    
    @Override
    public void clear() {
        elements.clear();
        initialized = false;
    }
    
    @Override
    public CSElement get(int index) {
        ensureInitialized();
        return elements.get(index);
    }
    
    @Override
    public CSElement set(int index, CSElement element) {
        ensureInitialized();
        return elements.set(index, element);
    }
    
    @Override
    public void add(int index, CSElement element) {
        ensureInitialized();
        elements.add(index, element);
    }
    
    @Override
    public CSElement remove(int index) {
        ensureInitialized();
        return elements.remove(index);
    }
    
    @Override
    public int indexOf(Object o) {
        ensureInitialized();
        return elements.indexOf(o);
    }
    
    @Override
    public int lastIndexOf(Object o) {
        ensureInitialized();
        return elements.lastIndexOf(o);
    }
    
    @Override
    public ListIterator<CSElement> listIterator() {
        ensureInitialized();
        return elements.listIterator();
    }
    
    @Override
    public ListIterator<CSElement> listIterator(int index) {
        ensureInitialized();
        return elements.listIterator(index);
    }
    
    @Override
    public List<CSElement> subList(int fromIndex, int toIndex) {
        ensureInitialized();
        return elements.subList(fromIndex, toIndex);
    }
    
    @Override
    public String toString() {
        ensureInitialized();
        return "CSElementList{" +
                "fieldName='" + fieldName + '\'' +
                ", size=" + elements.size() +
                ", elements=" + elements +
                '}';
    }
}