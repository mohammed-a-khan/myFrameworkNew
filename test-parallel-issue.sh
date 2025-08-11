#!/bin/bash

echo "========================================"
echo "Testing Parallel Execution Issue"
echo "========================================"
echo ""

# Clean up any existing Chrome processes
pkill -f chrome 2>/dev/null
sleep 2

# Run the test and capture output
echo "Running test with thread-count=2..."
timeout 90 mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml 2>&1 | tee parallel-test-debug.log &

# Wait a bit for test to start
sleep 10

# Monitor browsers
echo ""
echo "Checking browser instances..."
ps aux | grep -c "[c]hrome.*--remote"

# Wait for completion or timeout
wait

echo ""
echo "Test execution completed or timed out"
echo ""

# Extract key information
echo "Key metrics from test run:"
echo "=========================="
grep -E "Total test scenarios created:|Thread .* executing scenario|BROWSER.*CREATED|tests executed:" parallel-test-debug.log | head -20

echo ""
echo "Thread distribution:"
grep "Thread distribution:" parallel-test-debug.log | tail -1

echo ""
echo "Browser creation events:"
grep "BROWSER #" parallel-test-debug.log