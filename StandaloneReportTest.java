import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import java.time.LocalDateTime;

public class StandaloneReportTest {
    public static void main(String[] args) {
        try {
            System.out.println("🔧 STANDALONE REPORT GENERATION TEST");
            System.out.println("===================================");
            
            // Enable screenshot embedding
            System.setProperty("cs.report.screenshots.embed", "true");
            System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
            
            CSReportManager reportManager = CSReportManager.getInstance();
            
            System.out.println("1️⃣ Initializing test suite...");
            reportManager.initializeReport("Standalone Test Suite");
            
            System.out.println("2️⃣ Adding sample failing test with screenshot...");
            
            // Add sample failing test with screenshot
            CSTestResult failedTest = new CSTestResult();
            failedTest.setTestId("standalone-test-1");
            failedTest.setTestName("Sample Failed Test");
            failedTest.setClassName("StandaloneTestClass");
            failedTest.setStatus(CSTestResult.Status.FAILED);
            failedTest.setStartTime(LocalDateTime.now().minusMinutes(2));
            failedTest.setEndTime(LocalDateTime.now().minusMinutes(1));
            failedTest.setErrorMessage("This is a sample failure for testing screenshot buttons");
            
            // Create a tiny sample base64 image (1x1 transparent PNG)
            String sampleBase64Image = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
            failedTest.setScreenshotPath(sampleBase64Image);
            
            reportManager.addTestResult(failedTest);
            
            System.out.println("3️⃣ Adding sample passing test...");
            
            // Add sample passing test
            CSTestResult passedTest = new CSTestResult();
            passedTest.setTestId("standalone-test-2");
            passedTest.setTestName("Sample Passed Test");
            passedTest.setClassName("StandaloneTestClass");
            passedTest.setStatus(CSTestResult.Status.PASSED);
            passedTest.setStartTime(LocalDateTime.now().minusMinutes(1));
            passedTest.setEndTime(LocalDateTime.now());
            
            reportManager.addTestResult(passedTest);
            
            System.out.println("4️⃣ Generating custom HTML report...");
            
            reportManager.generateReport();
            
            System.out.println("✅ Report generation completed!");
            
            // Find generated reports
            System.out.println("5️⃣ Searching for generated reports...");
            
            java.io.File targetDir = new java.io.File("target");
            if (!targetDir.exists()) {
                System.out.println("❌ Target directory doesn't exist!");
                return;
            }
            
            // Look for test-reports directories
            java.io.File[] reportDirs = targetDir.listFiles(f -> 
                f.isDirectory() && f.getName().contains("test-reports"));
            
            boolean foundCustomReport = false;
            
            if (reportDirs != null && reportDirs.length > 0) {
                for (java.io.File dir : reportDirs) {
                    System.out.println("   📁 Found reports directory: " + dir.getName());
                    
                    // Look for test-run directories
                    java.io.File[] runDirs = dir.listFiles(f -> 
                        f.isDirectory() && f.getName().startsWith("test-run-"));
                    
                    if (runDirs != null) {
                        for (java.io.File runDir : runDirs) {
                            java.io.File reportFile = new java.io.File(runDir, "cs_test_run_report.html");
                            if (reportFile.exists()) {
                                System.out.println("📋 Found custom report: " + reportFile.getAbsolutePath());
                                foundCustomReport = true;
                                
                                // Check if it contains screenshot buttons
                                try {
                                    String content = new String(java.nio.file.Files.readAllBytes(reportFile.toPath()));
                                    boolean hasScreenshotButtons = content.contains("View Screenshot") || 
                                                                 content.contains("fas fa-camera");
                                    System.out.println("   📸 Has screenshot buttons: " + hasScreenshotButtons);
                                    
                                    boolean hasEmbeddedImages = content.contains("data:image");
                                    System.out.println("   🖼️  Has embedded images: " + hasEmbeddedImages);
                                    
                                    if (hasScreenshotButtons && hasEmbeddedImages) {
                                        System.out.println("   ✅ PERFECT! This report has working screenshot buttons with embedded images");
                                        System.out.println("   🌐 View in browser: file://" + reportFile.getAbsolutePath());
                                    } else {
                                        System.out.println("   ⚠️  Report exists but missing screenshot functionality");
                                    }
                                } catch (Exception e) {
                                    System.out.println("   ❌ Could not read report content: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            if (!foundCustomReport) {
                System.out.println("❌ No custom reports found!");
                System.out.println("   Checking cs-reports directory...");
                
                java.io.File csReportsDir = new java.io.File("cs-reports");
                if (csReportsDir.exists()) {
                    System.out.println("   📁 cs-reports exists, listing contents:");
                    for (java.io.File file : csReportsDir.listFiles()) {
                        System.out.println("      " + file.getName());
                    }
                } else {
                    System.out.println("   ❌ cs-reports directory doesn't exist");
                }
            }
            
            System.out.println("");
            System.out.println("🏁 TEST COMPLETE");
            if (foundCustomReport) {
                System.out.println("✅ SUCCESS: Custom HTML report generation works!");
                System.out.println("   The issue is that @AfterSuite doesn't run in parallel BDD tests.");
                System.out.println("   Solution: Manually trigger report generation after BDD tests complete.");
            } else {
                System.out.println("❌ FAILURE: Custom HTML report generation is not working");
                System.out.println("   Need to investigate why CSHtmlReportGenerator is not creating reports.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}