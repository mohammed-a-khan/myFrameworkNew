#!/bin/bash

echo "========================================="
echo "Testing Screenshot Redundancy Fix"
echo "========================================="
echo ""

# Clean up old reports and screenshots
echo "Cleaning up old reports..."
rm -rf cs-reports/test-run-*
rm -rf cs-reports/screenshots
rm -f cs-reports/*.png
echo "Cleanup complete."
echo ""

# Check cs-reports folder before test
echo "cs-reports folder BEFORE test:"
ls -la cs-reports/ 2>/dev/null | grep -E "png|screenshot" || echo "  No screenshots found (good!)"
echo ""

# Run the test
echo "Running test..."
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=1 \
    -Dthread.count=1 \
    -q

echo ""
echo "Test completed."
echo ""

# Check cs-reports folder after test
echo "cs-reports folder AFTER test (should have NO screenshots):"
ls -la cs-reports/ 2>/dev/null | grep -E "png|screenshot" || echo "  No screenshots found (good!)"
echo ""

# Check if screenshots folder exists in cs-reports
if [ -d "cs-reports/screenshots" ]; then
    echo "ERROR: cs-reports/screenshots folder exists (should not!)"
    ls -la cs-reports/screenshots/ | head -5
else
    echo "✓ No redundant screenshots folder in cs-reports"
fi
echo ""

# Check the test-run folder
latest_run=$(ls -td cs-reports/test-run-* 2>/dev/null | head -1)
if [ -n "$latest_run" ]; then
    echo "Latest test run: $latest_run"
    echo "Contents:"
    ls -la "$latest_run/"
    
    if [ -d "$latest_run/screenshots" ]; then
        echo ""
        echo "Screenshots in test-run folder (expected location):"
        ls "$latest_run/screenshots/" | head -3
    fi
else
    echo "No test run found!"
fi

echo ""
echo "========================================="
echo "Summary:"
echo "- Screenshots should ONLY be in test-run-*/screenshots/"
echo "- NO screenshots should be in cs-reports/ root"
echo "- NO cs-reports/screenshots/ folder should exist"
echo "========================================="