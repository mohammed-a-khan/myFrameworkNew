#!/bin/bash

echo "========================================"
echo "Testing Screenshot Display in HTML Report"
echo "========================================"
echo ""

# Clean up old reports
echo "Cleaning up old reports..."
rm -rf cs-reports/test-run-*

# Run the test
echo "Running test with screenshots..."
mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -q

# Find the latest report
LATEST_REPORT=$(ls -dt cs-reports/test-run-* | head -1)

if [ -z "$LATEST_REPORT" ]; then
    echo "No report generated!"
    exit 1
fi

echo ""
echo "Report generated at: $LATEST_REPORT"
echo ""

# Check if screenshots exist
SCREENSHOT_COUNT=$(ls -1 "$LATEST_REPORT/screenshots/" 2>/dev/null | wc -l)
echo "Screenshots saved: $SCREENSHOT_COUNT"

# Check if screenshots are in the HTML
SCREENSHOTS_IN_HTML=$(grep -c "screenshots: \[{" "$LATEST_REPORT/index.html" 2>/dev/null || echo "0")
echo "Tests with screenshots in HTML: $SCREENSHOTS_IN_HTML"

# Check screenshot arrays
echo ""
echo "Checking screenshot arrays in HTML..."
grep -o "screenshots: \[[^]]*\]" "$LATEST_REPORT/index.html" | head -3

echo ""
echo "========================================"
if [ "$SCREENSHOTS_IN_HTML" -gt 0 ]; then
    echo "✓ SUCCESS: Screenshots are properly displayed in HTML report!"
else
    echo "✗ FAILURE: Screenshots not found in HTML report"
fi
echo "========================================"