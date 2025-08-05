package com.testforge.cs.events.listeners;

import com.testforge.cs.events.CSEvent;
import com.testforge.cs.events.CSEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Event listener that logs events to SLF4J
 * Production-ready with configurable log levels and formatting
 */
public class CSLoggingEventListener implements CSEventListener {
    private static final Logger logger = LoggerFactory.getLogger(CSLoggingEventListener.class);
    
    private final Set<String> supportedEventTypes;
    private final Set<CSEvent.EventCategory> supportedCategories;
    private final CSEvent.EventSeverity minimumSeverity;
    private final boolean includeMetadata;
    private final boolean includeContext;
    private final boolean includeStackTrace;
    
    public CSLoggingEventListener() {
        this(Set.of("*"), EnumSet.allOf(CSEvent.EventCategory.class), 
             CSEvent.EventSeverity.INFO, true, true, false);
    }
    
    public CSLoggingEventListener(Set<String> supportedEventTypes,
                                Set<CSEvent.EventCategory> supportedCategories,
                                CSEvent.EventSeverity minimumSeverity,
                                boolean includeMetadata,
                                boolean includeContext,
                                boolean includeStackTrace) {
        this.supportedEventTypes = supportedEventTypes;
        this.supportedCategories = supportedCategories;
        this.minimumSeverity = minimumSeverity;
        this.includeMetadata = includeMetadata;
        this.includeContext = includeContext;
        this.includeStackTrace = includeStackTrace;
    }
    
    @Override
    public void handleEvent(CSEvent event) {
        String logMessage = formatLogMessage(event);
        
        // Log at appropriate level based on event severity
        switch (event.getSeverity()) {
            case TRACE:
                if (logger.isTraceEnabled()) {
                    logger.trace(logMessage);
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(logMessage);
                }
                break;
            case INFO:
                logger.info(logMessage);
                break;
            case WARN:
                logger.warn(logMessage);
                break;
            case ERROR:
                logger.error(logMessage);
                if (includeStackTrace && event.getContext().containsKey("exception")) {
                    Object exception = event.getContext().get("exception");
                    if (exception instanceof Throwable) {
                        logger.error("Exception details:", (Throwable) exception);
                    }
                }
                break;
            case FATAL:
                logger.error("FATAL: {}", logMessage);
                break;
        }
    }
    
    /**
     * Format log message with event details
     */
    private String formatLogMessage(CSEvent event) {
        StringBuilder message = new StringBuilder();
        
        // Basic event info
        message.append("[").append(event.getEventType()).append("] ");
        message.append("Source: ").append(event.getSource()).append(" | ");
        message.append("Category: ").append(event.getCategory()).append(" | ");
        message.append("Thread: ").append(event.getThreadName()).append(" | ");
        message.append("ID: ").append(event.getEventId().substring(0, 8));
        
        // Add context information
        if (includeContext && !event.getContext().isEmpty()) {
            message.append(" | Context: ");
            event.getContext().forEach((key, value) -> {
                if (value != null) {
                    message.append(key).append("=").append(value).append(", ");
                }
            });
            // Remove trailing comma
            if (message.toString().endsWith(", ")) {
                message.setLength(message.length() - 2);
            }
        }
        
        // Add metadata if requested
        if (includeMetadata && !event.getMetadata().isEmpty()) {
            message.append(" | Metadata: ");
            event.getMetadata().forEach((key, value) -> {
                if (value != null && !key.startsWith("jvm.") && !key.startsWith("os.")) {
                    message.append(key).append("=").append(value).append(", ");
                }
            });
            // Remove trailing comma
            if (message.toString().endsWith(", ")) {
                message.setLength(message.length() - 2);
            }
        }
        
        return message.toString();
    }
    
    @Override
    public Set<String> getSupportedEventTypes() {
        return supportedEventTypes;
    }
    
    @Override
    public Set<CSEvent.EventCategory> getSupportedCategories() {
        return supportedCategories;
    }
    
    @Override
    public CSEvent.EventSeverity getMinimumSeverity() {
        return minimumSeverity;
    }
    
    @Override
    public String getListenerName() {
        return "LoggingEventListener";
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for logging
    }
    
    @Override
    public void initialize() {
        logger.info("Initialized logging event listener with severity >= {}", minimumSeverity);
    }
    
    @Override
    public void cleanup() {
        logger.info("Logging event listener cleanup complete");
    }
}