package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation to mark page object classes
 * Supports automatic page initialization and object repository integration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSPage {
    
    /**
     * Name of the page for identification
     */
    String name() default "";
    
    /**
     * URL or partial URL of the page
     */
    String url() default "";
    
    /**
     * Title of the page (for validation)
     */
    String title() default "";
    
    /**
     * Whether to auto-navigate to the page URL on initialization
     */
    boolean autoNavigate() default false;
    
    /**
     * Wait time in seconds for page load
     */
    int waitTime() default 30;
    
    /**
     * Object repository prefix for this page
     * E.g., "login" will look for locators starting with "login."
     */
    String repositoryPrefix() default "";
    
    /**
     * Whether to validate page load on initialization
     */
    boolean validateOnLoad() default true;
    
    /**
     * JavaScript to execute for page readiness check
     */
    String readyScript() default "";
    
    /**
     * Description of the page
     */
    String description() default "";
}