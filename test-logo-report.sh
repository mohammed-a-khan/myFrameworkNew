#!/bin/bash

echo "==========================================="
echo "Testing Logo Integration in HTML Reports"
echo "==========================================="
echo ""

# Check if logo.png exists
if [ -f "logo.png" ]; then
    echo "✓ Logo file found: logo.png"
    echo "  Size: $(identify -format "%wx%h" logo.png 2>/dev/null || echo "Unable to determine size")"
else
    echo "✗ Logo file not found in project root!"
    echo "  Please ensure logo.png is in the project root directory"
    exit 1
fi

echo ""
echo "Running a simple test to generate report with logo..."
echo ""

# Create a simple test feature
cat > features/test-logo.feature << 'EOF'
@logotest
Feature: Test Logo Integration

  Scenario: Generate Report with Logo
    Given I am on the login page
    Then I log "Testing logo integration in report"
    And I wait for 2 seconds
    And I take a screenshot "logo_test_screenshot"
EOF

# Create a test suite XML
cat > suites/test-logo.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Logo Test Suite" parallel="false">
    <test name="Logo Integration Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

# Run test to generate report
mvn test \
    -DsuiteXmlFile=suites/test-logo.xml \
    -Dbdd.features.path=features/test-logo.feature \
    -Dbdd.tags.include=@logotest \
    -Dthread.count=1 \
    -Dcs.report.enabled=true 2>&1 | tee logo-test.log

# Check if report was generated
REPORT_DIR="target/cs-reports"
if [ -d "$REPORT_DIR" ]; then
    echo ""
    echo "✓ Report directory created: $REPORT_DIR"
    
    # Find the latest HTML report
    LATEST_REPORT=$(find "$REPORT_DIR" -name "*.html" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2)
    
    if [ -n "$LATEST_REPORT" ]; then
        echo "✓ HTML report generated: $LATEST_REPORT"
        
        # Check if logo is embedded in the report
        if grep -q "brand-logo" "$LATEST_REPORT"; then
            echo "✓ Brand logo CSS class found in report"
        else
            echo "✗ Brand logo CSS class not found in report"
        fi
        
        if grep -q "data:image/png;base64" "$LATEST_REPORT"; then
            echo "✓ Base64 encoded logo found in report"
        else
            echo "✗ Base64 encoded logo not found in report"
        fi
        
        if grep -q "brand-header" "$LATEST_REPORT"; then
            echo "✓ Brand header structure found in report"
        else
            echo "✗ Brand header structure not found in report"
        fi
        
        echo ""
        echo "Report location: $LATEST_REPORT"
        echo "Open this file in a browser to view the report with logo"
        
        # Try to open in browser (works on WSL)
        if command -v wslview &> /dev/null; then
            echo ""
            read -p "Would you like to open the report in your browser? (y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                wslview "$LATEST_REPORT"
            fi
        fi
    else
        echo "✗ No HTML report found in $REPORT_DIR"
    fi
else
    echo "✗ Report directory not created"
fi

# Cleanup
rm -f features/test-logo.feature
rm -f suites/test-logo.xml
rm -f logo-test.log

echo ""
echo "==========================================="
echo "Logo Integration Test Complete"
echo "==========================================="