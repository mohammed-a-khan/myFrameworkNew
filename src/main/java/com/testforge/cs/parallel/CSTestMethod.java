package com.testforge.cs.parallel;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Test method definition for parallel execution
 */
public class CSTestMethod {
    private final Class<?> testClass;
    private final Method method;
    private final String methodName;
    private final Map<String, Object> parameters;
    private final int priority;
    private final String[] groups;
    private final boolean enabled;
    
    public CSTestMethod(Class<?> testClass, Method method, Map<String, Object> parameters, 
                       int priority, String[] groups, boolean enabled) {
        this.testClass = testClass;
        this.method = method;
        this.methodName = method.getName();
        this.parameters = parameters;
        this.priority = priority;
        this.groups = groups;
        this.enabled = enabled;
    }
    
    public Class<?> getTestClass() {
        return testClass;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String[] getGroups() {
        return groups;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}