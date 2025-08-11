#!/bin/bash

echo "========================================="
echo "Testing Screenshot Embedding Feature"
echo "========================================="
echo ""

# Function to check file sizes in cs-reports directory
check_report_size() {
    local mode=$1
    echo "  Checking report size for $mode mode..."
    
    # Find the latest test-run directory
    latest_run=$(ls -td cs-reports/test-run-* 2>/dev/null | head -1)
    
    if [ -n "$latest_run" ]; then
        # Check HTML file size
        html_size=$(du -h "$latest_run/index.html" 2>/dev/null | cut -f1)
        echo "    HTML file size: $html_size"
        
        # Check if screenshots folder exists
        if [ -d "$latest_run/screenshots" ]; then
            screenshot_count=$(ls "$latest_run/screenshots/"*.png 2>/dev/null | wc -l)
            echo "    Screenshot files: $screenshot_count"
        else
            echo "    Screenshots folder: Not created (embedded mode)"
        fi
    fi
    echo ""
}

# Test 1: External screenshot mode (default)
echo "1. Testing EXTERNAL screenshot mode (cs.report.screenshots.embed=false)..."
echo "   Screenshots should be saved as separate files"
echo ""

# Run test with external mode
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dcs.report.screenshots.embed=false \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=1 \
    -Dthread.count=1 2>&1 | grep -E "Screenshot|Report generated|Embedding" | tail -5

check_report_size "EXTERNAL"

echo "========================================="
echo ""

# Test 2: Embedded screenshot mode
echo "2. Testing EMBEDDED screenshot mode (cs.report.screenshots.embed=true)..."
echo "   Screenshots should be embedded as Base64 in HTML"
echo ""

# Run test with embedded mode
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dcs.report.screenshots.embed=true \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=1 \
    -Dthread.count=1 2>&1 | grep -E "Screenshot|Report generated|Embedding" | tail -5

check_report_size "EMBEDDED"

echo "========================================="
echo "Test Summary:"
echo "- External mode: Screenshots as separate files in screenshots/ folder"
echo "- Embedded mode: Screenshots as Base64 in HTML (larger HTML, no separate files)"
echo "========================================="

# Compare the two latest reports
echo ""
echo "Comparing the two latest test runs:"
runs=($(ls -td cs-reports/test-run-* 2>/dev/null | head -2))
if [ ${#runs[@]} -eq 2 ]; then
    echo "  External mode run: ${runs[1]}"
    echo "  Embedded mode run: ${runs[0]}"
    echo ""
    echo "  HTML size comparison:"
    du -h "${runs[0]}/index.html" "${runs[1]}/index.html" 2>/dev/null
fi