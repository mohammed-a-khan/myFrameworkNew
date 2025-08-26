#!/bin/bash
# Test both soft fail and actual failure screenshots

echo "=== Testing Both Screenshot Types ==="

# Clean up any existing screenshots
rm -rf /tmp/cs-temp-screenshots 2>/dev/null

# Run the test with the deliberate failure scenario
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 60s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.soft.fail.capture.screenshot=true \
  -Dcs.screenshot.compression.enabled=true \
  -Dcs.browser.headless=false \
  -q

echo ""
echo "=== Screenshot Analysis ==="

# Check temp screenshots (soft fails)
if [ -d "/tmp/cs-temp-screenshots" ]; then
    echo "✓ Soft fail screenshots found:"
    ls -la /tmp/cs-temp-screenshots/ | head -10
else
    echo "✗ No soft fail screenshots"
fi

# Check for failure logs
if [ -f "target/logs/cs-framework.log" ]; then
    echo ""
    echo "Checking for failure screenshot captures..."
    grep -i "failure.*screenshot\|capturing failure" target/logs/cs-framework.log | head -5
    
    echo ""
    echo "Checking for URL/page info during failures..."
    grep -i "capturing.*url\|failure.*url" target/logs/cs-framework.log | head -5
fi

echo ""
echo "=== Report Analysis ==="

# Check latest report
if [ -f "cs-reports/latest-report.html" ]; then
    # Get actual report path
    actual_report=$(grep -o 'url=[^"]*' cs-reports/latest-report.html | sed 's/url=//')
    if [ -n "$actual_report" ]; then
        report_path="cs-reports/$actual_report"
        if [ -f "$report_path" ]; then
            echo "Checking report: $report_path"
            
            # Count different types of screenshots
            soft_fail_count=$(grep -c "Custom user message" "$report_path" 2>/dev/null || echo 0)
            actual_fail_count=$(grep -c "data:image.*jpeg\|data:image.*png" "$report_path" 2>/dev/null || echo 0)
            total_images=$(grep -c "data:image" "$report_path" 2>/dev/null || echo 0)
            
            echo "Screenshots in report:"
            echo "  - Total base64 images: $total_images"
            echo "  - Soft fail messages: $soft_fail_count" 
            echo "  - Actual failure screenshots expected: $(($total_images - $soft_fail_count))"
            
            if [ $total_images -gt 0 ]; then
                echo "✓ Screenshots are embedded in the report!"
                
                # Check for different screenshot sources
                if grep -q "dashboard" "$report_path" 2>/dev/null; then
                    echo "✓ Dashboard content found in report"
                fi
                if grep -q "login" "$report_path" 2>/dev/null; then
                    echo "✓ Login content found in report" 
                fi
            else
                echo "✗ No screenshots found in report"
            fi
        fi
    fi
fi

echo ""
echo "Test completed. Check the generated report for both soft fail and actual failure screenshots."