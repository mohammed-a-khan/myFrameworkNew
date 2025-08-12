#!/bin/bash

echo "==========================================="
echo "Internet Explorer Browser Test"
echo "==========================================="
echo ""
echo "NOTE: This test will only work on Windows machines with IE installed"
echo "Please ensure you have completed the IE setup as per IE_SETUP_GUIDE.md"
echo ""

# Check if running on Windows (WSL or Git Bash)
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ -n "$WSL_DISTRO_NAME" ]]; then
    echo "Detected Windows environment"
else
    echo "Warning: IE only works on Windows. This test may fail on Linux/Mac."
    echo "Consider using Edge or Chrome for cross-platform testing."
fi

echo ""
echo "Testing IE browser initialization..."

# Create a simple test feature
cat > features/test-ie.feature << 'EOF'
@ietest
Feature: Test Internet Explorer Support

  Scenario: Test IE Browser Launch
    Given I am on the login page
    Then I log "IE browser launched successfully"
    And I wait for 2 seconds
    And I take a screenshot "ie_test_screenshot"
EOF

echo "Running test with Internet Explorer..."
echo ""

# Run test with IE
mvn test \
    -Dtest=CSBDDRunner \
    -DfeaturesPath=features/test-ie.feature \
    -DcucumberOptions="--tags @ietest" \
    -Dbrowser.name=ie \
    -Die.ensure.clean.session=true \
    -Die.ignore.protected.mode=true \
    -Die.ignore.security.domains=true \
    -Dthread.count=1 2>&1 | tee ie-test.log

# Check results
if grep -q "BUILD SUCCESS" ie-test.log; then
    echo ""
    echo "✓ Internet Explorer test completed successfully!"
    echo ""
    echo "Browser capabilities used:"
    grep -E "Creating Internet Explorer driver|ignore.*domains|InternetExplorerOptions" ie-test.log | head -5
else
    echo ""
    echo "✗ Internet Explorer test failed"
    echo ""
    echo "Common issues:"
    echo "1. Protected Mode settings not consistent across zones"
    echo "2. Zoom level not at 100%"
    echo "3. IEDriverServer.exe not found"
    echo "4. Running on non-Windows platform"
    echo ""
    echo "Error details:"
    grep -E "ERROR|FAIL|Exception" ie-test.log | head -10
fi

# Cleanup
rm -f features/test-ie.feature
rm -f ie-test.log

echo ""
echo "==========================================="
echo "For detailed IE setup instructions, see IE_SETUP_GUIDE.md"
echo "==========================================="