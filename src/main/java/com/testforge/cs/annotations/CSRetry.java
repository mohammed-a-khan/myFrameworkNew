package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation for retry mechanism on test failures
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSRetry {
    
    /**
     * Number of retry attempts
     */
    int count() default 2;
    
    /**
     * Maximum number of retry attempts (alias for count)
     */
    int maxAttempts() default 2;
    
    /**
     * Delay between retries in milliseconds
     */
    long delay() default 1000;
    
    /**
     * Whether to increase delay exponentially
     */
    boolean exponentialBackoff() default false;
    
    /**
     * Maximum delay in milliseconds (for exponential backoff)
     */
    long maxDelay() default 30000;
    
    /**
     * Exception types to retry on
     */
    Class<? extends Throwable>[] retryOn() default {Exception.class};
    
    /**
     * Exception types to not retry on
     */
    Class<? extends Throwable>[] abortOn() default {};
    
    /**
     * Whether to retry on assertion errors
     */
    boolean retryOnAssertions() default false;
    
    /**
     * Custom retry condition class
     */
    Class<? extends RetryCondition> condition() default DefaultRetryCondition.class;
    
    /**
     * Retry condition interface
     */
    interface RetryCondition {
        boolean shouldRetry(Throwable throwable, int retryCount);
    }
    
    /**
     * Default retry condition
     */
    class DefaultRetryCondition implements RetryCondition {
        @Override
        public boolean shouldRetry(Throwable throwable, int retryCount) {
            return true;
        }
    }
}