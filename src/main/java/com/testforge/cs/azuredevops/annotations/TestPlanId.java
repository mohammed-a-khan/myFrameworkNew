package com.testforge.cs.azuredevops.annotations;

import java.lang.annotation.*;

/**
 * Annotation to specify the Azure DevOps test plan ID
 * Can be applied at class or method level
 * Usage: @TestPlanId(417)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface TestPlanId {
    int value();
}