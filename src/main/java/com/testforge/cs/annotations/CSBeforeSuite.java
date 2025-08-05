package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Test lifecycle hook annotations
 */

/**
 * Execute before suite
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSBeforeSuite {
    String description() default "";
    int order() default 0;
}

