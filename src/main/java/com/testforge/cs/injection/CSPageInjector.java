package com.testforge.cs.injection;

import com.testforge.cs.annotations.CSPageInjection;
import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.page.CSPageManager;
import com.testforge.cs.exceptions.CSFrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatic page object injector for step definition fields.
 * 
 * This class provides automatic injection of page objects into step definition fields,
 * eliminating the need for manual ThreadLocal management or CSPageManager calls.
 * 
 * Usage in step definitions:
 * private LoginPage loginPage; // Auto-injected
 * private DashboardPage dashboardPage; // Auto-injected
 * 
 * Thread Safety:
 * - Each thread gets its own page object instances
 * - Injection happens per step execution for perfect isolation
 * - Built on top of CSPageManager's thread-safe foundation
 */
public class CSPageInjector {
    private static final Logger logger = LoggerFactory.getLogger(CSPageInjector.class);
    
    // Cache for reflection operations to improve performance
    private static final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();
    private static final Map<Field, Boolean> injectableFieldCache = new ConcurrentHashMap<>();
    
    /**
     * Inject page objects into the given step instance.
     * This method is thread-safe and will provide each thread with its own page instances.
     * 
     * @param stepInstance The step definition instance to inject pages into
     */
    public static void injectPageObjects(Object stepInstance) {
        if (stepInstance == null) {
            return;
        }
        
        Class<?> stepClass = stepInstance.getClass();
        String threadName = Thread.currentThread().getName();
        
        try {
            // Get fields from cache or compute and cache them
            Field[] fields = fieldCache.computeIfAbsent(stepClass, clazz -> 
                getAllFields(clazz));
            
            int injectedCount = 0;
            
            // Process each field for potential injection
            for (Field field : fields) {
                if (isInjectablePageField(field)) {
                    injectPageField(stepInstance, field, threadName);
                    injectedCount++;
                }
            }
            
            if (injectedCount > 0) {
                logger.debug("[{}] Injected {} page objects into {}", 
                    threadName, injectedCount, stepClass.getSimpleName());
            }
            
        } catch (Exception e) {
            logger.error("[{}] Failed to inject page objects into {}: {}", 
                threadName, stepClass.getSimpleName(), e.getMessage());
            throw new CSFrameworkException(
                "Page object injection failed for class: " + stepClass.getName(), e);
        }
    }
    
    /**
     * Get all fields including inherited ones from the class hierarchy
     */
    private static Field[] getAllFields(Class<?> clazz) {
        Map<String, Field> allFields = new ConcurrentHashMap<>();
        
        // Walk up the class hierarchy to collect all fields
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Only add if not already present (child class fields take precedence)
                allFields.putIfAbsent(field.getName(), field);
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return allFields.values().toArray(new Field[0]);
    }
    
    /**
     * Check if a field should be injected with a page object.
     * 
     * Criteria for injection:
     * 1. Field type extends CSBasePage (is a page object)
     * 2. Field is not static or final
     * 3. Field is either annotated with @CSPageInjection or auto-injection is enabled
     */
    private static boolean isInjectablePageField(Field field) {
        // Use cache to avoid repeated reflection operations
        return injectableFieldCache.computeIfAbsent(field, f -> {
            Class<?> fieldType = f.getType();
            
            // Must be a page object (extends CSBasePage)
            if (!CSBasePage.class.isAssignableFrom(fieldType)) {
                return false;
            }
            
            // Skip static and final fields
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                return false;
            }
            
            // Check for explicit annotation or allow auto-injection for all CSBasePage fields
            // For now, we auto-inject all CSBasePage fields for maximum convenience
            // Later, can be made configurable via @CSPageInjection annotation
            return true;
        });
    }
    
    /**
     * Inject a page object instance into a specific field
     */
    private static void injectPageField(Object stepInstance, Field field, String threadName) 
            throws IllegalAccessException {
        
        // Make field accessible if it's private
        field.setAccessible(true);
        
        // Check if field is already injected (avoid re-injection)
        Object currentValue = field.get(stepInstance);
        if (currentValue != null) {
            // Field already has a value - skip injection
            logger.trace("[{}] Field {} already has value, skipping injection", 
                threadName, field.getName());
            return;
        }
        
        // Get thread-safe page instance from CSPageManager
        @SuppressWarnings("unchecked")
        Class<? extends CSBasePage> pageClass = (Class<? extends CSBasePage>) field.getType();
        
        CSBasePage pageInstance = CSPageManager.getPage(pageClass);
        
        // Inject the page instance
        field.set(stepInstance, pageInstance);
        
        logger.trace("[{}] Injected {} into field {}", 
            threadName, pageClass.getSimpleName(), field.getName());
    }
    
    /**
     * Clear injection caches (useful for testing or framework shutdown)
     */
    public static void clearCaches() {
        fieldCache.clear();
        injectableFieldCache.clear();
        logger.debug("Cleared page injection caches");
    }
    
    /**
     * Get statistics about cached reflection data
     */
    public static Map<String, Integer> getCacheStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        stats.put("fieldCacheSize", fieldCache.size());
        stats.put("injectableFieldCacheSize", injectableFieldCache.size());
        return stats;
    }
}