import com.testforge.cs.screenshot.CSScreenshotUtils;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.utils.CSImageUtils;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class DebugScreenshotBase64Test {
    
    @Test
    public void compareActualScreenshotWithReportBase64() {
        try {
            System.out.println("🔍 DEBUGGING SCREENSHOT BASE64 COMPARISON");
            System.out.println("==========================================");
            
            // Step 1: Initialize WebDriver and capture a REAL screenshot
            System.out.println("1️⃣ Initializing WebDriver and taking real screenshot...");
            
            WebDriver driver = null;
            try {
                driver = CSWebDriverManager.getDriver();
                driver.get("https://www.google.com");
                Thread.sleep(2000);
                
                // Take actual screenshot using framework method
                byte[] screenshotBytes = CSScreenshotUtils.captureScreenshot(driver);
                System.out.println("   📷 Real screenshot captured: " + screenshotBytes.length + " bytes");
                
                // Convert to base64 manually (same way framework does it)
                String realBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(screenshotBytes);
                System.out.println("   🔗 Real base64 length: " + realBase64.length() + " characters");
                System.out.println("   🔗 Real base64 starts with: " + realBase64.substring(0, Math.min(100, realBase64.length())));
                
                // Step 2: Save the screenshot and test CSImageUtils
                File tempFile = new File("temp_screenshot.png");
                Files.write(tempFile.toPath(), screenshotBytes);
                
                String utilsBase64 = CSImageUtils.imageToBase64DataUri(tempFile.getAbsolutePath());
                System.out.println("");
                System.out.println("2️⃣ Testing CSImageUtils conversion...");
                System.out.println("   🔧 Utils base64 length: " + (utilsBase64 != null ? utilsBase64.length() : "NULL"));
                if (utilsBase64 != null) {
                    System.out.println("   🔧 Utils base64 starts with: " + utilsBase64.substring(0, Math.min(100, utilsBase64.length())));
                }
                
                // Step 3: Compare with what I put in the report
                String dummyBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYGAAAAABAAFKxtUrAAAAAElFTkSuQmCC";
                System.out.println("");
                System.out.println("3️⃣ Comparing with dummy base64 from report...");
                System.out.println("   🎭 Dummy base64 length: " + dummyBase64.length() + " characters");
                System.out.println("   🎭 Dummy base64 starts with: " + dummyBase64.substring(0, Math.min(100, dummyBase64.length())));
                
                // Step 4: Check what's actually in the generated report
                System.out.println("");
                System.out.println("4️⃣ Checking actual report content...");
                
                File latestReport = new File("cs-reports/test-run-20250825_194837/cs_test_run_report.html");
                if (latestReport.exists()) {
                    String reportContent = new String(Files.readAllBytes(latestReport.toPath()));
                    
                    // Find base64 images in report
                    String searchPattern = "data:image/png;base64,";
                    int index = reportContent.indexOf(searchPattern);
                    if (index != -1) {
                        // Extract the base64 string
                        int endIndex = reportContent.indexOf("\"", index);
                        if (endIndex != -1) {
                            String reportBase64 = reportContent.substring(index, endIndex);
                            System.out.println("   📋 Report base64 length: " + reportBase64.length() + " characters");
                            System.out.println("   📋 Report base64 starts with: " + reportBase64.substring(0, Math.min(100, reportBase64.length())));
                            
                            // CRITICAL COMPARISON
                            System.out.println("");
                            System.out.println("🎯 COMPARISON RESULTS:");
                            System.out.println("======================================");
                            System.out.println("Real screenshot base64 length: " + realBase64.length());
                            System.out.println("Report screenshot base64 length: " + reportBase64.length());
                            System.out.println("Match: " + realBase64.equals(reportBase64));
                            
                            if (!realBase64.equals(reportBase64)) {
                                System.out.println("");
                                System.out.println("❌ BASE64 STRINGS DON'T MATCH!");
                                System.out.println("🔍 This explains why you see blank screens!");
                                System.out.println("");
                                System.out.println("Real screenshot is: " + realBase64.length() + " chars");
                                System.out.println("Report has: " + reportBase64.length() + " chars");
                                
                                if (reportBase64.equals(dummyBase64)) {
                                    System.out.println("⚠️  Report contains the dummy base64 I generated!");
                                    System.out.println("    This means real screenshots aren't being captured in your tests");
                                }
                            }
                        }
                    } else {
                        System.out.println("   ❌ No base64 images found in report!");
                    }
                } else {
                    System.out.println("   ❌ Report file not found!");
                }
                
                // Step 5: Test the actual CSReportManager.fail() method
                System.out.println("");
                System.out.println("5️⃣ Testing CSReportManager.fail() screenshot capture...");
                
                // This should capture a real screenshot and add it to the report
                CSReportManager.fail("Testing real screenshot capture in fail method");
                System.out.println("   ✅ CSReportManager.fail() called with screenshot capture");
                
                // Clean up
                tempFile.delete();
                
                System.out.println("");
                System.out.println("🏁 DEBUG COMPLETE");
                System.out.println("=================");
                
            } finally {
                if (driver != null) {
                    CSWebDriverManager.quitDriver();
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Debug failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}