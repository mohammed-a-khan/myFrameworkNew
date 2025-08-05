package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Base annotation for BDD step definitions
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSStep {
    
    /**
     * Step pattern with parameter placeholders
     * E.g., "I login with username {string} and password {string}"
     */
    String value() default "";
    
    /**
     * Step description (alternative to value)
     */
    String description() default "";
    
    /**
     * Step type
     */
    StepType type() default StepType.GIVEN;
    
    /**
     * Step timeout in seconds
     */
    int timeout() default 30;
    
    /**
     * Whether to take screenshot after step
     */
    boolean screenshot() default false;
    
    /**
     * Step types
     */
    enum StepType {
        GIVEN,
        WHEN,
        THEN,
        AND,
        BUT
    }
}

/**
 * Given step annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Given {
    String value();
    int timeout() default 30;
    boolean screenshot() default false;
}

/**
 * When step annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface When {
    String value();
    int timeout() default 30;
    boolean screenshot() default false;
}

/**
 * Then step annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Then {
    String value();
    int timeout() default 30;
    boolean screenshot() default false;
}

/**
 * And step annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface And {
    String value();
    int timeout() default 30;
    boolean screenshot() default false;
}

/**
 * But step annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface But {
    String value();
    int timeout() default 30;
    boolean screenshot() default false;
}