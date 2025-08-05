package com.testforge.cs.events.listeners;

import com.testforge.cs.events.CSEvent;
import com.testforge.cs.events.CSEventListener;
import com.testforge.cs.events.events.CSTestEvent;
import com.testforge.cs.events.events.CSWebDriverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Production-ready metrics collection event listener
 * Collects detailed performance and execution metrics for parallel execution
 */
public class CSMetricsEventListener implements CSEventListener {
    private static final Logger logger = LoggerFactory.getLogger(CSMetricsEventListener.class);
    
    // Test execution metrics
    private final Map<String, AtomicLong> testMetrics = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> executionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> threadMetrics = new ConcurrentHashMap<>();
    
    // WebDriver operation metrics
    private final AtomicLong navigationCount = new AtomicLong(0);
    private final AtomicLong elementOperations = new AtomicLong(0);
    private final AtomicLong screenshotCount = new AtomicLong(0);
    private final LongAdder totalNavigationTime = new LongAdder();
    private final LongAdder totalElementTime = new LongAdder();
    
    // Thread-specific metrics
    private final Map<Long, ThreadMetrics> threadSpecificMetrics = new ConcurrentHashMap<>();
    
    public CSMetricsEventListener() {
        // Initialize counters
        testMetrics.put("tests.started", new AtomicLong(0));
        testMetrics.put("tests.passed", new AtomicLong(0));
        testMetrics.put("tests.failed", new AtomicLong(0));
        testMetrics.put("tests.skipped", new AtomicLong(0));
        testMetrics.put("tests.retried", new AtomicLong(0));
        
        executionTimes.put("total.test.time", new LongAdder());
        executionTimes.put("total.setup.time", new LongAdder());
        executionTimes.put("total.teardown.time", new LongAdder());
        
        threadMetrics.put("threads.active", new AtomicLong(0));
        threadMetrics.put("threads.peak", new AtomicLong(0));
    }
    
    @Override
    public void handleEvent(CSEvent event) {
        try {
            long threadId = event.getThreadId();
            ThreadMetrics threadMetric = threadSpecificMetrics.computeIfAbsent(
                threadId, k -> new ThreadMetrics(threadId)
            );
            threadMetric.incrementEventCount();
            
            // Handle different event types
            switch (event.getCategory()) {
                case TEST_LIFECYCLE:
                    handleTestLifecycleMetrics(event, threadMetric);
                    break;
                case WEB_DRIVER:
                    handleWebDriverMetrics(event, threadMetric);
                    break;
                case API_TESTING:
                    handleApiMetrics(event, threadMetric);
                    break;
                case PERFORMANCE:
                    handlePerformanceMetrics(event, threadMetric);
                    break;
                default:
                    handleGenericMetrics(event, threadMetric);
                    break;
            }
            
            // Update thread activity
            updateThreadActivity();
            
        } catch (Exception e) {
            logger.error("Error collecting metrics for event: {}", event.getEventId(), e);
        }
    }
    
    /**
     * Handle test lifecycle metrics
     */
    private void handleTestLifecycleMetrics(CSEvent event, ThreadMetrics threadMetric) {
        if (event instanceof CSTestEvent) {
            CSTestEvent testEvent = (CSTestEvent) event;
            
            switch (event.getEventType()) {
                case CSTestEvent.TEST_STARTED:
                    testMetrics.get("tests.started").incrementAndGet();
                    threadMetric.incrementTestsStarted();
                    break;
                    
                case CSTestEvent.TEST_PASSED:
                    testMetrics.get("tests.passed").incrementAndGet();
                    threadMetric.incrementTestsPassed();
                    if (testEvent.getTestResult() != null) {
                        executionTimes.get("total.test.time").add(testEvent.getTestResult().getDuration());
                        threadMetric.addTestExecutionTime(testEvent.getTestResult().getDuration());
                    }
                    break;
                    
                case CSTestEvent.TEST_FAILED:
                    testMetrics.get("tests.failed").incrementAndGet();
                    threadMetric.incrementTestsFailed();
                    if (testEvent.getTestResult() != null) {
                        executionTimes.get("total.test.time").add(testEvent.getTestResult().getDuration());
                        threadMetric.addTestExecutionTime(testEvent.getTestResult().getDuration());
                    }
                    break;
                    
                case CSTestEvent.TEST_SKIPPED:
                    testMetrics.get("tests.skipped").incrementAndGet();
                    threadMetric.incrementTestsSkipped();
                    break;
                    
                case CSTestEvent.TEST_RETRY:
                    testMetrics.get("tests.retried").incrementAndGet();
                    threadMetric.incrementTestsRetried();
                    break;
            }
        }
    }
    
    /**
     * Handle WebDriver operation metrics
     */
    private void handleWebDriverMetrics(CSEvent event, ThreadMetrics threadMetric) {
        if (event instanceof CSWebDriverEvent) {
            CSWebDriverEvent driverEvent = (CSWebDriverEvent) event;
            long executionTime = driverEvent.getExecutionTime();
            
            switch (event.getEventType()) {
                case CSWebDriverEvent.NAVIGATION:
                    navigationCount.incrementAndGet();
                    totalNavigationTime.add(executionTime);
                    threadMetric.incrementNavigations();
                    threadMetric.addNavigationTime(executionTime);
                    break;
                    
                case CSWebDriverEvent.ELEMENT_CLICK:
                case CSWebDriverEvent.ELEMENT_SEND_KEYS:
                case CSWebDriverEvent.ELEMENT_FOUND:
                    elementOperations.incrementAndGet();
                    totalElementTime.add(executionTime);
                    threadMetric.incrementElementOperations();
                    threadMetric.addElementOperationTime(executionTime);
                    break;
                    
                case CSWebDriverEvent.SCREENSHOT_TAKEN:
                    screenshotCount.incrementAndGet();
                    threadMetric.incrementScreenshots();
                    break;
                    
                case CSWebDriverEvent.ELEMENT_NOT_FOUND:
                case CSWebDriverEvent.ELEMENT_TIMEOUT:
                    threadMetric.incrementElementErrors();
                    break;
            }
        }
    }
    
    /**
     * Handle API testing metrics
     */
    private void handleApiMetrics(CSEvent event, ThreadMetrics threadMetric) {
        Object responseTime = event.getContext().get("response.time");
        Object statusCode = event.getContext().get("response.status");
        
        threadMetric.incrementApiCalls();
        
        if (responseTime instanceof Number) {
            threadMetric.addApiResponseTime(((Number) responseTime).longValue());
        }
        
        if (statusCode instanceof Number) {
            int status = ((Number) statusCode).intValue();
            if (status >= 200 && status < 300) {
                threadMetric.incrementApiSuccess();
            } else {
                threadMetric.incrementApiErrors();
            }
        }
    }
    
    /**
     * Handle performance metrics
     */
    private void handlePerformanceMetrics(CSEvent event, ThreadMetrics threadMetric) {
        Object metric = event.getContext().get("performance.metric");
        Object value = event.getContext().get("performance.value");
        
        if (metric != null && value instanceof Number) {
            threadMetric.addPerformanceMetric(metric.toString(), ((Number) value).doubleValue());
        }
    }
    
    /**
     * Handle generic metrics
     */
    private void handleGenericMetrics(CSEvent event, ThreadMetrics threadMetric) {
        if (event.getSeverity().isHigherThan(CSEvent.EventSeverity.WARN)) {
            threadMetric.incrementErrors();
        }
    }
    
    /**
     * Update thread activity metrics
     */
    private void updateThreadActivity() {
        long activeThreads = threadSpecificMetrics.size();
        threadMetrics.get("threads.active").set(activeThreads);
        
        long currentPeak = threadMetrics.get("threads.peak").get();
        if (activeThreads > currentPeak) {
            threadMetrics.get("threads.peak").set(activeThreads);
        }
    }
    
    /**
     * Get comprehensive metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        Map<String, Long> testMetricsSnapshot = new ConcurrentHashMap<>();
        testMetrics.forEach((key, value) -> testMetricsSnapshot.put(key, value.get()));
        
        Map<String, Long> executionTimesSnapshot = new ConcurrentHashMap<>();
        executionTimes.forEach((key, value) -> executionTimesSnapshot.put(key, value.sum()));
        
        Map<String, Long> threadMetricsSnapshot = new ConcurrentHashMap<>();
        threadMetrics.forEach((key, value) -> threadMetricsSnapshot.put(key, value.get()));
        
        Map<Long, ThreadMetrics> threadSpecificSnapshot = new ConcurrentHashMap<>(threadSpecificMetrics);
        
        return new MetricsSnapshot(
            testMetricsSnapshot,
            executionTimesSnapshot,
            threadMetricsSnapshot,
            threadSpecificSnapshot,
            navigationCount.get(),
            elementOperations.get(),
            screenshotCount.get(),
            totalNavigationTime.sum(),
            totalElementTime.sum()
        );
    }
    
    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        testMetrics.values().forEach(counter -> counter.set(0));
        executionTimes.values().forEach(LongAdder::reset);
        threadMetrics.values().forEach(counter -> counter.set(0));
        threadSpecificMetrics.clear();
        
        navigationCount.set(0);
        elementOperations.set(0);
        screenshotCount.set(0);
        totalNavigationTime.reset();
        totalElementTime.reset();
    }
    
    @Override
    public Set<String> getSupportedEventTypes() {
        return Set.of("*"); // Handle all event types
    }
    
    @Override
    public Set<CSEvent.EventCategory> getSupportedCategories() {
        return EnumSet.allOf(CSEvent.EventCategory.class);
    }
    
    @Override
    public CSEvent.EventSeverity getMinimumSeverity() {
        return CSEvent.EventSeverity.DEBUG;
    }
    
    @Override
    public String getListenerName() {
        return "MetricsEventListener";
    }
    
    @Override
    public int getPriority() {
        return 80; // High priority for metrics collection
    }
    
    @Override
    public void initialize() {
        logger.info("Initialized metrics event listener for parallel execution");
    }
    
    @Override
    public void cleanup() {
        logger.info("Metrics event listener cleanup - Final metrics: Tests={}, WebDriver Ops={}, Threads={}",
            testMetrics.get("tests.started").get(),
            elementOperations.get(),
            threadMetrics.get("threads.peak").get());
    }
    
    /**
     * Thread-specific metrics collector
     */
    public static class ThreadMetrics {
        private final long threadId;
        private final AtomicLong eventCount = new AtomicLong(0);
        private final AtomicLong testsStarted = new AtomicLong(0);
        private final AtomicLong testsPassed = new AtomicLong(0);
        private final AtomicLong testsFailed = new AtomicLong(0);
        private final AtomicLong testsSkipped = new AtomicLong(0);
        private final AtomicLong testsRetried = new AtomicLong(0);
        private final LongAdder testExecutionTime = new LongAdder();
        
        private final AtomicLong navigations = new AtomicLong(0);
        private final AtomicLong elementOperations = new AtomicLong(0);
        private final AtomicLong elementErrors = new AtomicLong(0);
        private final AtomicLong screenshots = new AtomicLong(0);
        private final LongAdder navigationTime = new LongAdder();
        private final LongAdder elementOperationTime = new LongAdder();
        
        private final AtomicLong apiCalls = new AtomicLong(0);
        private final AtomicLong apiSuccess = new AtomicLong(0);
        private final AtomicLong apiErrors = new AtomicLong(0);
        private final LongAdder apiResponseTime = new LongAdder();
        
        private final AtomicLong errors = new AtomicLong(0);
        private final Map<String, Double> performanceMetrics = new ConcurrentHashMap<>();
        
        public ThreadMetrics(long threadId) {
            this.threadId = threadId;
        }
        
        // Increment methods
        public void incrementEventCount() { eventCount.incrementAndGet(); }
        public void incrementTestsStarted() { testsStarted.incrementAndGet(); }
        public void incrementTestsPassed() { testsPassed.incrementAndGet(); }
        public void incrementTestsFailed() { testsFailed.incrementAndGet(); }
        public void incrementTestsSkipped() { testsSkipped.incrementAndGet(); }
        public void incrementTestsRetried() { testsRetried.incrementAndGet(); }
        public void incrementNavigations() { navigations.incrementAndGet(); }
        public void incrementElementOperations() { elementOperations.incrementAndGet(); }
        public void incrementElementErrors() { elementErrors.incrementAndGet(); }
        public void incrementScreenshots() { screenshots.incrementAndGet(); }
        public void incrementApiCalls() { apiCalls.incrementAndGet(); }
        public void incrementApiSuccess() { apiSuccess.incrementAndGet(); }
        public void incrementApiErrors() { apiErrors.incrementAndGet(); }
        public void incrementErrors() { errors.incrementAndGet(); }
        
        // Add time methods
        public void addTestExecutionTime(long time) { testExecutionTime.add(time); }
        public void addNavigationTime(long time) { navigationTime.add(time); }
        public void addElementOperationTime(long time) { elementOperationTime.add(time); }
        public void addApiResponseTime(long time) { apiResponseTime.add(time); }
        public void addPerformanceMetric(String name, double value) { performanceMetrics.put(name, value); }
        
        // Getters
        public long getThreadId() { return threadId; }
        public long getEventCount() { return eventCount.get(); }
        public long getTestsStarted() { return testsStarted.get(); }
        public long getTestsPassed() { return testsPassed.get(); }
        public long getTestsFailed() { return testsFailed.get(); }
        public long getTestsSkipped() { return testsSkipped.get(); }
        public long getTestsRetried() { return testsRetried.get(); }
        public long getTestExecutionTime() { return testExecutionTime.sum(); }
        public long getNavigations() { return navigations.get(); }
        public long getElementOperations() { return elementOperations.get(); }
        public long getElementErrors() { return elementErrors.get(); }
        public long getScreenshots() { return screenshots.get(); }
        public long getNavigationTime() { return navigationTime.sum(); }
        public long getElementOperationTime() { return elementOperationTime.sum(); }
        public long getApiCalls() { return apiCalls.get(); }
        public long getApiSuccess() { return apiSuccess.get(); }
        public long getApiErrors() { return apiErrors.get(); }
        public long getApiResponseTime() { return apiResponseTime.sum(); }
        public long getErrors() { return errors.get(); }
        public Map<String, Double> getPerformanceMetrics() { return new ConcurrentHashMap<>(performanceMetrics); }
    }
    
    /**
     * Immutable metrics snapshot
     */
    public static class MetricsSnapshot {
        private final Map<String, Long> testMetrics;
        private final Map<String, Long> executionTimes;
        private final Map<String, Long> threadMetrics;
        private final Map<Long, ThreadMetrics> threadSpecificMetrics;
        private final long navigationCount;
        private final long elementOperations;
        private final long screenshotCount;
        private final long totalNavigationTime;
        private final long totalElementTime;
        
        public MetricsSnapshot(Map<String, Long> testMetrics, Map<String, Long> executionTimes,
                             Map<String, Long> threadMetrics, Map<Long, ThreadMetrics> threadSpecificMetrics,
                             long navigationCount, long elementOperations, long screenshotCount,
                             long totalNavigationTime, long totalElementTime) {
            this.testMetrics = testMetrics;
            this.executionTimes = executionTimes;
            this.threadMetrics = threadMetrics;
            this.threadSpecificMetrics = threadSpecificMetrics;
            this.navigationCount = navigationCount;
            this.elementOperations = elementOperations;
            this.screenshotCount = screenshotCount;
            this.totalNavigationTime = totalNavigationTime;
            this.totalElementTime = totalElementTime;
        }
        
        // Getters
        public Map<String, Long> getTestMetrics() { return testMetrics; }
        public Map<String, Long> getExecutionTimes() { return executionTimes; }
        public Map<String, Long> getThreadMetrics() { return threadMetrics; }
        public Map<Long, ThreadMetrics> getThreadSpecificMetrics() { return threadSpecificMetrics; }
        public long getNavigationCount() { return navigationCount; }
        public long getElementOperations() { return elementOperations; }
        public long getScreenshotCount() { return screenshotCount; }
        public long getTotalNavigationTime() { return totalNavigationTime; }
        public long getTotalElementTime() { return totalElementTime; }
        
        public double getAverageNavigationTime() {
            return navigationCount > 0 ? (double) totalNavigationTime / navigationCount : 0.0;
        }
        
        public double getAverageElementTime() {
            return elementOperations > 0 ? (double) totalElementTime / elementOperations : 0.0;
        }
    }
}