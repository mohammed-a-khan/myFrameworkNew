#!/bin/bash

echo "==========================================="
echo "Testing IE Driver W3C Capability Fix"
echo "==========================================="
echo ""
echo "NOTE: This test requires:"
echo "1. Windows OS with Internet Explorer installed"
echo "2. IE security settings properly configured"
echo "3. Protected Mode settings consistent across all zones"
echo ""

# Create a minimal test feature
cat > features/test-ie-fix.feature << 'EOF'
@iefix
Feature: Test IE Driver Fix

  Scenario: Test IE Browser Launch
    Given I am on the login page
    Then I log "IE browser launched successfully without W3C errors"
    And I wait for 2 seconds
EOF

# Create test suite with IE browser
cat > suites/test-ie-fix.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="IE Fix Test Suite" parallel="false">
    
    <parameter name="browser.name" value="ie"/>
    <parameter name="browser.headless" value="false"/>
    <parameter name="thread.count" value="1"/>
    
    <test name="IE Fix Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

echo "Testing IE driver initialization with W3C compliant capabilities..."
echo ""

# Run test with IE
timeout 30 mvn test \
    -DsuiteXmlFile=suites/test-ie-fix.xml \
    -Dbdd.features.path=features/test-ie-fix.feature \
    -Dbdd.tags.include=@iefix \
    -Dthread.count=1 2>&1 | tee ie-fix-test.log

# Check for W3C capability errors
if grep -q "Illegal key values seen in w3c capabilities.*javascriptEnabled" ie-fix-test.log; then
    echo ""
    echo "✗ W3C capability error still present - javascriptEnabled issue not fixed"
else
    echo ""
    echo "✓ No W3C capability errors for javascriptEnabled"
fi

if grep -q "Support for Legacy Capabilities is deprecated" ie-fix-test.log; then
    echo "⚠ Warning: Some legacy capabilities still being used"
    grep "invalid capabilities:" ie-fix-test.log | head -2
else
    echo "✓ No legacy capability warnings"
fi

if grep -q "Creating Internet Explorer driver" ie-fix-test.log; then
    echo "✓ IE driver creation attempted"
else
    echo "✗ IE driver was not created"
fi

if grep -q "SessionNotCreatedException" ie-fix-test.log; then
    echo "✗ Session creation failed - check IE configuration:"
    echo "  1. Ensure Protected Mode is same for all security zones"
    echo "  2. Ensure zoom level is at 100%"
    echo "  3. Ensure Enhanced Protected Mode is disabled"
    grep -A 2 "SessionNotCreatedException" ie-fix-test.log | head -3
else
    echo "✓ No session creation exceptions"
fi

# Cleanup
rm -f features/test-ie-fix.feature
rm -f suites/test-ie-fix.xml
rm -f ie-fix-test.log

echo ""
echo "==========================================="
echo "IE Driver Fix Test Complete"
echo "==========================================="
echo ""
echo "If IE still fails to start, please check:"
echo "1. Internet Options → Security → All zones have same Protected Mode setting"
echo "2. Internet Options → Advanced → Uncheck 'Enable Enhanced Protected Mode'"
echo "3. View → Zoom → Set to 100%"
echo "4. Run this script as Administrator if needed"