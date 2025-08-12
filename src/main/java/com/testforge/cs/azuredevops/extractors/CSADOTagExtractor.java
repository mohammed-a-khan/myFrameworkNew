package com.testforge.cs.azuredevops.extractors;

import com.testforge.cs.azuredevops.annotations.*;
import com.testforge.cs.azuredevops.config.CSADOConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Azure DevOps metadata from test annotations and tags
 * Equivalent to Playwright framework's ADOTagExtractor.ts
 */
public class CSADOTagExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CSADOTagExtractor.class);
    
    // Patterns for extracting IDs from method names or test descriptions
    private static final Pattern TEST_CASE_PATTERN = Pattern.compile("(?:TestCase|TC|ADOCase)[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEST_PLAN_PATTERN = Pattern.compile("(?:TestPlan|TP)[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEST_SUITE_PATTERN = Pattern.compile("(?:TestSuite|TS)[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);
    
    // Patterns for tag-style annotations in test descriptions
    private static final Pattern TAG_TEST_CASE_PATTERN = Pattern.compile("@?TestCaseId[-:](\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_TEST_PLAN_PATTERN = Pattern.compile("@?TestPlanId[-:](\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_TEST_SUITE_PATTERN = Pattern.compile("@?TestSuiteId[-:](\\d+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * ADO metadata container
     */
    public static class ADOMetadata {
        private Integer testPlanId;
        private Integer testSuiteId;
        private Integer testCaseId;
        
        public Integer getTestPlanId() { return testPlanId; }
        public void setTestPlanId(Integer testPlanId) { this.testPlanId = testPlanId; }
        
        public Integer getTestSuiteId() { return testSuiteId; }
        public void setTestSuiteId(Integer testSuiteId) { this.testSuiteId = testSuiteId; }
        
        public Integer getTestCaseId() { return testCaseId; }
        public void setTestCaseId(Integer testCaseId) { this.testCaseId = testCaseId; }
        
        public boolean hasTestCaseMapping() {
            return testCaseId != null;
        }
        
        @Override
        public String toString() {
            return String.format("ADOMetadata[TestPlan=%s, TestSuite=%s, TestCase=%s]", 
                testPlanId, testSuiteId, testCaseId);
        }
    }
    
    /**
     * Extract ADO metadata from test method and class
     */
    public static ADOMetadata extractADOMetadata(Method method, Class<?> testClass) {
        ADOMetadata metadata = new ADOMetadata();
        CSADOConfiguration config = CSADOConfiguration.getInstance();
        
        logger.debug("Extracting ADO metadata for method: {} in class: {}", 
            method.getName(), testClass.getName());
        
        // 1. First priority: Method-level annotations
        TestCaseId testCaseAnnotation = method.getAnnotation(TestCaseId.class);
        if (testCaseAnnotation != null) {
            metadata.setTestCaseId(testCaseAnnotation.value());
            logger.debug("Found @TestCaseId annotation: {}", metadata.getTestCaseId());
        }
        
        TestPlanId testPlanMethodAnnotation = method.getAnnotation(TestPlanId.class);
        if (testPlanMethodAnnotation != null) {
            metadata.setTestPlanId(testPlanMethodAnnotation.value());
            logger.debug("Found method-level @TestPlanId annotation: {}", metadata.getTestPlanId());
        }
        
        TestSuiteId testSuiteMethodAnnotation = method.getAnnotation(TestSuiteId.class);
        if (testSuiteMethodAnnotation != null) {
            metadata.setTestSuiteId(testSuiteMethodAnnotation.value());
            logger.debug("Found method-level @TestSuiteId annotation: {}", metadata.getTestSuiteId());
        }
        
        // 2. Second priority: Class-level annotations
        if (metadata.getTestPlanId() == null) {
            TestPlanId testPlanClassAnnotation = testClass.getAnnotation(TestPlanId.class);
            if (testPlanClassAnnotation != null) {
                metadata.setTestPlanId(testPlanClassAnnotation.value());
                logger.debug("Found class-level @TestPlanId annotation: {}", metadata.getTestPlanId());
            }
        }
        
        if (metadata.getTestSuiteId() == null) {
            TestSuiteId testSuiteClassAnnotation = testClass.getAnnotation(TestSuiteId.class);
            if (testSuiteClassAnnotation != null) {
                metadata.setTestSuiteId(testSuiteClassAnnotation.value());
                logger.debug("Found class-level @TestSuiteId annotation: {}", metadata.getTestSuiteId());
            }
        }
        
        // 3. Third priority: Extract from test description (TestNG @Test annotation)
        if (metadata.getTestCaseId() == null || metadata.getTestPlanId() == null || metadata.getTestSuiteId() == null) {
            Test testAnnotation = method.getAnnotation(Test.class);
            if (testAnnotation != null && testAnnotation.description() != null) {
                String description = testAnnotation.description();
                ADOMetadata descMetadata = extractFromDescription(description);
                
                if (metadata.getTestCaseId() == null && descMetadata.getTestCaseId() != null) {
                    metadata.setTestCaseId(descMetadata.getTestCaseId());
                    logger.debug("Found test case ID in description: {}", metadata.getTestCaseId());
                }
                if (metadata.getTestPlanId() == null && descMetadata.getTestPlanId() != null) {
                    metadata.setTestPlanId(descMetadata.getTestPlanId());
                    logger.debug("Found test plan ID in description: {}", metadata.getTestPlanId());
                }
                if (metadata.getTestSuiteId() == null && descMetadata.getTestSuiteId() != null) {
                    metadata.setTestSuiteId(descMetadata.getTestSuiteId());
                    logger.debug("Found test suite ID in description: {}", metadata.getTestSuiteId());
                }
            }
        }
        
        // 4. Fourth priority: Extract from method name
        if (metadata.getTestCaseId() == null) {
            Integer testCaseId = extractFromMethodName(method.getName());
            if (testCaseId != null) {
                metadata.setTestCaseId(testCaseId);
                logger.debug("Found test case ID in method name: {}", metadata.getTestCaseId());
            }
        }
        
        // 5. Fifth priority: Use configuration defaults
        if (metadata.getTestPlanId() == null && config.getTestPlanId() != null) {
            try {
                metadata.setTestPlanId(Integer.parseInt(config.getTestPlanId()));
                logger.debug("Using default test plan ID from config: {}", metadata.getTestPlanId());
            } catch (NumberFormatException e) {
                logger.warn("Invalid test plan ID in config: {}", config.getTestPlanId());
            }
        }
        
        if (metadata.getTestSuiteId() == null && config.getTestSuiteId() != null) {
            try {
                metadata.setTestSuiteId(Integer.parseInt(config.getTestSuiteId()));
                logger.debug("Using default test suite ID from config: {}", metadata.getTestSuiteId());
            } catch (NumberFormatException e) {
                logger.warn("Invalid test suite ID in config: {}", config.getTestSuiteId());
            }
        }
        
        logger.debug("Extracted ADO metadata: {}", metadata);
        return metadata;
    }
    
    /**
     * Extract ADO metadata from test description/tags
     * Supports patterns like @TestCaseId:419 or @TestCaseId-419
     */
    private static ADOMetadata extractFromDescription(String description) {
        ADOMetadata metadata = new ADOMetadata();
        
        if (description == null || description.isEmpty()) {
            return metadata;
        }
        
        // Check for tag-style patterns (@TestCaseId:419, @TestPlanId:417, etc.)
        Matcher testCaseMatcher = TAG_TEST_CASE_PATTERN.matcher(description);
        if (testCaseMatcher.find()) {
            metadata.setTestCaseId(Integer.parseInt(testCaseMatcher.group(1)));
        }
        
        Matcher testPlanMatcher = TAG_TEST_PLAN_PATTERN.matcher(description);
        if (testPlanMatcher.find()) {
            metadata.setTestPlanId(Integer.parseInt(testPlanMatcher.group(1)));
        }
        
        Matcher testSuiteMatcher = TAG_TEST_SUITE_PATTERN.matcher(description);
        if (testSuiteMatcher.find()) {
            metadata.setTestSuiteId(Integer.parseInt(testSuiteMatcher.group(1)));
        }
        
        // Also check for patterns in the description text
        if (metadata.getTestCaseId() == null) {
            Matcher caseMatcher = TEST_CASE_PATTERN.matcher(description);
            if (caseMatcher.find()) {
                metadata.setTestCaseId(Integer.parseInt(caseMatcher.group(1)));
            }
        }
        
        return metadata;
    }
    
    /**
     * Extract test case ID from method name
     * Supports patterns like testADOCase419_ValidLogin, testCase420_Invalid, etc.
     */
    private static Integer extractFromMethodName(String methodName) {
        if (methodName == null) {
            return null;
        }
        
        Matcher matcher = TEST_CASE_PATTERN.matcher(methodName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return null;
    }
    
    /**
     * Convert metadata to a map for easy storage
     */
    public static Map<String, String> toMetadataMap(ADOMetadata metadata) {
        Map<String, String> map = new HashMap<>();
        
        if (metadata.getTestCaseId() != null) {
            map.put("ado.testcase.id", metadata.getTestCaseId().toString());
        }
        if (metadata.getTestPlanId() != null) {
            map.put("ado.testplan.id", metadata.getTestPlanId().toString());
        }
        if (metadata.getTestSuiteId() != null) {
            map.put("ado.testsuite.id", metadata.getTestSuiteId().toString());
        }
        
        return map;
    }
    
    /**
     * Check if method has test case mapping
     */
    public static boolean hasTestCaseMapping(Method method) {
        // Check for @TestCaseId annotation
        if (method.getAnnotation(TestCaseId.class) != null) {
            return true;
        }
        
        // Check in description
        Test testAnnotation = method.getAnnotation(Test.class);
        if (testAnnotation != null && testAnnotation.description() != null) {
            String description = testAnnotation.description();
            if (TAG_TEST_CASE_PATTERN.matcher(description).find() || 
                TEST_CASE_PATTERN.matcher(description).find()) {
                return true;
            }
        }
        
        // Check in method name
        return TEST_CASE_PATTERN.matcher(method.getName()).find();
    }
}