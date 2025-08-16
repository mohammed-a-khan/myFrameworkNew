#!/bin/bash

echo "Testing Parallel BDD Execution Fix"
echo "==================================="
echo ""
echo "This test will verify that:"
echo "1. Multiple browsers open for parallel execution"
echo "2. Each thread gets its own browser instance"
echo "3. All browsers are properly closed after test completion"
echo ""

# Clean up any existing test results
rm -rf target/surefire-reports/*
rm -rf cs-reports/*

echo "Running OrangeHRM tests with parallel execution (2 threads)..."
echo "--------------------------------------------------------------"

# Run the test with detailed logging
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
         -Dcs.browser.reuse.instance=true \
         -Dcs.browser.headless=false 2>&1 | tee parallel-test.log

echo ""
echo "Test execution complete!"
echo ""

# Check the results
echo "Checking test results..."
echo "------------------------"

# Count how many browser instances were created
BROWSER_CREATED=$(grep -c "Creating NEW .* driver" parallel-test.log)
echo "✓ Browser instances created: $BROWSER_CREATED"

# Count how many browsers were closed
BROWSER_CLOSED=$(grep -c "Closing.*browser" parallel-test.log)
echo "✓ Browser instances closed: $BROWSER_CLOSED"

# Check for parallel execution
PARALLEL_THREADS=$(grep "pool-.*-thread-" parallel-test.log | cut -d':' -f1 | sort -u | wc -l)
echo "✓ Unique threads used: $PARALLEL_THREADS"

# Check for any unclosed browsers
DRIVER_POOL_SIZE=$(grep "Driver pool size" parallel-test.log | tail -1)
echo "✓ Final driver pool: $DRIVER_POOL_SIZE"

echo ""
echo "Summary:"
echo "--------"
if [ "$BROWSER_CREATED" -gt 1 ] && [ "$BROWSER_CLOSED" -gt 0 ]; then
    echo "✅ SUCCESS: Multiple browsers were created and properly closed!"
    echo "   - Created: $BROWSER_CREATED browsers"
    echo "   - Closed: $BROWSER_CLOSED browsers"
    echo "   - Threads: $PARALLEL_THREADS"
else
    echo "❌ ISSUE DETECTED:"
    echo "   - Created: $BROWSER_CREATED browsers"
    echo "   - Closed: $BROWSER_CLOSED browsers"
    echo "   - Expected multiple browsers for parallel execution"
fi

echo ""
echo "Check the HTML report at: cs-reports/latest-report.html"
echo "Log file saved as: parallel-test.log"