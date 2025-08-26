package com.testforge.cs.listeners;

import com.testforge.cs.analysis.CSFailureAnalyzer;
import com.testforge.cs.annotations.CSRetry;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.utils.CSFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;

import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener for test execution events
 */
public class CSTestListener implements ITestListener, ISuiteListener, IInvokedMethodListener, IRetryAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CSTestListener.class);
    private static final CSConfigManager config = CSConfigManager.getInstance();
    private static final CSReportManager reportManager = CSReportManager.getInstance();
    
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();
    private final Map<String, CSTestResult> testResultMap = new ConcurrentHashMap<>();
    
    @Override
    public void onStart(ISuite suite) {
        logger.info("Suite started: {}", suite.getName());
        
        // Set suite parameters as system properties AND update config manager
        Map<String, String> parameters = suite.getXmlSuite().getParameters();
        parameters.forEach((key, value) -> {
            System.setProperty("suite." + key, value);
            // Also set in config manager for immediate availability
            config.setProperty(key, value);
            logger.info("Set suite parameter override: {} = {}", key, value);
        });
        
        logger.info("Suite parameters processed: {} parameters set", parameters.size());
    }
    
    @Override
    public void onFinish(ISuite suite) {
        logger.info("Suite finished: {}", suite.getName());
        
        // Log suite results
        ISuiteResult suiteResult = suite.getResults().values().iterator().next();
        ITestContext context = suiteResult.getTestContext();
        
        logger.info("Suite Summary - Passed: {}, Failed: {}, Skipped: {}",
                   context.getPassedTests().size(),
                   context.getFailedTests().size(),
                   context.getSkippedTests().size());
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        logger.info("Test started: {}", testName);
        
        // Create test result object
        CSTestResult testResult = new CSTestResult();
        testResult.setTestName(testName);
        testResult.setClassName(result.getTestClass().getName());
        testResult.setMethodName(result.getMethod().getMethodName());
        testResult.setStartTime(LocalDateTime.now());
        testResult.setStatus(CSTestResult.Status.RUNNING);
        
        // Store in thread-local map
        testResultMap.put(getTestId(result), testResult);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getTestName(result);
        logger.info("Test passed: {}", testName);
        
        // Update test result
        CSTestResult testResult = testResultMap.get(getTestId(result));
        if (testResult != null) {
            testResult.setStatus(CSTestResult.Status.PASSED);
            testResult.setEndTime(LocalDateTime.now());
            testResult.setDuration(result.getEndMillis() - result.getStartMillis());
            
            // Pass the test result to report manager
            try {
                reportManager.addTestResult(testResult);
                logger.info("Passed test result added to report manager: {}", testResult.getTestName());
            } catch (Exception e) {
                logger.error("Failed to add passed test result to report manager: {}", e.getMessage());
            }
        }
        
        // Clean up
        cleanupAfterTest(result);
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getTestName(result);
        logger.error("Test failed: {}", testName, result.getThrowable());
        
        // Update test result
        CSTestResult testResult = testResultMap.get(getTestId(result));
        
        if (testResult != null) {
            testResult.setStatus(CSTestResult.Status.FAILED);
            testResult.setEndTime(LocalDateTime.now());
            testResult.setDuration(result.getEndMillis() - result.getStartMillis());
            testResult.setErrorMessage(result.getThrowable().getMessage());
            testResult.setStackTrace(getStackTrace(result.getThrowable()));
            
            // Analyze the failure and categorize it
            CSFailureAnalyzer.FailureAnalysis analysis = CSFailureAnalyzer.analyzeFailure(
                result.getThrowable(), 
                testName, 
                result.getTestClass().getName()
            );
            testResult.setFailureAnalysis(analysis);
            
            // Log the analysis results
            logger.info("Failure Analysis for {}: Category={}, Flaky={}, Score={}", 
                testName, 
                analysis.getCategory().getDisplayName(),
                analysis.isFlaky(),
                String.format("%.2f", analysis.getFlakinessScore())
            );
            
            // Capture failure artifacts
            captureFailureArtifacts(result, testResult);
        }
        
        // CRITICAL: Pass the test result to report manager
        if (testResult != null) {
            try {
                reportManager.addTestResult(testResult);
                logger.info("Test result with failure artifacts added to report manager: {}", testResult.getTestName());
            } catch (Exception e) {
                logger.error("Failed to add test result to report manager: {}", e.getMessage());
            }
        }
        
        // Clean up
        cleanupAfterTest(result);
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getTestName(result);
        logger.warn("Test skipped: {}", testName);
        
        // Update test result
        CSTestResult testResult = testResultMap.get(getTestId(result));
        if (testResult != null) {
            testResult.setStatus(CSTestResult.Status.SKIPPED);
            testResult.setEndTime(LocalDateTime.now());
            if (result.getThrowable() != null) {
                testResult.setErrorMessage(result.getThrowable().getMessage());
            }
            
            // Pass the test result to report manager
            try {
                reportManager.addTestResult(testResult);
                logger.info("Skipped test result added to report manager: {}", testResult.getTestName());
            } catch (Exception e) {
                logger.error("Failed to add skipped test result to report manager: {}", e.getMessage());
            }
        }
        
        // Clean up
        cleanupAfterTest(result);
    }
    
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Not commonly used, but implemented for completeness
        onTestFailure(result);
    }
    
    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        String testName = getTestName(result);
        logger.error("Test failed with timeout: {}", testName);
        
        // Update test result
        CSTestResult testResult = testResultMap.get(getTestId(result));
        if (testResult != null) {
            testResult.setErrorMessage("Test execution timed out");
            
            // Timeout failures are usually flaky
            CSFailureAnalyzer.FailureAnalysis analysis = new CSFailureAnalyzer.FailureAnalysis();
            analysis.setCategory(CSFailureAnalyzer.FailureCategory.FLAKY_TIMEOUT);
            analysis.setRootCause("Test execution exceeded the configured timeout limit");
            analysis.addRecommendation("Increase test timeout in configuration");
            analysis.addRecommendation("Investigate if application performance has degraded");
            analysis.setFlakinessScore(0.8);
            testResult.setFailureAnalysis(analysis);
        }
        
        onTestFailure(result);
    }
    
    @Override
    public void onStart(ITestContext context) {
        logger.info("Test context started: {}", context.getName());
    }
    
    @Override
    public void onFinish(ITestContext context) {
        logger.info("Test context finished: {}", context.getName());
        
        // Log test context results
        logger.info("Context Summary - Passed: {}, Failed: {}, Skipped: {}",
                   context.getPassedTests().size(),
                   context.getFailedTests().size(),
                   context.getSkippedTests().size());
    }
    
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            logger.debug("Before test method: {}", method.getTestMethod().getMethodName());
            
            // Set retry analyzer if @CSRetry is present
            Method testMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
            if (testMethod.isAnnotationPresent(CSRetry.class)) {
                testResult.getMethod().setRetryAnalyzerClass(this.getClass());
            }
        }
    }
    
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            logger.debug("After test method: {}", method.getTestMethod().getMethodName());
        }
    }
    
    @Override
    public boolean retry(ITestResult result) {
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        CSRetry retryAnnotation = method.getAnnotation(CSRetry.class);
        
        if (retryAnnotation == null) {
            return false;
        }
        
        String testId = getTestId(result);
        int currentRetryCount = retryCountMap.getOrDefault(testId, 0);
        
        if (currentRetryCount < retryAnnotation.count()) {
            currentRetryCount++;
            retryCountMap.put(testId, currentRetryCount);
            
            logger.info("Retrying test: {} (attempt {} of {})",
                       getTestName(result), currentRetryCount, retryAnnotation.count());
            
            // Update test result for retry
            CSTestResult testResult = testResultMap.get(testId);
            if (testResult != null) {
                testResult.setStatus(CSTestResult.Status.RETRIED);
                testResult.setRetryCount(currentRetryCount);
            }
            
            // Wait before retry if specified
            if (retryAnnotation.delay() > 0) {
                try {
                    Thread.sleep(retryAnnotation.delay());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Capture failure artifacts
     */
    private void captureFailureArtifacts(ITestResult result, CSTestResult testResult) {
        try {
            // Take screenshot if browser is available
            if (CSWebDriverManager.getDriver() != null) {
                logger.info("Capturing failure artifacts for test: {}", result.getMethod().getMethodName());
                
                // Use temp directory like soft fails for consistency
                String tempDir = System.getProperty("java.io.tmpdir") + "/cs-temp-screenshots";
                new File(tempDir).mkdirs();
                
                // Use unique timestamp and thread ID to avoid conflicts - make sure it's different from soft fail
                String screenshotName = String.format("HARD_FAILURE_%s_Thread_%s_Time_%s.png",
                    result.getMethod().getMethodName(),
                    Thread.currentThread().getId(),
                    System.currentTimeMillis());
                String screenshotPath = tempDir + File.separator + screenshotName;
                
                // Log current page information for debugging
                try {
                    String currentUrl = CSWebDriverManager.getDriver().getCurrentUrl();
                    String pageTitle = CSWebDriverManager.getDriver().getTitle();
                    logger.info("Capturing failure screenshot - URL: {}, Title: {}", currentUrl, pageTitle);
                } catch (Exception e) {
                    logger.warn("Could not get page details for failure screenshot: {}", e.getMessage());
                }
                
                File screenshot = CSWebDriverManager.takeScreenshot(screenshotPath);
                if (screenshot != null && screenshot.exists()) {
                    logger.info("Failure screenshot captured: {}", screenshotPath);
                    
                    // Convert to optimized base64 like soft fails
                    try {
                        String base64Screenshot = null;
                        boolean enableCompression = config.getBooleanProperty("cs.screenshot.compression.enabled", true);
                        
                        if (enableCompression) {
                            int maxWidth = config.getIntegerProperty("cs.screenshot.max.width", 800);
                            float quality = config.getFloatProperty("cs.screenshot.quality", 0.7f);
                            
                            // Import CSImageUtils for compression
                            base64Screenshot = com.testforge.cs.utils.CSImageUtils.compressImageToBase64(
                                screenshot.getAbsolutePath(), maxWidth, quality);
                            
                            if (base64Screenshot != null) {
                                logger.info("Failure screenshot optimized for web display (size: {})", 
                                           com.testforge.cs.utils.CSImageUtils.getBase64Size(base64Screenshot));
                            }
                        }
                        
                        // Fallback to uncompressed if compression failed
                        if (base64Screenshot == null) {
                            byte[] fileContent = java.nio.file.Files.readAllBytes(screenshot.toPath());
                            base64Screenshot = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(fileContent);
                            logger.info("Failure screenshot converted to base64 (uncompressed)");
                        }
                        
                        // Set the base64 screenshot on the test result
                        testResult.setScreenshotPath(base64Screenshot);
                        testResult.addAttachment("Failure Screenshot", base64Screenshot);
                        logger.info("Base64 failure screenshot attached to test result");
                        
                    } catch (Exception e) {
                        logger.error("Failed to convert failure screenshot to base64: {}", e.getMessage());
                        // Fallback to file path
                        testResult.addAttachment("Failure Screenshot", screenshot.getAbsolutePath());
                        testResult.setScreenshotPath(screenshot.getAbsolutePath());
                    }
                } else {
                    logger.warn("Failed to capture failure screenshot");
                }
                
                // Page source capture is disabled - not needed for production reports
            } else {
                logger.warn("No WebDriver available for failure artifact capture");
            }
            
        } catch (Exception e) {
            logger.error("Failed to capture failure artifacts", e);
        }
    }
    
    /**
     * Cleanup after test
     */
    private void cleanupAfterTest(ITestResult result) {
        String testId = getTestId(result);
        
        // Remove from retry count map
        retryCountMap.remove(testId);
        
        // Remove from test result map (already added to report manager)
        testResultMap.remove(testId);
    }
    
    /**
     * Get test name from result
     */
    private String getTestName(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        
        // Include parameters if present
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            StringBuilder params = new StringBuilder();
            for (Object param : parameters) {
                if (params.length() > 0) {
                    params.append(", ");
                }
                params.append(param != null ? param.toString() : "null");
            }
            testName += "[" + params + "]";
        }
        
        return testName;
    }
    
    /**
     * Get unique test ID
     */
    private String getTestId(ITestResult result) {
        return result.getTestClass().getName() + "#" + 
               result.getMethod().getMethodName() + "#" + 
               System.identityHashCode(result);
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        // Include cause if present
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(getStackTrace(cause));
        }
        
        return sb.toString();
    }
}