#!/bin/bash

echo "=== Testing COMPLETE FIX for Soft Fail + Browser Cleanup ==="

# Check browser processes before test
echo "=== Browser processes BEFORE test ==="
pgrep -f 'chrome|firefox|msedge' || echo "No browser processes found"

# Run the test
echo ""
echo "=== Running test with CSReportManager.fail() ==="
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
timeout 45s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
-Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
2>&1 | tee test-complete-fix.log

# Check for soft fail behavior
echo ""
echo "=== Checking for SOFT FAIL behavior ==="
grep -i "Execution continues after fail" test-complete-fix.log && echo "✅ SOFT FAIL working - execution continued!"
grep -i "Soft fail recorded" test-complete-fix.log && echo "✅ Soft fail was recorded!"
grep -i "Custom user message" test-complete-fix.log && echo "✅ Custom message found!"

# Check browser processes after test
echo ""
echo "=== Browser processes AFTER test (should be empty) ==="
sleep 3  # Give cleanup time to complete
pgrep -f 'chrome|firefox|msedge' && echo "❌ CLEANUP FAILED - browsers still running" || echo "✅ CLEANUP SUCCESS - no browsers running"

# Check if nuclear cleanup was performed
echo ""
echo "=== Checking cleanup logs ==="
grep -i "NUCLEAR.*CLEANUP" test-complete-fix.log && echo "✅ Nuclear cleanup was performed"

# Check HTML report
echo ""
echo "=== Checking HTML report generation ==="
find target/test-reports -name "*.html" -newer test-complete-fix.log | head -3

echo ""
echo "=== Test completed - check results above ==="