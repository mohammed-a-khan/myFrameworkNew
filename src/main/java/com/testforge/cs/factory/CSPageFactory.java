package com.testforge.cs.factory;

import com.testforge.cs.annotations.CSLocator;
import com.testforge.cs.annotations.CSPage;
import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.elements.CSElement;
import com.testforge.cs.elements.CSElementList;
import com.testforge.cs.elements.CSSelfHealingLocator;
import com.testforge.cs.exceptions.CSPageInitializationException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Page factory for initializing page objects with self-healing locators
 */
public class CSPageFactory {
    private static final Logger logger = LoggerFactory.getLogger(CSPageFactory.class);
    private static final CSSelfHealingLocator selfHealingLocator = CSSelfHealingLocator.getInstance();
    
    /**
     * Initialize page elements with self-healing
     */
    public static void initElements(CSBasePage page) {
        WebDriver driver = CSWebDriverManager.getDriver();
        if (driver == null) {
            throw new CSPageInitializationException("WebDriver not initialized");
        }
        
        Class<?> pageClass = page.getClass();
        
        // Process CSPage annotation
        processPageAnnotation(page, pageClass);
        
        // Initialize all fields with @CSLocator
        initializeLocatorFields(page, pageClass, driver);
    }
    
    /**
     * Process @CSPage annotation
     */
    private static void processPageAnnotation(CSBasePage page, Class<?> pageClass) {
        CSPage pageAnnotation = pageClass.getAnnotation(CSPage.class);
        if (pageAnnotation != null) {
            // Page annotation processing is handled by CSBasePage
            logger.debug("Processing @CSPage for class: {}", pageClass.getName());
        }
    }
    
    /**
     * Initialize fields with @CSLocator
     */
    private static void initializeLocatorFields(CSBasePage page, Class<?> pageClass, WebDriver driver) {
        // Process all fields including inherited ones
        Class<?> currentClass = pageClass;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            Field[] fields = currentClass.getDeclaredFields();
            
            for (Field field : fields) {
                CSLocator locatorAnnotation = field.getAnnotation(CSLocator.class);
                if (locatorAnnotation != null) {
                    initializeField(page, field, locatorAnnotation, driver);
                }
            }
            
            currentClass = currentClass.getSuperclass();
        }
    }
    
    /**
     * Initialize a single field
     */
    private static void initializeField(CSBasePage page, Field field, CSLocator annotation, WebDriver driver) {
        try {
            field.setAccessible(true);
            
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            
            if (CSElement.class.isAssignableFrom(fieldType)) {
                // Single element
                CSElement element = selfHealingLocator.createElement(driver, annotation, fieldName);
                field.set(page, element);
                logger.debug("Initialized CSElement field: {}", fieldName);
                
            } else if (List.class.isAssignableFrom(fieldType)) {
                // List of elements - check generic type
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericType;
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    
                    if (typeArgs.length > 0 && typeArgs[0] == CSElement.class) {
                        // Initialize with CSElementList that populates on-demand
                        CSElementList elementList = new CSElementList(annotation, fieldName);
                        field.set(page, elementList);
                        logger.debug("Initialized List<CSElement> field with CSElementList: {}", fieldName);
                    }
                }
            }
            
        } catch (Exception e) {
            throw new CSPageInitializationException(
                "Failed to initialize field: " + field.getName(), e);
        }
    }
}