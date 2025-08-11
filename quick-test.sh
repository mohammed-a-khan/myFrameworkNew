#!/bin/bash

echo "Running quick test..."
timeout 60 mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -q 2>&1 | tail -20

echo ""
echo "Checking latest report..."
LATEST=$(find cs-reports -name "test-run-*" -type d | sort | tail -1)
if [ -n "$LATEST" ]; then
    echo "Latest report: $LATEST"
    echo "Screenshots in report:"
    grep -o "screenshots: \[[^]]*\]" "$LATEST/index.html" 2>/dev/null | head -2
fi