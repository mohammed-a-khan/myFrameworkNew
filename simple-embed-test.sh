#!/bin/bash

echo "========================================="
echo "Simple Screenshot Embedding Test"
echo "========================================="
echo ""

# Clean up old reports
rm -rf cs-reports/test-run-*

echo "Running test with embedding enabled..."
echo ""

# Run with explicit property setting
mvn test \
    -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dtest=CSBDDRunner \
    -Dcs.report.screenshots.embed=true \
    -Dparallel.core.threads=1 \
    -Dthread.count=1 \
    -q

# Check latest report
echo ""
echo "Checking generated report..."
latest_run=$(ls -td cs-reports/test-run-* 2>/dev/null | head -1)

if [ -n "$latest_run" ]; then
    echo "Latest run: $latest_run"
    
    # Check for Base64 images
    base64_count=$(grep -c "data:image" "$latest_run/index.html" 2>/dev/null || echo "0")
    echo "Base64 embedded images found: $base64_count"
    
    # Check for screenshot files
    if [ -d "$latest_run/screenshots" ]; then
        file_count=$(ls "$latest_run/screenshots/"*.png 2>/dev/null | wc -l)
        echo "Screenshot files in folder: $file_count"
    else
        echo "Screenshots folder: Not created (as expected with embedding)"
    fi
    
    # Check HTML file size
    html_size=$(du -h "$latest_run/index.html" | cut -f1)
    echo "HTML file size: $html_size"
else
    echo "No test run found!"
fi