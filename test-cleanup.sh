#!/bin/bash

echo "==========================================="
echo "Testing Browser Cleanup with 3 Threads"
echo "==========================================="
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
echo ""

# Run the test
timeout 180 mvn test \
    -DsuiteXmlFile=suites/orangehrm-failure-test.xml \
    -Dtest=CSBDDRunner \
    -Dparallel.core.threads=3 \
    -Dthread.count=3 2>&1 | tee test-output.log &

TEST_PID=$!

echo "Test PID: $TEST_PID"
echo "Monitoring browser activity..."
echo ""

# Monitor for test completion
MAX_WAIT=150
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if ! ps -p $TEST_PID > /dev/null 2>&1; then
        echo "Test process completed"
        break
    fi
    
    CURRENT=$(count_chrome_processes)
    if [ $((ELAPSED % 10)) -eq 0 ]; then
        echo "[$ELAPSED s] Browsers: $CURRENT"
    fi
    
    sleep 1
    ELAPSED=$((ELAPSED + 1))
done

# Wait a bit for cleanup
echo ""
echo "Waiting for cleanup..."
sleep 5

# Final check
FINAL_COUNT=$(count_chrome_processes)

echo ""
echo "==========================================="
echo "Test Results:"
echo "==========================================="

# Check test results
if grep -q "BUILD SUCCESS" test-output.log 2>/dev/null; then
    echo "✓ Tests completed successfully"
else
    echo "✗ Tests may have failed or timed out"
fi

echo ""
echo "Browser Statistics:"
echo "  Remaining browsers after test: $FINAL_COUNT"
echo ""

if [ $FINAL_COUNT -eq 0 ]; then
    echo "✓ All browsers closed properly"
else
    echo "✗ WARNING: $FINAL_COUNT browser(s) still running!"
    echo "  Cleaning up remaining browsers..."
    pkill -f chrome 2>/dev/null
    pkill -f chromedriver 2>/dev/null
fi

echo "==========================================="

# Cleanup
rm -f test-output.log