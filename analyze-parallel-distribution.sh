#!/bin/bash

echo "=== DETAILED PARALLEL EXECUTION ANALYSIS ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null
pkill -f java 2>/dev/null
sleep 3

echo "Starting browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"
echo ""

# Run test with detailed logging but timeout to prevent hanging
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 90s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  2>&1 | tee parallel-analysis.log

echo ""
echo "=== THREAD TO BROWSER MAPPING ANALYSIS ==="

echo "Browsers created per thread:"
grep "BROWSER #.*BEING CREATED.*Thread:" parallel-analysis.log | sort

echo ""
echo "Tests assigned to threads:"
grep -E "executeBDDScenario.*TestNG-PoolService" parallel-analysis.log | head -10

echo ""
echo "Step executions by thread:"
grep -E "\[TestNG-PoolService-[123]\].*Executing step" parallel-analysis.log | head -15

echo ""
echo "Page navigations by thread:"
grep -E "\[TestNG-PoolService-[123]\].*Navigating to\|login page\|dashboard" parallel-analysis.log | head -10

echo ""
echo "Browser cleanup activities:"
grep -E "Closing.*browser\|quitAllDrivers\|@AfterClass" parallel-analysis.log

echo ""
echo "Final browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
if [ $BROWSER_COUNT -gt 0 ]; then
    echo ""
    echo "Remaining browser processes:"
    ps aux | grep chrome | grep -v grep | head -5
fi

echo ""
echo "=== THREAD DISTRIBUTION SUMMARY ==="
echo "Thread 1 scenarios: $(grep -c "TestNG-PoolService-1.*executeBDDScenario" parallel-analysis.log)"
echo "Thread 2 scenarios: $(grep -c "TestNG-PoolService-2.*executeBDDScenario" parallel-analysis.log)" 
echo "Thread 3 scenarios: $(grep -c "TestNG-PoolService-3.*executeBDDScenario" parallel-analysis.log)"
