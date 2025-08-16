#!/bin/bash

echo "Testing BDD Failure Analysis Feature"
echo "====================================="
echo ""
echo "This test will verify that failure analysis works for BDD tests"
echo ""

# Clean up
rm -rf cs-reports/* 2>/dev/null
rm -f failure-analysis-test.log

echo "Running BDD tests that will fail to demonstrate failure analysis..."
echo "-------------------------------------------------------------------"

# Run the tests (they will fail, which is expected for this test)
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
         -Dcs.browser.headless=true 2>&1 | tee failure-analysis-test.log

echo ""
echo "Checking Failure Analysis Results..."
echo "------------------------------------"

# Check if failure analysis was performed
echo ""
echo "1. Failure Analysis Execution:"
grep -E "Failure Analysis for.*Category=.*Flaky=.*Score=" failure-analysis-test.log | head -10

# Count flaky vs genuine failures
FLAKY_COUNT=$(grep -c "Flaky=true" failure-analysis-test.log)
GENUINE_COUNT=$(grep -c "Flaky=false" failure-analysis-test.log)

echo ""
echo "2. Failure Categorization:"
echo "   - Flaky failures detected: $FLAKY_COUNT"
echo "   - Genuine failures detected: $GENUINE_COUNT"

# Check specific failure categories
echo ""
echo "3. Failure Categories Found:"
grep -oE "Category=[^,]+" failure-analysis-test.log | sort -u

echo ""
echo "4. Flakiness Scores:"
grep -oE "Score=[0-9.]+" failure-analysis-test.log | head -10

# Extract the report path
REPORT_PATH=$(ls -t cs-reports/test-run-*/cs_test_run_report.html 2>/dev/null | head -1)

if [ -f "$REPORT_PATH" ]; then
    echo ""
    echo "5. Checking HTML Report for Failure Analysis..."
    echo "   ---------------------------------------------"
    
    # Check if failure analysis sections exist in HTML
    if grep -q "Intelligent Failure Analysis" "$REPORT_PATH"; then
        echo "   ✅ Intelligent Failure Analysis section found in report"
    else
        echo "   ❌ Intelligent Failure Analysis section NOT found in report"
    fi
    
    if grep -q "Flaky Tests" "$REPORT_PATH"; then
        echo "   ✅ Flaky Tests section found in report"
        # Extract flaky test count from HTML
        FLAKY_IN_REPORT=$(grep -oE ">Flaky Tests</div>.*metric-value\">[0-9]+" "$REPORT_PATH" | grep -oE "[0-9]+$" | head -1)
        echo "   📊 Flaky tests shown in report: ${FLAKY_IN_REPORT:-0}"
    else
        echo "   ❌ Flaky Tests section NOT found in report"
    fi
    
    if grep -q "Test Reliability Metrics" "$REPORT_PATH"; then
        echo "   ✅ Test Reliability Metrics section found in report"
    else
        echo "   ❌ Test Reliability Metrics section NOT found in report"
    fi
    
    if grep -q "Recommended Actions" "$REPORT_PATH"; then
        echo "   ✅ Recommended Actions found in failure analysis"
    else
        echo "   ⚠️  Recommended Actions not found (may not have failures)"
    fi
fi

echo ""
echo "========================================="
echo "Summary:"
echo "========================================="

if [ "$FLAKY_COUNT" -gt 0 ] || [ "$GENUINE_COUNT" -gt 0 ]; then
    echo "✅ Failure analysis is working!"
    echo "   - Detected $FLAKY_COUNT flaky failures"
    echo "   - Detected $GENUINE_COUNT genuine failures"
    echo ""
    echo "Check the full report at: $REPORT_PATH"
    echo "The report should show:"
    echo "  • Flaky test count in Test Reliability Metrics"
    echo "  • Detailed failure analysis for each failed test"
    echo "  • Root cause and recommendations"
else
    echo "⚠️  No failure analysis detected in logs"
    echo "   This could mean:"
    echo "   - All tests passed (no failures to analyze)"
    echo "   - Failure analysis not triggering properly"
fi

echo ""
echo "Log saved to: failure-analysis-test.log"