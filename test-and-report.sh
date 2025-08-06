#!/bin/bash

# Run tests and ensure report is generated

echo "========================================="
echo "Running Tests with Report Generation"
echo "========================================="

# Clean previous reports
echo "Cleaning previous reports..."
rm -rf target/surefire-reports
mkdir -p cs-reports

# Run tests
echo "Running tests..."
mvn clean test \
  -DsuiteXmlFile=suites/orangehrm-tests.xml \
  -Dbrowser.name=chrome \
  -Dbrowser.headless=true \
  -Denvironment.name=qa \
  -Dtest.screenshot.on.failure=true

# Check if report was generated
echo ""
echo "========================================="
echo "Checking Report Generation..."
echo "========================================="

# Find the latest report
if [ -f "cs-reports/latest-report.html" ]; then
    REPORT_PATH=$(grep -oP 'url=\K[^"]+' "cs-reports/latest-report.html" 2>/dev/null || echo "")
    if [ -n "$REPORT_PATH" ] && [ -f "cs-reports/$REPORT_PATH" ]; then
        echo "✅ Report generated successfully!"
        echo "📍 Location: cs-reports/$REPORT_PATH"
        
        # Show report stats
        REPORT_SIZE=$(du -h "cs-reports/$REPORT_PATH" | cut -f1)
        echo "📊 Report size: $REPORT_SIZE"
        
        # Extract test summary if possible
        echo ""
        echo "Test Summary:"
        grep -oP 'Total:.*?</div>' "cs-reports/$REPORT_PATH" | sed 's/<[^>]*>//g' | head -3
    else
        echo "❌ Report file not found at expected location"
    fi
else
    # Look for any report in test-run directories
    LATEST_DIR=$(ls -dt cs-reports/test-run-* 2>/dev/null | head -1)
    if [ -n "$LATEST_DIR" ] && [ -f "$LATEST_DIR/index.html" ]; then
        echo "✅ Report found in: $LATEST_DIR/index.html"
        REPORT_SIZE=$(du -h "$LATEST_DIR/index.html" | cut -f1)
        echo "📊 Report size: $REPORT_SIZE"
    else
        echo "❌ No report generated!"
        echo ""
        echo "Troubleshooting:"
        echo "1. Check if tests ran: cat target/surefire-reports/*.xml | grep testcase"
        echo "2. Check logs: grep -i 'report' target/surefire-reports/*.txt"
        echo "3. Check report directory: ls -la cs-reports/"
    fi
fi

echo ""
echo "========================================="
echo "To view the report, run: ./open-report.sh"
echo "========================================="