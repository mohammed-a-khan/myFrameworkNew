package com.testforge.cs.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.azuredevops.CSAzureDevOpsPublisher;
import com.testforge.cs.azuredevops.extractors.CSADOTagExtractor;
import com.testforge.cs.reporting.CSTestResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.*;
import org.testng.ITestResult;
import org.testng.ITestContext;
import org.testng.Assert;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Example test class showing different ways to tag tests for Azure DevOps
 * Demonstrates tag-based mapping similar to Playwright's feature file tags
 */
public class CSADOTaggedTest extends CSBaseTest {
    
    private CSAzureDevOpsPublisher adoPublisher;
    
    @BeforeClass
    public void setupADOIntegration(ITestContext context) {
        super.setupClass(context);
        
        // Initialize ADO publisher
        adoPublisher = CSAzureDevOpsPublisher.getInstance();
        
        if (adoPublisher.isEnabled()) {
            logger.info("Azure DevOps integration is enabled");
            
            // Start a new test run
            String runName = "ADO Tagged Test Run - " + new java.util.Date();
            adoPublisher.startTestRun(runName, null, "417");
        }
    }
    
    /**
     * Test using tags in description (like Playwright feature file tags)
     * The tags @TestCaseId:421 @TestPlanId:417 @TestSuiteId:418 are extracted
     */
    @Test(description = "@TestCaseId:421 @TestPlanId:417 @TestSuiteId:418 - User Profile Update Test")
    public void testUserProfileUpdate() {
        // Navigate to login page
        driver.get("https://opensource-demo.orangehrmlive.com/");
        
        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Login first
        performLogin(wait);
        
        // Navigate to My Info section
        By myInfoLink = By.xpath("//span[text()='My Info']");
        wait.until(ExpectedConditions.elementToBeClickable(myInfoLink));
        driver.findElement(myInfoLink).click();
        
        // Verify My Info page is loaded
        By personalDetailsHeader = By.xpath("//h6[text()='Personal Details']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(personalDetailsHeader));
        
        // Take screenshot
        takeScreenshot("ado_421_profile_page");
        
        logger.info("ADO Test Case 421 (from tags) completed successfully");
    }
    
    /**
     * Test using method name pattern for test case ID
     * The method name contains TestCase422 which is extracted
     */
    @Test(description = "@TestPlanId:417 @TestSuiteId:418 - Leave Request Test")
    public void testCase422_LeaveRequest() {
        // Navigate to login page
        driver.get("https://opensource-demo.orangehrmlive.com/");
        
        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Login first
        performLogin(wait);
        
        // Navigate to Leave section
        By leaveLink = By.xpath("//span[text()='Leave']");
        wait.until(ExpectedConditions.elementToBeClickable(leaveLink));
        driver.findElement(leaveLink).click();
        
        // Verify Leave page is loaded
        By leaveListHeader = By.xpath("//h5[text()='Leave List']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(leaveListHeader));
        
        // Take screenshot
        takeScreenshot("ado_422_leave_page");
        
        logger.info("ADO Test Case 422 (from method name) completed successfully");
    }
    
    /**
     * Helper method to perform login
     */
    private void performLogin(WebDriverWait wait) {
        By usernameField = By.xpath("//input[@name='username']");
        By passwordField = By.xpath("//input[@name='password']");
        By loginButton = By.xpath("//button[@type='submit']");
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
        
        // Enter credentials
        driver.findElement(usernameField).sendKeys("Admin");
        driver.findElement(passwordField).sendKeys("admin123");
        driver.findElement(loginButton).click();
        
        // Wait for dashboard
        By dashboardHeader = By.xpath("//h6[text()='Dashboard']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dashboardHeader));
    }
    
    @AfterMethod
    public void publishResultToADO(ITestResult result, Method method) {
        if (adoPublisher != null && adoPublisher.isEnabled()) {
            try {
                // Extract ADO metadata using the tag extractor
                CSADOTagExtractor.ADOMetadata adoMetadata = 
                    CSADOTagExtractor.extractADOMetadata(method, this.getClass());
                
                // Create enhanced test result with ADO mapping
                CSTestResult testResult = new CSTestResult();
                testResult.setTestName(method.getName());
                testResult.setClassName(this.getClass().getName());
                testResult.setMethodName(method.getName());
                testResult.setStartTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(result.getStartMillis()), 
                    java.time.ZoneId.systemDefault()));
                testResult.setEndTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(result.getEndMillis()), 
                    java.time.ZoneId.systemDefault()));
                testResult.setDuration(result.getEndMillis() - result.getStartMillis());
                
                // Set status based on TestNG result
                switch (result.getStatus()) {
                    case ITestResult.SUCCESS:
                        testResult.setStatus(CSTestResult.Status.PASSED);
                        break;
                    case ITestResult.FAILURE:
                        testResult.setStatus(CSTestResult.Status.FAILED);
                        if (result.getThrowable() != null) {
                            testResult.setErrorMessage(result.getThrowable().getMessage());
                            testResult.setStackTrace(getStackTrace(result.getThrowable()));
                        }
                        break;
                    case ITestResult.SKIP:
                        testResult.setStatus(CSTestResult.Status.SKIPPED);
                        break;
                    default:
                        testResult.setStatus(CSTestResult.Status.BROKEN);
                }
                
                // Add ADO metadata to test result
                if (testResult.getMetadata() == null) {
                    testResult.setMetadata(new HashMap<>());
                }
                
                // Add all extracted metadata
                Map<String, String> metadataMap = CSADOTagExtractor.toMetadataMap(adoMetadata);
                testResult.getMetadata().putAll(metadataMap);
                
                if (adoMetadata.hasTestCaseMapping()) {
                    logger.info("Publishing result for ADO test case: {} (Plan: {}, Suite: {})", 
                        adoMetadata.getTestCaseId(), 
                        adoMetadata.getTestPlanId(), 
                        adoMetadata.getTestSuiteId());
                }
                
                // Publish to ADO
                adoPublisher.publishTestResult(testResult);
                
            } catch (Exception e) {
                logger.error("Failed to publish result to ADO", e);
            }
        }
    }
    
    @AfterClass
    public void completeADOTestRun() {
        if (adoPublisher != null && adoPublisher.isEnabled()) {
            try {
                // Complete the test run
                adoPublisher.completeTestRun();
                logger.info("Azure DevOps test run completed");
            } catch (Exception e) {
                logger.error("Failed to complete ADO test run", e);
            }
        }
        
        super.teardownClass();
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}