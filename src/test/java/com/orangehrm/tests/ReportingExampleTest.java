package com.orangehrm.tests;

import com.testforge.cs.core.CSBaseTest;
import com.testforge.cs.reporting.CSReportManager;
import com.orangehrm.pages.LoginPageNew;
import com.orangehrm.pages.DashboardPageNew;
import org.testng.annotations.Test;
import org.testng.Assert;
import java.time.LocalDateTime;

/**
 * Example test demonstrating custom reporting capabilities
 */
public class ReportingExampleTest extends CSBaseTest {
    
    @Test(description = "Demo test showing various reporting features")
    public void testWithCustomReporting() {
        // Test start logging
        reportManager.logInfo("═══════════════════════════════════════════════════════");
        reportManager.logInfo("TEST: Custom Reporting Demo");
        reportManager.logInfo("Started at: " + LocalDateTime.now());
        reportManager.logInfo("Environment: " + config.getProperty("environment.name"));
        reportManager.logInfo("Browser: " + config.getProperty("browser.name"));
        reportManager.logInfo("═══════════════════════════════════════════════════════");
        
        // Step 1: Navigate to login page
        reportManager.logInfo("STEP 1: Navigate to OrangeHRM login page");
        LoginPageNew loginPage = new LoginPageNew();
        loginPage.navigateTo();
        
        // Verify page loaded
        if (loginPage.isDisplayed()) {
            reportManager.logInfo("✓ PASS: Login page loaded successfully");
        } else {
            reportManager.logError("✗ FAIL: Login page failed to load");
        }
        
        // Step 2: Enter credentials
        reportManager.logInfo("STEP 2: Enter user credentials");
        String username = "Admin";
        String password = "admin123";
        
        reportManager.logInfo("  Username: " + username);
        reportManager.logInfo("  Password: " + "*".repeat(password.length()));
        
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        reportManager.logInfo("✓ Credentials entered successfully");
        
        // Step 3: Submit login
        reportManager.logInfo("STEP 3: Submit login form");
        loginPage.clickLogin();
        
        // Add wait for demo
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Step 4: Verify dashboard
        reportManager.logInfo("STEP 4: Verify successful login");
        DashboardPageNew dashboard = new DashboardPageNew();
        
        try {
            Assert.assertTrue(dashboard.isDisplayed(), "Dashboard should be displayed");
            reportManager.logInfo("✓ PASS: Login successful - Dashboard is displayed");
            
            // Log additional details
            String headerTitle = dashboard.getHeaderTitle();
            reportManager.logInfo("  Dashboard Title: " + headerTitle);
            reportManager.logInfo("  User: " + dashboard.getUserName());
            
        } catch (AssertionError e) {
            reportManager.logError("✗ FAIL: Dashboard verification failed");
            reportManager.logError("  Error: " + e.getMessage());
            reportManager.logError("  Current URL: " + driver.getCurrentUrl());
            throw e;
        }
        
        // Test completion
        reportManager.logInfo("═══════════════════════════════════════════════════════");
        reportManager.logInfo("TEST COMPLETED SUCCESSFULLY");
        reportManager.logInfo("═══════════════════════════════════════════════════════");
    }
    
    @Test(description = "Demo test with warnings and different log levels")
    public void testWithWarningsAndInfo() {
        reportManager.logInfo(">>> Starting test with various log levels");
        
        // Info messages
        reportManager.logInfo("ℹ️ INFO: This is an informational message");
        reportManager.logInfo("📋 Test configuration loaded from: application.properties");
        
        // Warning messages
        reportManager.logWarning("⚠️ WARNING: Using default timeout value (30s)");
        reportManager.logWarning("⚠️ WARNING: Test environment may have limited data");
        
        // Simulate different scenarios
        reportManager.logInfo("Checking system resources...");
        
        // Check memory (example)
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        
        reportManager.logInfo(String.format("Memory Usage: %d MB / %d MB", 
            totalMemory - freeMemory, totalMemory));
        
        if (freeMemory < 500) {
            reportManager.logWarning("⚠️ Low memory warning: Less than 500MB free");
        }
        
        // Error scenario (non-fatal)
        reportManager.logInfo("Attempting to connect to optional service...");
        reportManager.logError("❌ ERROR: Optional analytics service unavailable (non-critical)");
        reportManager.logInfo("ℹ️ Continuing test without analytics tracking");
        
        // Success message
        reportManager.logInfo("✅ SUCCESS: All critical services are operational");
        
        // Test data logging
        reportManager.logInfo("Test Data Summary:");
        reportManager.logInfo("  - Test ID: " + testResult.getTestId());
        reportManager.logInfo("  - Browser: " + testResult.getBrowser());
        reportManager.logInfo("  - Environment: " + testResult.getEnvironment());
        
        reportManager.logInfo("<<< Test completed with warnings");
    }
    
    @Test(description = "Demo test with structured logging")
    public void testWithStructuredLogging() {
        // Use structured format for easy parsing
        reportManager.logInfo("[TEST_START] testWithStructuredLogging");
        reportManager.logInfo("[CONFIG] browser=" + config.getProperty("browser.name"));
        reportManager.logInfo("[CONFIG] headless=" + config.getProperty("browser.headless"));
        
        // Action logging
        reportManager.logInfo("[ACTION] Navigate to login page");
        reportManager.logInfo("[RESULT] SUCCESS - Page loaded in 1.2s");
        
        reportManager.logInfo("[ACTION] Enter credentials");
        reportManager.logInfo("[INPUT] username=testuser");
        reportManager.logInfo("[INPUT] password=******");
        reportManager.logInfo("[RESULT] SUCCESS - Credentials entered");
        
        // Validation logging
        reportManager.logInfo("[VALIDATE] Check page title");
        reportManager.logInfo("[EXPECTED] OrangeHRM");
        reportManager.logInfo("[ACTUAL] OrangeHRM");
        reportManager.logInfo("[RESULT] PASS");
        
        // Performance logging
        reportManager.logInfo("[PERFORMANCE] Page load time: 1.2s");
        reportManager.logInfo("[PERFORMANCE] API response time: 345ms");
        reportManager.logInfo("[PERFORMANCE] Database query time: 67ms");
        
        // Test summary
        reportManager.logInfo("[SUMMARY] Test execution time: 5.4s");
        reportManager.logInfo("[SUMMARY] Assertions passed: 3/3");
        reportManager.logInfo("[SUMMARY] Screenshots captured: 2");
        
        reportManager.logInfo("[TEST_END] testWithStructuredLogging - PASSED");
    }
}