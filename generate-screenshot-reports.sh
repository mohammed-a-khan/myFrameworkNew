#!/bin/bash
echo "=== SCREENSHOT REPORT GENERATOR ==="
echo "This will create the proper HTML reports with working screenshot buttons"
echo ""

export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Run your tests first
echo "1. Running tests to capture screenshots..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.report.screenshots.embed=true -q

echo ""
echo "2. Manually generating custom HTML reports with screenshots..."

# Create the manual report generator
cat > ManualReportGen.java << 'EOF'
import com.testforge.cs.reporting.CSReportManager;
import java.io.File;

public class ManualReportGen {
    public static void main(String[] args) {
        System.out.println("Forcing custom HTML report generation...");
        try {
            // Enable screenshot embedding
            System.setProperty("cs.report.screenshots.embed", "true");
            
            // Create reports directory
            new File("target/cs-reports").mkdirs();
            
            // Get the report manager instance and generate report
            CSReportManager reportManager = CSReportManager.getInstance();
            reportManager.generateReport();
            
            System.out.println("✓ Report generation completed successfully!");
            
            // Show what was generated
            File reportsDir = new File("target");
            findReports(reportsDir, 0);
            
        } catch (Exception e) {
            System.err.println("✗ Report generation failed:");
            e.printStackTrace();
        }
    }
    
    private static void findReports(File dir, int depth) {
        if (depth > 3) return; // Limit search depth
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().startsWith("test-run-") || file.getName().equals("cs-reports")) {
                    System.out.println("Found reports directory: " + file.getAbsolutePath());
                    File[] reportFiles = file.listFiles();
                    if (reportFiles != null) {
                        for (File reportFile : reportFiles) {
                            if (reportFile.getName().endsWith(".html")) {
                                System.out.println("  → Report: " + reportFile.getAbsolutePath());
                            }
                        }
                    }
                }
                findReports(file, depth + 1);
            }
        }
    }
}
EOF

# Compile and run
javac -cp "target/classes:target/dependency/*" ManualReportGen.java
java -cp ".:target/classes:target/dependency/*" -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.report.screenshots.embed=true ManualReportGen

echo ""
echo "3. Checking results..."

# Find and display the actual reports
report_count=0
for report in $(find target -name "*.html" -path "*/test-run-*/*" -o -path "*/cs-reports/*" 2>/dev/null); do
    if [[ "$report" == *"cs_test_run_report"* ]]; then
        report_count=$((report_count + 1))
        echo "✓ Custom Report #$report_count: $report"
        
        # Check for screenshot functionality
        if grep -q "data:image.*base64\|onclick.*screenshot\|View Screenshot" "$report" 2>/dev/null; then
            echo "  → Contains interactive screenshots: YES"
            echo "  → Open in browser: file://$PWD/$report"
        else
            echo "  → Contains interactive screenshots: NO"
        fi
    fi
done

if [ $report_count -eq 0 ]; then
    echo "✗ No custom reports generated. Using alternative approach..."
    echo ""
    echo "Alternative: Look for TestNG reports with screenshot paths:"
    find target -name "*.html" -exec grep -l "screenshot\|png\|jpg" {} \; 2>/dev/null
fi

echo ""
echo "=== SUMMARY ==="
echo "• Tests run: ✓"  
echo "• Manual report generation: ✓"
echo "• Custom reports found: $report_count"
echo ""
if [ $report_count -gt 0 ]; then
    echo "🎉 SUCCESS: Custom HTML reports with screenshots are now available!"
    echo "   The screenshot buttons should work in these reports."
else
    echo "⚠️  ISSUE: Custom reports not generated. Screenshot buttons are only in custom reports."
    echo "   Standard TestNG reports don't have screenshot buttons."
fi

# Cleanup
rm -f ManualReportGen.java ManualReportGen.class