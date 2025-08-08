#!/bin/bash

echo "========================================="
echo "Verifying Browser Launch"
echo "========================================="

# Run a simple test with visible output
mvn test -DsuiteXmlFile=suites/orangehrm-tests.xml \
  -Dbrowser.name=chrome \
  -Dbrowser.headless=false \
  -Denvironment.name=qa \
  -Dtest=CSBDDRunner \
  -DforkCount=0 \
  2>&1 | tee test-output.log

echo ""
echo "========================================="
echo "Checking for browser launch..."
echo "========================================="

# Check if browser was created
if grep -q "Creating chrome driver" test-output.log; then
    echo "✓ Browser initialization started"
    if grep -q "headless: false" test-output.log; then
        echo "✓ Running in headed mode (browser should be visible)"
    else
        echo "✗ Running in headless mode"
    fi
else
    echo "✗ Browser was not initialized"
fi

# Check for WebDriver creation
if grep -q "WebDriver created" test-output.log; then
    echo "✓ WebDriver successfully created"
else
    echo "✗ WebDriver creation may have failed"
fi

# Check test execution
echo ""
echo "Test Execution Summary:"
grep -E "(Test passed|Test failed|Tests run)" test-output.log | tail -10

echo ""
echo "Full output saved to: test-output.log"
echo "========================================="