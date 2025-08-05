package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Executed before a BDD feature
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CSBeforeFeature {
    String description() default "";
}