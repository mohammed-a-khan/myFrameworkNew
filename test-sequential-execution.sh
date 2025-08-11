#!/bin/bash

echo "======================================"
echo "Testing Sequential Execution"
echo "======================================"
echo ""
echo "This test verifies that when parallel='none' is set,"
echo "tests run sequentially with only one browser at a time."
echo ""
echo "Suite XML has: parallel='none'"
echo "Expected behavior:"
echo "- Only 1 browser opens at a time"
echo "- Tests run sequentially"
echo "- No parallel threads"
echo "======================================"
echo ""

# Run the test
mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml

echo ""
echo "======================================"
echo "Test completed!"
echo ""
echo "Verify in the logs that:"
echo "1. 'Sequential execution mode' message appears"
echo "2. Only one browser was open at a time"
echo "3. Tests ran one after another"
echo "======================================" 