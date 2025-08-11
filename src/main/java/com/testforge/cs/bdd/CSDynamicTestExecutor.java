package com.testforge.cs.bdd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * Dynamic test executor that implements work-stealing pattern
 * to ensure threads don't remain idle when they finish their tests early
 */
public class CSDynamicTestExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CSDynamicTestExecutor.class);
    
    // Singleton instance
    private static CSDynamicTestExecutor instance;
    
    // Work queue for tests
    private final BlockingQueue<TestScenario> testQueue;
    
    // Track active threads
    private final Map<String, ThreadInfo> threadInfoMap = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger completedTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    
    // Thread pool for better control
    private final ExecutorService executorService;
    private final int maxThreads;
    
    private CSDynamicTestExecutor(int maxThreads) {
        this.maxThreads = maxThreads;
        this.testQueue = new LinkedBlockingQueue<>();
        this.executorService = new ThreadPoolExecutor(
            maxThreads,
            maxThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "TestExecutor-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            }
        );
        logger.info("Dynamic test executor initialized with {} threads", maxThreads);
    }
    
    /**
     * Get or create singleton instance
     */
    public static synchronized CSDynamicTestExecutor getInstance(int maxThreads) {
        if (instance == null) {
            instance = new CSDynamicTestExecutor(maxThreads);
        }
        return instance;
    }
    
    /**
     * Add test scenario to the queue
     */
    public void addTest(TestScenario scenario) {
        testQueue.offer(scenario);
        totalTests.incrementAndGet();
        logger.debug("Added test to queue: {} (queue size: {})", 
            scenario.getName(), testQueue.size());
    }
    
    /**
     * Get next available test from queue
     * This implements work-stealing - any free thread can take next test
     */
    public TestScenario getNextTest() {
        try {
            TestScenario scenario = testQueue.poll(100, TimeUnit.MILLISECONDS);
            if (scenario != null) {
                String threadName = Thread.currentThread().getName();
                updateThreadInfo(threadName, "Running: " + scenario.getName());
                logger.info("[{}] Taking test from queue: {} (remaining: {})", 
                    threadName, scenario.getName(), testQueue.size());
            }
            return scenario;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Check if there are more tests available
     */
    public boolean hasMoreTests() {
        return !testQueue.isEmpty();
    }
    
    /**
     * Update thread status
     */
    private void updateThreadInfo(String threadName, String status) {
        threadInfoMap.compute(threadName, (k, v) -> {
            if (v == null) {
                v = new ThreadInfo(threadName);
            }
            v.status = status;
            v.lastActivity = System.currentTimeMillis();
            return v;
        });
    }
    
    /**
     * Mark test as completed
     */
    public void markTestCompleted(TestScenario scenario, boolean success) {
        completedTests.incrementAndGet();
        if (!success) {
            failedTests.incrementAndGet();
        }
        
        String threadName = Thread.currentThread().getName();
        ThreadInfo info = threadInfoMap.get(threadName);
        if (info != null) {
            info.testsCompleted++;
            updateThreadInfo(threadName, "Idle - waiting for next test");
        }
        
        logger.info("[{}] Completed test: {} (Total completed: {}/{}, Queue remaining: {})",
            threadName, scenario.getName(), completedTests.get(), totalTests.get(), testQueue.size());
        
        // Log thread distribution
        if (completedTests.get() % 5 == 0 || !hasMoreTests()) {
            logThreadDistribution();
        }
    }
    
    /**
     * Log current thread distribution
     */
    private void logThreadDistribution() {
        logger.info("=== Thread Distribution ===");
        threadInfoMap.forEach((thread, info) -> {
            logger.info("  {}: {} tests completed, Status: {}", 
                thread, info.testsCompleted, info.status);
        });
        logger.info("  Queue remaining: {}", testQueue.size());
        logger.info("===========================");
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests.get());
        stats.put("completedTests", completedTests.get());
        stats.put("failedTests", failedTests.get());
        stats.put("remainingTests", testQueue.size());
        stats.put("threadsActive", threadInfoMap.size());
        
        Map<String, Integer> threadStats = new HashMap<>();
        threadInfoMap.forEach((thread, info) -> 
            threadStats.put(thread, info.testsCompleted));
        stats.put("threadDistribution", threadStats);
        
        return stats;
    }
    
    /**
     * Shutdown executor
     */
    public void shutdown() {
        logger.info("Shutting down dynamic test executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logThreadDistribution();
        logger.info("Final statistics: {}", getStatistics());
    }
    
    /**
     * Reset for new test run
     */
    public void reset() {
        testQueue.clear();
        threadInfoMap.clear();
        totalTests.set(0);
        completedTests.set(0);
        failedTests.set(0);
        logger.info("Dynamic test executor reset");
    }
    
    /**
     * Test scenario wrapper
     */
    public static class TestScenario {
        private final String featureFile;
        private final CSFeatureFile feature;
        private final CSFeatureFile.Scenario scenario;
        
        public TestScenario(String featureFile, CSFeatureFile feature, CSFeatureFile.Scenario scenario) {
            this.featureFile = featureFile;
            this.feature = feature;
            this.scenario = scenario;
        }
        
        public String getName() {
            return scenario.getName() + 
                (scenario.getDataRow() != null ? " [" + scenario.getDataRow() + "]" : "");
        }
        
        public String getFeatureFile() { return featureFile; }
        public CSFeatureFile getFeature() { return feature; }
        public CSFeatureFile.Scenario getScenario() { return scenario; }
    }
    
    /**
     * Thread information
     */
    private static class ThreadInfo {
        String name;
        String status = "Idle";
        int testsCompleted = 0;
        long lastActivity = System.currentTimeMillis();
        
        ThreadInfo(String name) {
            this.name = name;
        }
    }
}