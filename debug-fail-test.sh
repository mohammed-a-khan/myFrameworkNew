#!/bin/bash

echo "=== Testing CSReportManager.fail() functionality ==="

# Run the test and capture ALL output including stderr
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
mvn test -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
-Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
-q 2>&1 | tee debug-fail-output.log

echo ""
echo "=== Checking for our debug messages ==="
grep -i "CSReportManager.fail" debug-fail-output.log || echo "No CSReportManager.fail() calls found"
grep -i "Custom user message" debug-fail-output.log || echo "No custom user message found"
grep -i "addAction" debug-fail-output.log || echo "No addAction calls found"
grep -i "AssertionError" debug-fail-output.log || echo "No AssertionError found"
grep -i "navigateToLoginPage" debug-fail-output.log || echo "No navigateToLoginPage found"

echo ""
echo "=== Checking test results ==="
grep -E "(PASSED|FAILED|ERROR)" debug-fail-output.log | head -10

echo ""
echo "=== Check if HTML report was generated ==="
find target/test-reports -name "*.html" -newer debug-fail-output.log 2>/dev/null | head -5