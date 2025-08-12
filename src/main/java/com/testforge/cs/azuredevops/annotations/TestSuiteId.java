package com.testforge.cs.azuredevops.annotations;

import java.lang.annotation.*;

/**
 * Annotation to specify the Azure DevOps test suite ID
 * Can be applied at class or method level
 * Usage: @TestSuiteId(418)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface TestSuiteId {
    int value();
}