#!/bin/bash

echo "=== Testing Screenshot Fix ==="

# Run a simple test that should generate different screenshots
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
    -Dtest=CSBDDRunner \
    -Dsurefire.suiteXmlFiles=suites/orangehrm-simple-only.xml \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.soft.fail.capture.screenshot=true \
    -q

echo ""
echo "Test completed. Let's check the latest report for different screenshots..."

# Find the latest report
LATEST_REPORT=$(find cs-reports -name "test-run-*" -type d | sort | tail -1)
REPORT_FILE="$LATEST_REPORT/cs_test_run_report.html"

if [ -f "$REPORT_FILE" ]; then
    echo "Found report: $REPORT_FILE"
    
    # Count unique base64 screenshots in the report
    UNIQUE_SCREENSHOTS=$(grep -o "data:image/png;base64,[^\"]*" "$REPORT_FILE" | sort | uniq | wc -l)
    TOTAL_SCREENSHOTS=$(grep -o "data:image/png;base64,[^\"]*" "$REPORT_FILE" | wc -l)
    
    echo "Total screenshot references: $TOTAL_SCREENSHOTS"
    echo "Unique screenshots found: $UNIQUE_SCREENSHOTS"
    
    if [ $UNIQUE_SCREENSHOTS -gt 1 ]; then
        echo "✅ SUCCESS: Multiple different screenshots found! The fix is working!"
    elif [ $UNIQUE_SCREENSHOTS -eq 1 ]; then
        echo "⚠️ ISSUE: Only 1 unique screenshot found - same screenshot is still being reused"
    else
        echo "❌ ERROR: No screenshots found in report"
    fi
    
    # Show first few characters of each unique screenshot
    echo ""
    echo "Screenshot data preview (first 50 chars of each unique screenshot):"
    grep -o "data:image/png;base64,[^\"]*" "$REPORT_FILE" | sort | uniq | cut -c1-50 | nl
else
    echo "ERROR: Report file not found: $REPORT_FILE"
fi