package com.testforge.cs.page;

import com.testforge.cs.core.CSBasePage;
import com.testforge.cs.exceptions.CSFrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Page Object Manager
 * Automatically manages ThreadLocal instances of page objects for parallel execution.
 * 
 * Usage in step definitions:
 * LoginPage loginPage = CSPageManager.getPage(LoginPage.class);
 * 
 * This eliminates the need for manual ThreadLocal management in step definitions.
 */
public class CSPageManager {
    private static final Logger logger = LoggerFactory.getLogger(CSPageManager.class);
    
    // ThreadLocal storage for page instances - each thread gets its own map of pages
    private static final ThreadLocal<Map<Class<?>, CSBasePage>> threadLocalPages = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Cache for constructors to improve performance
    private static final Map<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
    
    /**
     * Get or create a thread-safe instance of the specified page class.
     * Each thread will get its own instance, ensuring thread safety in parallel execution.
     * 
     * @param pageClass The page class to instantiate
     * @param <T> The type of the page
     * @return Thread-local instance of the page
     */
    @SuppressWarnings("unchecked")
    public static <T extends CSBasePage> T getPage(Class<T> pageClass) {
        Map<Class<?>, CSBasePage> pages = threadLocalPages.get();
        
        // Check if this thread already has an instance of this page
        T page = (T) pages.get(pageClass);
        
        if (page == null) {
            // Create new instance for this thread
            page = createPageInstance(pageClass);
            pages.put(pageClass, page);
            
            String threadName = Thread.currentThread().getName();
            logger.debug("[{}] Created new instance of {}", threadName, pageClass.getSimpleName());
        }
        
        return page;
    }
    
    /**
     * Create a new instance of the page class
     */
    private static <T extends CSBasePage> T createPageInstance(Class<T> pageClass) {
        try {
            // Get or cache the constructor
            Constructor<?> constructor = constructorCache.computeIfAbsent(pageClass, clazz -> {
                try {
                    Constructor<?> ctor = clazz.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    return ctor;
                } catch (NoSuchMethodException e) {
                    throw new CSFrameworkException(
                        "Page class " + clazz.getName() + " must have a no-argument constructor", e);
                }
            });
            
            @SuppressWarnings("unchecked")
            T instance = (T) constructor.newInstance();
            return instance;
            
        } catch (Exception e) {
            throw new CSFrameworkException(
                "Failed to create instance of page class: " + pageClass.getName(), e);
        }
    }
    
    /**
     * Clear all page instances for the current thread.
     * Should be called at the end of test execution.
     */
    public static void clearThreadPages() {
        Map<Class<?>, CSBasePage> pages = threadLocalPages.get();
        if (pages != null) {
            String threadName = Thread.currentThread().getName();
            logger.debug("[{}] Clearing {} page instances", threadName, pages.size());
            pages.clear();
        }
        threadLocalPages.remove();
    }
    
    /**
     * Reset a specific page instance for the current thread.
     * Useful when you need a fresh instance of a page.
     * 
     * @param pageClass The page class to reset
     */
    public static void resetPage(Class<? extends CSBasePage> pageClass) {
        Map<Class<?>, CSBasePage> pages = threadLocalPages.get();
        if (pages != null) {
            pages.remove(pageClass);
            String threadName = Thread.currentThread().getName();
            logger.debug("[{}] Reset page instance: {}", threadName, pageClass.getSimpleName());
        }
    }
    
    /**
     * Check if a page instance exists for the current thread
     * 
     * @param pageClass The page class to check
     * @return true if an instance exists for this thread
     */
    public static boolean hasPage(Class<? extends CSBasePage> pageClass) {
        Map<Class<?>, CSBasePage> pages = threadLocalPages.get();
        return pages != null && pages.containsKey(pageClass);
    }
    
    /**
     * Get the number of page instances for the current thread
     * 
     * @return Number of page instances
     */
    public static int getPageCount() {
        Map<Class<?>, CSBasePage> pages = threadLocalPages.get();
        return pages != null ? pages.size() : 0;
    }
    
    /**
     * Clear all ThreadLocal data for all threads (use with caution)
     * This should only be called during framework shutdown
     */
    public static void clearAll() {
        logger.info("Clearing all ThreadLocal page instances");
        threadLocalPages.remove();
        constructorCache.clear();
    }
}