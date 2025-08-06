package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation to inject the complete data row into a step definition parameter.
 * Used in data-driven scenarios to access all data fields.
 * 
 * Example:
 * <pre>
 * @CSStep(description = "I verify all user data")
 * public void verifyUserData(@CSDataRow Map<String, String> dataRow) {
 *     String username = dataRow.get("username");
 *     String email = dataRow.get("email");
 *     // Access any field from the data row
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSDataRow {
    /**
     * Whether to include metadata fields (like dataSourceType, dataSourceFile)
     * Default is false - only test data fields are included
     */
    boolean includeMetadata() default false;
}