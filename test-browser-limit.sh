#!/bin/bash

echo "========================================"
echo "Testing Browser Limit with thread-count=2"
echo "========================================"
echo ""
echo "Expected: EXACTLY 2 browsers should open, not 3"
echo "Configuration: parallel='methods' thread-count='2'"
echo ""

# Run the test with thread-count=2
echo "Running test with thread-count=2..."
mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml 2>&1 | tee test-output.log

echo ""
echo "========================================"
echo "Test completed. Checking results..."
echo "========================================"

# Count how many times we see "BROWSER #X BEING CREATED"
browser_count=$(grep -c "BROWSER #[0-9]* BEING CREATED" test-output.log)
echo "Total browsers created: $browser_count"

# Check for browser limit messages
limit_reached=$(grep -c "BROWSER LIMIT REACHED" test-output.log)
if [ $limit_reached -gt 0 ]; then
    echo "Browser limit was enforced: $limit_reached attempts blocked"
fi

# Check semaphore permits
echo ""
echo "Semaphore permit activity:"
grep -E "(Available permits:|Released browser permit)" test-output.log | tail -10

echo ""
if [ $browser_count -eq 2 ]; then
    echo "✓ SUCCESS: Exactly 2 browsers were created as expected!"
else
    echo "✗ FAILURE: Expected 2 browsers but $browser_count were created"
fi

echo "========================================"