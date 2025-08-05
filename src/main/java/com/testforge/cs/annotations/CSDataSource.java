package com.testforge.cs.annotations;

import java.lang.annotation.*;

/**
 * Annotation for data-driven testing
 * Supports Excel, CSV, JSON, and Database data sources
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSDataSource {
    
    /**
     * Type of data source
     */
    Type type();
    
    /**
     * Path to data file or SQL query key
     */
    String source() default "";
    
    /**
     * Path to data file (alias for source)
     */
    String path() default "";
    
    /**
     * SQL query string
     */
    String query() default "";
    
    /**
     * SQL query key from query manager
     */
    String queryKey() default "";
    
    /**
     * Query parameters
     */
    String[] queryParams() default {};
    
    /**
     * Sheet name for Excel files
     */
    String sheet() default "";
    
    /**
     * Key field for filtering data
     */
    String key() default "";
    
    /**
     * Key field for filtering data (alias)
     */
    String keyField() default "";
    
    /**
     * Key values to include (comma-separated)
     */
    String keyValues() default "";
    
    /**
     * Filter expression for additional filtering
     */
    String filter() default "";
    
    /**
     * Column mapping (column1=param1,column2=param2)
     */
    String mapping() default "";
    
    /**
     * Whether to include header row
     */
    boolean hasHeader() default true;
    
    /**
     * Database name for SQL queries
     */
    String database() default "default";
    
    /**
     * Whether to run tests in parallel for each data row
     */
    boolean parallel() default false;
    
    /**
     * Cache data in memory
     */
    boolean cache() default true;
    
    /**
     * Data source types
     */
    enum Type {
        EXCEL,
        CSV,
        JSON,
        DATABASE,
        PROPERTIES,
        YAML
    }
}