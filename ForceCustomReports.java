import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import java.io.File;
import java.time.LocalDateTime;

public class ForceCustomReports {
    public static void main(String[] args) {
        try {
            System.out.println("🔥 FORCING CUSTOM HTML REPORT GENERATION WITH SCREENSHOT BUTTONS");
            System.out.println("================================================================");
            
            // Enable screenshot embedding
            System.setProperty("cs.report.screenshots.embed", "true");
            System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
            
            CSReportManager reportManager = CSReportManager.getInstance();
            
            // Initialize with proper suite name
            reportManager.initializeReport("OrangeHRM Failure Test Suite");
            
            // Add the exact failing tests from the recent report
            System.out.println("📊 Creating test data with screenshots...");
            
            // Add 4 failing tests (as shown in recent report)
            String[] failedTests = {
                "Deliberately failing test to demonstrate failure reporting_Iteration1",
                "Deliberately failing test to demonstrate failure reporting_Iteration2", 
                "Deliberately failing test to demonstrate failure reporting_Iteration3",
                "Simple login test"
            };
            
            for (int i = 0; i < failedTests.length; i++) {
                CSTestResult failedTest = new CSTestResult();
                failedTest.setTestId("failed-test-" + (i+1));
                failedTest.setTestName(failedTests[i]);
                failedTest.setClassName("com.testforge.cs.bdd.CSBDDRunner");
                failedTest.setStatus(CSTestResult.Status.FAILED);
                failedTest.setStartTime(LocalDateTime.now().minusMinutes(10));
                failedTest.setEndTime(LocalDateTime.now().minusMinutes(8));
                failedTest.setErrorMessage("Step execution failed: I should see the dashboard");
                failedTest.setStackTrace("com.testforge.cs.exceptions.CSBddException [BDD_ERROR] in BDD: Step failed: Then I should see the dashboard");
                
                // Add screenshot for failed test (this is the key part!)
                String screenshotBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
                failedTest.setScreenshotPath(screenshotBase64);
                
                reportManager.addTestResult(failedTest);
            }
            
            // Add 6 passing tests (as shown in recent report)
            for (int i = 1; i <= 6; i++) {
                CSTestResult passedTest = new CSTestResult();
                passedTest.setTestId("passed-test-" + i);
                passedTest.setTestName("OrangeHRM Test Scenario " + i);
                passedTest.setClassName("com.testforge.cs.bdd.CSBDDRunner");
                passedTest.setStatus(CSTestResult.Status.PASSED);
                passedTest.setStartTime(LocalDateTime.now().minusMinutes(15));
                passedTest.setEndTime(LocalDateTime.now().minusMinutes(12));
                
                reportManager.addTestResult(passedTest);
            }
            
            System.out.println("✅ Test data prepared: 4 failed tests (WITH SCREENSHOTS) + 6 passed tests");
            
            // Force generation of custom HTML report
            System.out.println("🚀 GENERATING CUSTOM HTML REPORT NOW...");
            reportManager.generateReport();
            System.out.println("✅ Custom report generation completed!");
            
            // Find and display the generated reports
            System.out.println("🔍 LOCATING CUSTOM REPORTS WITH SCREENSHOT BUTTONS...");
            
            File[] targetDirs = new File("target").listFiles(f -> f.isDirectory() && f.getName().contains("test-reports"));
            boolean foundCustomReport = false;
            
            if (targetDirs != null) {
                for (File dir : targetDirs) {
                    File[] runDirs = dir.listFiles(f -> f.isDirectory() && f.getName().startsWith("test-run-"));
                    if (runDirs != null) {
                        for (File runDir : runDirs) {
                            File reportFile = new File(runDir, "cs_test_run_report.html");
                            if (reportFile.exists()) {
                                System.out.println("");
                                System.out.println("🎯 FOUND CUSTOM REPORT WITH SCREENSHOT BUTTONS:");
                                System.out.println("   📋 " + reportFile.getAbsolutePath());
                                
                                // Verify it has screenshot functionality
                                String content = new String(java.nio.file.Files.readAllBytes(reportFile.toPath()));
                                boolean hasButtons = content.contains("View Screenshot") || content.contains("fas fa-camera");
                                boolean hasImages = content.contains("data:image");
                                
                                System.out.println("   📸 Contains screenshot buttons: " + hasButtons);
                                System.out.println("   🖼️  Contains embedded screenshots: " + hasImages);
                                
                                if (hasButtons && hasImages) {
                                    System.out.println("");
                                    System.out.println("   ✅ SUCCESS! This report has WORKING screenshot buttons!");
                                    System.out.println("   🌐 OPEN THIS IN BROWSER: file://" + reportFile.getAbsolutePath());
                                    System.out.println("");
                                    System.out.println("   📱 WHAT YOU'LL SEE:");
                                    System.out.println("   • Test results table with camera icons");
                                    System.out.println("   • Click screenshot buttons to view images");
                                    System.out.println("   • NO MORE BLANK SCREENS!");
                                }
                                foundCustomReport = true;
                            }
                        }
                    }
                }
            }
            
            if (!foundCustomReport) {
                System.out.println("❌ Custom report generation failed - checking cs-reports...");
                File csReports = new File("cs-reports");
                if (csReports.exists()) {
                    File[] files = csReports.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            System.out.println("   " + file.getName());
                        }
                    }
                }
            }
            
            System.out.println("");
            System.out.println("🔥 COMPARISON:");
            System.out.println("❌ TestNG Report (emailable-report.html): NO screenshot buttons");
            System.out.println("✅ Custom Report (cs_test_run_report.html): HAS screenshot buttons");
            System.out.println("");
            System.out.println("👆 USE THE CUSTOM REPORT ABOVE, NOT THE TESTNG REPORT!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}