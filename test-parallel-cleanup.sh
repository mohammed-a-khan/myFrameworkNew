#\!/bin/bash

echo "========================================="
echo "Testing Parallel Execution Browser Cleanup"
echo "========================================="
echo ""

# Function to count Chrome processes
count_chrome_processes() {
    ps aux | grep -E "[c]hrome|[c]hromedriver" | wc -l
}

# Clean up any existing Chrome processes
echo "Cleaning up any existing Chrome processes..."
pkill -f chrome 2>/dev/null
pkill -f chromedriver 2>/dev/null
sleep 2

echo "Initial Chrome processes: $(count_chrome_processes)"
echo ""

echo "Running test with thread-count=3..."
echo "Expected behavior:"
echo "  - 3 browsers should open initially"
echo "  - All 3 browsers should execute tests"
echo "  - No browser should remain idle"
echo "  - All browsers should close after test completion"
echo ""

# Run the test with timeout to prevent hanging
timeout 120 mvn test \
    -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=3 \
    -Dthread.count=3 \
    -q &

# Store the PID
TEST_PID=$\!

# Monitor Chrome processes during execution
echo "Monitoring browser processes..."
MAX_BROWSERS=0
for i in {1..30}; do
    CURRENT=$(count_chrome_processes)
    if [ $CURRENT -gt $MAX_BROWSERS ]; then
        MAX_BROWSERS=$CURRENT
    fi
    
    # Check every 2 seconds for first 60 seconds
    if [ $i -le 30 ]; then
        if [ $((i % 5)) -eq 0 ]; then
            echo "  Time: $((i*2))s - Chrome processes: $CURRENT"
        fi
    fi
    
    # Check if test is still running
    if \! ps -p $TEST_PID > /dev/null 2>&1; then
        echo "  Test completed at $((i*2)) seconds"
        break
    fi
    
    sleep 2
done

# Wait for test to complete
wait $TEST_PID 2>/dev/null

echo ""
echo "Test execution completed."
sleep 5

# Check remaining Chrome processes
FINAL_COUNT=$(count_chrome_processes)
echo ""
echo "========================================="
echo "Results:"
echo "  Maximum browsers during execution: $MAX_BROWSERS"
echo "  Remaining Chrome processes after test: $FINAL_COUNT"
echo ""

if [ $FINAL_COUNT -eq 0 ]; then
    echo "✓ SUCCESS: All browsers closed properly\!"
else
    echo "✗ FAILURE: $FINAL_COUNT browser(s) still running\!"
    echo ""
    echo "Remaining processes:"
    ps aux | grep -E "[c]hrome|[c]hromedriver" | head -5
fi

echo "========================================="

# Clean up any remaining processes
if [ $FINAL_COUNT -gt 0 ]; then
    echo ""
    echo "Cleaning up remaining processes..."
    pkill -f chrome 2>/dev/null
    pkill -f chromedriver 2>/dev/null
fi
