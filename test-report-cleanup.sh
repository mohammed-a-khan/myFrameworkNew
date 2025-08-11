#!/bin/bash

echo "========================================="
echo "Testing Report File Cleanup"
echo "========================================="
echo ""

# Clean up old files first
echo "Cleaning up old reports..."
rm -rf cs-reports/test-run-*
rm -f cs-reports/*.html cs-reports/*.json
echo ""

echo "cs-reports folder BEFORE test:"
ls -la cs-reports/ 2>/dev/null | grep -v "history\|trends" || echo "Empty"
echo ""

echo "Running test..."
timeout 60 mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -q

echo ""
echo "Test completed."
echo ""

echo "cs-reports folder AFTER test:"
ls -la cs-reports/ 2>/dev/null | grep -v "history\|trends\|test-run" || echo "Clean!"
echo ""

# Check for redundant files
REDUNDANT_FILES=$(ls cs-reports/*.json cs-reports/test-report_*.html 2>/dev/null | wc -l)

if [ $REDUNDANT_FILES -eq 0 ]; then
    echo "✓ SUCCESS: No redundant files in cs-reports root!"
else
    echo "✗ FAILURE: Found $REDUNDANT_FILES redundant file(s):"
    ls cs-reports/*.json cs-reports/test-report_*.html 2>/dev/null
fi

echo ""
echo "Test-run folders:"
ls -d cs-reports/test-run-* 2>/dev/null | tail -3

echo ""
echo "Latest report redirect:"
if [ -f "cs-reports/latest-report.html" ]; then
    echo "✓ latest-report.html exists (good - it's just a redirect)"
    grep -o 'url=[^"]*' cs-reports/latest-report.html | head -1
else
    echo "✗ latest-report.html missing"
fi

echo ""
echo "========================================="
echo "Expected structure:"
echo "  cs-reports/"
echo "    ├── latest-report.html (redirect only)"
echo "    ├── history/ (optional)"
echo "    ├── trends/ (optional)"
echo "    └── test-run-*/ (actual reports)"
echo "         ├── index.html"
echo "         ├── report-data.json"
echo "         └── screenshots/"
echo "========================================="