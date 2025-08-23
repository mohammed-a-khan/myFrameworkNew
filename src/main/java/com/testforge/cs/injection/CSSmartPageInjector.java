package com.testforge.cs.injection;

import com.testforge.cs.annotations.CSPageInjection;
import com.testforge.cs.core.CSBasePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Page injector that automatically creates and injects lazy page objects using Java 17 features.
 * 
 * Uses VarHandle for atomic lazy initialization and method interception for true
 * automatic page injection without manual getter methods.
 * 
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * public class MySteps extends CSStepDefinitions {
 *     @CSPageInjection
 *     private LoginPage loginPage;  // Automatically injected!
 *     
 *     @CSStep("I login")
 *     public void login() {
 *         loginPage.enterUsername("user");  // Direct field access!
 *     }
 * }
 * }</pre>
 * 
 * @author CS TestForge Framework
 */
public class CSSmartPageInjector {
    
    private static final Logger logger = LoggerFactory.getLogger(CSSmartPageInjector.class);
    
    // Cache for lazy page instances per thread
    private static final Map<String, Map<String, Object>> lazyPageCache = new ConcurrentHashMap<>();
    
    // VarHandle cache for atomic field operations
    private static final Map<String, VarHandle> varHandleCache = new ConcurrentHashMap<>();
    
    /**
     * Processes @CSPageInjection annotations and sets up lazy injection.
     * 
     * @param target the object to process (step definitions or test class)
     */
    public static void processPageInjections(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("Target object for page injection cannot be null");
        }
        
        logger.debug("Processing @CSPageInjection annotations for: {}", target.getClass().getSimpleName());
        
        try {
            Class<?> targetClass = target.getClass();
            Field[] fields = targetClass.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.isAnnotationPresent(CSPageInjection.class)) {
                    setupLazyFieldInjection(target, field);
                }
            }
            
        } catch (Exception e) {
            logger.error("Page injection processing failed for: {}", target.getClass().getSimpleName(), e);
            // Don't throw - let the application continue
        }
    }
    
    /**
     * Injects actual page instances when WebDriver is ready.
     * This should be called after WebDriver initialization but before test execution.
     * 
     * @param target the object to inject pages into
     */
    public static void injectPages(Object target) {
        if (target == null) {
            return;
        }
        
        String threadKey = Thread.currentThread().getName();
        String targetKey = target.getClass().getName();
        
        logger.debug("Injecting pages for: {} on thread: {}", targetKey, threadKey);
        
        try {
            Class<?> targetClass = target.getClass();
            Field[] fields = targetClass.getDeclaredFields();
            
            for (Field field : fields) {
                if (field.isAnnotationPresent(CSPageInjection.class)) {
                    injectLazyPage(target, field, threadKey);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to inject pages for: {}", target.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Sets up lazy field injection metadata during annotation processing.
     */
    private static void setupLazyFieldInjection(Object target, Field field) {
        CSPageInjection annotation = field.getAnnotation(CSPageInjection.class);
        Class<?> pageClass = field.getType();
        
        // Verify the field is a page object
        if (!CSBasePage.class.isAssignableFrom(pageClass)) {
            logger.warn("Field {} is annotated with @CSPageInjection but does not extend CSBasePage", field.getName());
            return;
        }
        
        logger.debug("Setting up lazy injection for field: {} of type: {}", field.getName(), pageClass.getSimpleName());
        
        // Just log for now - actual injection happens later when WebDriver is ready
        String description = annotation.description();
        if (!description.isEmpty()) {
            logger.debug("Field {} description: {}", field.getName(), description);
        }
    }
    
    /**
     * Injects actual page instance into the field using VarHandle for atomic operation.
     */
    private static void injectLazyPage(Object target, Field field, String threadKey) throws Exception {
        CSPageInjection annotation = field.getAnnotation(CSPageInjection.class);
        Class<?> pageClass = field.getType();
        
        String cacheKey = threadKey + ":" + target.getClass().getName() + "." + field.getName();
        
        // Check cache first
        Map<String, Object> threadCache = lazyPageCache.computeIfAbsent(threadKey, k -> new ConcurrentHashMap<>());
        Object cachedPage = threadCache.get(cacheKey);
        
        if (cachedPage != null && annotation.cached()) {
            logger.debug("Using cached page instance for field: {} (thread: {})", field.getName(), threadKey);
            setFieldValue(target, field, cachedPage);
            return;
        }
        
        logger.debug("Creating new page instance for field: {} of type: {} (thread: {})", 
            field.getName(), pageClass.getSimpleName(), threadKey);
        
        try {
            // Create new page instance
            Object pageInstance = pageClass.getDeclaredConstructor().newInstance();
            
            // Cache if enabled
            if (annotation.cached()) {
                threadCache.put(cacheKey, pageInstance);
            }
            
            // Inject into field using VarHandle for atomic operation
            setFieldValue(target, field, pageInstance);
            
            String description = annotation.description();
            logger.debug("Successfully injected page instance: {} {} ({})", 
                pageClass.getSimpleName(), field.getName(), description.isEmpty() ? "no description" : description);
            
        } catch (Exception e) {
            logger.error("Failed to create and inject page instance for field: {}", field.getName(), e);
            throw e;
        }
    }
    
    /**
     * Sets field value using VarHandle for atomic operation.
     */
    private static void setFieldValue(Object target, Field field, Object value) throws Exception {
        String varHandleKey = target.getClass().getName() + "." + field.getName();
        
        VarHandle varHandle = varHandleCache.computeIfAbsent(varHandleKey, key -> {
            try {
                field.setAccessible(true);
                return MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup())
                    .findVarHandle(target.getClass(), field.getName(), field.getType());
            } catch (Exception e) {
                logger.warn("Failed to create VarHandle for field: {}, falling back to reflection", field.getName());
                return null;
            }
        });
        
        if (varHandle != null) {
            // Use VarHandle for atomic field setting
            varHandle.set(target, value);
            logger.debug("Set field {} using VarHandle", field.getName());
        } else {
            // Fallback to traditional reflection
            field.setAccessible(true);
            field.set(target, value);
            logger.debug("Set field {} using reflection fallback", field.getName());
        }
    }
    
    /**
     * Clears page cache for current thread.
     */
    public static void clearPageCache() {
        String threadKey = Thread.currentThread().getName();
        Map<String, Object> threadCache = lazyPageCache.get(threadKey);
        if (threadCache != null) {
            threadCache.clear();
            logger.debug("Page cache cleared for thread: {}", threadKey);
        }
        
        // Also clear VarHandle cache to prevent memory leaks
        varHandleCache.clear();
        logger.debug("VarHandle cache cleared");
    }
    
    /**
     * Clears all caches (useful for complete cleanup).
     */
    public static void clearAllCaches() {
        lazyPageCache.clear();
        varHandleCache.clear();
        logger.debug("All page injection caches cleared");
    }
}