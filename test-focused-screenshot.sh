#!/bin/bash
# Focused test to verify screenshot content capture works

echo "=== Running Focused Screenshot Test ==="

# Clean up any existing screenshots
rm -rf /tmp/cs-temp-screenshots 2>/dev/null

# Run a quick test with just one scenario
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 45s mvn test \
  -Dtest=CSBDDRunner \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.soft.fail.capture.screenshot=true \
  -Dcs.screenshot.compression.enabled=false \
  -Dcs.browser.headless=false \
  -Dbrowser.name=chrome \
  -Denvironment.name=qa \
  -Dcs.bdd.features.path=features/orangehrm-simple-tests.feature \
  -Dcs.bdd.stepdefs.packages=com.orangehrm.stepdefs \
  -Dcs.azure.devops.enabled=false \
  -q

echo ""
echo "=== Checking Results ==="

# Check for screenshots
if [ -d "/tmp/cs-temp-screenshots" ]; then
    echo "✓ Screenshots directory created"
    ls -la /tmp/cs-temp-screenshots/
    
    # Check file contents
    for file in /tmp/cs-temp-screenshots/*.png; do
        if [ -f "$file" ]; then
            size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "0")
            echo "Screenshot: $(basename "$file") - Size: $size bytes"
            
            if [ $size -gt 1000 ]; then
                echo "✓ Screenshot appears to have content (>1KB)"
            else
                echo "✗ Screenshot too small, may be blank"
            fi
        fi
    done
else
    echo "✗ No screenshots directory found"
fi

# Check if there are any logs
if [ -f "target/surefire-reports/testng-results.xml" ]; then
    echo ""
    echo "=== TestNG Results ==="
    grep -E "(test.*name|status)" target/surefire-reports/testng-results.xml | head -10
fi

echo "Test completed."