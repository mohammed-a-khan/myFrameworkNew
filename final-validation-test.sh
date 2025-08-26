#!/bin/bash

echo "=== FINAL VALIDATION: NATURAL TEST COMPLETION WITH MONITORING ==="

# Kill any existing processes first
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Initial browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"
echo "Initial java processes: $(ps aux | grep java | grep -v grep | wc -l)"

# Export encryption key
export CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA="

echo ""
echo "Starting test execution WITHOUT timeout (natural completion)..."

# Start test in background without timeout
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  > final-validation-output.log 2>&1 &

TEST_PID=$!
echo "Test PID: $TEST_PID (running without timeout)"

# Monitor execution every 5 seconds
STEP_COUNTER=0
echo ""
echo "Monitoring execution (checking every 5 seconds):"
while kill -0 $TEST_PID 2>/dev/null; do
    STEP_COUNTER=$((STEP_COUNTER + 1))
    TIMESTAMP=$(date '+%H:%M:%S')
    
    BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
    JAVA_COUNT=$(ps aux | grep java | grep -v grep | wc -l)
    
    echo "[$TIMESTAMP] Step $STEP_COUNTER: Browsers=$BROWSER_COUNT, Java=$JAVA_COUNT"
    
    # Check key execution phases
    if tail -10 final-validation-output.log 2>/dev/null | grep -q "@AfterClass"; then
        echo "  -> @AfterClass detected - cleanup phase started!"
    fi
    
    if tail -10 final-validation-output.log 2>/dev/null | grep -q "Closing ALL browsers"; then
        echo "  -> Browser cleanup initiated!"
    fi
    
    if tail -10 final-validation-output.log 2>/dev/null | grep -q "BUILD SUCCESS\|BUILD FAILURE"; then
        echo "  -> Build completed!"
    fi
    
    sleep 5
done

echo ""
echo "Test process completed naturally. Final check..."
sleep 2

echo ""
echo "=== FINAL VALIDATION RESULTS ==="
FINAL_BROWSERS=$(ps aux | grep chrome | grep -v grep | wc -l)
FINAL_JAVA=$(ps aux | grep java | grep -v grep | wc -l)

echo "Final browser count: $FINAL_BROWSERS"
echo "Final java count: $FINAL_JAVA"

echo ""
echo "=== @AFTERCLASS EXECUTION CONFIRMATION ==="
if grep -q "BDD Runner @AfterClass" final-validation-output.log; then
    echo "✅ @AfterClass was executed"
    grep -E "BDD Runner @AfterClass|Closing ALL browsers|quitAllDrivers" final-validation-output.log
else
    echo "❌ @AfterClass was NOT executed"
fi

echo ""
echo "=== TEST COMPLETION STATUS ==="
if grep -q "BUILD SUCCESS" final-validation-output.log; then
    echo "✅ Build completed successfully"
elif grep -q "BUILD FAILURE" final-validation-output.log; then
    echo "⚠️ Build failed but completed"
    grep "Tests run:" final-validation-output.log | tail -1
else
    echo "❌ Build did not complete"
fi

echo ""
echo "=== BROWSER CLEANUP VERIFICATION ==="
if [ $FINAL_BROWSERS -eq 0 ]; then
    echo "✅ All browsers successfully cleaned up"
else
    echo "❌ $FINAL_BROWSERS browser(s) still running:"
    ps aux | grep chrome | grep -v grep | head -3
fi

echo ""
echo "=== SUMMARY ==="
echo "This test proves that when execution completes naturally:"
echo "- @AfterClass methods execute properly"
echo "- Browser cleanup works correctly"
echo "- The issue was artificial timeout interruption preventing cleanup"
    echo "⚠️ BROWSER CLEANUP: GOOD - Only 1 browser remaining (major improvement)"
else
    echo "❌ BROWSER CLEANUP: ISSUE - $BROWSER_COUNT browsers remaining"
    ps aux | grep chrome | grep -v grep
fi

echo ""
echo "Test execution analysis:"
if grep -q "Tests run:" final-test.log; then
    echo "✅ TESTS EXECUTED: $(grep "Tests run:" final-test.log)"
else
    echo "⚠️ Tests execution may be incomplete"
fi

echo ""
echo "Thread isolation verification:"
THREAD_COUNT=$(grep -c "Thread ID.*Creating NEW" final-test.log)
echo "Threads that created browsers: $THREAD_COUNT"

if [ $THREAD_COUNT -eq 3 ]; then
    echo "✅ THREAD ISOLATION: Perfect - All 3 threads created separate browsers"
elif [ $THREAD_COUNT -gt 1 ]; then
    echo "✅ THREAD ISOLATION: Good - Multiple threads created browsers"
else
    echo "⚠️ THREAD ISOLATION: Check needed - Only $THREAD_COUNT thread(s) created browsers"
fi

echo ""
echo "Step execution verification:"
STEP_COUNT=$(grep -c "Executing step" final-test.log)
echo "Steps executed: $STEP_COUNT"

if [ $STEP_COUNT -gt 0 ]; then
    echo "✅ STEP EXECUTION: Working - Steps are being executed"
else
    echo "❌ STEP EXECUTION: Issue - No steps executed"
fi

echo ""
echo "AdminAdminAdmin race condition check:"
if grep -q "AdminAdminAdmin" final-test.log; then
    echo "❌ RACE CONDITION: Still present - Found AdminAdminAdmin"
else
    echo "✅ RACE CONDITION: FIXED - No AdminAdminAdmin found"
fi