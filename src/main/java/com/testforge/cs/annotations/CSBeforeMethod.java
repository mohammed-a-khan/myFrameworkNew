package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Executed before each test method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CSBeforeMethod {
    String description() default "";
    boolean alwaysRun() default false;
}