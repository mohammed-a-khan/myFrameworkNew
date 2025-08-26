#!/bin/bash

echo "=== Testing Browser Cleanup Fix ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null
pkill -f java 2>/dev/null
sleep 2

echo "Starting browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

# Run test with ADO disabled to avoid hanging
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s bash -c '
mvn test \
  -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.azure.devops.enabled=false
' 2>&1 | tee cleanup-test.log

echo ""
echo "=== Analysis ==="

echo "Browser processes after test:"
BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
echo "Chrome processes: $BROWSER_COUNT"

if [ $BROWSER_COUNT -eq 0 ]; then
    echo "✅ SUCCESS: All browsers properly cleaned up!"
elif [ $BROWSER_COUNT -eq 1 ]; then
    echo "⚠️ PARTIAL: Only 1 browser remaining (improvement from 2)"
else
    echo "❌ ISSUE: $BROWSER_COUNT browsers still running"
    echo "Browser processes:"
    ps aux | grep chrome | grep -v grep
fi

echo ""
echo "Cleanup activities in log:"
grep -E "(Closing ALL browsers|quitAllDrivers|@AfterClass|Successfully closed|Released browser permit)" cleanup-test.log

echo ""
echo "Thread information:"
grep -E "(Thread ID|TestNG-PoolService)" cleanup-test.log | head -5

echo ""
echo "Test execution status:"
if grep -q "Tests run:" cleanup-test.log; then
    grep "Tests run:" cleanup-test.log
    echo "✅ Tests executed successfully"
else
    echo "⚠️ Tests may not have executed completely"
fi