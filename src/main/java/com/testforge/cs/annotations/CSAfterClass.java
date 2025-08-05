package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Executed after all test methods in the class
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CSAfterClass {
    String description() default "";
    boolean alwaysRun() default false;
}