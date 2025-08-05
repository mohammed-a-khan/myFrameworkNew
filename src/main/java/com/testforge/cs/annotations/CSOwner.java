package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Owner annotation for test methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface CSOwner {
    String value();
}