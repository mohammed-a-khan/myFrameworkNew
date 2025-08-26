#!/bin/bash

echo "Fix: Ensuring custom HTML reports with screenshots are generated"

# Set encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Enable screenshot embedding
echo "cs.report.screenshots.embed=true" >> resources/config/application.properties

# Run tests and force report generation
echo "Running tests with forced report generation..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.report.screenshots.embed=true

# Manual report generation since @AfterSuite isn't running
echo ""
echo "Manually triggering report generation..."
cat > ForceReportGen.java << 'EOF'
import com.testforge.cs.reporting.CSReportManager;

public class ForceReportGen {
    public static void main(String[] args) {
        System.out.println("Manually generating custom HTML report...");
        try {
            // Set embedding property
            System.setProperty("cs.report.screenshots.embed", "true");
            
            // Force report generation
            CSReportManager.getInstance().generateReport();
            
            System.out.println("✓ Custom HTML report generation completed");
        } catch (Exception e) {
            System.err.println("✗ Report generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# Compile and run the manual report generator
javac -cp "target/classes:target/dependency/*" ForceReportGen.java
java -cp ".:target/classes:target/dependency/*" -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.report.screenshots.embed=true ForceReportGen

# Check results
echo ""
echo "=== RESULTS ==="
if find target -name "*cs_test_run_report*" 2>/dev/null; then
    echo "✓ Custom HTML reports found!"
    
    report_file=$(find target -name "cs_test_run_report.html" 2>/dev/null | head -1)
    if [[ -n "$report_file" ]]; then
        echo "Report location: $report_file"
        
        # Check for screenshot buttons
        if grep -q "onclick.*screenshot\|button.*screenshot\|View Screenshot" "$report_file" 2>/dev/null; then
            echo "✓ Screenshot buttons found in report!"
        else
            echo "? No screenshot buttons found (checking for embedded images...)"
            if grep -q "data:image.*base64" "$report_file"; then
                echo "✓ Embedded base64 screenshots found!"
            else
                echo "✗ No screenshots found in report"
            fi
        fi
        
        echo ""
        echo "Open this file in your browser to view screenshots: file://$PWD/$report_file"
    fi
else
    echo "✗ No custom reports generated"
    echo "Check target/cs-reports/ directory:"
    ls -la target/cs-reports/ 2>/dev/null || echo "Directory not found"
fi

# Clean up
rm -f ForceReportGen.java ForceReportGen.class
sed -i '/cs.report.screenshots.embed=true/d' resources/config/application.properties 2>/dev/null