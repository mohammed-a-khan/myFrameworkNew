package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Priority annotation for test methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface CSPriority {
    String value() default "MEDIUM";
}