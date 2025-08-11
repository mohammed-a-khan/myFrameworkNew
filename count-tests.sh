#!/bin/bash

echo "========================================"
echo "Test Count Verification"
echo "========================================"
echo ""

echo "1. Running with @deliberate-failure tag (current configuration):"
echo "   Expected: 3 tests (from 1 Scenario Outline with 3 examples)"
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -Dtest=CSBDDRunner 2>&1 | grep -E "DataProvider created .* total test scenarios|Total tests executed:" | tail -2

echo ""
echo "2. Running ALL scenarios (no tag filter):"
echo "   Expected: 10 tests (4 Scenarios + 6 from Scenario Outlines)"
mvn test -DsuiteXmlFile=suites/orangehrm-all-tests.xml -Dtest=CSBDDRunner 2>&1 | grep -E "DataProvider created .* total test scenarios|Total tests executed:" | tail -2

echo ""
echo "========================================"
echo "This shows why you only see 3 tests:"
echo "The suite is filtering with --tags @deliberate-failure"
echo "========================================"