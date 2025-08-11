#!/bin/bash

echo "======================================"
echo "Testing Both Sequential and Parallel Modes"
echo "======================================"
echo ""

echo "1. TESTING SEQUENTIAL MODE"
echo "---------------------------"
echo "Configuration: parallel='none' data-provider-thread-count='1'"
echo "Expected: 1 browser at a time, tests run one after another"
echo ""
mvn clean test -DsuiteXmlFile=suites/orangehrm-sequential-test.xml
echo ""
echo "Sequential mode test completed!"
echo ""

echo "======================================"
echo ""

echo "2. TESTING PARALLEL MODE"
echo "------------------------"
echo "Configuration: parallel='methods' thread-count='3' data-provider-thread-count='3'"
echo "Expected: 3 browsers open simultaneously, tests run in parallel"
echo ""
mvn clean test -DsuiteXmlFile=suites/orangehrm-parallel-test.xml
echo ""
echo "Parallel mode test completed!"
echo ""

echo "======================================"
echo "Both tests completed!"
echo "Check the logs to verify:"
echo "- Sequential: Only 1 browser at a time"
echo "- Parallel: Multiple browsers running simultaneously"
echo "======================================" 