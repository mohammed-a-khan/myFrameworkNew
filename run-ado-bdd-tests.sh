#!/bin/bash

echo "======================================"
echo "Running Azure DevOps BDD Tests"
echo "======================================"
echo ""
echo "Test Configuration:"
echo "  Organization: mdakhan"
echo "  Project: myproject"
echo "  Test Plan ID: 417"
echo "  Test Suite ID: 418"
echo "  Test Cases: 419, 420"
echo "  Feature File: features/ado-mapped-tests.feature"
echo ""
echo "IMPORTANT: Make sure ADO_PAT_TOKEN environment variable is set!"
echo ""

# Check if PAT token is set
if [ -z "$ADO_PAT_TOKEN" ]; then
    echo "ERROR: ADO_PAT_TOKEN environment variable is not set!"
    echo "Please set it using: export ADO_PAT_TOKEN=your_pat_token_here"
    echo ""
    exit 1
fi

echo "Starting BDD test execution with ADO integration..."
echo ""

# Run the ADO BDD test suite (CSBDDRunner will handle ADO automatically when enabled)
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml

echo ""
echo "======================================"
echo "Test execution completed!"
echo "Check Azure DevOps for test results:"
echo "https://dev.azure.com/mdakhan/myproject/_testManagement"
echo ""
echo "Test Plan 417 - Test Suite 418"
echo "======================================"