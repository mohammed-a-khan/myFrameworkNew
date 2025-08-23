#!/bin/bash

echo "=== Counting Thread Distribution in Parallel Execution ==="

# Run test and capture thread activity
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" 2>&1 > thread-output.log 2>&1

echo "=== Test Execution Summary ==="
grep "Tests run:" thread-output.log | tail -1

echo ""
echo "=== Thread Activity Count ==="
echo "Thread 1 (TestNG-PoolService-1) executions:"
grep -c "TestNG-PoolService-1.*Starting test" thread-output.log

echo "Thread 2 (TestNG-PoolService-2) executions:"
grep -c "TestNG-PoolService-2.*Starting test" thread-output.log

echo "Thread 3 (TestNG-PoolService-3) executions:"
grep -c "TestNG-PoolService-3.*Starting test" thread-output.log

echo ""
echo "=== Scenario Distribution ==="
echo "Thread 1 scenarios:"
grep "TestNG-PoolService-1.*scenario:" thread-output.log | sed 's/.*scenario: \([^,]*\).*/\1/' | sort | uniq -c

echo "Thread 2 scenarios:"
grep "TestNG-PoolService-2.*scenario:" thread-output.log | sed 's/.*scenario: \([^,]*\).*/\1/' | sort | uniq -c

echo "Thread 3 scenarios:"
grep "TestNG-PoolService-3.*scenario:" thread-output.log | sed 's/.*scenario: \([^,]*\).*/\1/' | sort | uniq -c

echo ""
echo "=== Browser Creation Activity ==="
grep "BROWSER #" thread-output.log

echo ""
echo "=== Total Scenarios Executed ==="
grep -E "Starting test.*scenario:" thread-output.log | wc -l

rm -f thread-output.log