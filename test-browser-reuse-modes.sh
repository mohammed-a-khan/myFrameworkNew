#!/bin/bash

echo "Testing Browser Reuse in Different Modes"
echo "========================================="
echo ""

# Function to run test and analyze results
run_test_mode() {
    local MODE=$1
    local REUSE=$2
    local PARALLEL=$3
    
    echo ""
    echo "Test Configuration:"
    echo "  - Execution Mode: $MODE"
    echo "  - Browser Reuse: $REUSE"
    echo "  - Parallel: $PARALLEL"
    echo "-----------------------------------"
    
    # Clean previous results
    rm -rf target/surefire-reports/* 2>/dev/null
    
    # Create a simple test suite
    if [ "$PARALLEL" == "yes" ]; then
        cat > suites/test-reuse-${MODE}.xml << EOF
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Browser Reuse Test - ${MODE}" parallel="methods" thread-count="2">
    <parameter name="browser.name" value="chrome"/>
    <parameter name="cs.browser.headless" value="false"/>
    <parameter name="cs.browser.reuse.instance" value="${REUSE}"/>
    <parameter name="cs.bdd.features.path" value="features/orangehrm-simple-tests.feature"/>
    <parameter name="cs.bdd.tags" value="@simple"/>
    <test name="Browser Reuse Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF
    else
        cat > suites/test-reuse-${MODE}.xml << EOF
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Browser Reuse Test - ${MODE}" parallel="none">
    <parameter name="browser.name" value="chrome"/>
    <parameter name="cs.browser.headless" value="false"/>
    <parameter name="cs.browser.reuse.instance" value="${REUSE}"/>
    <parameter name="cs.bdd.features.path" value="features/orangehrm-simple-tests.feature"/>
    <parameter name="cs.bdd.tags" value="@simple"/>
    <test name="Browser Reuse Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF
    fi
    
    # Run the test
    echo "Running test..."
    mvn test -Dsurefire.suiteXmlFiles=suites/test-reuse-${MODE}.xml \
             -Dcs.browser.reuse.instance=${REUSE} 2>&1 | tee test-${MODE}.log > /dev/null
    
    # Analyze results
    echo ""
    echo "Results:"
    BROWSERS_CREATED=$(grep -c "Creating NEW .* driver" test-${MODE}.log)
    BROWSERS_REUSED=$(grep -c "Reusing existing driver" test-${MODE}.log)
    BROWSERS_CLOSED=$(grep -c "Closing.*browser\|Closing.*driver\|quitDriver" test-${MODE}.log)
    THREADS_USED=$(grep "pool-.*-thread-" test-${MODE}.log | cut -d':' -f1 | sort -u | wc -l)
    
    echo "  ✓ Browsers created: $BROWSERS_CREATED"
    echo "  ✓ Browsers reused: $BROWSERS_REUSED"
    echo "  ✓ Browsers closed: $BROWSERS_CLOSED"
    echo "  ✓ Threads used: $THREADS_USED"
    
    # Determine if behavior is correct
    if [ "$PARALLEL" == "yes" ] && [ "$REUSE" == "true" ]; then
        echo ""
        echo "  Expected: Each thread creates 1 browser and reuses it"
        if [ "$BROWSERS_CREATED" -ge "$THREADS_USED" ] && [ "$BROWSERS_REUSED" -gt 0 ]; then
            echo "  ✅ CORRECT: Browsers are reused within threads"
        else
            echo "  ⚠️  CHECK: Browser reuse may not be working as expected"
        fi
    elif [ "$PARALLEL" == "yes" ] && [ "$REUSE" == "false" ]; then
        echo ""
        echo "  Expected: New browser for each test"
        if [ "$BROWSERS_CREATED" -gt "$THREADS_USED" ]; then
            echo "  ✅ CORRECT: New browser created for each test"
        else
            echo "  ⚠️  CHECK: Should create more browsers"
        fi
    elif [ "$PARALLEL" == "no" ] && [ "$REUSE" == "true" ]; then
        echo ""
        echo "  Expected: Single browser reused for all tests"
        if [ "$BROWSERS_CREATED" -eq 1 ] && [ "$BROWSERS_REUSED" -gt 0 ]; then
            echo "  ✅ CORRECT: Single browser reused"
        else
            echo "  ⚠️  CHECK: Should reuse single browser"
        fi
    else
        echo ""
        echo "  Expected: New browser for each test (sequential)"
        if [ "$BROWSERS_CREATED" -gt 1 ]; then
            echo "  ✅ CORRECT: Multiple browsers created"
        else
            echo "  ⚠️  CHECK: Should create multiple browsers"
        fi
    fi
}

# Test 1: Sequential with reuse=true
echo ""
echo "TEST 1: Sequential Execution with Browser Reuse"
echo "================================================"
run_test_mode "seq-reuse" "true" "no"

# Test 2: Sequential with reuse=false
echo ""
echo ""
echo "TEST 2: Sequential Execution without Browser Reuse"
echo "==================================================="
run_test_mode "seq-no-reuse" "false" "no"

# Test 3: Parallel with reuse=true
echo ""
echo ""
echo "TEST 3: Parallel Execution with Browser Reuse"
echo "=============================================="
run_test_mode "parallel-reuse" "true" "yes"

# Test 4: Parallel with reuse=false
echo ""
echo ""
echo "TEST 4: Parallel Execution without Browser Reuse"
echo "================================================="
run_test_mode "parallel-no-reuse" "false" "yes"

echo ""
echo ""
echo "========================================="
echo "SUMMARY OF BROWSER REUSE BEHAVIOR:"
echo "========================================="
echo ""
echo "1. Sequential + reuse=true:  One browser for all tests (FASTEST)"
echo "2. Sequential + reuse=false: New browser per test (SLOWER)"
echo "3. Parallel + reuse=true:    One browser per thread (OPTIMAL)"
echo "4. Parallel + reuse=false:   New browser per test (ISOLATED)"
echo ""
echo "Recommendation:"
echo "- Use reuse=true with parallel execution for best performance"
echo "- Each thread maintains its own browser, providing isolation"
echo "- Tests within same thread reuse the browser, saving time"
echo ""

# Clean up
rm -f suites/test-reuse-*.xml
echo "Test files cleaned up."