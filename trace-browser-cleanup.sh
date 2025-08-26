#!/bin/bash

echo "=== DETAILED BROWSER CLEANUP TRACING ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Starting browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

# Start test in background with detailed logging
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  > browser-cleanup-trace.log 2>&1 &

TEST_PID=$!
echo "Test PID: $TEST_PID"

# Monitor browsers and capture PIDs
echo ""
echo "Browser monitoring during execution:"
BROWSER_PIDS_FILE="/tmp/browser_pids.txt"
echo "" > $BROWSER_PIDS_FILE

while kill -0 $TEST_PID 2>/dev/null; do
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    CURRENT_PIDS=$(ps aux | grep chrome | grep -v grep | awk '{print $2}' | sort)
    
    echo "[$(date +%H:%M:%S)] Browsers: $BROWSER_COUNT"
    if [ $BROWSER_COUNT -gt 0 ]; then
        echo "  PIDs: $CURRENT_PIDS"
        echo "$CURRENT_PIDS" >> $BROWSER_PIDS_FILE
    fi
    
    sleep 3
done

echo ""
echo "Test completed. Waiting for cleanup..."

# Monitor cleanup phase
for i in {1..10}; do
    sleep 1
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    REMAINING_PIDS=$(ps aux | grep chrome | grep -v grep | awk '{print $2}' | sort)
    
    echo "[Cleanup +${i}s] Browsers: $BROWSER_COUNT"
    if [ $BROWSER_COUNT -gt 0 ]; then
        echo "  Remaining PIDs: $REMAINING_PIDS"
        # Show detailed process info
        ps aux | grep chrome | grep -v grep | head -3
    fi
done

echo ""
echo "=== FINAL ANALYSIS ==="
FINAL_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
echo "FINAL BROWSER COUNT: $FINAL_COUNT"

if [ $FINAL_COUNT -gt 0 ]; then
    echo ""
    echo "REMAINING BROWSER PROCESS(ES):"
    ps aux | grep chrome | grep -v grep
    
    echo ""
    echo "CLEANUP SEQUENCE FROM LOG:"
    grep -i "closing.*browser\|quitalldriver\|afterclass\|aftersuite\|aggressive.*cleanup\|pkill" browser-cleanup-trace.log
    
    echo ""
    echo "THREAD INFORMATION:"
    grep -E "Thread.*ID.*Starting|@AfterClass.*thread" browser-cleanup-trace.log | tail -5
    
    echo ""
    echo "ERROR MESSAGES:"
    grep -i "error\|exception\|failed" browser-cleanup-trace.log | grep -i "browser\|driver\|cleanup" | tail -3
    
else
    echo "✅ All browsers cleaned up successfully"
fi