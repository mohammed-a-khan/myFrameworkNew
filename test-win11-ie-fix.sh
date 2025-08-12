#!/bin/bash

echo "==========================================="
echo "Testing Windows 11 IE to Edge Auto-Switch"
echo "==========================================="
echo ""
echo "This test will:"
echo "1. Detect if you're on Windows 11"
echo "2. Automatically use Edge instead of IE if on Windows 11"
echo "3. Use regular IE if on Windows 10 or earlier"
echo ""

# Create a simple test feature
cat > features/test-win11-ie.feature << 'EOF'
@win11test
Feature: Test Windows 11 IE Compatibility

  Scenario: Test Browser Launch on Windows 11
    Given I am on the login page
    Then I log "Browser launched successfully"
    And I wait for 2 seconds
    And I take a screenshot "win11_browser_test"
EOF

# Create test suite with IE browser
cat > suites/test-win11-ie.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Windows 11 IE Test Suite" parallel="none">
    
    <!-- Still specify IE - framework will auto-switch to Edge on Win11 -->
    <parameter name="browser.name" value="ie"/>
    <parameter name="browser.headless" value="false"/>
    <parameter name="thread.count" value="1"/>
    
    <test name="Windows 11 IE Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

echo "Running test with IE parameter (will auto-switch to Edge on Windows 11)..."
echo ""

# Run test
mvn test \
    -DsuiteXmlFile=suites/test-win11-ie.xml \
    -Dbdd.features.path=features/test-win11-ie.feature \
    -Dbdd.tags.include=@win11test \
    -Dthread.count=1 2>&1 | tee win11-ie-test.log

# Check results
echo ""
echo "==========================================="
echo "Test Results:"
echo "==========================================="

if grep -q "Windows 11 detected - IE is not available. Automatically switching to Edge" win11-ie-test.log; then
    echo "✓ Windows 11 detected - automatic switch to Edge occurred"
    echo "✓ Framework correctly handled IE retirement on Windows 11"
else
    if grep -q "InternetExplorerDriver created successfully" win11-ie-test.log; then
        echo "✓ Running on Windows 10 or earlier - IE driver used"
    fi
fi

if grep -q "Edge driver created successfully in IE compatibility mode" win11-ie-test.log; then
    echo "✓ Edge driver successfully created for Windows 11"
fi

if grep -q "BUILD SUCCESS" win11-ie-test.log; then
    echo ""
    echo "✓ Test completed successfully!"
else
    echo ""
    echo "✗ Test failed - check win11-ie-test.log for details"
fi

# Cleanup
rm -f features/test-win11-ie.feature
rm -f suites/test-win11-ie.xml

echo ""
echo "==========================================="
echo "Summary:"
echo "- On Windows 11: Framework automatically uses Edge"
echo "- On Windows 10: Framework uses Internet Explorer"
echo "- No code changes needed - just use browser.name=ie"
echo "==========================================="