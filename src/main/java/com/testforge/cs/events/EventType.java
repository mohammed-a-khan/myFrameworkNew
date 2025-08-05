package com.testforge.cs.events;

/**
 * Event types for the event management system
 */
public enum EventType {
    // Test lifecycle events
    TEST_STARTED,
    TEST_PASSED,
    TEST_FAILED,
    TEST_SKIPPED,
    SUITE_STARTED,
    SUITE_FINISHED,
    
    // Step events
    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,
    
    // Browser events
    BROWSER_OPENED,
    BROWSER_CLOSED,
    NAVIGATION,
    
    // Element events
    ELEMENT_CLICKED,
    ELEMENT_TYPED,
    ELEMENT_FOUND,
    ELEMENT_NOT_FOUND,
    
    // Screenshot events
    SCREENSHOT_TAKEN,
    VIDEO_STARTED,
    VIDEO_STOPPED,
    
    // Custom events
    CUSTOM
}