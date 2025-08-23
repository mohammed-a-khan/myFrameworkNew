#!/bin/bash

echo "=== Testing All Scenarios Execution ==="

# Run test with longer timeout and capture complete output
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 120s mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" 2>&1 > complete-test-log.txt 2>&1

echo "=== Complete Test Results ==="
grep "Tests run:" complete-test-log.txt

echo ""
echo "=== All Test Method Invocations ==="
grep -c "executeBDDScenario.*Started" complete-test-log.txt

echo ""
echo "=== DataProvider Output ==="
grep "DataProvider created" complete-test-log.txt

echo ""
echo "=== All Executed Scenarios (Unique) ==="
grep "Starting test.*scenario:" complete-test-log.txt | sed 's/.*scenario: \([^,]*\).*/\1/' | sort | uniq

echo ""
echo "=== Thread Distribution ==="
for i in {1..10}; do
  count=$(grep -c "TestNG-PoolService-$i.*Starting test.*scenario:" complete-test-log.txt)
  if [ $count -gt 0 ]; then
    echo "Thread $i: $count scenarios"
  fi
done

echo ""
echo "=== Any Errors or Failures ==="
grep -E "(FAILED|ERROR|Exception)" complete-test-log.txt | head -5

rm -f complete-test-log.txt