package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation for BDD feature definition
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSFeature {
    
    /**
     * Feature name
     */
    String name();
    
    /**
     * Feature description
     */
    String description() default "";
    
    /**
     * Feature ID for external system integration
     */
    String id() default "";
    
    /**
     * Tags for the feature
     */
    String[] tags() default {};
    
    /**
     * Epic or module this feature belongs to
     */
    String epic() default "";
    
    /**
     * Author of the feature
     */
    String author() default "";
    
    /**
     * JIRA ticket or requirement ID
     */
    String requirement() default "";
    
    /**
     * Priority of the feature
     */
    int priority() default 3;
    
    /**
     * Whether feature is enabled
     */
    boolean enabled() default true;
}