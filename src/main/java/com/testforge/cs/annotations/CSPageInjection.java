package com.testforge.cs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatic page object injection in step definition classes.
 * 
 * This annotation enables automatic initialization of page objects when they are first accessed,
 * eliminating the need for manual page object creation in step definitions.
 * 
 * Features:
 * - Thread-safe lazy initialization
 * - Automatic caching of page instances
 * - Parallel execution support by default
 * - No timing issues with WebDriver initialization
 * 
 * Usage:
 * <pre>
 * public class MySteps extends CSStepDefinitions {
 *     
 *     {@code @CSPageInjection}
 *     private LoginPage loginPage;
 *     
 *     {@code @CSPageInjection(lazy = false)}
 *     private HomePage homePage;
 *     
 *     {@code @CSStep("I login")}
 *     public void login() {
 *         loginPage.enterUsername("user");  // Page auto-initialized on first access
 *         loginPage.clickLogin();
 *     }
 * }
 * </pre>
 * 
 * Configuration Options:
 * - lazy: Controls when page object is initialized (default: true)
 * - cache: Whether to cache the page instance (default: true)
 * - threadSafe: Ensures thread-safe access in parallel execution (default: true)
 * 
 * @author CS TestForge Framework
 * @since 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CSPageInjection {
    
    /**
     * Controls lazy initialization of the page object.
     * 
     * When true (default), the page object is created only when first accessed.
     * When false, the page object is created during step class initialization.
     * 
     * Note: Setting to false may cause initialization issues if WebDriver is not ready.
     * 
     * @return true for lazy initialization, false for immediate initialization
     */
    boolean lazy() default true;
    
    /**
     * Whether to cache the page instance after creation.
     * 
     * When true (default), the same page instance is reused across multiple accesses.
     * When false, a new page instance is created each time the field is accessed.
     * 
     * Caching improves performance by avoiding repeated page object creation.
     * 
     * @return true to cache the instance, false to create new instances each time
     */
    boolean cache() default true;
    
    /**
     * Ensures thread-safe access in parallel test execution.
     * 
     * When true (default), page objects are stored in thread-local storage to ensure
     * each thread has its own page instance, preventing conflicts during parallel execution.
     * 
     * When false, page objects are shared across threads (not recommended for parallel execution).
     * 
     * @return true for thread-safe access, false for shared access
     */
    boolean threadSafe() default true;
    
    /**
     * Optional name for the page injection (for debugging/logging purposes).
     * 
     * If not specified, the field name is used as the injection name.
     * 
     * @return custom name for this page injection
     */
    String name() default "";
    
    /**
     * Priority for page initialization when multiple pages are injected.
     * 
     * Lower numbers indicate higher priority (initialized first).
     * Pages with the same priority are initialized in field declaration order.
     * 
     * This is mainly useful when pages have dependencies on each other.
     * 
     * @return initialization priority (lower = higher priority)
     */
    int priority() default 1000;
}