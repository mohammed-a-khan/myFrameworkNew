#!/bin/bash

echo "========================================"
echo "Testing Configuration Loading"
echo "========================================"
echo ""

# Run a quick test and capture the log output
echo "Running test to check configuration loading..."
mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -Dtest=CSBDDRunner 2>&1 | head -100 | grep -E "CSConfigManager|Loaded properties|Property not found|environment:" 

echo ""
echo "========================================"
echo "Configuration loading test completed!"
echo "========================================"