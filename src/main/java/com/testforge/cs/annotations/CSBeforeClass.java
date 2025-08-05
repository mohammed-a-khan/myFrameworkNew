package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Executed before the first test method in the class
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CSBeforeClass {
    String description() default "";
    boolean alwaysRun() default false;
}