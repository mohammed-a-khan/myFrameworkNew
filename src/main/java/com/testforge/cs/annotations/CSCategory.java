package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Category annotation for test methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface CSCategory {
    String value();
}