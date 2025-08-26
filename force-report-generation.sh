#!/bin/bash

echo "Testing screenshot embedding by forcing report generation"

# Set encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Clean up old reports and ensure directories exist
rm -rf target/cs-reports target/test-run-*
mkdir -p target/cs-reports

# Enable screenshot embedding
echo "Enabling screenshot embedding..."
echo "cs.report.screenshots.embed=true" >> resources/config/application.properties

# Run a test but let it complete properly (no timeout)
echo "Running test to completion to ensure @AfterSuite runs..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.report.screenshots.embed=true -q

# Check if custom reports were generated
echo ""
echo "=== Custom Report Generation Results ==="
echo "Checking for custom HTML reports:"

if find target -name "*cs_test_run_report*" -o -name "test-run-*" -type d 2>/dev/null; then
    echo "✓ Custom reports found!"
    
    # Find the actual report file
    report_file=$(find target -name "cs_test_run_report.html" 2>/dev/null | head -1)
    
    if [[ -n "$report_file" ]]; then
        echo "Found report: $report_file"
        
        # Check for embedded screenshots (base64 data)
        echo "Checking for embedded screenshots in report..."
        if grep -q "data:image.*base64" "$report_file"; then
            echo "✓ Base64 embedded images found!"
            
            # Count embedded images
            image_count=$(grep -o "data:image.*base64" "$report_file" | wc -l)
            echo "Found $image_count embedded screenshots"
            
            # Extract and validate one base64 image
            base64_data=$(grep -o 'data:image/[^;]*;base64,[^"]*' "$report_file" | head -1)
            if [[ -n "$base64_data" ]]; then
                # Extract just the base64 part
                pure_base64=$(echo "$base64_data" | sed 's/.*base64,//')
                echo "Sample base64 length: ${#pure_base64}"
                
                # Try to decode and validate
                echo "$pure_base64" | base64 -d > /tmp/test_embedded.png 2>/dev/null
                if file /tmp/test_embedded.png | grep -i "image data"; then
                    echo "✓ Embedded base64 data is VALID image!"
                    file /tmp/test_embedded.png
                else
                    echo "✗ Embedded base64 data is NOT valid image"
                fi
                rm -f /tmp/test_embedded.png
            fi
            
        else
            echo "✗ No base64 embedded images found in report"
            echo "Report contains these image references:"
            grep -i "img\|screenshot" "$report_file" | head -3
        fi
        
        # Show report path
        echo ""
        echo "Report generated at: $report_file"
        echo "File size: $(stat -f%z "$report_file" 2>/dev/null || stat -c%s "$report_file" 2>/dev/null) bytes"
        
    else
        echo "✗ No cs_test_run_report.html found"
    fi
    
else
    echo "✗ No custom reports generated"
    
    # Check for any report generation logs
    echo "Checking for report generation logs..."
    if ls target/surefire-reports/*.txt >/dev/null 2>&1; then
        grep -i "teardownSuite\|generateReport\|AfterSuite" target/surefire-reports/*.txt || echo "No @AfterSuite logs found"
    fi
fi

# Clean up the property we added
sed -i '/cs.report.screenshots.embed=true/d' resources/config/application.properties