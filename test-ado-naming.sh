#!/bin/bash

echo "Testing Azure DevOps Test Run Naming..."
echo "======================================="

# Test 1: With suite name
echo ""
echo "Test 1: Running with suite name 'Azure DevOps BDD Test Suite'"
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
mvn test -Dtest=CSBDDRunner \
    -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.azure.devops.enabled=true 2>&1 | grep -E "(Test Suite Name:|Creating test run with name:|Test Run Name:)"

# Test 2: With a suite file that has no name (testing fallback)
echo ""
echo "Test 2: Creating suite without name to test fallback..."
cat > suites/test-no-name-suite.xml << 'EOF'
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite verbose="2" parallel="none">
    <parameter name="environment" value="test"/>
    <test name="No Name Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

echo "Running with suite without name (should use fallback 'CS BDD Test Run')"
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
mvn test -Dtest=CSBDDRunner \
    -Dsurefire.suiteXmlFiles=suites/test-no-name-suite.xml \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.azure.devops.enabled=true 2>&1 | grep -E "(Test Suite Name:|Creating test run with name:|Test Run Name:)"

echo ""
echo "Test complete. Check the output above to verify:"
echo "1. First test should show: 'Azure DevOps BDD Test Suite - Test Run - [timestamp]'"
echo "2. Second test should show: 'CS BDD Test Run - Test Run - [timestamp]'"

# Clean up
rm -f suites/test-no-name-suite.xml