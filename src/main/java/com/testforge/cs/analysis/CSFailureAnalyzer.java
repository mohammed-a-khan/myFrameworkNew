package com.testforge.cs.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Intelligent failure analyzer that categorizes test failures and provides actionable insights
 */
public class CSFailureAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CSFailureAnalyzer.class);
    
    public enum FailureCategory {
        FLAKY_SYNC("Synchronization Issue", true),
        FLAKY_ELEMENT("Element Interaction Issue", true),
        FLAKY_NETWORK("Network/Loading Issue", true),
        FLAKY_STALE("Stale Element Issue", true),
        FLAKY_TIMEOUT("Timeout Issue", true),
        FLAKY_ANIMATION("Animation/Transition Issue", true),
        FLAKY_POPUP("Popup/Modal Issue", true),
        ASSERTION_FAILURE("Assertion/Validation Failure", false),
        DATA_ISSUE("Test Data Issue", false),
        ENVIRONMENT_ISSUE("Environment Configuration Issue", false),
        CODE_ERROR("Code/Logic Error", false),
        FRAMEWORK_ISSUE("Framework Issue", false),
        UNKNOWN("Unknown Failure", false);
        
        private final String displayName;
        private final boolean isFlaky;
        
        FailureCategory(String displayName, boolean isFlaky) {
            this.displayName = displayName;
            this.isFlaky = isFlaky;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isFlaky() {
            return isFlaky;
        }
    }
    
    public static class FailureAnalysis {
        private FailureCategory category;
        private String rootCause;
        private List<String> recommendations;
        private double flakinessScore;
        private Map<String, String> metadata;
        
        public FailureAnalysis() {
            this.recommendations = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.flakinessScore = 0.0;
        }
        
        // Getters and setters
        public FailureCategory getCategory() { return category; }
        public void setCategory(FailureCategory category) { this.category = category; }
        
        public String getRootCause() { return rootCause; }
        public void setRootCause(String rootCause) { this.rootCause = rootCause; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void addRecommendation(String recommendation) { this.recommendations.add(recommendation); }
        
        public double getFlakinessScore() { return flakinessScore; }
        public void setFlakinessScore(double score) { this.flakinessScore = score; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void addMetadata(String key, String value) { this.metadata.put(key, value); }
        
        public boolean isFlaky() {
            return category != null && category.isFlaky();
        }
    }
    
    // Pattern definitions for different failure types
    private static final Map<FailureCategory, List<Pattern>> FAILURE_PATTERNS = new HashMap<>();
    
    static {
        // Synchronization issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_SYNC, Arrays.asList(
            Pattern.compile("(?i)element.*not.*clickable", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*not.*interactable", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)click.*intercepted", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)other.*element.*would.*receive.*click", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*is.*not.*currently.*visible", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*is.*obscured", Pattern.CASE_INSENSITIVE)
        ));
        
        // Element not found issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_ELEMENT, Arrays.asList(
            Pattern.compile("(?i)no.*such.*element", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)unable.*to.*locate.*element", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*not.*found", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)could.*not.*find.*element", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)NoSuchElementException", Pattern.CASE_INSENSITIVE)
        ));
        
        // Network/Loading issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_NETWORK, Arrays.asList(
            Pattern.compile("(?i)timeout.*waiting.*for.*page", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)page.*load.*timeout", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)connection.*refused", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)connection.*timeout", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)UnreachableBrowserException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)ERR_.*CONNECTION", Pattern.CASE_INSENSITIVE)
        ));
        
        // Stale element issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_STALE, Arrays.asList(
            Pattern.compile("(?i)stale.*element.*reference", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*is.*no.*longer.*attached", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)StaleElementReferenceException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)element.*reference.*is.*stale", Pattern.CASE_INSENSITIVE)
        ));
        
        // Timeout issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_TIMEOUT, Arrays.asList(
            Pattern.compile("(?i)wait.*timed.*out", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)timeout.*exception", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)expected.*condition.*failed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)TimeoutException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)waiting.*for.*visibility", Pattern.CASE_INSENSITIVE)
        ));
        
        // Animation/Transition issues
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_ANIMATION, Arrays.asList(
            Pattern.compile("(?i)element.*is.*moving", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)animation.*in.*progress", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)transition.*not.*complete", Pattern.CASE_INSENSITIVE)
        ));
        
        // Popup/Modal issues  
        FAILURE_PATTERNS.put(FailureCategory.FLAKY_POPUP, Arrays.asList(
            Pattern.compile("(?i)unexpected.*alert", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)modal.*dialog", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)UnhandledAlertException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)popup.*blocked", Pattern.CASE_INSENSITIVE)
        ));
        
        // Assertion failures
        FAILURE_PATTERNS.put(FailureCategory.ASSERTION_FAILURE, Arrays.asList(
            Pattern.compile("(?i)assertion.*failed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)expected.*but.*was", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)expected.*but.*found", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)AssertionError", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)expected.*actual", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)verification.*failed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)does.*not.*match", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)expected \\[.*\\] but found \\[.*\\]", Pattern.CASE_INSENSITIVE)
        ));
        
        // Data issues
        FAILURE_PATTERNS.put(FailureCategory.DATA_ISSUE, Arrays.asList(
            Pattern.compile("(?i)test.*data.*not.*found", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)invalid.*test.*data", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)null.*pointer.*exception", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)NumberFormatException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)IllegalArgumentException", Pattern.CASE_INSENSITIVE)
        ));
        
        // Environment issues
        FAILURE_PATTERNS.put(FailureCategory.ENVIRONMENT_ISSUE, Arrays.asList(
            Pattern.compile("(?i)environment.*not.*available", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)configuration.*error", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)driver.*not.*found", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)browser.*not.*installed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)ChromeDriver.*version", Pattern.CASE_INSENSITIVE)
        ));
        
        // Code errors
        FAILURE_PATTERNS.put(FailureCategory.CODE_ERROR, Arrays.asList(
            Pattern.compile("(?i)ClassNotFoundException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)NoSuchMethodException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)IllegalStateException", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)ArrayIndexOutOfBoundsException", Pattern.CASE_INSENSITIVE)
        ));
    }
    
    /**
     * Analyze a failure and provide detailed insights
     */
    public static FailureAnalysis analyzeFailure(Throwable throwable, String testName, String className) {
        FailureAnalysis analysis = new FailureAnalysis();
        
        if (throwable == null) {
            analysis.setCategory(FailureCategory.UNKNOWN);
            analysis.setRootCause("No exception information available");
            return analysis;
        }
        
        String errorMessage = throwable.getMessage() != null ? throwable.getMessage() : "";
        String stackTrace = getFullStackTrace(throwable);
        
        // Determine the failure category
        FailureCategory category = categorizeFailure(errorMessage, stackTrace, throwable);
        analysis.setCategory(category);
        
        // Calculate flakiness score
        double flakinessScore = calculateFlakinessScore(category, errorMessage, stackTrace);
        analysis.setFlakinessScore(flakinessScore);
        
        // Identify root cause
        String rootCause = identifyRootCause(category, errorMessage, throwable);
        analysis.setRootCause(rootCause);
        
        // Generate recommendations
        List<String> recommendations = generateRecommendations(category, errorMessage, stackTrace);
        recommendations.forEach(analysis::addRecommendation);
        
        // Add metadata
        analysis.addMetadata("testName", testName);
        analysis.addMetadata("className", className);
        analysis.addMetadata("exceptionType", throwable.getClass().getSimpleName());
        if (errorMessage != null && !errorMessage.isEmpty()) {
            analysis.addMetadata("errorMessage", errorMessage.length() > 200 ? 
                errorMessage.substring(0, 200) + "..." : errorMessage);
        }
        
        logger.debug("Analyzed failure for {}: Category={}, Flakiness={}", 
            testName, category, flakinessScore);
        
        return analysis;
    }
    
    /**
     * Categorize the failure based on error patterns
     */
    private static FailureCategory categorizeFailure(String errorMessage, String stackTrace, Throwable throwable) {
        String combinedText = errorMessage + " " + stackTrace;
        
        // Special handling for BDD exceptions - check the message for element-related issues
        if (throwable.getClass().getName().contains("CSBddException") || 
            throwable.getClass().getName().contains("BddException")) {
            if (errorMessage != null) {
                String lowerMsg = errorMessage.toLowerCase();
                // Check for element-related failures in BDD steps
                if (lowerMsg.contains("i should see") && lowerMsg.contains("element")) {
                    return FailureCategory.FLAKY_ELEMENT;
                }
                if (lowerMsg.contains("element not found") || lowerMsg.contains("unable to locate")) {
                    return FailureCategory.FLAKY_ELEMENT;
                }
                if (lowerMsg.contains("timeout") || lowerMsg.contains("timed out")) {
                    return FailureCategory.FLAKY_TIMEOUT;
                }
                if (lowerMsg.contains("not clickable") || lowerMsg.contains("not interactable")) {
                    return FailureCategory.FLAKY_SYNC;
                }
            }
        }
        
        // Check each category's patterns
        for (Map.Entry<FailureCategory, List<Pattern>> entry : FAILURE_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(combinedText).find()) {
                    return entry.getKey();
                }
            }
        }
        
        // Check for specific exception types (check full class name)
        String exceptionName = throwable.getClass().getName();
        String simpleExceptionName = throwable.getClass().getSimpleName();
        
        // Check for Selenium-specific exceptions first (these are usually flaky)
        if (exceptionName.contains("NoSuchElementException")) {
            return FailureCategory.FLAKY_ELEMENT;
        } else if (exceptionName.contains("StaleElementReferenceException")) {
            return FailureCategory.FLAKY_STALE;
        } else if (exceptionName.contains("TimeoutException")) {
            return FailureCategory.FLAKY_TIMEOUT;
        } else if (exceptionName.contains("ElementNotInteractableException") || 
                   exceptionName.contains("ElementClickInterceptedException")) {
            return FailureCategory.FLAKY_SYNC;
        } else if (exceptionName.contains("UnhandledAlertException")) {
            return FailureCategory.FLAKY_POPUP;
        }
        // Check for assertion failures (these are usually genuine)
        else if (exceptionName.contains("AssertionError") || 
                 simpleExceptionName.contains("Assert")) {
            return FailureCategory.ASSERTION_FAILURE;
        }
        
        return FailureCategory.UNKNOWN;
    }
    
    /**
     * Calculate flakiness score (0.0 to 1.0)
     */
    private static double calculateFlakinessScore(FailureCategory category, String errorMessage, String stackTrace) {
        double score = 0.0;
        
        // Base score from category
        if (category.isFlaky()) {
            score = 0.7; // Base flaky score
        } else {
            score = 0.2; // Base non-flaky score
        }
        
        // Adjust based on specific indicators
        String combinedText = (errorMessage + " " + stackTrace).toLowerCase();
        
        // High flakiness indicators
        if (combinedText.contains("intermittent") || combinedText.contains("sometimes")) {
            score += 0.2;
        }
        if (combinedText.contains("timing") || combinedText.contains("race condition")) {
            score += 0.15;
        }
        if (combinedText.contains("retry") || combinedText.contains("retrying")) {
            score += 0.1;
        }
        
        // Low flakiness indicators
        if (combinedText.contains("always fails") || combinedText.contains("consistently")) {
            score -= 0.3;
        }
        if (combinedText.contains("compilation") || combinedText.contains("syntax")) {
            score -= 0.4;
        }
        
        // Normalize score
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Identify the root cause of the failure
     */
    private static String identifyRootCause(FailureCategory category, String errorMessage, Throwable throwable) {
        switch (category) {
            case FLAKY_SYNC:
                return "Element was not ready for interaction. The page might still be loading or JavaScript is modifying the DOM.";
            
            case FLAKY_ELEMENT:
                return "Element could not be found on the page. It may appear after a delay or require scrolling into view.";
            
            case FLAKY_NETWORK:
                return "Network or page loading issue. The application might be slow to respond or experiencing connectivity issues.";
            
            case FLAKY_STALE:
                return "The DOM was modified after the element was located. The page structure changed between finding and using the element.";
            
            case FLAKY_TIMEOUT:
                return "Operation exceeded the configured timeout. The application is responding slower than expected.";
            
            case FLAKY_ANIMATION:
                return "Element is in motion due to animations or transitions. The UI is not stable for interaction.";
            
            case FLAKY_POPUP:
                return "Unexpected popup or modal dialog interfered with the test execution.";
            
            case ASSERTION_FAILURE:
                if (errorMessage != null && errorMessage.contains("expected")) {
                    return "Actual value did not match expected value. This is likely a genuine test failure.";
                }
                return "Test validation failed. The application behavior doesn't match test expectations.";
            
            case DATA_ISSUE:
                return "Problem with test data or data setup. Required test data may be missing or invalid.";
            
            case ENVIRONMENT_ISSUE:
                return "Environment configuration problem. Check browser drivers, system dependencies, and configurations.";
            
            case CODE_ERROR:
                return "Test code error. There's a problem with the test implementation itself.";
            
            case FRAMEWORK_ISSUE:
                return "Framework or infrastructure issue. The test automation framework encountered an internal error.";
            
            default:
                return "Unable to determine specific root cause. Manual investigation required.";
        }
    }
    
    /**
     * Generate actionable recommendations
     */
    private static List<String> generateRecommendations(FailureCategory category, String errorMessage, String stackTrace) {
        List<String> recommendations = new ArrayList<>();
        
        switch (category) {
            case FLAKY_SYNC:
                recommendations.add("Add explicit wait for element to be clickable: WebDriverWait.until(ExpectedConditions.elementToBeClickable())");
                recommendations.add("Check for overlapping elements using JavaScript: document.elementFromPoint(x, y)");
                recommendations.add("Wait for page/AJAX to complete before interaction");
                recommendations.add("Consider adding a small delay before clicking if animations are present");
                recommendations.add("Use JavaScript click as fallback: ((JavascriptExecutor)driver).executeScript(\"arguments[0].click();\", element)");
                break;
                
            case FLAKY_ELEMENT:
                recommendations.add("Increase wait timeout for element presence");
                recommendations.add("Verify the selector is correct and unique");
                recommendations.add("Check if element is inside an iframe");
                recommendations.add("Ensure element is scrolled into view before interaction");
                recommendations.add("Consider using more stable locators (id > name > css > xpath)");
                if (errorMessage.contains("xpath")) {
                    recommendations.add("XPath detected - consider using CSS selector for better performance and stability");
                }
                break;
                
            case FLAKY_NETWORK:
                recommendations.add("Increase page load timeout");
                recommendations.add("Add retry mechanism for page loads");
                recommendations.add("Check network connectivity and application availability");
                recommendations.add("Consider implementing a health check before test execution");
                recommendations.add("Add wait for document ready state: wait.until(webDriver -> ((JavascriptExecutor)webDriver).executeScript(\"return document.readyState\").equals(\"complete\"))");
                break;
                
            case FLAKY_STALE:
                recommendations.add("Re-find element before each interaction");
                recommendations.add("Use Page Object pattern with dynamic element lookup");
                recommendations.add("Implement StaleElementReferenceException retry handler");
                recommendations.add("Wait for page to stabilize after AJAX operations");
                recommendations.add("Consider using @FindBy with PageFactory for automatic re-initialization");
                break;
                
            case FLAKY_TIMEOUT:
                recommendations.add("Increase timeout values in configuration");
                recommendations.add("Check if application performance has degraded");
                recommendations.add("Add performance logging to identify slow operations");
                recommendations.add("Consider parallel execution to reduce overall execution time");
                recommendations.add("Implement smart waits that poll more frequently");
                break;
                
            case FLAKY_ANIMATION:
                recommendations.add("Wait for animations to complete: wait for CSS transition end");
                recommendations.add("Disable animations in test environment via CSS");
                recommendations.add("Add custom wait for element to stop moving");
                recommendations.add("Use JavaScript to check animation state");
                break;
                
            case FLAKY_POPUP:
                recommendations.add("Add alert handling: driver.switchTo().alert().accept()");
                recommendations.add("Check for and close popups before test actions");
                recommendations.add("Implement UnhandledAlertException handler");
                recommendations.add("Disable popups in test environment if possible");
                break;
                
            case ASSERTION_FAILURE:
                recommendations.add("Review expected values - ensure they match current application behavior");
                recommendations.add("Add detailed assertion messages for better debugging");
                recommendations.add("Consider using soft assertions to collect all failures");
                recommendations.add("Verify test data setup is correct");
                recommendations.add("Check if application functionality has changed");
                break;
                
            case DATA_ISSUE:
                recommendations.add("Verify test data exists and is valid");
                recommendations.add("Add null checks before using test data");
                recommendations.add("Implement test data validation");
                recommendations.add("Consider using test data factories or builders");
                recommendations.add("Check database state before test execution");
                break;
                
            case ENVIRONMENT_ISSUE:
                recommendations.add("Update browser driver to latest version");
                recommendations.add("Verify browser version compatibility");
                recommendations.add("Check system requirements and dependencies");
                recommendations.add("Ensure proper permissions for test execution");
                recommendations.add("Validate environment configuration properties");
                break;
                
            case CODE_ERROR:
                recommendations.add("Review test code for logical errors");
                recommendations.add("Check method signatures and parameters");
                recommendations.add("Ensure all required dependencies are available");
                recommendations.add("Add proper error handling in test code");
                recommendations.add("Consider refactoring complex test logic");
                break;
                
            default:
                recommendations.add("Review full stack trace for more details");
                recommendations.add("Check recent code changes that might have caused the issue");
                recommendations.add("Run test in debug mode for detailed analysis");
                recommendations.add("Compare with previous successful runs");
                break;
        }
        
        // Add general recommendations for flaky tests
        if (category.isFlaky()) {
            recommendations.add("Consider implementing retry mechanism for flaky tests");
            recommendations.add("Run test multiple times to confirm flakiness");
            recommendations.add("Add detailed logging to capture intermittent issues");
        }
        
        return recommendations;
    }
    
    /**
     * Get full stack trace as string
     */
    private static String getFullStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        if (throwable.getCause() != null) {
            sb.append("Caused by: ");
            sb.append(getFullStackTrace(throwable.getCause()));
        }
        
        return sb.toString();
    }
    
    /**
     * Get a summary of flaky test statistics
     */
    public static Map<String, Object> getFlakyTestStats(List<FailureAnalysis> analyses) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalFailures = analyses.size();
        long flakyFailures = analyses.stream().filter(FailureAnalysis::isFlaky).count();
        long genuineFailures = totalFailures - flakyFailures;
        
        stats.put("totalFailures", totalFailures);
        stats.put("flakyFailures", flakyFailures);
        stats.put("genuineFailures", genuineFailures);
        stats.put("flakyPercentage", totalFailures > 0 ? (flakyFailures * 100.0 / totalFailures) : 0.0);
        
        // Category breakdown
        Map<String, Long> categoryCount = new HashMap<>();
        for (FailureAnalysis analysis : analyses) {
            String category = analysis.getCategory().getDisplayName();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0L) + 1);
        }
        stats.put("categoryBreakdown", categoryCount);
        
        return stats;
    }
}