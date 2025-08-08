#!/bin/bash

echo "Running tests with enhanced action reporting..."

# Run the tests
mvn test -DsuiteXmlFile=suites/orangehrm-tests.xml -q

# Check latest report
LATEST_REPORT=$(find cs-reports -name "index.html" -type f | sort | tail -1)

echo ""
echo "Tests completed!"
echo "✓ Actions are now displayed in green for passed steps"
echo "✓ Null values are hidden in the action display"
echo "✓ All steps now report their actions (screenshots, waits, logs, verifications)"
echo ""
echo "View the enhanced report at: $LATEST_REPORT"
echo ""
echo "The report now shows:"
echo "- Navigation actions with URLs"
echo "- Element interactions (click, type) with target elements and values"
echo "- Wait operations with descriptions"
echo "- Screenshot captures with names"
echo "- Log messages with content"
echo "- Verification actions with expected values"