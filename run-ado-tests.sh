#!/bin/bash

echo "======================================"
echo "Running Azure DevOps Mapped Tests"
echo "======================================"
echo ""
echo "Test Configuration:"
echo "  Organization: mdakhan"
echo "  Project: myproject"
echo "  Test Plan ID: 417"
echo "  Test Suite ID: 418"
echo "  Test Cases: 419, 420"
echo ""
echo "Starting test execution..."
echo ""

# Run the ADO mapped test suite
mvn test -Dsurefire.suiteXmlFiles=suites/ado-mapped-suite.xml -Dtest=CSADOMappedTest

echo ""
echo "======================================"
echo "Test execution completed!"
echo "Check Azure DevOps for test results:"
echo "https://dev.azure.com/mdakhan/myproject/_testManagement"
echo "======================================"