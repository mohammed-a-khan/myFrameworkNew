#!/bin/bash

echo "=== DETAILED BROWSER LIFECYCLE MONITORING ==="

# Clean start
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Initial state: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"

# Start test in background and monitor
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  > detailed-test.log 2>&1 &

TEST_PID=$!
echo "Started test process PID: $TEST_PID"

# Monitor browser count every 5 seconds
echo ""
echo "Monitoring browser count during execution:"
MONITOR_COUNT=0
while kill -0 $TEST_PID 2>/dev/null; do
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    echo "[$(date +%H:%M:%S)] Browsers: $BROWSER_COUNT"
    
    if [ $BROWSER_COUNT -gt 0 ]; then
        # Show browser PIDs
        ps aux | grep chrome | grep -v grep | awk '{print "  PID:", $2, "CMD:", $11, $12, $13}' | head -3
    fi
    
    sleep 5
    MONITOR_COUNT=$((MONITOR_COUNT + 1))
    
    # Safety exit after 3 minutes
    if [ $MONITOR_COUNT -gt 36 ]; then
        echo "Stopping monitoring after 3 minutes"
        kill $TEST_PID 2>/dev/null || true
        break
    fi
done

echo ""
echo "Test process completed"

# Wait a moment for cleanup
sleep 5

# Final check
FINAL_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
echo ""
echo "FINAL BROWSER COUNT: $FINAL_COUNT"

if [ $FINAL_COUNT -gt 0 ]; then
    echo ""
    echo "❌ BROWSERS STILL RUNNING:"
    ps aux | grep chrome | grep -v grep | awk '{print "PID:", $2, "User:", $1, "CMD:", $11, $12, $13, $14}'
    
    echo ""
    echo "Checking for cleanup activities:"
    grep -i "afterclass\|closing.*browser\|quit.*driver\|teardown" detailed-test.log | tail -10
    
    echo ""
    echo "Last 10 lines of test output:"
    tail -10 detailed-test.log
    
else
    echo "✅ All browsers successfully cleaned up"
fi