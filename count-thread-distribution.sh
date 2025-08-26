#!/bin/bash

echo "=== THREAD DISTRIBUTION ANALYSIS ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null
pkill -f java 2>/dev/null
sleep 2

echo "Running test with timeout to catch execution state..."

# Run for shorter time to see browser states during execution
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 60s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  2>&1 | tee thread-dist.log &

# Monitor browser count during execution
TEST_PID=$!
echo "Monitoring browsers during test execution (PID: $TEST_PID)..."

for i in {1..20}; do
    sleep 3
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    echo "[$i] Time: $(date +%H:%M:%S), Browsers: $BROWSER_COUNT"
    
    if [ $BROWSER_COUNT -eq 3 ]; then
        echo "  -> All 3 browsers are running"
    elif [ $BROWSER_COUNT -eq 2 ]; then
        echo "  -> Only 2 browsers running (1 may have finished)"  
    elif [ $BROWSER_COUNT -eq 1 ]; then
        echo "  -> Only 1 browser running (2 have finished)"
    elif [ $BROWSER_COUNT -eq 0 ]; then
        echo "  -> All browsers closed"
        break
    fi
    
    # Check if test is still running
    if ! kill -0 $TEST_PID 2>/dev/null; then
        echo "Test process completed"
        break
    fi
done

# Final check
wait $TEST_PID 2>/dev/null
echo ""
echo "Final browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

echo ""
echo "=== EXECUTION ANALYSIS ==="
echo "Browsers created: $(grep -c "BROWSER.*BEING CREATED" thread-dist.log)"
echo "Cleanup calls: $(grep -c "Closing ALL browsers\|quitAllDrivers" thread-dist.log)"
