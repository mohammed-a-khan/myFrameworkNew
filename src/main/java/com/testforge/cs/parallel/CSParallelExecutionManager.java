package com.testforge.cs.parallel;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.context.CSThreadContext;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-ready parallel execution manager
 * Handles thread-safe parallel test execution with resource isolation
 */
public class CSParallelExecutionManager {
    private static final Logger logger = LoggerFactory.getLogger(CSParallelExecutionManager.class);
    private static volatile CSParallelExecutionManager instance;
    
    private final ThreadPoolExecutor executorService;
    private final ScheduledExecutorService monitoringService;
    private final Map<String, CSExecutionSuite> activeSuites;
    private final AtomicLong executionIdGenerator;
    private final AtomicInteger activeThreads;
    
    private final int maxThreads;
    private final int coreThreads;
    private final long keepAliveTime;
    private final boolean enableMonitoring;
    
    private volatile boolean shutdown = false;
    
    private CSParallelExecutionManager() {
        CSConfigManager config = CSConfigManager.getInstance();
        
        // Load configuration
        this.maxThreads = Integer.parseInt(config.getProperty("parallel.max.threads", 
            String.valueOf(Runtime.getRuntime().availableProcessors() * 2)));
        this.coreThreads = Integer.parseInt(config.getProperty("parallel.core.threads", 
            String.valueOf(Runtime.getRuntime().availableProcessors())));
        this.keepAliveTime = Long.parseLong(config.getProperty("parallel.keepalive.seconds", "60"));
        this.enableMonitoring = Boolean.parseBoolean(config.getProperty("parallel.monitoring.enabled", "true"));
        
        // Initialize thread pool with custom thread factory
        this.executorService = new ThreadPoolExecutor(
            coreThreads,
            maxThreads,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new CSThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Allow core threads to timeout
        this.executorService.allowCoreThreadTimeOut(true);
        
        // Initialize monitoring
        this.monitoringService = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "CS-Parallel-Monitor")
        );
        
        this.activeSuites = new ConcurrentHashMap<>();
        this.executionIdGenerator = new AtomicLong(System.currentTimeMillis());
        this.activeThreads = new AtomicInteger(0);
        
        if (enableMonitoring) {
            startMonitoring();
        }
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        logger.info("Parallel execution manager initialized - Core: {}, Max: {}, KeepAlive: {}s", 
            coreThreads, maxThreads, keepAliveTime);
    }
    
    public static CSParallelExecutionManager getInstance() {
        if (instance == null) {
            synchronized (CSParallelExecutionManager.class) {
                if (instance == null) {
                    instance = new CSParallelExecutionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Execute test suite in parallel
     */
    public CompletableFuture<CSExecutionResult> executeTestSuite(CSTestSuite testSuite) {
        if (shutdown) {
            throw new IllegalStateException("Parallel execution manager is shut down");
        }
        
        String suiteId = generateExecutionId();
        CSExecutionSuite executionSuite = new CSExecutionSuite(suiteId, testSuite);
        activeSuites.put(suiteId, executionSuite);
        
        logger.info("Starting parallel execution of test suite: {} with {} tests", 
            testSuite.getName(), testSuite.getTestMethods().size());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeTestSuiteInternal(executionSuite);
            } catch (Exception e) {
                logger.error("Error executing test suite: {}", testSuite.getName(), e);
                throw new RuntimeException("Test suite execution failed", e);
            } finally {
                activeSuites.remove(suiteId);
            }
        }, executorService);
    }
    
    /**
     * Execute individual test method
     */
    public CompletableFuture<CSTestResult> executeTest(CSTestMethod testMethod, String suiteId) {
        if (shutdown) {
            throw new IllegalStateException("Parallel execution manager is shut down");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            String originalThreadName = Thread.currentThread().getName();
            
            try {
                // Set thread name for better debugging
                Thread.currentThread().setName(
                    String.format("CS-Test-%s.%s-%d", 
                        testMethod.getTestClass().getSimpleName(),
                        testMethod.getMethodName(),
                        Thread.currentThread().getId())
                );
                
                // Initialize thread context
                CSThreadContext.getCurrentContext().setParallelExecution(true);
                CSThreadContext.getCurrentContext().setSuiteId(suiteId);
                CSThreadContext.getCurrentContext().setExecutionId(generateExecutionId());
                CSThreadContext.getCurrentContext().setCurrentTestClass(testMethod.getTestClass().getName());
                CSThreadContext.getCurrentContext().setCurrentTestMethod(testMethod.getMethodName());
                
                activeThreads.incrementAndGet();
                
                // Create test result
                CSTestResult testResult = new CSTestResult();
                testResult.setTestId(UUID.randomUUID().toString());
                testResult.setTestName(testMethod.getMethodName());
                testResult.setClassName(testMethod.getTestClass().getName());
                testResult.setMethodName(testMethod.getMethodName());
                testResult.setStartTime(LocalDateTime.now());
                testResult.setStatus(CSTestResult.Status.RUNNING);
                
                // Set in thread context
                CSThreadContext.setCurrentTestResult(testResult);
                
                logger.debug("Executing test: {}.{} on thread: {}", 
                    testMethod.getTestClass().getName(), testMethod.getMethodName(), 
                    Thread.currentThread().getName());
                
                // Execute the test method
                Object testInstance = testMethod.getTestClass().getDeclaredConstructor().newInstance();
                testMethod.getMethod().setAccessible(true);
                testMethod.getMethod().invoke(testInstance);
                
                // Test passed
                testResult.setStatus(CSTestResult.Status.PASSED);
                testResult.setEndTime(LocalDateTime.now());
                testResult.setDuration(testResult.calculateDuration());
                
                // Add to report manager (thread-safe)
                CSReportManager.getInstance().addTestResult(testResult);
                
                logger.debug("Test completed successfully: {}.{}", 
                    testMethod.getTestClass().getName(), testMethod.getMethodName());
                
                return testResult;
                
            } catch (Exception e) {
                // Test failed
                CSTestResult testResult = CSThreadContext.getCurrentTestResult();
                if (testResult != null) {
                    testResult.setStatus(CSTestResult.Status.FAILED);
                    testResult.setErrorMessage(e.getMessage());
                    testResult.setStackTrace(getStackTrace(e));
                    testResult.setEndTime(LocalDateTime.now());
                    testResult.setDuration(testResult.calculateDuration());
                    
                    // Add to report manager (thread-safe)
                    CSReportManager.getInstance().addTestResult(testResult);
                }
                
                logger.error("Test failed: {}.{}", 
                    testMethod.getTestClass().getName(), testMethod.getMethodName(), e);
                
                throw new RuntimeException("Test execution failed", e);
                
            } finally {
                activeThreads.decrementAndGet();
                
                // Clean up thread context
                CSThreadContext.clearContext();
                
                // Restore original thread name
                Thread.currentThread().setName(originalThreadName);
            }
        }, executorService);
    }
    
    /**
     * Execute test suite internally
     */
    private CSExecutionResult executeTestSuiteInternal(CSExecutionSuite executionSuite) {
        CSTestSuite testSuite = executionSuite.getTestSuite();
        List<CSTestMethod> testMethods = testSuite.getTestMethods();
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // Submit all tests for parallel execution
        List<CompletableFuture<CSTestResult>> futures = new ArrayList<>();
        
        for (CSTestMethod testMethod : testMethods) {
            CompletableFuture<CSTestResult> future = executeTest(testMethod, executionSuite.getSuiteId());
            futures.add(future);
        }
        
        // Wait for all tests to complete
        List<CSTestResult> results = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        
        for (CompletableFuture<CSTestResult> future : futures) {
            try {
                CSTestResult result = future.get();
                results.add(result);
            } catch (Exception e) {
                exceptions.add(e);
                logger.error("Test execution exception", e);
            }
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        
        // Create execution result
        CSExecutionResult executionResult = new CSExecutionResult(
            executionSuite.getSuiteId(),
            testSuite.getName(),
            startTime,
            endTime,
            results,
            exceptions
        );
        
        logger.info("Test suite completed: {} - {} tests, {} passed, {} failed, {} exceptions in {}ms",
            testSuite.getName(),
            results.size(),
            results.stream().mapToInt(r -> r.getStatus() == CSTestResult.Status.PASSED ? 1 : 0).sum(),
            results.stream().mapToInt(r -> r.getStatus() == CSTestResult.Status.FAILED ? 1 : 0).sum(),
            exceptions.size(),
            java.time.Duration.between(startTime, endTime).toMillis()
        );
        
        return executionResult;
    }
    
    /**
     * Start monitoring
     */
    private void startMonitoring() {
        monitoringService.scheduleAtFixedRate(() -> {
            try {
                int activeCount = executorService.getActiveCount();
                int poolSize = executorService.getPoolSize();
                long completedTasks = executorService.getCompletedTaskCount();
                long totalTasks = executorService.getTaskCount();
                int queueSize = executorService.getQueue().size();
                
                logger.debug("Parallel Execution Stats - Active: {}, Pool: {}, Queue: {}, " +
                           "Completed: {}/{}, ActiveThreads: {}, ActiveSuites: {}",
                    activeCount, poolSize, queueSize, completedTasks, totalTasks,
                    activeThreads.get(), activeSuites.size());
                
                // Check for potential issues
                if (queueSize > maxThreads * 2) {
                    logger.warn("High queue size detected: {}. Consider increasing thread pool size.", queueSize);
                }
                
                if (activeCount == maxThreads && queueSize > 0) {
                    logger.warn("Thread pool at maximum capacity with queued tasks. " +
                              "Performance may be impacted.");
                }
                
            } catch (Exception e) {
                logger.error("Error in parallel execution monitoring", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Generate unique execution ID
     */
    private String generateExecutionId() {
        return "EXE-" + executionIdGenerator.incrementAndGet();
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Shutdown parallel execution manager
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        logger.info("Shutting down parallel execution manager...");
        
        // Shutdown monitoring
        monitoringService.shutdown();
        
        // Shutdown executor
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
                
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown interrupted");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear active suites
        activeSuites.clear();
        
        logger.info("Parallel execution manager shutdown complete");
    }
    
    // Getters for monitoring
    public int getActiveThreadCount() {
        return activeThreads.get();
    }
    
    public int getActiveSuiteCount() {
        return activeSuites.size();
    }
    
    public ThreadPoolExecutor getExecutorService() {
        return executorService;
    }
    
    public Map<String, CSExecutionSuite> getActiveSuites() {
        return new HashMap<>(activeSuites);
    }
    
    /**
     * Custom thread factory for parallel execution
     */
    private static class CSThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "CSParallel-";
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}