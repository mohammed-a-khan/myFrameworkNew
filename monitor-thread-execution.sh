#!/bin/bash

echo "=== MONITORING THREAD-LOCAL STEP INSTANCE EXECUTION ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Initial browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

echo ""
echo "=== STARTING MONITORED TEST EXECUTION ==="

# Run test with detailed monitoring
timeout 60s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Droot.loglevel=DEBUG > thread-execution.log 2>&1 &

TEST_PID=$!

# Monitor browser lifecycle in real-time
echo "Monitoring browser lifecycle (PID: $TEST_PID)..."

MONITOR_COUNT=0
while kill -0 $TEST_PID 2>/dev/null && [ $MONITOR_COUNT -lt 30 ]; do
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    JAVA_PROCESSES=$(ps aux | grep "mvn test" | grep -v grep | wc -l)
    
    echo "[$(date +%H:%M:%S)] Browsers: $BROWSER_COUNT, Java processes: $JAVA_PROCESSES"
    
    # Check for specific execution patterns
    if [ $BROWSER_COUNT -eq 3 ] && [ $MONITOR_COUNT -eq 0 ]; then
        echo "✅ All 3 browsers opened successfully"
    fi
    
    sleep 2
    MONITOR_COUNT=$((MONITOR_COUNT + 1))
done

# Wait for test completion
wait $TEST_PID
TEST_RESULT=$?

echo ""
echo "=== EXECUTION ANALYSIS ==="

# Check thread activity in logs
echo "Thread activity analysis:"
echo "- TestNG-PoolService-1 activities: $(grep -c "TestNG-PoolService-1" thread-execution.log)"
echo "- TestNG-PoolService-2 activities: $(grep -c "TestNG-PoolService-2" thread-execution.log)"  
echo "- TestNG-PoolService-3 activities: $(grep -c "TestNG-PoolService-3" thread-execution.log)"

echo ""
echo "Step execution distribution:"
grep "Injected.*page objects" thread-execution.log | cut -d']' -f1 | sort | uniq -c || echo "No injection logs found"

echo ""
echo "Login attempt distribution:"
grep "Entering username" thread-execution.log | cut -d']' -f1 | sort | uniq -c || echo "No login attempts found"

# Check final state
FINAL_BROWSERS=$(ps aux | grep chrome | grep -v grep | wc -l)
echo ""
echo "=== FINAL STATE ==="
echo "Final browser count: $FINAL_BROWSERS"
echo "Test result: $TEST_RESULT"

if [ $FINAL_BROWSERS -eq 0 ]; then
    echo "✅ Perfect cleanup achieved"
else
    echo "⚠️ $FINAL_BROWSERS browsers remaining"
fi

# Check for thread isolation
echo ""
echo "=== THREAD ISOLATION CHECK ==="
if grep -q "threadLocalStepInstances" thread-execution.log; then
    echo "✅ Thread-local step instances are being used"
else
    echo "⚠️ No thread-local step instance activity detected"
fi

echo ""
echo "=== SUMMARY ==="
if [ $(grep -c "TestNG-PoolService-[123]" thread-execution.log) -ge 3 ]; then
    echo "✅ Multiple threads are active"
else
    echo "❌ Limited thread activity detected"
fi