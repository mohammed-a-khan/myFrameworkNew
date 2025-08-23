#!/bin/bash

echo "=== Analyzing Thread and Browser Lifecycle ==="

# Run test and capture detailed thread/browser lifecycle
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 60s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" 2>&1 > thread-lifecycle.log 2>&1

echo "=== Browser Creation Pattern ==="
grep "BROWSER #" thread-lifecycle.log

echo ""
echo "=== Test Execution by Thread ==="
for i in {1..3}; do
  echo "Thread $i (TestNG-PoolService-$i):"
  grep "TestNG-PoolService-$i.*Starting test.*scenario:" thread-lifecycle.log | wc -l
  echo "  Scenarios executed:"
  grep "TestNG-PoolService-$i.*Starting test.*scenario:" thread-lifecycle.log | sed 's/.*scenario: \([^,]*\).*/    - \1/'
done

echo ""
echo "=== Browser Cleanup Events ==="
grep -E "(Closing browser|Browser closed|quitDriver|cleanup)" thread-lifecycle.log | grep -E "TestNG-PoolService-[1-3]" | head -10

echo ""
echo "=== Thread Final State ==="
grep -E "(TestNG-PoolService-[1-3].*@AfterMethod|TestNG-PoolService-[1-3].*cleanup)" thread-lifecycle.log | tail -10

echo ""
echo "=== Total Scenarios Created vs Executed ==="
echo -n "Created: "
grep "DataProvider created" thread-lifecycle.log | sed 's/.*created \([0-9]*\).*/\1/'
echo -n "Executed: "
grep -c "Starting test.*scenario:" thread-lifecycle.log

rm -f thread-lifecycle.log