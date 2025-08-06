#!/bin/bash

# Run OrangeHRM tests with optional parameters
# Usage: ./run-tests.sh [browser] [headless] [environment]

BROWSER=${1:-chrome}
HEADLESS=${2:-true}
ENVIRONMENT=${3:-qa}

echo "========================================="
echo "Running OrangeHRM Tests"
echo "========================================="
echo "Browser: $BROWSER"
echo "Headless: $HEADLESS"
echo "Environment: $ENVIRONMENT"
echo "========================================="

mvn clean test \
  -DsuiteXmlFile=suites/orangehrm-tests.xml \
  -Dbrowser.name=$BROWSER \
  -Dbrowser.headless=$HEADLESS \
  -Denvironment.name=$ENVIRONMENT \
  -Dtest.screenshot.on.failure=true \
  -q

echo ""
echo "========================================="
echo "Test execution completed!"
echo "Report available at: cs-reports/index.html"
echo "========================================="