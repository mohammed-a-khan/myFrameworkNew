#!/bin/bash
# Debug script to validate screenshot content capture

echo "=== Testing Screenshot Content Capture ==="

# Compile the enhanced code
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
mvn clean compile -q

echo "Running test with enhanced screenshot debugging..."

# Run focused test with debug logging
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 30s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.soft.fail.capture.screenshot=true \
  -Dcs.screenshot.compression.enabled=false \
  -Dcs.browser.headless=false \
  -Droot.loglevel=INFO \
  -q

echo ""
echo "=== Analyzing Screenshot Debug Information ==="

if [ -f "target/logs/cs-framework.log" ]; then
    echo "Looking for URL and page information in logs..."
    grep -i "current url\|page title\|screenshot capture" target/logs/cs-framework.log | head -10
    echo ""
    echo "Looking for WebDriver session info..."
    grep -i "webdriver available\|driver.*chrome" target/logs/cs-framework.log | head -5
else
    echo "No log file found"
fi

echo ""
echo "=== Checking Screenshot Files ==="
if [ -d "/tmp/cs-temp-screenshots" ]; then
    echo "Screenshot files found:"
    ls -la /tmp/cs-temp-screenshots/
    echo ""
    
    # Check file sizes to see if they contain actual content
    for file in /tmp/cs-temp-screenshots/*.png; do
        if [ -f "$file" ]; then
            size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "unknown")
            echo "Screenshot: $(basename "$file") - Size: $size bytes"
        fi
    done
else
    echo "No temp screenshots directory found"
fi

echo ""
echo "=== Checking Latest Report ==="
if [ -f "cs-reports/latest-report.html" ]; then
    # Follow the redirect to get actual report
    actual_report=$(grep -o 'url=[^"]*' cs-reports/latest-report.html | sed 's/url=//')
    if [ -n "$actual_report" ]; then
        report_path="cs-reports/$actual_report"
        if [ -f "$report_path" ]; then
            echo "Checking actual report: $report_path"
            base64_count=$(grep -c "data:image" "$report_path" 2>/dev/null || echo 0)
            echo "Base64 images found: $base64_count"
            
            if [ $base64_count -gt 0 ]; then
                echo "✓ Screenshots are embedded in report"
            else
                echo "✗ No screenshots found in report"
            fi
        fi
    fi
fi

echo "Debug analysis complete."