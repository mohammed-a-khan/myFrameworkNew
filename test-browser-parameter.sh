#!/bin/bash

echo "==========================================="
echo "Testing Browser Parameter from TestNG XML"
echo "==========================================="
echo ""

# Create a simple test feature
cat > features/test-browser-param.feature << 'EOF'
@browsertest
Feature: Test Browser Parameter

  Scenario: Test Browser Selection
    Given I am on the login page
    Then I log "Testing browser parameter from TestNG XML"
    And I wait for 1 seconds
EOF

# Create test suite with IE browser
cat > suites/test-ie-param.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="IE Parameter Test Suite" parallel="false">
    
    <parameter name="browser.name" value="ie"/>
    <parameter name="browser.headless" value="false"/>
    
    <test name="IE Parameter Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

echo "Testing with IE browser parameter..."
echo ""
echo "NOTE: This will only work on Windows with IE properly configured."
echo "If you're not on Windows, the test will fail to launch IE."
echo ""

# Run test with IE parameter
mvn test \
    -DsuiteXmlFile=suites/test-ie-param.xml \
    -Dbdd.features.path=features/test-browser-param.feature \
    -Dbdd.tags.include=@browsertest \
    -Dthread.count=1 2>&1 | tee browser-param-test.log

# Check if IE was actually used
if grep -q "Using browser from TestNG parameter: ie" browser-param-test.log; then
    echo ""
    echo "✓ Browser parameter correctly read from TestNG XML!"
    echo "  IE browser was requested as expected"
else
    echo ""
    echo "✗ Browser parameter not properly read from TestNG XML"
    echo "  Check the logs for details"
fi

if grep -q "Creating Internet Explorer driver" browser-param-test.log; then
    echo "✓ Internet Explorer driver creation attempted"
else
    echo "✗ Internet Explorer driver was not created"
fi

# Cleanup
rm -f features/test-browser-param.feature
rm -f suites/test-ie-param.xml
rm -f browser-param-test.log

echo ""
echo "==========================================="
echo "Browser Parameter Test Complete"
echo "==========================================="