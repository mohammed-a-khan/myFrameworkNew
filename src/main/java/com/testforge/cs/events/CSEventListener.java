package com.testforge.cs.events;

/**
 * Interface for event listeners
 * Provides comprehensive event handling capabilities
 */
public interface CSEventListener {
    
    /**
     * Handle an event
     * @param event The event to handle
     */
    void handleEvent(CSEvent event);
    
    /**
     * Check if this listener should handle the given event
     * @param event The event to check
     * @return true if this listener should handle the event
     */
    default boolean shouldHandle(CSEvent event) {
        return getSupportedEventTypes().contains(event.getEventType()) &&
               getSupportedCategories().contains(event.getCategory()) &&
               getMinimumSeverity().getLevel() <= event.getSeverity().getLevel();
    }
    
    /**
     * Get supported event types
     * @return Set of supported event type names
     */
    java.util.Set<String> getSupportedEventTypes();
    
    /**
     * Get supported event categories
     * @return Set of supported event categories
     */
    java.util.Set<CSEvent.EventCategory> getSupportedCategories();
    
    /**
     * Get minimum severity level
     * @return Minimum severity level for events to handle
     */
    CSEvent.EventSeverity getMinimumSeverity();
    
    /**
     * Get listener name
     * @return Unique name for this listener
     */
    String getListenerName();
    
    /**
     * Get listener priority (higher values = higher priority)
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Check if listener is enabled
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Initialize the listener
     */
    default void initialize() {
        // Default: no initialization needed
    }
    
    /**
     * Cleanup resources
     */
    default void cleanup() {
        // Default: no cleanup needed
    }
    
    /**
     * Handle listener errors
     * @param event The event that caused the error
     * @param error The error that occurred
     */
    default void handleError(CSEvent event, Exception error) {
        System.err.println("Error in event listener " + getListenerName() + 
                         " while handling event " + event.getEventId() + ": " + error.getMessage());
        error.printStackTrace();
    }
}