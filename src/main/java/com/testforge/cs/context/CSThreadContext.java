package com.testforge.cs.context;

import com.testforge.cs.reporting.CSTestResult;
import org.openqa.selenium.WebDriver;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local context for test execution
 * Provides complete thread isolation for parallel execution
 */
public class CSThreadContext {
    private static final ThreadLocal<CSTestExecutionContext> threadLocal = new ThreadLocal<>();
    private static final Map<Long, CSTestExecutionContext> allContexts = new ConcurrentHashMap<>();
    
    /**
     * Get current thread's execution context
     */
    public static CSTestExecutionContext getCurrentContext() {
        CSTestExecutionContext context = threadLocal.get();
        if (context == null) {
            context = new CSTestExecutionContext(Thread.currentThread().getId());
            threadLocal.set(context);
            allContexts.put(Thread.currentThread().getId(), context);
        }
        return context;
    }
    
    /**
     * Clear current thread's context
     */
    public static void clearContext() {
        CSTestExecutionContext context = threadLocal.get();
        if (context != null) {
            allContexts.remove(Thread.currentThread().getId());
            threadLocal.remove();
        }
    }
    
    /**
     * Get context for specific thread
     */
    public static CSTestExecutionContext getContextForThread(long threadId) {
        return allContexts.get(threadId);
    }
    
    /**
     * Get all active contexts
     */
    public static Map<Long, CSTestExecutionContext> getAllContexts() {
        return new ConcurrentHashMap<>(allContexts);
    }
    
    /**
     * Set current test result
     */
    public static void setCurrentTestResult(CSTestResult testResult) {
        getCurrentContext().setCurrentTestResult(testResult);
    }
    
    /**
     * Get current test result
     */
    public static CSTestResult getCurrentTestResult() {
        return getCurrentContext().getCurrentTestResult();
    }
    
    /**
     * Set WebDriver for current thread
     */
    public static void setWebDriver(WebDriver driver) {
        getCurrentContext().setWebDriver(driver);
    }
    
    /**
     * Get WebDriver for current thread
     */
    public static WebDriver getWebDriver() {
        return getCurrentContext().getWebDriver();
    }
    
    /**
     * Add context data
     */
    public static void setContextData(String key, Object value) {
        getCurrentContext().setContextData(key, value);
    }
    
    /**
     * Get context data
     */
    public static Object getContextData(String key) {
        return getCurrentContext().getContextData(key);
    }
    
    /**
     * Remove context data
     */
    public static void removeContextData(String key) {
        getCurrentContext().removeContextData(key);
    }
    
    /**
     * Test execution context for a single thread
     */
    public static class CSTestExecutionContext {
        private final long threadId;
        private final LocalDateTime createdAt;
        private final Map<String, Object> contextData;
        
        private CSTestResult currentTestResult;
        private WebDriver webDriver;
        private String currentTestClass;
        private String currentTestMethod;
        private String suiteId;
        private String executionId;
        private boolean parallelExecution;
        
        public CSTestExecutionContext(long threadId) {
            this.threadId = threadId;
            this.createdAt = LocalDateTime.now();
            this.contextData = new ConcurrentHashMap<>();
            this.parallelExecution = false;
        }
        
        // Getters and setters
        public long getThreadId() {
            return threadId;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public CSTestResult getCurrentTestResult() {
            return currentTestResult;
        }
        
        public void setCurrentTestResult(CSTestResult currentTestResult) {
            this.currentTestResult = currentTestResult;
        }
        
        public WebDriver getWebDriver() {
            return webDriver;
        }
        
        public void setWebDriver(WebDriver webDriver) {
            this.webDriver = webDriver;
        }
        
        public String getCurrentTestClass() {
            return currentTestClass;
        }
        
        public void setCurrentTestClass(String currentTestClass) {
            this.currentTestClass = currentTestClass;
        }
        
        public String getCurrentTestMethod() {
            return currentTestMethod;
        }
        
        public void setCurrentTestMethod(String currentTestMethod) {
            this.currentTestMethod = currentTestMethod;
        }
        
        public String getSuiteId() {
            return suiteId;
        }
        
        public void setSuiteId(String suiteId) {
            this.suiteId = suiteId;
        }
        
        public String getExecutionId() {
            return executionId;
        }
        
        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }
        
        public boolean isParallelExecution() {
            return parallelExecution;
        }
        
        public void setParallelExecution(boolean parallelExecution) {
            this.parallelExecution = parallelExecution;
        }
        
        public Object getContextData(String key) {
            return contextData.get(key);
        }
        
        public void setContextData(String key, Object value) {
            contextData.put(key, value);
        }
        
        public void removeContextData(String key) {
            contextData.remove(key);
        }
        
        public Map<String, Object> getAllContextData() {
            return new ConcurrentHashMap<>(contextData);
        }
        
        public void clearAllContextData() {
            contextData.clear();
        }
        
        @Override
        public String toString() {
            return String.format("CSTestExecutionContext{threadId=%d, testClass='%s', testMethod='%s', createdAt=%s}", 
                threadId, currentTestClass, currentTestMethod, createdAt);
        }
    }
}