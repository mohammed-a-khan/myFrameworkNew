#!/bin/bash

echo "==========================================="
echo "COMPREHENSIVE IE MODE TEST FOR WINDOWS 11"
echo "==========================================="
echo ""
echo "This test will verify IE driver with automatic Edge fallback"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create test feature
cat > features/test-ie-mode.feature << 'EOF'
@ietest
Feature: IE Mode Compatibility Test

  Scenario: Test IE Driver on Windows 11
    Given I am on the login page
    Then I wait for 2 seconds
    And I take a screenshot "ie_mode_test"
    When I enter "Admin" in the "username" field
    And I enter "admin123" in the "password" field
    And I click the "Login" button
    Then I should see the dashboard
    And I take a screenshot "ie_mode_dashboard"
EOF

# Create test suite
cat > suites/test-ie-mode.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="IE Mode Test Suite" parallel="none">
    
    <!-- Request IE browser - framework will handle Windows 11 compatibility -->
    <parameter name="browser.name" value="ie"/>
    <parameter name="browser.headless" value="false"/>
    <parameter name="thread.count" value="1"/>
    
    <test name="IE Mode Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

echo -e "${YELLOW}Starting IE Mode Test...${NC}"
echo "Configuration:"
echo "  - Browser: IE (with automatic Edge fallback on Windows 11)"
echo "  - Mode: Non-headless"
echo "  - Threads: 1"
echo ""

# Run the test
mvn test \
    -DsuiteXmlFile=suites/test-ie-mode.xml \
    -Dbdd.features.path=features/test-ie-mode.feature \
    -Dbdd.tags.include=@ietest \
    -Dthread.count=1 2>&1 | tee ie-mode-test.log

# Analyze results
echo ""
echo "==========================================="
echo "TEST RESULTS ANALYSIS"
echo "==========================================="

# Check for Windows 11 detection
if grep -q "Windows 11 detected" ie-mode-test.log; then
    echo -e "${GREEN}✓${NC} Windows 11 detected by framework"
    
    # Check for IE driver attempt
    if grep -q "WINDOWS 11 IE MODE INITIALIZATION" ie-mode-test.log; then
        echo -e "${YELLOW}→${NC} IE driver initialization attempted"
        
        # Check if it timed out and fell back to Edge
        if grep -q "IE DRIVER CREATION TIMED OUT" ie-mode-test.log; then
            echo -e "${YELLOW}→${NC} IE driver timed out (expected on Windows 11)"
            
            if grep -q "IE DRIVER FAILED - AUTOMATICALLY SWITCHING TO EDGE" ie-mode-test.log; then
                echo -e "${GREEN}✓${NC} Automatic fallback to Edge triggered"
                
                if grep -q "EDGE DRIVER READY - FALLBACK SUCCESSFUL" ie-mode-test.log; then
                    echo -e "${GREEN}✓${NC} Edge driver created successfully as fallback"
                fi
            fi
        elif grep -q "IE MODE.*Driver created successfully" ie-mode-test.log; then
            echo -e "${GREEN}✓${NC} IE driver created successfully with Edge IE mode!"
            echo -e "${GREEN}✓${NC} QAF-style configuration worked!"
        fi
    fi
else
    # Windows 10 or earlier
    if grep -q "Creating standard InternetExplorerDriver" ie-mode-test.log; then
        echo -e "${GREEN}✓${NC} Standard IE driver used (Windows 10 or earlier)"
    fi
fi

# Check for successful test execution
if grep -q "BUILD SUCCESS" ie-mode-test.log; then
    echo ""
    echo -e "${GREEN}==========================================${NC}"
    echo -e "${GREEN}✓ TEST COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}==========================================${NC}"
    
    # Provide summary based on what happened
    if grep -q "EDGE DRIVER READY - FALLBACK SUCCESSFUL" ie-mode-test.log; then
        echo ""
        echo "Summary:"
        echo "  - Windows 11 detected"
        echo "  - IEDriverServer could not connect to Edge IE mode"
        echo "  - Framework automatically switched to Edge browser"
        echo "  - Tests ran successfully in Edge"
        echo ""
        echo "Recommendation:"
        echo "  For Windows 11, use 'browser.name=edge' directly for better performance"
    elif grep -q "IE MODE.*Driver created successfully" ie-mode-test.log; then
        echo ""
        echo "Summary:"
        echo "  - Windows 11 detected"
        echo "  - IEDriverServer successfully connected to Edge IE mode"
        echo "  - Tests ran in Edge with IE compatibility mode"
        echo ""
        echo "Note: This is the ideal scenario for legacy IE applications"
    else
        echo ""
        echo "Summary:"
        echo "  - Tests ran successfully with standard IE driver"
    fi
else
    echo ""
    echo -e "${RED}==========================================${NC}"
    echo -e "${RED}✗ TEST FAILED${NC}"
    echo -e "${RED}==========================================${NC}"
    echo ""
    echo "Check ie-mode-test.log for detailed error information"
    
    # Check for common issues
    if grep -q "Protected Mode settings" ie-mode-test.log; then
        echo ""
        echo -e "${YELLOW}Possible Issue: Protected Mode settings${NC}"
        echo "Solution: Run setup-ie-mode-windows11.bat as Administrator"
    fi
    
    if grep -q "zoom level" ie-mode-test.log; then
        echo ""
        echo -e "${YELLOW}Possible Issue: Browser zoom level${NC}"
        echo "Solution: Set IE/Edge zoom to 100%"
    fi
fi

# Cleanup
rm -f features/test-ie-mode.feature
rm -f suites/test-ie-mode.xml

echo ""
echo "Log file saved as: ie-mode-test.log"
echo ""