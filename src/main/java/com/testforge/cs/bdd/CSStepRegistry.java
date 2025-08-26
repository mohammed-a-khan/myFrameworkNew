package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.exceptions.CSBddException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for step definitions
 * Manages step discovery and matching
 */
public class CSStepRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CSStepRegistry.class);
    private static final CSStepRegistry instance = new CSStepRegistry();
    
    private final Map<CSStepDefinition.StepType, List<CSStepDefinition>> stepDefinitions;
    private final Map<Class<?>, Object> stepClassInstances;
    // CRITICAL FIX: Thread-local step instances for automatic page injection
    // Each thread needs its own step instance to avoid field sharing issues
    private final ThreadLocal<Map<Class<?>, Object>> threadLocalStepInstances;
    private final Map<String, List<CSStepDefinition>> stepsByPattern;
    private final Map<String, List<Method>> methodsByName;
    private final Set<String> validationErrors;
    
    private CSStepRegistry() {
        this.stepDefinitions = new ConcurrentHashMap<>();
        this.stepClassInstances = new ConcurrentHashMap<>();
        this.threadLocalStepInstances = ThreadLocal.withInitial(ConcurrentHashMap::new);
        this.stepsByPattern = new ConcurrentHashMap<>();
        this.methodsByName = new ConcurrentHashMap<>();
        this.validationErrors = new HashSet<>();
        
        // Initialize step type lists
        for (CSStepDefinition.StepType type : CSStepDefinition.StepType.values()) {
            stepDefinitions.put(type, new ArrayList<>());
        }
    }
    
    public static CSStepRegistry getInstance() {
        return instance;
    }
    
    /**
     * Register a step class
     */
    public void registerStepClass(Class<?> stepClass) {
        try {
            // Check if class is already registered
            if (stepClassInstances.containsKey(stepClass)) {
                logger.debug("Step class {} is already registered, skipping", stepClass.getName());
                return;
            }
            
            logger.info("Registering step class: {}", stepClass.getName());
            
            // Create instance if not exists
            Object instance = stepClassInstances.computeIfAbsent(stepClass, k -> {
                try {
                    // First try to check if it extends CSStepDefinitions
                    if (CSStepDefinitions.class.isAssignableFrom(k)) {
                        logger.info("Step class {} extends CSStepDefinitions, using specialized instantiation", k.getName());
                        // For CSStepDefinitions subclasses, try no-arg constructor first
                        try {
                            return k.getDeclaredConstructor().newInstance();
                        } catch (NoSuchMethodException e) {
                            // If no no-arg constructor, try to find any constructor
                            Constructor<?>[] constructors = k.getDeclaredConstructors();
                            if (constructors.length > 0) {
                                Constructor<?> constructor = constructors[0];
                                constructor.setAccessible(true);
                                
                                // Create default arguments for the constructor
                                Class<?>[] paramTypes = constructor.getParameterTypes();
                                Object[] args = new Object[paramTypes.length];
                                for (int i = 0; i < paramTypes.length; i++) {
                                    args[i] = getDefaultValue(paramTypes[i]);
                                }
                                
                                logger.info("Using constructor with {} parameters for class {}", paramTypes.length, k.getName());
                                return constructor.newInstance(args);
                            }
                        }
                    }
                    
                    // For other classes, try standard instantiation
                    return k.getDeclaredConstructor().newInstance();
                    
                } catch (Exception e) {
                    logger.error("Failed to instantiate step class: {} - Error: {}", k.getName(), e.getMessage());
                    logger.error("Make sure the step definition class has:", e);
                    logger.error("  1. A public no-argument constructor, OR");
                    logger.error("  2. Extends CSStepDefinitions (for framework integration)");
                    logger.error("  3. Is not an abstract class or interface");
                    logger.error("  4. Is accessible from the classpath");
                    throw new CSBddException("Failed to instantiate step class: " + k.getName() + ". " +
                        "Ensure it has a no-argument constructor or extends CSStepDefinitions.", e);
                }
            });
            
            // First pass: collect all methods for validation
            Map<String, Method> methodsInClass = new HashMap<>();
            for (Method method : stepClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(CSStep.class)) {
                    // Check for duplicate method names across packages
                    String methodName = method.getName();
                    methodsByName.computeIfAbsent(methodName, k -> new ArrayList<>()).add(method);
                    methodsInClass.put(methodName, method);
                }
            }
            
            // Second pass: register steps and validate
            for (Method method : stepClass.getDeclaredMethods()) {
                CSStep stepAnnotation = method.getAnnotation(CSStep.class);
                if (stepAnnotation != null) {
                    // Use description if provided, otherwise fall back to value
                    String pattern = !stepAnnotation.description().isEmpty() 
                        ? stepAnnotation.description() 
                        : stepAnnotation.value();
                    
                    if (!pattern.isEmpty()) {
                        // Check for duplicate step patterns
                        stepsByPattern.computeIfAbsent(pattern, k -> new ArrayList<>()).add(
                            new CSStepDefinition(pattern, method, instance, CSStepDefinition.StepType.ANY)
                        );
                        
                        // Always register as ANY type since we're removing type specification
                        registerStep(pattern, method, instance, "ANY");
                    }
                }
            }
            
            // Validate after registration
            validateStepDefinitions();
            
        } catch (Exception e) {
            throw new CSBddException("Failed to register step class: " + stepClass.getName(), e);
        }
    }
    
    /**
     * Register a single step definition
     */
    public void registerStep(String pattern, Method method, Object instance, String type) {
        CSStepDefinition.StepType stepType = parseStepType(type);
        CSStepDefinition stepDef = new CSStepDefinition(pattern, method, instance, stepType);
        
        stepDefinitions.get(stepType).add(stepDef);
        logger.debug("Registered step: {} - {}", stepType, pattern);
    }
    
    /**
     * Validate step definitions for duplicates
     */
    private void validateStepDefinitions() {
        validationErrors.clear();
        
        // Check for duplicate step patterns
        for (Map.Entry<String, List<CSStepDefinition>> entry : stepsByPattern.entrySet()) {
            if (entry.getValue().size() > 1) {
                String pattern = entry.getKey();
                List<String> locations = entry.getValue().stream()
                    .map(def -> String.format("%s.%s", 
                        def.getInstance().getClass().getName(), 
                        def.getMethod().getName()))
                    .collect(Collectors.toList());
                
                String error = String.format(
                    "ERROR: Duplicate step definition found for pattern '%s':\n" +
                    "  Found in multiple locations:\n%s\n" +
                    "  Please ensure each step pattern is unique across all step definition classes.",
                    pattern,
                    locations.stream().map(loc -> "    - " + loc).collect(Collectors.joining("\n"))
                );
                
                validationErrors.add(error);
                logger.error(error);
            }
        }
        
        // Check for duplicate method names
        for (Map.Entry<String, List<Method>> entry : methodsByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                String methodName = entry.getKey();
                List<String> locations = entry.getValue().stream()
                    .map(method -> method.getDeclaringClass().getName())
                    .distinct()
                    .collect(Collectors.toList());
                
                if (locations.size() > 1) {
                    String error = String.format(
                        "WARNING: Method name '%s' found in multiple classes:\n%s\n" +
                        "  Consider using unique method names to avoid confusion.",
                        methodName,
                        locations.stream().map(loc -> "    - " + loc).collect(Collectors.joining("\n"))
                    );
                    
                    validationErrors.add(error);
                    logger.warn(error);
                }
            }
        }
        
        // Throw exception if there are errors
        if (!validationErrors.isEmpty()) {
            throw new CSBddException(
                "Step definition validation failed:\n\n" + 
                String.join("\n\n", validationErrors) +
                "\n\nPlease fix these issues before running tests."
            );
        }
    }
    
    /**
     * Find matching step definition
     */
    public CSStepDefinition findStep(String stepText, CSStepDefinition.StepType preferredType) {
        // Since all steps are now ANY type, we just need to find a matching pattern
        for (List<CSStepDefinition> definitions : stepDefinitions.values()) {
            for (CSStepDefinition def : definitions) {
                if (def.matches(stepText)) {
                    return def;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Execute a step
     */
    public void executeStep(String stepText, CSStepDefinition.StepType stepType) {
        CSStepDefinition stepDef = findStep(stepText, stepType);
        
        if (stepDef == null) {
            String suggestions = getSuggestions(stepText);
            throw new CSBddException(
                String.format(
                    "No matching step definition found for: '%s'\n\n" +
                    "Please ensure:\n" +
                    "1. The step definition exists in your step classes\n" +
                    "2. The step pattern matches exactly (including parameters)\n" +
                    "3. The step class is properly registered\n" +
                    "%s",
                    stepText,
                    suggestions
                )
            );
        }
        
        try {
            Object[] parameters = stepDef.extractParameters(stepText);
            logger.debug("Executing step: {} with {} parameters", stepText, parameters.length);
            
            // Set test context for step reporting before execution
            CSScenarioRunner currentRunner = CSScenarioRunner.getCurrentInstance();
            if (currentRunner != null && currentRunner.getCurrentTestResult() != null) {
                com.testforge.cs.reporting.CSReportManager.setCurrentTestContext(
                    currentRunner.getCurrentTestResult().getTestId()
                );
            }
            
            stepDef.execute(parameters);
        } catch (Exception e) {
            throw new CSBddException("Failed to execute step: " + stepText, e);
        }
    }
    
    /**
     * Execute a step with context (for data row injection)
     */
    public void executeStep(String stepText, CSStepDefinition.StepType stepType, Map<String, Object> context) {
        CSStepDefinition stepDef = findStep(stepText, stepType);
        
        if (stepDef == null) {
            String suggestions = getSuggestions(stepText);
            throw new CSBddException(
                String.format(
                    "No matching step definition found for: '%s'\n\n" +
                    "Please ensure:\n" +
                    "1. The step definition exists in your step classes\n" +
                    "2. The step pattern matches exactly (including parameters)\n" +
                    "3. The step class is properly registered\n" +
                    "%s",
                    stepText,
                    suggestions
                )
            );
        }
        
        try {
            Object[] parameters = stepDef.extractParametersWithContext(stepText, context);
            logger.debug("Executing step: {} with {} parameters (context-aware)", stepText, parameters.length);
            
            // Set test context for step reporting before execution
            CSScenarioRunner currentRunner = CSScenarioRunner.getCurrentInstance();
            if (currentRunner != null && currentRunner.getCurrentTestResult() != null) {
                com.testforge.cs.reporting.CSReportManager.setCurrentTestContext(
                    currentRunner.getCurrentTestResult().getTestId()
                );
            }
            
            stepDef.execute(parameters);
        } catch (Exception e) {
            throw new CSBddException("Failed to execute step: " + stepText, e);
        }
    }
    
    /**
     * Get suggestions for similar step definitions
     */
    private String getSuggestions(String stepText) {
        List<String> availableSteps = new ArrayList<>();
        for (List<CSStepDefinition> definitions : stepDefinitions.values()) {
            for (CSStepDefinition def : definitions) {
                availableSteps.add(def.getOriginalPattern());
            }
        }
        
        if (availableSteps.isEmpty()) {
            return "\nNo step definitions registered. Please check your step class configuration.";
        }
        
        // Find similar steps (simple string contains check)
        List<String> similar = availableSteps.stream()
            .filter(pattern -> {
                String[] words = stepText.toLowerCase().split("\\s+");
                String lowerPattern = pattern.toLowerCase();
                return Arrays.stream(words)
                    .filter(word -> word.length() > 3)
                    .anyMatch(lowerPattern::contains);
            })
            .limit(5)
            .collect(Collectors.toList());
        
        if (!similar.isEmpty()) {
            return "\nDid you mean one of these?\n" +
                similar.stream()
                    .map(s -> "  - " + s)
                    .collect(Collectors.joining("\n"));
        }
        
        return "\nAvailable step patterns:\n" +
            availableSteps.stream()
                .limit(10)
                .map(s -> "  - " + s)
                .collect(Collectors.joining("\n")) +
            (availableSteps.size() > 10 ? "\n  ... and " + (availableSteps.size() - 10) + " more" : "");
    }
    
    /**
     * Clear all registered steps
     */
    public void clear() {
        stepDefinitions.values().forEach(List::clear);
        stepClassInstances.clear();
    }
    
    /**
     * Get all registered steps
     */
    public Map<CSStepDefinition.StepType, List<CSStepDefinition>> getAllSteps() {
        Map<CSStepDefinition.StepType, List<CSStepDefinition>> result = new HashMap<>();
        stepDefinitions.forEach((type, list) -> result.put(type, new ArrayList<>(list)));
        return result;
    }
    
    /**
     * Get all registered step definition instances
     */
    public Map<Class<?>, Object> getStepInstances() {
        return new HashMap<>(stepClassInstances);
    }
    
    /**
     * Get all registered step definition instances (static access)
     */
    public static Map<Class<?>, Object> getStepClassInstances() {
        return getInstance().getStepInstances();
    }
    
    /**
     * Parse step type from string
     */
    private CSStepDefinition.StepType parseStepType(String type) {
        String normalized = type.toUpperCase().trim();
        
        // Handle common variations
        if (normalized.startsWith("GIVEN")) {
            return CSStepDefinition.StepType.GIVEN;
        } else if (normalized.startsWith("WHEN")) {
            return CSStepDefinition.StepType.WHEN;
        } else if (normalized.startsWith("THEN")) {
            return CSStepDefinition.StepType.THEN;
        } else if (normalized.equals("AND")) {
            return CSStepDefinition.StepType.AND;
        } else if (normalized.equals("BUT")) {
            return CSStepDefinition.StepType.BUT;
        } else if (normalized.equals("ANY")) {
            return CSStepDefinition.StepType.ANY;
        }
        
        // Default to GIVEN
        return CSStepDefinition.StepType.GIVEN;
    }
    
    /**
     * Scan package for step classes
     */
    public void scanPackage(String packageName) {
        logger.info("Scanning package for step definitions: {}", packageName);
        
        try {
            // Get all classes in package
            List<Class<?>> classes = getClassesInPackage(packageName);
            
            for (Class<?> clazz : classes) {
                // Check if class has any @CSStep annotated methods
                boolean hasSteps = Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(CSStep.class));
                
                if (hasSteps) {
                    registerStepClass(clazz);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to scan package: {}", packageName, e);
        }
    }
    
    /**
     * Get classes in package using classpath scanning
     */
    private List<Class<?>> getClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.util.Enumeration<java.net.URL> resources = classLoader.getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    // Handle file system
                    classes.addAll(findClassesInDirectory(new java.io.File(resource.toURI()), packageName));
                } else if (resource.getProtocol().equals("jar")) {
                    // Handle JAR files
                    classes.addAll(findClassesInJar(resource, packagePath));
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning package: {}", packageName, e);
        }
        
        return classes;
    }
    
    /**
     * Find classes in directory
     */
    private List<Class<?>> findClassesInDirectory(java.io.File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        logger.warn("Could not load class: {}", className);
                    }
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Find classes in JAR file
     */
    private List<Class<?>> findClassesInJar(java.net.URL jarUrl, String packagePath) {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.startsWith(packagePath) && name.endsWith(".class")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            logger.warn("Could not load class: {}", className);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning JAR: {}", jarUrl, e);
        }
        
        return classes;
    }
    
    /**
     * Get default value for a given type
     */
    private Object getDefaultValue(Class<?> type) {
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
        return null; // For reference types
    }
    
    /**
     * Get thread-local step instance for the given class.
     * Each thread gets its own instance to ensure field isolation for automatic page injection.
     */
    public Object getThreadLocalStepInstance(Class<?> stepClass) {
        Map<Class<?>, Object> threadInstances = threadLocalStepInstances.get();
        
        return threadInstances.computeIfAbsent(stepClass, clazz -> {
            try {
                String threadName = Thread.currentThread().getName();
                logger.debug("[{}] Creating thread-local step instance for: {}", threadName, clazz.getSimpleName());
                
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object instance = constructor.newInstance();
                
                logger.debug("[{}] Created thread-local step instance: {}", threadName, clazz.getSimpleName());
                return instance;
                
            } catch (Exception e) {
                logger.error("Failed to create thread-local step instance for: {}", clazz.getName(), e);
                throw new RuntimeException("Failed to create thread-local step instance", e);
            }
        });
    }
    
    /**
     * Clear thread-local step instances for the current thread
     */
    public void clearThreadLocalInstances() {
        Map<Class<?>, Object> threadInstances = threadLocalStepInstances.get();
        if (!threadInstances.isEmpty()) {
            String threadName = Thread.currentThread().getName();
            logger.debug("[{}] Clearing {} thread-local step instances", threadName, threadInstances.size());
            threadInstances.clear();
        }
        threadLocalStepInstances.remove();
    }
}