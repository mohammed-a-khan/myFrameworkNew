package com.testforge.cs.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.azuredevops.CSAzureDevOpsPublisher;
import com.testforge.cs.azuredevops.managers.CSTestSuiteManager;
import com.testforge.cs.azuredevops.models.CSTestPoint;
import com.testforge.cs.azuredevops.annotations.*;
import com.testforge.cs.azuredevops.extractors.CSADOTagExtractor;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.config.CSConfigManager;
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
 * Test class with Azure DevOps test case mapping using annotations
 * Uses @TestCaseId, @TestPlanId, @TestSuiteId annotations for mapping
 */
@TestPlanId(417)    // Test Plan ID at class level
@TestSuiteId(418)    // Test Suite ID at class level
public class CSADOMappedTest extends CSBaseTest {
    
    private CSAzureDevOpsPublisher adoPublisher;
    
    @BeforeClass
    public void setupADOIntegration(ITestContext context) {
        super.setupClass(context);
        
        // Initialize ADO publisher
        adoPublisher = CSAzureDevOpsPublisher.getInstance();
        
        if (adoPublisher.isEnabled()) {
            logger.info("Azure DevOps integration is enabled");
            
            // Start a new test run
            String runName = "ADO Mapped Test Run - " + new java.util.Date();
            adoPublisher.startTestRun(runName, null, "417");
            
            // Initialize test point mapping
            CSTestSuiteManager suiteManager = adoPublisher.getTestSuiteManager();
            if (suiteManager != null) {
                try {
                    // Pre-load test points for better mapping
                    suiteManager.initializeTestPoints();
                } catch (Exception e) {
                    logger.warn("Failed to initialize test points", e);
                }
            }
        }
    }
    
    @Test(description = "@TestCaseId:419 - Valid Login Test")
    @TestCaseId(419)  // Direct annotation for test case mapping
    public void testADOCase419_ValidLogin() {
        
        // Navigate to login page
        driver.get(config.getProperty("cs.orangehrm.url", "https://opensource-demo.orangehrmlive.com"));
        
        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Enter credentials using page object methods
        By usernameField = By.xpath("//input[@name='username']");
        By passwordField = By.xpath("//input[@name='password']");
        By loginButton = By.xpath("//button[@type='submit']");
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
        
        // Enter username
        WebElement username = driver.findElement(usernameField);
        username.clear();
        username.sendKeys(config.getProperty("cs.orangehrm.username", "Admin"));
        
        // Enter password
        WebElement password = driver.findElement(passwordField);
        password.clear();
        password.sendKeys(config.getProperty("cs.orangehrm.password", "admin123"));
        
        // Take screenshot before login
        takeScreenshot("ado_419_before_login");
        
        // Click login button
        driver.findElement(loginButton).click();
        
        // Verify dashboard is displayed
        By dashboardHeader = By.xpath("//h6[text()='Dashboard']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dashboardHeader));
        
        // Take success screenshot
        takeScreenshot("ado_419_success");
        
        logger.info("ADO Test Case 419 completed successfully");
    }
    
    @Test(description = "@TestCaseId:420 - Invalid Login Test")
    @TestCaseId(420)  // Direct annotation for test case mapping
    public void testADOCase420_InvalidLogin() {
        
        // Navigate to login page
        driver.get(config.getProperty("cs.orangehrm.url", "https://opensource-demo.orangehrmlive.com"));
        
        // Wait for page to load
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Enter invalid credentials
        By usernameField = By.xpath("//input[@name='username']");
        By passwordField = By.xpath("//input[@name='password']");
        By loginButton = By.xpath("//button[@type='submit']");
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
        
        // Enter invalid username
        WebElement username = driver.findElement(usernameField);
        username.clear();
        username.sendKeys("invalid");
        
        // Enter invalid password
        WebElement password = driver.findElement(passwordField);
        password.clear();
        password.sendKeys("wrongpassword");
        
        // Take screenshot before login
        takeScreenshot("ado_420_before_login");
        
        // Click login button
        driver.findElement(loginButton).click();
        
        // Verify error message is displayed
        By errorMessage = By.xpath("//p[contains(@class,'oxd-alert-content-text')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage));
        
        String errorText = driver.findElement(errorMessage).getText();
        
        // Take validation screenshot
        takeScreenshot("ado_420_validation");
        
        // Verify error message
        Assert.assertTrue(errorText != null && errorText.contains("Invalid credentials"),
            "Expected error message not found. Got: " + errorText);
        
        logger.info("ADO Test Case 420 completed successfully");
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