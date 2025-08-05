package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation for defining element locators with support for multiple strategies
 * and object repository integration
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSLocator {
    
    /**
     * Primary locator value (optional when using other attributes)
     */
    String value() default "";
    
    /**
     * Object repository key reference
     */
    String locatorKey() default "";
    
    /**
     * ID locator
     */
    String id() default "";
    
    /**
     * Name locator
     */
    String name() default "";
    
    /**
     * CSS selector
     */
    String css() default "";
    
    /**
     * XPath locator
     */
    String xpath() default "";
    
    /**
     * Class name locator
     */
    String className() default "";
    
    /**
     * Tag name locator
     */
    String tagName() default "";
    
    /**
     * Link text locator
     */
    String linkText() default "";
    
    /**
     * Partial link text locator
     */
    String partialLinkText() default "";
    
    /**
     * Alternative locator strategies (can reference object repository keys)
     * Used as fallback if primary locator fails
     */
    String[] alternativeLocators() default {};
    
    /**
     * Description of the element for logging and AI healing
     */
    String description() default "";
    
    /**
     * Whether AI self-healing is enabled
     */
    boolean aiEnabled() default false;
    
    /**
     * AI description for self-healing
     */
    String aiDescription() default "";
    
    /**
     * Wait condition for the element
     */
    WaitCondition waitCondition() default WaitCondition.VISIBLE;
    
    /**
     * Whether to wait for element to be visible
     */
    boolean waitForVisible() default true;
    
    /**
     * Whether to wait for element to be clickable
     */
    boolean waitForClickable() default false;
    
    /**
     * Custom wait time in seconds (overrides default)
     */
    int waitTime() default -1;
    
    /**
     * Whether this element is optional (don't fail if not found)
     */
    boolean optional() default false;
    
    /**
     * Whether to cache the element once found
     */
    boolean cache() default false;
    
    /**
     * Whether to highlight the element when interacting
     */
    boolean highlight() default true;
    
    /**
     * Frame name/index if element is inside a frame
     */
    String frame() default "";
    
    /**
     * Shadow root selector if element is inside shadow DOM
     */
    String shadowRoot() default "";
    
    /**
     * Wait conditions
     */
    enum WaitCondition {
        PRESENT,
        VISIBLE,
        CLICKABLE,
        INVISIBLE,
        SELECTED,
        ENABLED,
        DISABLED,
        NONE
    }
}