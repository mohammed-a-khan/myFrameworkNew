#!/bin/bash

echo "=== Testing Screenshot Capture Fix ==="

# Clean up any existing temp screenshots
echo "Cleaning up temp directories..."
find /tmp -name "*cs-temp*" -type d -exec rm -rf {} + 2>/dev/null || true

echo "Running orangehrm failure test with screenshot capture enabled..."

# Run the test with timeout and capture output
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 15s mvn test \
    -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.soft.fail.capture.screenshot=true \
    -Droot.loglevel=DEBUG \
    -q > test_output.log 2>&1

# Check temp directories for screenshots
echo ""
echo "=== Checking for Screenshots ==="
TEMP_DIRS=$(find /tmp -name "*cs-temp*" -type d 2>/dev/null)

if [ -n "$TEMP_DIRS" ]; then
    echo "Found temp screenshot directories:"
    for dir in $TEMP_DIRS; do
        echo "  $dir"
        ls -la "$dir" | grep -E "(SOFT_FAIL|HARD_FAILURE)" || echo "  No screenshots found"
    done
else
    echo "No temp screenshot directories found"
fi

# Check latest report
echo ""
echo "=== Checking Latest Report ==="
LATEST_REPORT=$(ls -t cs-reports/test-run-*/cs_test_run_report.html 2>/dev/null | head -1)
if [ -n "$LATEST_REPORT" ]; then
    REPORT_DIR=$(dirname "$LATEST_REPORT")
    echo "Latest report: $LATEST_REPORT"
    
    if [ -d "$REPORT_DIR/screenshots" ]; then
        echo "Screenshots in report:"
        ls -la "$REPORT_DIR/screenshots/" | head -10
    else
        echo "No screenshots directory in report"
    fi
else
    echo "No reports found"
fi

echo ""
echo "=== Test completed ==="
