#!/bin/bash

echo "🔍 Testing Azure DevOps Enabled Override Fix"
echo "============================================="
echo ""

echo "📋 PROBLEM DESCRIPTION:"
echo "- application.properties has cs.azure.devops.enabled=false"
echo "- Suite XML has cs.azure.devops.enabled=true"
echo "- CSBDDRunner correctly reads suite parameter override"
echo "- But CSADOConfiguration was ignoring the override and only reading from properties"
echo ""

echo "🔧 FIX IMPLEMENTED:"
echo "- Added setEnabledOverride() method to CSADOConfiguration"
echo "- Modified CSBDDRunner to call setEnabledOverride() when suite parameter is detected"
echo "- Added reinitializeWithOverride() to force re-initialization with correct value"
echo ""

echo "🧪 TESTING THE FIX:"
echo "==================="

# Create a temporary minimal test
cat > test-minimal-ado.xml << 'EOF'
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Test ADO Override" verbose="1">
    <parameter name="cs.azure.devops.enabled" value="true"/>
    <parameter name="cs.bdd.features.path" value="features/simple-test.feature"/>
    <parameter name="cs.bdd.stepdefs.packages" value="com.orangehrm.stepdefs"/>
    <test name="ADO Override Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
</suite>
EOF

# Check current application.properties setting
echo "1. Current application.properties setting:"
grep "cs.azure.devops.enabled" resources/config/application.properties

echo ""
echo "2. Suite XML parameter:"
grep "cs.azure.devops.enabled" test-minimal-ado.xml

echo ""
echo "3. Running test with suite parameter override..."
echo "   Looking for these key log messages:"
echo "   - 'Using cs.azure.devops.enabled from suite parameter: true'"
echo "   - 'Applying Azure DevOps enabled override from suite parameter'"  
echo "   - 'Azure DevOps integration is enabled' (instead of disabled)"

echo ""
echo "4. Test execution logs:"
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test -Dtest=CSBDDRunner \
    -Dsurefire.suiteXmlFiles=test-minimal-ado.xml -q 2>&1 | \
    grep -E "(cs\.azure\.devops\.enabled|Azure DevOps integration|Applying Azure DevOps|Using enabled|Using cs\.azure\.devops\.enabled)" | \
    head -10

echo ""
echo "5. Cleanup:"
rm -f test-minimal-ado.xml

echo ""
echo "✅ TEST ANALYSIS:"
echo "If the fix is working, you should see:"
echo "- 'Using cs.azure.devops.enabled from suite parameter: true'"
echo "- 'Applying Azure DevOps enabled override from suite parameter'"
echo "- 'Azure DevOps integration is enabled' (NOT disabled)"
echo ""
echo "If the fix is NOT working, you would see:"
echo "- 'Azure DevOps integration is disabled'"
echo ""
echo "The key is that CSADOConfiguration should now respect the suite parameter override!"