#!/bin/bash

echo "=== QUICK TEST TO CHECK @AFTERCLASS EXECUTION ==="

# Kill any existing processes first
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Initial browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

# Export encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

echo "Starting test WITHOUT timeout to see if @AfterClass runs..."

# Run test WITHOUT timeout to let it complete naturally
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -q > quick-test-output.log 2>&1

echo "Test completed naturally. Checking results..."

echo ""
echo "=== LOOKING FOR @AFTERCLASS EXECUTION ==="
grep -E "teardownBDDRunner|@AfterClass|Closing ALL browsers|quitAllDrivers" quick-test-output.log

echo ""
echo "=== BROWSER COUNT AFTER NATURAL COMPLETION ==="
FINAL_BROWSERS=$(ps aux | grep chrome | grep -v grep | wc -l)
echo "Final browser count: $FINAL_BROWSERS"

if [ $FINAL_BROWSERS -gt 0 ]; then
    echo "REMAINING BROWSER PROCESSES:"
    ps aux | grep chrome | grep -v grep | head -3
fi

echo ""
echo "=== TEST RESULTS ==="
grep -E "Tests run|BUILD SUCCESS|BUILD FAILURE" quick-test-output.log

echo ""
echo "=== LAST 10 LINES OF OUTPUT ==="
tail -10 quick-test-output.log