#!/bin/bash

echo "🔍 DEBUGGING CUSTOM REPORT GENERATION"
echo "===================================="

export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Step 1: Run tests to generate data for reports
echo "1️⃣ Running tests to populate CSReportManager..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 25s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.report.screenshots.embed=true \
  -q

echo ""
echo "2️⃣ Creating comprehensive debug report generation..."

# Step 2: Create debug report generator that shows what's happening
cat > DebugReportGeneration.java << 'EOF'
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.reporting.CSReportData;
import com.testforge.cs.reporting.CSHtmlReportGenerator;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

public class DebugReportGeneration {
    public static void main(String[] args) {
        try {
            System.out.println("🔧 DEBUG: Starting report generation investigation...");
            
            // Enable screenshot embedding
            System.setProperty("cs.report.screenshots.embed", "true");
            System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
            
            CSReportManager reportManager = CSReportManager.getInstance();
            
            // Check if we have any test results
            Collection<CSTestResult> testResults = reportManager.getAllTestResults();
            System.out.println("🧪 Test results in CSReportManager: " + testResults.size());
            
            if (testResults.isEmpty()) {
                System.out.println("❌ No test results found! This explains why custom reports aren't generated.");
                System.out.println("   The @AfterSuite method requires test data to generate reports.");
                
                // Create sample test data to demonstrate report generation
                System.out.println("🎯 Creating sample test data to show report generation works...");
                
                reportManager.initializeReport("Debug Test Suite");
                
                // Add sample failing test with screenshot
                CSTestResult failedTest = new CSTestResult();
                failedTest.setTestId("debug-test-1");
                failedTest.setTestName("Sample Failed Test");
                failedTest.setClassName("DebugTestClass");
                failedTest.setStatus(CSTestResult.TestStatus.FAILED);
                failedTest.setStartTime(LocalDateTime.now().minusMinutes(2));
                failedTest.setEndTime(LocalDateTime.now().minusMinutes(1));
                failedTest.setError("This is a sample failure for testing screenshot buttons");
                failedTest.setScreenshotPath("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="); // Tiny sample image
                
                reportManager.addTestResult(failedTest);
                
                // Add sample passing test
                CSTestResult passedTest = new CSTestResult();
                passedTest.setTestId("debug-test-2");
                passedTest.setTestName("Sample Passed Test");
                passedTest.setClassName("DebugTestClass");
                passedTest.setStatus(CSTestResult.TestStatus.PASSED);
                passedTest.setStartTime(LocalDateTime.now().minusMinutes(1));
                passedTest.setEndTime(LocalDateTime.now());
                
                reportManager.addTestResult(passedTest);
                
                System.out.println("✅ Sample test data created");
            } else {
                System.out.println("✅ Found test results, checking their data...");
                int testsWithScreenshots = 0;
                for (CSTestResult result : testResults) {
                    System.out.println("  📊 Test: " + result.getTestName() + 
                        " Status: " + result.getStatus() + 
                        " Screenshot: " + (result.getScreenshotPath() != null ? "YES" : "NO"));
                    if (result.getScreenshotPath() != null) {
                        testsWithScreenshots++;
                    }
                }
                System.out.println("  📷 Tests with screenshots: " + testsWithScreenshots);
            }
            
            // Create reports directory
            File reportsDir = new File("target/cs-reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
                System.out.println("📁 Created reports directory");
            }
            
            System.out.println("🚀 Generating custom HTML report...");
            reportManager.generateReport();
            
            System.out.println("✅ Report generation completed!");
            
            // Find generated reports
            System.out.println("🔍 Searching for generated reports...");
            
            // Look for cs_test_run_report.html files
            File[] reportDirs = new File("target").listFiles(f -> 
                f.isDirectory() && f.getName().contains("test-reports"));
            
            if (reportDirs != null && reportDirs.length > 0) {
                for (File dir : reportDirs) {
                    File[] runDirs = dir.listFiles(f -> 
                        f.isDirectory() && f.getName().startsWith("test-run-"));
                    if (runDirs != null) {
                        for (File runDir : runDirs) {
                            File reportFile = new File(runDir, "cs_test_run_report.html");
                            if (reportFile.exists()) {
                                System.out.println("📋 Found custom report: " + reportFile.getAbsolutePath());
                                
                                // Check if it contains screenshot buttons
                                String content = new String(java.nio.file.Files.readAllBytes(reportFile.toPath()));
                                boolean hasScreenshotButtons = content.contains("View Screenshot") || 
                                                             content.contains("fas fa-camera");
                                System.out.println("   📸 Has screenshot buttons: " + hasScreenshotButtons);
                                
                                boolean hasEmbeddedImages = content.contains("data:image");
                                System.out.println("   🖼️  Has embedded images: " + hasEmbeddedImages);
                                
                                System.out.println("   🌐 View in browser: file://" + reportFile.getAbsolutePath());
                            }
                        }
                    }
                }
            } else {
                System.out.println("❌ No report directories found!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Report generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# Compile and run
echo "3️⃣ Compiling debug report generator..."
javac -cp "target/classes:target/dependency/*" DebugReportGeneration.java

echo ""
echo "4️⃣ Running debug report generation..."
java -cp ".:target/classes:target/dependency/*" \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.report.screenshots.embed=true \
  DebugReportGeneration

echo ""
echo "🏁 DEBUG SUMMARY"
echo "================"

# Check results
custom_reports=$(find target -name "cs_test_run_report.html" 2>/dev/null)
if [[ -n "$custom_reports" ]]; then
    echo "✅ SUCCESS: Custom HTML reports generated!"
    echo "$custom_reports" | while read report; do
        echo "   📋 $report"
        if grep -q "View Screenshot" "$report" 2>/dev/null; then
            echo "   📸 Contains screenshot buttons: YES"
        else
            echo "   📸 Contains screenshot buttons: NO"
        fi
    done
else
    echo "❌ No custom reports generated"
    echo "   This means the issue is in the report generation process"
fi

# Cleanup
rm -f DebugReportGeneration.java DebugReportGeneration.class