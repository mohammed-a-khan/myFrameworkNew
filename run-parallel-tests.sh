#!/bin/bash

# Run OrangeHRM tests in parallel mode
# Usage: ./run-parallel-tests.sh [suite] [browser] [headless] [threads]

SUITE=${1:-parallel}  # parallel, parallel-advanced, or parallel-tags
BROWSER=${2:-chrome}
HEADLESS=${3:-true}
THREADS=${4:-3}

echo "========================================="
echo "Running OrangeHRM Parallel Tests"
echo "========================================="
echo "Suite: orangehrm-${SUITE}-tests.xml"
echo "Browser: $BROWSER"
echo "Headless: $HEADLESS"
echo "Thread Count: $THREADS"
echo "========================================="

# Set thread count dynamically
mvn clean test \
  -DsuiteXmlFile=suites/orangehrm-${SUITE}-tests.xml \
  -Dbrowser.name=$BROWSER \
  -Dbrowser.headless=$HEADLESS \
  -Dtest.thread.count=$THREADS \
  -Dtest.screenshot.on.failure=true \
  -q

echo ""
echo "========================================="
echo "Parallel test execution completed!"
echo "Report available at: cs-reports/index.html"
echo "========================================="