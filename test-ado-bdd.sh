#!/bin/bash

echo "================================================"
echo "Testing BDD with Azure DevOps Integration"
echo "================================================"
echo ""

# Check if PAT token is provided as argument or environment variable
if [ -n "$1" ]; then
    export ADO_PAT_TOKEN="$1"
    echo "Using PAT token from command line argument"
elif [ -z "$ADO_PAT_TOKEN" ]; then
    echo "WARNING: ADO_PAT_TOKEN not set. ADO integration may fail."
    echo "Usage: ./test-ado-bdd.sh YOUR_PAT_TOKEN"
    echo "Or set: export ADO_PAT_TOKEN=your_token"
    echo ""
    echo "Continuing anyway (tests will run without ADO publishing)..."
    echo ""
fi

echo "Configuration:"
echo "  Feature: features/ado-mapped-tests.feature"
echo "  ADO Enabled: true (in application.properties)"
echo "  Test Plan: 417"
echo "  Test Suite: 418"
echo "  Test Cases: 419, 420"
echo ""

echo "Starting test execution..."
echo "================================================"

# Run the tests
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml -Dtest.verbose=true

echo ""
echo "================================================"
echo "Test execution completed!"
echo ""
if [ -n "$ADO_PAT_TOKEN" ]; then
    echo "Check Azure DevOps for results:"
    echo "https://dev.azure.com/mdakhan/myproject/_testManagement"
else
    echo "ADO publishing was skipped (no PAT token)"
fi
echo "================================================"