import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSReportData;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.reporting.CSStepAction;
import com.testforge.cs.reporting.CSHtmlReportGenerator;
import com.testforge.cs.config.CSConfigManager;
import java.util.*;
import java.io.File;

public class TestReportGeneration {
    public static void main(String[] args) {
        System.out.println("Testing HTML report generation with screenshots...");
        
        try {
            // Set screenshot embedding mode
            System.setProperty("cs.report.screenshots.embed", "true");
            
            // Create a mock report data with screenshots
            CSReportData reportData = createMockReportWithScreenshots();
            
            // Generate the report
            CSHtmlReportGenerator generator = new CSHtmlReportGenerator();
            String reportDir = "target/test-reports";
            
            // Create target directory
            File targetDir = new File(reportDir);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            System.out.println("Generating HTML report...");
            String reportPath = generator.generateReport(reportData, reportDir);
            
            System.out.println("Report generated at: " + reportPath);
            
            // Check if report contains embedded screenshots
            if (reportPath != null && new File(reportPath).exists()) {
                System.out.println("✓ Report file exists");
                
                // Read and check for base64 screenshots
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(reportPath));
                
                if (content.contains("data:image")) {
                    System.out.println("✓ Report contains embedded screenshots!");
                    int count = content.split("data:image").length - 1;
                    System.out.println("Found " + count + " embedded images");
                    
                    // Extract first base64 and validate
                    String base64Pattern = "data:image/[^;]*;base64,([^\"'\\s>]*)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(base64Pattern);
                    java.util.regex.Matcher matcher = pattern.matcher(content);
                    
                    if (matcher.find()) {
                        String base64Data = matcher.group(1);
                        System.out.println("Sample base64 length: " + base64Data.length());
                        
                        // Try to decode
                        try {
                            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data);
                            System.out.println("✓ Base64 data is valid, decoded to " + decoded.length + " bytes");
                            
                            // Check if it's a valid image by looking at the header
                            if (decoded.length > 10) {
                                String header = String.format("%02x%02x%02x%02x", 
                                    decoded[0] & 0xff, decoded[1] & 0xff, decoded[2] & 0xff, decoded[3] & 0xff);
                                if (header.startsWith("89504e47")) {
                                    System.out.println("✓ Base64 data is a valid PNG image!");
                                } else if (header.startsWith("ffd8ff")) {
                                    System.out.println("✓ Base64 data is a valid JPEG image!");
                                } else {
                                    System.out.println("? Base64 data may not be a valid image (header: " + header + ")");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("✗ Base64 data is invalid: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("✗ Report does not contain embedded screenshots");
                    System.out.println("Report contains: " + content.substring(0, Math.min(500, content.length())));
                }
            } else {
                System.out.println("✗ Report file was not created");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static CSReportData createMockReportWithScreenshots() {
        CSReportData reportData = new CSReportData();
        reportData.setSuiteName("Test Suite");
        reportData.setStartTime(new Date());
        reportData.setEndTime(new Date());
        
        // Create a test result with screenshot
        CSTestResult testResult = new CSTestResult();
        testResult.setTestName("Mock Test");
        testResult.setStatus("PASSED");
        testResult.setStartTime(new Date());
        testResult.setEndTime(new Date());
        
        // Create a step action with screenshot
        CSStepAction stepAction = new CSStepAction();
        stepAction.setStepText("Take screenshot");
        stepAction.setStatus("PASS");
        stepAction.setTimestamp(new Date());
        
        // Create a simple mock screenshot (small PNG)
        byte[] mockScreenshot = createMockPngBytes();
        stepAction.setScreenshotData(mockScreenshot);
        stepAction.setScreenshotName("mock_screenshot.png");
        
        testResult.addStepAction(stepAction);
        reportData.addTestResult(testResult);
        
        return reportData;
    }
    
    private static byte[] createMockPngBytes() {
        // Create a minimal valid PNG (1x1 pixel, transparent)
        return new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,         // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,         // 1x1 dimensions
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,   // bit depth, color type, etc
            (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,   // IDAT chunk
            0x54, 0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,   // compressed data
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4, 0x00,  // more data
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE,  // IEND chunk
            0x42, 0x60, (byte)0x82
        };
    }
}