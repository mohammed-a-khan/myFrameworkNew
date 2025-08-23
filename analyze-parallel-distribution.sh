#!/bin/bash

echo "=== Analyzing Parallel Test Distribution ==="

# Run the test and extract thread information
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 60s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" 2>&1 | \
grep -E "(Thread ID|Starting test|TestNG-PoolService|scenario:|Starting|Executing|Thread.*Starting)" | \
tee test-execution-log.txt

echo ""
echo "=== Thread Distribution Analysis ==="
echo "Thread 1 executions:"
grep -c "TestNG-PoolService-1" test-execution-log.txt || echo "0"

echo "Thread 2 executions:"
grep -c "TestNG-PoolService-2" test-execution-log.txt || echo "0"

echo "Thread 3 executions:"
grep -c "TestNG-PoolService-3" test-execution-log.txt || echo "0"

echo ""
echo "=== Total Test Cases ==="
grep -E "Total.*/" test-execution-log.txt | tail -1

echo ""
echo "=== Detailed Thread Activity ==="
grep -E "Thread ID.*Starting test" test-execution-log.txt | head -10

rm -f test-execution-log.txt