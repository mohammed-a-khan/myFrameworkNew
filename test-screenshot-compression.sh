#!/bin/bash
# Test script to verify screenshot compression is working

echo "Testing screenshot compression optimization..."

# Run a simple test with compressed screenshots enabled
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
mvn test \
  -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.soft.fail.capture.screenshot=true \
  -Dcs.screenshot.compression.enabled=true \
  -Dcs.screenshot.max.width=600 \
  -Dcs.screenshot.quality=0.5 \
  -q \
  -Dtest.timeout=20

echo "Checking for compression log messages..."
grep -i "compressed" target/logs/cs-framework.log | head -5 || echo "No compression logs found"

echo "Checking generated report for optimized screenshots..."
if [ -d "cs-reports" ]; then
    latest_report=$(find cs-reports -name "*.html" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
    if [ -n "$latest_report" ]; then
        echo "Latest report: $latest_report"
        # Check if the report contains compressed screenshots (data:image/jpeg)
        if grep -q "data:image/jpeg" "$latest_report" 2>/dev/null; then
            echo "✓ Found compressed JPEG screenshots in report"
        else
            echo "✗ No compressed JPEG screenshots found"
        fi
    fi
fi

echo "Test complete."