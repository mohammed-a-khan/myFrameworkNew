#!/bin/bash

echo "=== TESTING BROWSER ALLOCATION PATTERN ==="

# Clean up
pkill -f chrome 2>/dev/null || echo "No chrome processes to kill"
sleep 2

# Run test and monitor
echo "Running test with browser pattern monitoring..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 45s mvn test \
-Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
-Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
-Dcs.browser.reuse.instance=false \
-Dcs.browser.headless=true \
-q 2>&1 | \
grep -E "BROWSER.*BEING CREATED|Released browser permit" | \
while IFS= read -r line; do
    timestamp=$(date '+%H:%M:%S.%3N')
    browser_count=$(pgrep -f chrome | wc -l)
    echo "[$timestamp] [Active browsers: $browser_count] $line"
done

echo "=== FINAL RESULT ==="
echo "Chrome processes remaining: $(pgrep -f chrome | wc -l)"