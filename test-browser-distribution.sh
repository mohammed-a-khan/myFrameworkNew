#!/bin/bash

echo "========================================="
echo "Testing Browser Distribution with 3 Threads"
echo "========================================="
echo ""

# Function to count Chrome processes
count_chrome_processes() {
    ps aux | grep -E "[c]hrome|[c]hromedriver" | grep -v defunct | wc -l
}

# Clean up any existing Chrome processes
echo "Cleaning up any existing Chrome processes..."
pkill -f chrome 2>/dev/null
pkill -f chromedriver 2>/dev/null
sleep 2

echo "Initial Chrome processes: $(count_chrome_processes)"
echo ""

echo "Starting test with thread-count=3..."
echo "Feature file has ~10 test scenarios"
echo "Expected distribution: 4-3-3 tests across 3 threads"
echo ""

# Run the test and capture output
timeout 120 mvn test \
    -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=3 \
    -Dthread.count=3 2>&1 | tee test-output.log &

TEST_PID=$!

echo "Monitoring browser activity..."
echo ""

# Monitor for 60 seconds
IDLE_COUNT=0
MAX_BROWSERS=0
for i in {1..30}; do
    sleep 2
    
    CURRENT=$(count_chrome_processes)
    
    # Track max browsers
    if [ $CURRENT -gt $MAX_BROWSERS ]; then
        MAX_BROWSERS=$CURRENT
    fi
    
    # Count Chrome windows (not processes)
    CHROME_WINDOWS=$(ps aux | grep -E "chrome.*--new-window" | grep -v grep | wc -l)
    
    # Show status every 6 seconds
    if [ $((i % 3)) -eq 0 ]; then
        echo "[$((i*2))s] Browsers: $CURRENT (Windows: ~$((CURRENT/10)))"
        
        # Check for idle browsers (those on about:blank or login page for too long)
        if [ $i -gt 10 ]; then
            IDLE=$(ps aux | grep chrome | grep -E "about:blank|data:," | wc -l)
            if [ $IDLE -gt 0 ]; then
                IDLE_COUNT=$((IDLE_COUNT + 1))
                echo "      WARNING: Detected $IDLE potentially idle browser(s)"
            fi
        fi
    fi
    
    # Check if test finished
    if ! ps -p $TEST_PID > /dev/null 2>&1; then
        echo "      Test completed"
        break
    fi
done

# Wait for test to fully complete
wait $TEST_PID 2>/dev/null
sleep 3

# Final check
FINAL_COUNT=$(count_chrome_processes)

echo ""
echo "========================================="
echo "Test Results:"
echo "========================================="

# Analyze test output for thread distribution
echo ""
echo "Thread Distribution:"
grep -E "Thread .* has completed .* tests" test-output.log | tail -10 || echo "No thread completion info found"

echo ""
echo "Browser Statistics:"
echo "  Maximum concurrent browsers: $MAX_BROWSERS"
echo "  Expected browsers: 3"
echo "  Idle browser detections: $IDLE_COUNT"
echo "  Remaining browsers after test: $FINAL_COUNT"

echo ""
if [ $FINAL_COUNT -eq 0 ]; then
    echo "✓ All browsers closed properly"
else
    echo "✗ WARNING: $FINAL_COUNT browser(s) still running!"
fi

if [ $IDLE_COUNT -gt 3 ]; then
    echo "✗ WARNING: Browsers were idle multiple times during execution"
else
    echo "✓ Good browser utilization"
fi

echo "========================================="

# Cleanup
pkill -f chrome 2>/dev/null
pkill -f chromedriver 2>/dev/null
rm -f test-output.log