package com.testforge.cs.bdd;

import com.testforge.cs.annotations.CSStep;
import com.testforge.cs.exceptions.CSBddException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for step definitions
 * Manages step discovery and matching
 */
public class CSStepRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CSStepRegistry.class);
    private static final CSStepRegistry instance = new CSStepRegistry();
    
    private final Map<CSStepDefinition.StepType, List<CSStepDefinition>> stepDefinitions;
    private final Map<Class<?>, Object> stepClassInstances;
    
    private CSStepRegistry() {
        this.stepDefinitions = new ConcurrentHashMap<>();
        this.stepClassInstances = new ConcurrentHashMap<>();
        
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
            logger.info("Registering step class: {}", stepClass.getName());
            
            // Create instance if not exists
            Object instance = stepClassInstances.computeIfAbsent(stepClass, k -> {
                try {
                    return k.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new CSBddException("Failed to instantiate step class: " + k.getName(), e);
                }
            });
            
            // Scan methods for step annotations
            for (Method method : stepClass.getDeclaredMethods()) {
                CSStep stepAnnotation = method.getAnnotation(CSStep.class);
                if (stepAnnotation != null) {
                    registerStep(stepAnnotation.value(), method, instance, stepAnnotation.type().name());
                }
            }
            
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
     * Find matching step definition
     */
    public CSStepDefinition findStep(String stepText, CSStepDefinition.StepType preferredType) {
        // First try with preferred type
        if (preferredType != null) {
            List<CSStepDefinition> definitions = stepDefinitions.get(preferredType);
            for (CSStepDefinition def : definitions) {
                if (def.matches(stepText)) {
                    return def;
                }
            }
        }
        
        // If not found, try all types
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
            throw new CSBddException("No matching step definition found for: " + stepText);
        }
        
        try {
            Object[] parameters = stepDef.extractParameters(stepText);
            logger.debug("Executing step: {} with {} parameters", stepText, parameters.length);
            stepDef.execute(parameters);
        } catch (Exception e) {
            throw new CSBddException("Failed to execute step: " + stepText, e);
        }
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
}