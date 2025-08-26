#!/bin/bash

echo "=== DEBUGGING SCREENSHOT CAPTURE ==="
echo "Testing if screenshots are actually being taken..."

# Set encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Clean up old screenshots
rm -rf target/screenshots cs-reports/screenshots target/cs-reports

# Run test with debug logging for screenshots
echo "Running test with screenshot debugging..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.report.screenshots.embed=false \
  -Droot.loglevel=DEBUG \
  -q 2>&1 | grep -i "screenshot\|Taking screenshot\|captureScreenshot\|saved to" | tee screenshot-debug.log

echo ""
echo "=== SCREENSHOT CAPTURE LOGS ==="
if [[ -s screenshot-debug.log ]]; then
    echo "Found screenshot logs:"
    cat screenshot-debug.log
else
    echo "No screenshot logs found!"
fi

echo ""
echo "=== CHECKING FOR SCREENSHOT FILES ==="

# Check various locations for screenshots
echo "Checking target/screenshots:"
ls -la target/screenshots/ 2>/dev/null || echo "  Directory not found"

echo ""
echo "Checking cs-reports/screenshots:"
ls -la cs-reports/screenshots/ 2>/dev/null || echo "  Directory not found"

echo ""
echo "Checking target/cs-reports:"
find target/cs-reports -name "*.png" -o -name "*.jpg" 2>/dev/null || echo "  No images found"

echo ""
echo "Checking entire target directory for PNG files:"
find target -name "*.png" 2>/dev/null | head -10 || echo "  No PNG files found"

echo ""
echo "=== CHECKING REPORT CONTENT ==="
# Check if reports mention screenshots
if ls target/surefire-reports/*.html >/dev/null 2>&1; then
    echo "Checking for screenshot references in reports:"
    grep -l "screenshot\|png\|jpg" target/surefire-reports/*.html 2>/dev/null | head -5 || echo "  No screenshot references found"
fi

echo ""
echo "=== SUMMARY ==="
screenshot_count=$(find target -name "*.png" 2>/dev/null | wc -l)
echo "Total PNG files found: $screenshot_count"

if [[ $screenshot_count -eq 0 ]]; then
    echo "❌ NO SCREENSHOTS WERE CAPTURED!"
    echo ""
    echo "Possible issues:"
    echo "1. WebDriver is not initialized when screenshot is taken"
    echo "2. Screenshot steps are not being executed"
    echo "3. Screenshot capture is throwing exceptions"
    echo ""
    echo "Checking test output for errors..."
    grep -i "error\|exception\|failed" screenshot-debug.log 2>/dev/null | head -5
else
    echo "✓ Screenshots were captured"
fi

rm -f screenshot-debug.log