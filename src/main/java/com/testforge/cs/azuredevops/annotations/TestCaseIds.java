package com.testforge.cs.azuredevops.annotations;

import java.lang.annotation.*;

/**
 * Annotation to map a test method to multiple Azure DevOps test case IDs
 * Usage: @TestCaseIds({419, 420, 421})
 * 
 * This is useful when:
 * - A single test validates multiple test cases
 * - The same scenario covers multiple ADO test cases
 * - You want to update multiple test cases with the same result
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface TestCaseIds {
    /**
     * Array of test case IDs
     */
    int[] value();
    
    /**
     * Optional description of why multiple test cases are mapped
     */
    String description() default "";
}