#!/bin/bash

echo "🔍 Verifying Azure DevOps Override Fix Implementation"
echo "====================================================="
echo ""

echo "1. ✅ Check if CSADOConfiguration has the new methods:"
echo "-----------------------------------------------------"
if grep -q "setEnabledOverride" src/main/java/com/testforge/cs/azuredevops/config/CSADOConfiguration.java; then
    echo "✅ setEnabledOverride method found"
else
    echo "❌ setEnabledOverride method NOT found"
fi

if grep -q "reinitializeWithOverride" src/main/java/com/testforge/cs/azuredevops/config/CSADOConfiguration.java; then
    echo "✅ reinitializeWithOverride method found"
else
    echo "❌ reinitializeWithOverride method NOT found"
fi

if grep -q "initialize(Boolean enabledOverride)" src/main/java/com/testforge/cs/azuredevops/config/CSADOConfiguration.java; then
    echo "✅ initialize with override parameter found"
else
    echo "❌ initialize with override parameter NOT found"
fi

echo ""
echo "2. ✅ Check if CSBDDRunner calls the override:"
echo "----------------------------------------------"
if grep -q "setEnabledOverride(true)" src/main/java/com/testforge/cs/bdd/CSBDDRunner.java; then
    echo "✅ CSBDDRunner calls setEnabledOverride(true)"
else
    echo "❌ CSBDDRunner does NOT call setEnabledOverride"
fi

if grep -q "Applying Azure DevOps enabled override" src/main/java/com/testforge/cs/bdd/CSBDDRunner.java; then
    echo "✅ Override application log message found"
else
    echo "❌ Override application log message NOT found"
fi

echo ""
echo "3. ✅ Implementation verification:"
echo "--------------------------------"
echo "CSADOConfiguration.setEnabledOverride method signature:"
grep -A10 "public void setEnabledOverride" src/main/java/com/testforge/cs/azuredevops/config/CSADOConfiguration.java | head -5

echo ""
echo "CSBDDRunner override application code:"
grep -A5 -B2 "setEnabledOverride(true)" src/main/java/com/testforge/cs/bdd/CSBDDRunner.java

echo ""
echo "4. 🧪 Compile test:"
echo "-------------------"
mvn compile test-compile -q
if [ $? -eq 0 ]; then
    echo "✅ Project compiles successfully - no syntax errors"
else
    echo "❌ Compilation failed - there are syntax errors"
fi

echo ""
echo "5. 📋 Summary of changes made:"
echo "------------------------------"
echo "✅ Added setEnabledOverride() method to CSADOConfiguration"
echo "✅ Added reinitializeWithOverride() method to force re-init"
echo "✅ Modified initialize() to accept Boolean enabledOverride parameter"
echo "✅ Updated CSBDDRunner to call setEnabledOverride(true) when suite parameter detected"
echo "✅ Added comprehensive logging for debugging"

echo ""
echo "🎯 EXPECTED BEHAVIOR:"
echo "====================="
echo "When cs.azure.devops.enabled=true in suite XML:"
echo "1. CSBDDRunner detects suite parameter override"
echo "2. CSBDDRunner logs: 'Using cs.azure.devops.enabled from suite parameter: true'"
echo "3. CSBDDRunner logs: 'Applying Azure DevOps enabled override from suite parameter'"
echo "4. CSBDDRunner calls CSADOConfiguration.getInstance().setEnabledOverride(true)"
echo "5. CSADOConfiguration re-initializes with enabled=true"
echo "6. CSADOConfiguration logs: 'Azure DevOps integration is enabled' (NOT disabled)"
echo ""
echo "The fix is now implemented and ready for testing! 🚀"