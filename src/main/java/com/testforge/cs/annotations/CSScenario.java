package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation for BDD scenario definition
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSScenario {
    
    /**
     * Scenario name
     */
    String name();
    
    /**
     * Scenario description
     */
    String description() default "";
    
    /**
     * Scenario ID for external system integration
     */
    String id() default "";
    
    /**
     * Tags for the scenario
     */
    String[] tags() default {};
    
    /**
     * Scenario type
     */
    ScenarioType type() default ScenarioType.SCENARIO;
    
    /**
     * Examples for scenario outline
     */
    String examples() default "";
    
    /**
     * Whether scenario is enabled
     */
    boolean enabled() default true;
    
    /**
     * Priority of the scenario
     */
    int priority() default 3;
    
    /**
     * Dependencies on other scenarios
     */
    String[] dependsOn() default {};
    
    /**
     * Steps for the scenario
     */
    String[] steps() default {};
    
    /**
     * Scenario types
     */
    enum ScenarioType {
        SCENARIO,
        SCENARIO_OUTLINE,
        BACKGROUND
    }
}