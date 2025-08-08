#!/bin/bash

# Run tests in headed mode (with visible browser)
echo "========================================="
echo "Running Tests in Headed Mode"
echo "========================================="

# Export system properties to ensure they're picked up
export MAVEN_OPTS="-Dbrowser.headless=false"

# Run tests with explicit parameters
mvn clean test \
  -DsuiteXmlFile=suites/orangehrm-tests.xml \
  -Dbrowser.name=chrome \
  -Dbrowser.headless=false \
  -Denvironment.name=qa \
  -Dtest.screenshot.on.failure=true \
  -Dparallel=none \
  -DthreadCount=1 \
  -Dtest.thread.count=1 \
  -Dexecution.mode=sequential

echo ""
echo "========================================="
echo "Test execution completed!"
echo "Report available at: cs-reports/latest-report.html"
echo "========================================="