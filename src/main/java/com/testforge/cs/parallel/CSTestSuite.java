package com.testforge.cs.parallel;

import java.util.List;
import java.util.Map;

/**
 * Test suite definition for parallel execution
 */
public class CSTestSuite {
    private final String name;
    private final List<CSTestMethod> testMethods;
    private final Map<String, Object> parameters;
    private final int priority;
    private final boolean parallel;
    
    public CSTestSuite(String name, List<CSTestMethod> testMethods, Map<String, Object> parameters, 
                      int priority, boolean parallel) {
        this.name = name;
        this.testMethods = testMethods;
        this.parameters = parameters;
        this.priority = priority;
        this.parallel = parallel;
    }
    
    public String getName() {
        return name;
    }
    
    public List<CSTestMethod> getTestMethods() {
        return testMethods;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isParallel() {
        return parallel;
    }
}