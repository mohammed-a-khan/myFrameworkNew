#!/bin/bash

echo "Testing if CSHtmlReportGenerator is being called"

# Set encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

# Clean up old reports
rm -rf target/test-run-* target/cs-reports/ target/reports/

# Run a simple test with debug logging for reporting
echo "Running test with report generation debugging..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test -Dsurefire.suiteXmlFiles=suites/simple-threadlocal-test.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -q

echo ""
echo "=== Report Generation Analysis ==="

# Check for CSHtmlReportGenerator logs in surefire reports
if ls target/surefire-reports/*.txt >/dev/null 2>&1; then
    echo "Searching for CSHtmlReportGenerator activity in test logs..."
    grep -i "CSHtmlReportGenerator\|generateReport\|Screenshot embedding" target/surefire-reports/*.txt || echo "No CSHtmlReportGenerator logs found"
else
    echo "No surefire report text files found"
fi

# Check if any custom reports were generated
echo ""
echo "Checking for generated custom reports:"
find target -name "*cs_test_run_report*" -o -name "*cs-report*" -o -name "test-run-*" -type d 2>/dev/null || echo "No custom reports found"

# Check if CSReportManager data exists
echo ""
echo "Checking for CSReportManager data files:"
find target -name "*report-data.json*" -o -name "*trend-data*" 2>/dev/null || echo "No CSReportManager data files found"

# Check what's actually in target directory
echo ""
echo "Contents of target directory:"
ls -la target/ 2>/dev/null || echo "No target directory"

echo ""
echo "Contents of target/surefire-reports/:"
ls -la target/surefire-reports/ 2>/dev/null || echo "No surefire-reports directory"