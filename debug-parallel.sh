#!/bin/bash

echo "=== DEBUG PARALLEL BROWSER CREATION ==="

# Kill existing browsers
pkill -f chrome 2>/dev/null || echo "No existing chrome processes"
sleep 2

echo "Starting test..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 20s mvn test \
-Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
-Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
-Dcs.browser.reuse.instance=false \
-Dcs.browser.headless=true \
-q 2>&1 | grep -E "BROWSER.*BEING CREATED|Thread ID.*Starting test.*#"

echo "=== FINAL CHROME PROCESS COUNT ==="
pgrep -f chrome | wc -l