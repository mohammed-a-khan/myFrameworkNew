#!/bin/bash

echo "Starting browser creation pattern monitoring..."

# Kill any existing chrome processes
pkill -f chrome || echo "No existing chrome processes found"
sleep 2

# Start the test and monitor browser creation
echo "Running test with browser monitoring..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 45s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -Dcs.browser.reuse.instance=false -q 2>&1 | \
tee test_output.log | \
grep -E "BROWSER.*BEING CREATED|Thread ID.*Starting test.*#.*for scenario|Thread.*acquired browser permit|Available permits|ThreadNG-PoolService" | \
while read line; do
    # Get current browser count
    browser_count=$(pgrep -f "chromium|chrome" | wc -l)
    timestamp=$(date '+%H:%M:%S.%3N')
    echo "[$timestamp] [BROWSERS: $browser_count] $line"
done