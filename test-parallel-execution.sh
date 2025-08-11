#!/bin/bash

echo "======================================"
echo "Testing Parallel Execution Fix"
echo "======================================"
echo ""
echo "This test should:"
echo "1. Open 3 browsers simultaneously"
echo "2. Execute tests in all 3 browsers in parallel"
echo "3. Each browser should complete its test"
echo ""
echo "Running: mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml"
echo "======================================"

mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml

echo ""
echo "======================================"
echo "Test execution completed!"
echo "Check the logs above to verify:"
echo "- 3 browsers opened"
echo "- All 3 tests executed"
echo "- Thread distribution shows 3 different threads"
echo "======================================" 