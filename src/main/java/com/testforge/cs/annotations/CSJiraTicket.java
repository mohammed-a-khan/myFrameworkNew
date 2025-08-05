package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * JIRA ticket annotation for test methods
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface CSJiraTicket {
    String value();
}