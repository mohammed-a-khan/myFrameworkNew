package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Video recording annotation for test methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CSVideoRecord {
    boolean enabled() default true;
    String outputPath() default "";
}