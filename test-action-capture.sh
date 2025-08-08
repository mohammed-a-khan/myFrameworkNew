#!/bin/bash

echo "Testing action capture in reports..."

# Run only the simple login test which should have actions
mvn test -Dtest=CSBDDRunner#executeBDDScenario -DsuiteXmlFile=suites/orangehrm-tests.xml \
  -Dcucumber.filter.tags="@login-simple" 2>&1 | tee test-output.log

# Check if actions are in the report JSON
echo ""
echo "Checking for actions in report data..."
if grep -q '"actions"' cs-reports/report-data.json; then
    echo "✓ Actions found in report data!"
    echo ""
    echo "Sample action data:"
    grep -A5 -B5 '"actions"' cs-reports/report-data.json | head -20
else
    echo "✗ No actions found in report data"
fi

echo ""
echo "Test complete. Check cs-reports/test-run-*/index.html for the full report."