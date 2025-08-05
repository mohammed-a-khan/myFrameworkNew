package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Execute after suite
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSAfterSuite {
    String description() default "";
    int order() default 0;
    boolean alwaysRun() default false;
}