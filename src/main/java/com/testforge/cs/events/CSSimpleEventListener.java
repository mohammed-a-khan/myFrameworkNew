package com.testforge.cs.events;

/**
 * Simple event listener interface for basic event handling
 */
public interface CSSimpleEventListener {
    
    /**
     * Handle event
     * @param eventType The type of event
     * @param eventData The event data
     */
    void onEvent(EventType eventType, Object eventData);
}