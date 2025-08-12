#!/bin/bash

echo "==========================================="
echo "Testing Browser Reuse Configuration"
echo "==========================================="
echo ""

# Function to count Chrome processes
count_chrome_processes() {
    ps aux | grep -E "[c]hrome|[c]hromedriver" | grep -v defunct | wc -l
}

# Function to run test with specific reuse setting
test_browser_reuse() {
    local reuse_setting=$1
    local test_name=$2
    
    echo ""
    echo "-------------------------------------------"
    echo "Test: $test_name"
    echo "Setting: browser.reuse.instance=$reuse_setting"
    echo "-------------------------------------------"
    
    # Clean up any existing Chrome processes
    pkill -f chrome 2>/dev/null
    pkill -f chromedriver 2>/dev/null
    sleep 2
    
    echo "Initial Chrome processes: $(count_chrome_processes)"
    
    # Create a simple feature file with 3 tests
    cat > features/test-browser-reuse.feature << 'EOF'
@browsertest
Feature: Test Browser Reuse

  @test1
  Scenario: Test 1 - Simple login
    Given I am on the login page
    When I enter username "Admin" and password "admin123"
    Then I click the login button

  @test2
  Scenario: Test 2 - Invalid login
    Given I am on the login page
    When I login with username "invalid" and password "wrong"
    Then I should see an error message "Invalid credentials"

  @test3
  Scenario: Test 3 - Navigate to login
    Given I am on the login page
    Then I log "Test completed"
EOF

    # Run test with specified reuse setting
    echo "Running tests..."
    timeout 90 mvn test \
        -Dtest=CSBDDRunner \
        -DfeaturesPath=features/test-browser-reuse.feature \
        -DcucumberOptions="--tags @browsertest" \
        -Dbrowser.reuse.instance=$reuse_setting \
        -Dthread.count=1 \
        -Dparallel.core.threads=1 2>&1 | tee test-output-$reuse_setting.log > /dev/null &
    
    TEST_PID=$!
    
    # Monitor browser activity
    MAX_BROWSERS=0
    BROWSER_OPENS=0
    PREV_COUNT=0
    
    for i in {1..60}; do
        sleep 1
        CURRENT=$(count_chrome_processes)
        
        # Detect new browser opening
        if [ $CURRENT -gt $PREV_COUNT ] && [ $PREV_COUNT -eq 0 ]; then
            BROWSER_OPENS=$((BROWSER_OPENS + 1))
            echo "  [${i}s] Browser #$BROWSER_OPENS opened (processes: $CURRENT)"
        fi
        
        # Track max browsers
        if [ $CURRENT -gt $MAX_BROWSERS ]; then
            MAX_BROWSERS=$CURRENT
        fi
        
        # Detect browser closing
        if [ $CURRENT -eq 0 ] && [ $PREV_COUNT -gt 0 ]; then
            echo "  [${i}s] Browser closed"
        fi
        
        PREV_COUNT=$CURRENT
        
        # Check if test finished
        if ! ps -p $TEST_PID > /dev/null 2>&1; then
            break
        fi
    done
    
    wait $TEST_PID 2>/dev/null
    
    # Check logs for browser creation/reuse messages
    echo ""
    echo "Browser Management Log:"
    grep -E "Creating NEW.*driver|Reusing existing driver|Closing browser after test|Keeping browser open" test-output-$reuse_setting.log | head -10
    
    # Final check
    sleep 2
    FINAL_COUNT=$(count_chrome_processes)
    
    echo ""
    echo "Results:"
    echo "  Browser opens detected: $BROWSER_OPENS"
    echo "  Remaining browsers: $FINAL_COUNT"
    
    if [ "$reuse_setting" = "true" ]; then
        if [ $BROWSER_OPENS -eq 1 ]; then
            echo "  ✓ Correct: Single browser instance reused for all tests"
        else
            echo "  ✗ Issue: Expected 1 browser open, got $BROWSER_OPENS"
        fi
    else
        if [ $BROWSER_OPENS -eq 3 ]; then
            echo "  ✓ Correct: New browser created for each test (3 tests = 3 browsers)"
        else
            echo "  ✗ Issue: Expected 3 browser opens, got $BROWSER_OPENS"
        fi
    fi
    
    # Cleanup
    pkill -f chrome 2>/dev/null
    pkill -f chromedriver 2>/dev/null
    rm -f test-output-$reuse_setting.log
}

# Test with browser reuse enabled (default)
test_browser_reuse "true" "Browser Reuse ENABLED (default)"

# Test with browser reuse disabled
test_browser_reuse "false" "Browser Reuse DISABLED"

# Cleanup
rm -f features/test-browser-reuse.feature

echo ""
echo "==========================================="
echo "Test Summary:"
echo "- browser.reuse.instance=true: Reuses same browser for all tests"
echo "- browser.reuse.instance=false: Creates new browser for each test"
echo "==========================================="