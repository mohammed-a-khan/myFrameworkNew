package com.testforge.cs.events;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base event class for framework events
 * Provides comprehensive event data and metadata
 */
public abstract class CSEvent {
    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;
    private final String threadName;
    private final long threadId;
    private final String source;
    private final Map<String, Object> metadata;
    private final Map<String, Object> context;
    
    protected CSEvent(String eventType, String source) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.threadName = Thread.currentThread().getName();
        this.threadId = Thread.currentThread().getId();
        this.source = source;
        this.metadata = new HashMap<>();
        this.context = new HashMap<>();
        
        // Add default metadata
        metadata.put("jvm.version", System.getProperty("java.version"));
        metadata.put("os.name", System.getProperty("os.name"));
        metadata.put("user.name", System.getProperty("user.name"));
        
        try {
            metadata.put("hostname", java.net.InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            metadata.put("hostname", "unknown");
        }
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public long getThreadId() {
        return threadId;
    }
    
    public String getSource() {
        return source;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public Object getContext(String key) {
        return context.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', type='%s', source='%s', timestamp=%s, thread='%s'}", 
            getClass().getSimpleName(), eventId, eventType, source, timestamp, threadName);
    }
    
    /**
     * Get event severity level
     */
    public abstract EventSeverity getSeverity();
    
    /**
     * Get event category
     */
    public abstract EventCategory getCategory();
    
    /**
     * Event severity levels
     */
    public enum EventSeverity {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);
        
        private final int level;
        
        EventSeverity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
        
        public boolean isHigherThan(EventSeverity other) {
            return this.level > other.level;
        }
    }
    
    /**
     * Event categories
     */
    public enum EventCategory {
        FRAMEWORK,
        TEST_LIFECYCLE,
        WEB_DRIVER,
        PAGE_OBJECT,
        DATA_PROVIDER,
        REPORTING,
        API_TESTING,
        BDD,
        CONFIGURATION,
        DATABASE,
        AZURE_DEVOPS,
        SECURITY,
        PERFORMANCE,
        INTEGRATION,
        CUSTOM
    }
}