package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Unified BDD step annotation for all step types
 * No regex patterns - just plain descriptions
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSBDDStep {
    
    /**
     * Step description in plain English
     * E.g., "I am on the login page", "I enter username {string} and password {string}"
     */
    String description();
    
    /**
     * Optional timeout in seconds for this step
     */
    int timeout() default 30;
    
    /**
     * Whether to take screenshot after this step
     */
    boolean screenshot() default false;
    
    /**
     * Whether to log this step in report
     */
    boolean logStep() default true;
}