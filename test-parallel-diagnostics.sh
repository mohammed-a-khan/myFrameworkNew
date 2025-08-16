#!/bin/bash

echo "Parallel Execution Diagnostics"
echo "=============================="
echo ""
echo "This test will diagnose parallel execution issues"
echo ""

# Clean up
rm -rf target/surefire-reports/* 2>/dev/null
rm -rf cs-reports/* 2>/dev/null
rm -f parallel-diagnostic.log

echo "Running parallel BDD test with detailed logging..."
echo "--------------------------------------------------"

# Run with extra debugging
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
         -Dcs.browser.headless=false \
         -Dcs.browser.reuse.instance=false \
         -X 2>&1 | tee parallel-diagnostic.log

echo ""
echo "Analyzing execution patterns..."
echo "--------------------------------"

# Check thread distribution
echo ""
echo "1. Thread Activity:"
echo "   ---------------"
grep -E "Starting test #[0-9]+ for scenario" parallel-diagnostic.log | head -20

echo ""
echo "2. Browser Creation Timeline:"
echo "   -------------------------"
grep -E "Creating NEW .* driver|Driver properly initialized" parallel-diagnostic.log | head -20

echo ""
echo "3. Thread Distribution:"
echo "   -------------------"
grep "Thread distribution after" parallel-diagnostic.log | tail -5

echo ""
echo "4. Test Execution Order:"
echo "   ---------------------"
grep -E "Test execution #[0-9]+ - Thread:" parallel-diagnostic.log | head -20

echo ""
echo "5. DataProvider Configuration:"
echo "   --------------------------"
grep -E "data-provider-thread-count|DataProvider returning|parallel=" parallel-diagnostic.log | head -10

echo ""
echo "6. Potential Issues:"
echo "   -----------------"
# Check for synchronization issues
SYNC_ISSUES=$(grep -c "synchronized\|lock\|wait" parallel-diagnostic.log)
echo "   - Synchronization mentions: $SYNC_ISSUES"

# Check for thread starvation
THREAD_COUNT=$(grep "pool-.*-thread-" parallel-diagnostic.log | cut -d':' -f1 | sort -u | wc -l)
echo "   - Unique threads used: $THREAD_COUNT"

# Check test distribution
echo ""
echo "7. Test Distribution by Thread:"
echo "   ---------------------------"
grep "pool-.*-thread-" parallel-diagnostic.log | cut -d':' -f1 | sort | uniq -c | head -10

echo ""
echo "8. Timing Analysis:"
echo "   ----------------"
# Check for slow operations
grep -E "took [0-9]+ ms|elapsed|duration" parallel-diagnostic.log | grep -v "0 ms" | head -10

echo ""
echo "Summary:"
echo "--------"
if [ "$THREAD_COUNT" -lt 2 ]; then
    echo "❌ ISSUE: Only $THREAD_COUNT thread(s) detected. Expected 2 for parallel execution."
elif grep -q "Starting test #1 for scenario.*pool-.*-thread-1" parallel-diagnostic.log && \
     grep -q "Starting test #2 for scenario.*pool-.*-thread-2" parallel-diagnostic.log; then
    echo "✅ Tests are distributed across multiple threads"
else
    echo "⚠️  Tests may not be properly distributed across threads"
fi

echo ""
echo "Full log saved to: parallel-diagnostic.log"
echo "Check HTML report at: cs-reports/latest-report.html"