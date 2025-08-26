import com.testforge.cs.utils.CSImageUtils;
import com.testforge.cs.screenshot.CSScreenshotUtils;
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Base64ComparisonTest {
    
    @Test
    public void compareBase64Strings() {
        try {
            System.out.println("🔍 COMPARING FRAMEWORK BASE64 VS REPORT BASE64");
            System.out.println("================================================");
            
            // Use specific test run with known screenshots
            File latestTestRun = new File("cs-reports/test-run-20250825_194748");
            System.out.println("📁 Using test run: " + latestTestRun.getName());
            
            // Look for screenshot files
            File screenshotsDir = new File(latestTestRun, "screenshots");
            if (!screenshotsDir.exists()) {
                System.out.println("❌ No screenshots directory found in " + latestTestRun.getName());
                return;
            }
            
            File[] screenshots = screenshotsDir.listFiles(f -> f.getName().endsWith(".png"));
            if (screenshots == null || screenshots.length == 0) {
                System.out.println("❌ No screenshot files found");
                return;
            }
            
            // Use the first screenshot
            File screenshotFile = screenshots[0];
            System.out.println("📸 Using screenshot: " + screenshotFile.getName());
            System.out.println("📏 Screenshot file size: " + CSImageUtils.getFileSize(screenshotFile.getAbsolutePath()));
            
            // Generate base64 using framework method
            System.out.println("\n🔧 Generating base64 using framework method...");
            String frameworkBase64 = CSImageUtils.imageToBase64DataUri(screenshotFile.getAbsolutePath());
            
            if (frameworkBase64 == null) {
                System.out.println("❌ Framework failed to generate base64");
                return;
            }
            
            System.out.println("✅ Framework base64 generated");
            System.out.println("📏 Framework base64 length: " + frameworkBase64.length() + " characters");
            System.out.println("🎯 Framework base64 prefix: " + frameworkBase64.substring(0, Math.min(100, frameworkBase64.length())));
            
            // Now extract base64 from the actual HTML report
            System.out.println("\n🔍 Extracting base64 from HTML report...");
            File reportFile = new File(latestTestRun, "cs_test_run_report.html");
            if (!reportFile.exists()) {
                System.out.println("❌ Report file not found: " + reportFile.getAbsolutePath());
                return;
            }
            
            String reportContent = new String(Files.readAllBytes(reportFile.toPath()));
            
            // Find base64 strings in the report
            Pattern base64Pattern = Pattern.compile("data:image/[^;]+;base64,([A-Za-z0-9+/=]+)");
            Matcher matcher = base64Pattern.matcher(reportContent);
            
            if (matcher.find()) {
                String reportBase64 = matcher.group(0); // Full data URI
                System.out.println("✅ Found base64 in report");
                System.out.println("📏 Report base64 length: " + reportBase64.length() + " characters");
                System.out.println("🎯 Report base64 prefix: " + reportBase64.substring(0, Math.min(100, reportBase64.length())));
                
                // Compare them
                System.out.println("\n🔄 COMPARISON RESULTS:");
                System.out.println("======================");
                System.out.println("Framework length: " + frameworkBase64.length());
                System.out.println("Report length:    " + reportBase64.length());
                System.out.println("Lengths match:    " + (frameworkBase64.length() == reportBase64.length()));
                System.out.println("Content match:    " + frameworkBase64.equals(reportBase64));
                
                if (!frameworkBase64.equals(reportBase64)) {
                    System.out.println("\n❌ BASE64 STRINGS DON'T MATCH!");
                    System.out.println("This explains why screenshots don't display properly.");
                    
                    // Write both to files for detailed comparison
                    writeToFile("framework-base64.txt", frameworkBase64);
                    writeToFile("report-base64.txt", reportBase64);
                    System.out.println("📝 Written both strings to files for comparison");
                    
                    // Show first 500 characters of each for debugging
                    System.out.println("\n🔍 DETAILED COMPARISON (first 500 chars):");
                    System.out.println("Framework: " + frameworkBase64.substring(0, Math.min(500, frameworkBase64.length())));
                    System.out.println("Report:    " + reportBase64.substring(0, Math.min(500, reportBase64.length())));
                } else {
                    System.out.println("\n✅ BASE64 STRINGS MATCH PERFECTLY");
                    System.out.println("The issue is not with base64 generation.");
                    System.out.println("The problem is that the base64 string is TOO LARGE for browsers to handle.");
                    System.out.println("Current size: " + frameworkBase64.length() + " characters");
                    System.out.println("Browser limit: ~10,000-20,000 characters");
                    System.out.println("We need to compress/resize images before base64 conversion.");
                }
                
            } else {
                System.out.println("❌ No base64 strings found in report");
                System.out.println("Report might be using different format or no screenshots embedded");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Comparison failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void writeToFile(String filename, String content) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
            System.out.println("📝 Written to: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to write " + filename + ": " + e.getMessage());
        }
    }
}