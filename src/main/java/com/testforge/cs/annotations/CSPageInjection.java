package com.testforge.cs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for automatic page object injection in step definitions and test classes.
 * 
 * Pages marked with this annotation are automatically initialized with proper
 * WebDriver and configuration when first accessed, eliminating the need for
 * manual getPage() calls or repetitive getter methods.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * public class MySteps extends CSStepDefinitions {
 *     @CSPageInjection
 *     private LoginPage loginPage;
 *     
 *     @CSStep("I login with username {username}")
 *     public void login(String username) {
 *         loginPage.enterUsername(username);  // Direct field access!
 *         loginPage.clickLogin();
 *     }
 * }
 * }</pre>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li><b>Lazy Initialization:</b> Pages are created only when first accessed</li>
 *   <li><b>Thread Safe:</b> Each test thread gets its own page instances</li>
 *   <li><b>Automatic Cleanup:</b> Pages are cleared after each test scenario</li>
 *   <li><b>Error Resilient:</b> Graceful handling of WebDriver timing issues</li>
 * </ul>
 * 
 * <h3>Timing:</h3>
 * <p>The annotation is processed during step definition instantiation, but actual
 * page creation is deferred until the first method call on the page object.
 * This ensures WebDriver is properly initialized before page construction.</p>
 * 
 * @see com.testforge.cs.core.CSBasePage
 * @see com.testforge.cs.bdd.CSStepDefinitions
 * @author CS TestForge Framework
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CSPageInjection {
    
    /**
     * Indicates whether the page should be cached for reuse within the same test scenario.
     * 
     * <p><b>true (default):</b> Page instance is created once and reused for all subsequent calls
     * within the same test scenario. This is more efficient and maintains state.</p>
     * 
     * <p><b>false:</b> A new page instance is created on every access. Use this for pages
     * that need fresh state or have side effects in constructor.</p>
     * 
     * @return true if page should be cached, false for new instance on each access
     */
    boolean cached() default true;
    
    /**
     * Optional description for documentation and debugging purposes.
     * 
     * <p>This description appears in framework logs when the page is first created,
     * helping with debugging and understanding test flow.</p>
     * 
     * @return human-readable description of the page's purpose
     */
    String description() default "";
}