#!/bin/bash

echo "=== Debugging Scenario Creation and Distribution ==="

# Run test and capture detailed scenario information
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" 2>&1 > debug-output.log 2>&1

echo "=== DataProvider Scenario Creation ==="
grep -E "(DataProvider created|Adding scenario to test data|Found.*matching scenarios|has.*scenarios)" debug-output.log

echo ""
echo "=== Test Method Executions ==="
grep -E "executeBDDScenario.*Started" debug-output.log | head -10

echo ""
echo "=== Scenario Names Being Executed ==="
grep -E "Starting test.*scenario:" debug-output.log | sed 's/.*scenario: \([^,]*\).*/\1/' | sort | uniq -c

echo ""
echo "=== Feature Parsing Info ==="
grep -E "(Feature.*is included|parseFeatureFile)" debug-output.log

echo ""
echo "=== Test Summary ==="
grep "Tests run:" debug-output.log | tail -1

echo ""
echo "=== All Scenario Executions by Thread ==="
echo "Thread 1:"
grep "TestNG-PoolService-1.*Starting test.*scenario:" debug-output.log | wc -l

echo "Thread 2:"
grep "TestNG-PoolService-2.*Starting test.*scenario:" debug-output.log | wc -l

echo "Thread 3:"
grep "TestNG-PoolService-3.*Starting test.*scenario:" debug-output.log | wc -l

rm -f debug-output.log