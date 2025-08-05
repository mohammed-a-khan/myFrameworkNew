package com.testforge.cs.events;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collection for event processing
 * Thread-safe performance monitoring
 */
public class CSEventMetrics {
    private final AtomicLong eventsReceived = new AtomicLong(0);
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsFiltered = new AtomicLong(0);
    private final AtomicLong eventsDropped = new AtomicLong(0);
    private final AtomicLong eventErrors = new AtomicLong(0);
    private final AtomicLong eventTimeouts = new AtomicLong(0);
    private final AtomicLong eventsWithNoListeners = new AtomicLong(0);
    
    private final LongAdder totalProcessingTime = new LongAdder();
    private final AtomicLong processedCount = new AtomicLong(0);
    
    private final long startTime = System.currentTimeMillis();
    
    public void incrementEventsReceived() {
        eventsReceived.incrementAndGet();
    }
    
    public void incrementEventsProcessed() {
        eventsProcessed.incrementAndGet();
    }
    
    public void incrementEventsFiltered() {
        eventsFiltered.incrementAndGet();
    }
    
    public void incrementEventsDropped() {
        eventsDropped.incrementAndGet();
    }
    
    public void incrementEventErrors() {
        eventErrors.incrementAndGet();
    }
    
    public void incrementEventTimeouts() {
        eventTimeouts.incrementAndGet();
    }
    
    public void incrementEventsWithNoListeners() {
        eventsWithNoListeners.incrementAndGet();
    }
    
    public void addProcessingTime(long nanoseconds) {
        totalProcessingTime.add(nanoseconds);
        processedCount.incrementAndGet();
    }
    
    public MetricsSnapshot getSnapshot() {
        long processed = processedCount.get();
        long totalTime = totalProcessingTime.sum();
        double avgProcessingTimeMs = processed > 0 ? (totalTime / processed) / 1_000_000.0 : 0.0;
        
        return new MetricsSnapshot(
            eventsReceived.get(),
            eventsProcessed.get(),
            eventsFiltered.get(),
            eventsDropped.get(),
            eventErrors.get(),
            eventTimeouts.get(),
            eventsWithNoListeners.get(),
            avgProcessingTimeMs,
            System.currentTimeMillis() - startTime
        );
    }
    
    public void reset() {
        eventsReceived.set(0);
        eventsProcessed.set(0);
        eventsFiltered.set(0);
        eventsDropped.set(0);
        eventErrors.set(0);
        eventTimeouts.set(0);
        eventsWithNoListeners.set(0);
        totalProcessingTime.reset();
        processedCount.set(0);
    }
    
    /**
     * Immutable metrics snapshot
     */
    public static class MetricsSnapshot {
        private final long eventsReceived;
        private final long eventsProcessed;
        private final long eventsFiltered;
        private final long eventsDropped;
        private final long eventErrors;
        private final long eventTimeouts;
        private final long eventsWithNoListeners;
        private final double averageProcessingTimeMs;
        private final long uptimeMs;
        
        public MetricsSnapshot(long eventsReceived, long eventsProcessed, long eventsFiltered,
                             long eventsDropped, long eventErrors, long eventTimeouts,
                             long eventsWithNoListeners, double averageProcessingTimeMs, long uptimeMs) {
            this.eventsReceived = eventsReceived;
            this.eventsProcessed = eventsProcessed;
            this.eventsFiltered = eventsFiltered;
            this.eventsDropped = eventsDropped;
            this.eventErrors = eventErrors;
            this.eventTimeouts = eventTimeouts;
            this.eventsWithNoListeners = eventsWithNoListeners;
            this.averageProcessingTimeMs = averageProcessingTimeMs;
            this.uptimeMs = uptimeMs;
        }
        
        public long getEventsReceived() { return eventsReceived; }
        public long getEventsProcessed() { return eventsProcessed; }
        public long getEventsFiltered() { return eventsFiltered; }
        public long getEventsDropped() { return eventsDropped; }
        public long getEventErrors() { return eventErrors; }
        public long getEventTimeouts() { return eventTimeouts; }
        public long getEventsWithNoListeners() { return eventsWithNoListeners; }
        public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
        public long getUptimeMs() { return uptimeMs; }
        
        public double getSuccessRate() {
            return eventsReceived > 0 ? (double) eventsProcessed / eventsReceived : 0.0;
        }
        
        public double getErrorRate() {
            return eventsReceived > 0 ? (double) eventErrors / eventsReceived : 0.0;
        }
        
        public double getEventsPerSecond() {
            return uptimeMs > 0 ? eventsReceived / (uptimeMs / 1000.0) : 0.0;
        }
    }
}