package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Enhanced test annotation with additional metadata and configuration
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSTest {
    
    /**
     * Test name
     */
    String name() default "";
    
    /**
     * Test description
     */
    String description() default "";
    
    /**
     * Test ID for external system integration
     */
    String id() default "";
    
    /**
     * Test category
     */
    String category() default "";
    
    /**
     * Test tags
     */
    String[] tags() default {};
    
    /**
     * Test categories/tags
     */
    String[] categories() default {};
    
    /**
     * Test priority (1-5, 1 being highest)
     */
    int priority() default 3;
    
    /**
     * Test severity level
     */
    Severity severity() default Severity.NORMAL;
    
    /**
     * Test type
     */
    TestType type() default TestType.FUNCTIONAL;
    
    /**
     * Whether test is enabled
     */
    boolean enabled() default true;
    
    /**
     * Timeout in milliseconds
     */
    long timeout() default 300000; // 5 minutes
    
    /**
     * Dependencies on other tests
     */
    String[] dependsOn() default {};
    
    /**
     * Environment(s) where test should run
     */
    String[] environments() default {};
    
    /**
     * Browser(s) for test execution
     */
    String[] browsers() default {};
    
    /**
     * Author of the test
     */
    String author() default "";
    
    /**
     * JIRA ticket or requirement ID
     */
    String requirement() default "";
    
    /**
     * Whether to take screenshot on success
     */
    boolean screenshotOnSuccess() default false;
    
    /**
     * Whether to record video
     */
    boolean recordVideo() default false;
    
    /**
     * Severity levels
     */
    enum Severity {
        BLOCKER,
        CRITICAL,
        MAJOR,
        NORMAL,
        MINOR,
        TRIVIAL
    }
    
    /**
     * Test types
     */
    enum TestType {
        FUNCTIONAL,
        INTEGRATION,
        SMOKE,
        REGRESSION,
        PERFORMANCE,
        SECURITY,
        API,
        UI,
        DATABASE,
        E2E
    }
}