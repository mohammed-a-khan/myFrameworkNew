package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.exceptions.CSBddException;
import com.testforge.cs.injection.CSSmartPageInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe step registry that creates separate step instances per thread.
 * This ensures proper isolation in parallel test execution.
 */
public class CSThreadLocalStepRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CSThreadLocalStepRegistry.class);
    
    // Store step classes (not instances) globally
    private static final Map<Class<?>, List<Method>> stepClassMethods = new ConcurrentHashMap<>();
    
    // ThreadLocal storage for step instances - each thread gets its own instances
    private static final ThreadLocal<Map<Class<?>, Object>> threadLocalStepInstances = 
        ThreadLocal.withInitial(HashMap::new);
    
    // Store step definitions per thread
    private static final ThreadLocal<Map<String, CSStepDefinition>> threadLocalStepDefinitions = 
        ThreadLocal.withInitial(HashMap::new);
    
    /**
     * Register a step class (stores class info, not instances)
     */
    public static void registerStepClass(Class<?> stepClass) {
        if (stepClassMethods.containsKey(stepClass)) {
            logger.debug("Step class {} already registered", stepClass.getName());
            return;
        }
        
        logger.info("Registering step class: {}", stepClass.getName());
        
        // Store methods with @CSStep annotation
        List<Method> stepMethods = new ArrayList<>();
        for (Method method : stepClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CSStep.class)) {
                stepMethods.add(method);
            }
        }
        
        stepClassMethods.put(stepClass, stepMethods);
        logger.info("Registered {} step methods from {}", stepMethods.size(), stepClass.getName());
    }
    
    /**
     * Get or create step instance for current thread
     */
    private static Object getOrCreateStepInstance(Class<?> stepClass) {
        Map<Class<?>, Object> threadInstances = threadLocalStepInstances.get();
        
        return threadInstances.computeIfAbsent(stepClass, clazz -> {
            String threadName = Thread.currentThread().getName();
            logger.info("[{}] Creating new step instance for class: {}", threadName, clazz.getName());
            
            try {
                Object instance;
                
                // Try to instantiate the step class
                if (CSStepDefinitions.class.isAssignableFrom(clazz)) {
                    // For CSStepDefinitions subclasses
                    try {
                        instance = clazz.getDeclaredConstructor().newInstance();
                    } catch (NoSuchMethodException e) {
                        // Try with parameters if no default constructor
                        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                        if (constructors.length > 0) {
                            Constructor<?> constructor = constructors[0];
                            constructor.setAccessible(true);
                            Class<?>[] paramTypes = constructor.getParameterTypes();
                            Object[] args = new Object[paramTypes.length];
                            for (int i = 0; i < paramTypes.length; i++) {
                                args[i] = getDefaultValue(paramTypes[i]);
                            }
                            instance = constructor.newInstance(args);
                        } else {
                            throw e;
                        }
                    }
                } else {
                    instance = clazz.getDeclaredConstructor().newInstance();
                }
                
                // Process page injections for this thread's instance
                CSSmartPageInjector.processPageInjections(instance);
                
                logger.info("[{}] Successfully created step instance for: {}", threadName, clazz.getName());
                return instance;
                
            } catch (Exception e) {
                logger.error("[{}] Failed to create step instance for: {}", threadName, clazz.getName(), e);
                throw new CSBddException("Failed to create step instance: " + clazz.getName(), e);
            }
        });
    }
    
    /**
     * Get step definitions for current thread
     */
    public static Map<String, CSStepDefinition> getThreadStepDefinitions() {
        Map<String, CSStepDefinition> threadDefinitions = threadLocalStepDefinitions.get();
        
        // If empty, build definitions for this thread
        if (threadDefinitions.isEmpty()) {
            buildThreadStepDefinitions();
        }
        
        return threadDefinitions;
    }
    
    /**
     * Build step definitions for current thread
     */
    private static void buildThreadStepDefinitions() {
        String threadName = Thread.currentThread().getName();
        logger.info("[{}] Building step definitions for thread", threadName);
        
        Map<String, CSStepDefinition> threadDefinitions = threadLocalStepDefinitions.get();
        
        for (Map.Entry<Class<?>, List<Method>> entry : stepClassMethods.entrySet()) {
            Class<?> stepClass = entry.getKey();
            List<Method> methods = entry.getValue();
            
            // Get or create instance for this thread
            Object instance = getOrCreateStepInstance(stepClass);
            
            // Create step definitions for this thread
            for (Method method : methods) {
                CSStep annotation = method.getAnnotation(CSStep.class);
                if (annotation != null) {
                    String pattern = annotation.value();
                    CSStepDefinition.StepType type = CSStepDefinition.StepType.ANY;
                    
                    CSStepDefinition stepDef = new CSStepDefinition(pattern, method, instance, type);
                    threadDefinitions.put(pattern, stepDef);
                    
                    logger.debug("[{}] Registered step: {}", threadName, pattern);
                }
            }
        }
        
        logger.info("[{}] Built {} step definitions", threadName, threadDefinitions.size());
    }
    
    /**
     * Find matching step definition for current thread
     */
    public static CSStepDefinition findStep(String stepText) {
        Map<String, CSStepDefinition> threadDefinitions = getThreadStepDefinitions();
        
        for (CSStepDefinition stepDef : threadDefinitions.values()) {
            if (stepDef.matches(stepText)) {
                return stepDef;
            }
        }
        
        return null;
    }
    
    /**
     * Execute step for current thread
     */
    public static void executeStep(String stepText, Map<String, Object> context) {
        String threadName = Thread.currentThread().getName();
        CSStepDefinition stepDef = findStep(stepText);
        
        if (stepDef == null) {
            throw new CSBddException("No matching step definition found for: " + stepText);
        }
        
        try {
            Object[] parameters;
            if (context != null && !context.isEmpty()) {
                parameters = stepDef.extractParametersWithContext(stepText, context);
                logger.debug("[{}] Executing step with context: {}", threadName, stepText);
            } else {
                parameters = stepDef.extractParameters(stepText);
                logger.debug("[{}] Executing step: {}", threadName, stepText);
            }
            
            stepDef.execute(parameters);
        } catch (Exception e) {
            throw new CSBddException("Failed to execute step: " + stepText, e);
        }
    }
    
    /**
     * Clear thread-local data for current thread
     */
    public static void clearThreadData() {
        String threadName = Thread.currentThread().getName();
        logger.info("[{}] Clearing thread-local step data", threadName);
        
        threadLocalStepInstances.remove();
        threadLocalStepDefinitions.remove();
        
        // Also clear page cache for this thread
        CSSmartPageInjector.clearPageCache();
    }
    
    /**
     * Get default value for primitive types
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0d;
            if (type == char.class) return '\0';
        }
        return null;
    }
    
    /**
     * Initialize pages for current thread (called after WebDriver is ready)
     */
    public static void initializeThreadPages() {
        String threadName = Thread.currentThread().getName();
        logger.info("[{}] Initializing pages for thread", threadName);
        
        Map<Class<?>, Object> threadInstances = threadLocalStepInstances.get();
        
        for (Object instance : threadInstances.values()) {
            CSSmartPageInjector.injectPages(instance);
        }
        
        logger.info("[{}] Page initialization complete", threadName);
    }
}