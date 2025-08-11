#!/bin/bash

echo "======================================"
echo "Verifying Scenario Name Display Fix"
echo "======================================"
echo ""
echo "This test verifies that scenario names in the HTML report"
echo "follow the format: <scenario_name>_Iteration<number>"
echo ""
echo "Expected results in Test Suite tab:"
echo "- 'Deliberately failing test to demonstrate failure reporting_Iteration1'"
echo "- 'Deliberately failing test to demonstrate failure reporting_Iteration2'"
echo "- 'Deliberately failing test to demonstrate failure reporting_Iteration3'"
echo "- 'Multiple login attempts_Iteration1'"
echo "- 'Multiple login attempts_Iteration2'"
echo "- 'Multiple login attempts_Iteration3'"
echo ""
echo "Instead of showing data values like 'test1', 'test2', 'admin', etc."
echo "======================================"
echo ""

# Clean and run the test
mvn clean test -DsuiteXmlFile=suites/orangehrm-failure-test.xml

echo ""
echo "======================================"
echo "Test completed!"
echo "Check the generated HTML report at:"
echo "target/cs-reports/cs-test-report.html"
echo ""
echo "Open the report and verify the Test Suite tab shows"
echo "proper scenario names with iteration numbers."
echo "======================================" 