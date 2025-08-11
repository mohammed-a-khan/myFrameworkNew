#!/bin/bash

echo "==========================================="
echo "Testing Timeout Reporting"
echo "==========================================="
echo ""
echo "This test will verify that timeout values are"
echo "correctly calculated and reported when elements"
echo "are not found."
echo ""

# Clean up any existing Chrome processes
echo "Cleaning up any existing Chrome processes..."
pkill -f chrome 2>/dev/null
pkill -f chromedriver 2>/dev/null
sleep 2

echo "Running test that deliberately looks for non-existent elements..."
echo "Watch for the timeout reporting in the logs..."
echo ""

# Run test with deliberate failure tag to trigger element not found
mvn test \
    -Dtest=CSBDDRunner \
    -DfeaturesPath=features/orangehrm-simple-tests.feature \
    -DcucumberOptions="--tags @deliberate-failure" \
    -Dthread.count=1 \
    -Dparallel.core.threads=1 2>&1 | grep -E "Element not found|Function.*failed|RETRY|attempt|after.*seconds" | head -30

echo ""
echo "==========================================="
echo "Test completed. Check the output above for:"
echo "1. Accurate timeout reporting (not hardcoded 10s)"
echo "2. Clear retry attempt information"
echo "3. Total time spent searching for elements"
echo "==========================================="

# Cleanup
pkill -f chrome 2>/dev/null
pkill -f chromedriver 2>/dev/null