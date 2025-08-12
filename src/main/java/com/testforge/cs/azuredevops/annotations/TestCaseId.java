package com.testforge.cs.azuredevops.annotations;

import java.lang.annotation.*;

/**
 * Annotation to map a test method to an Azure DevOps test case ID
 * Usage: @TestCaseId(419)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface TestCaseId {
    int value();
}