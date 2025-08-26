#!/bin/bash

echo "=== Testing Thread Isolation Fix ==="
echo "Running parallel test to verify no AdminAdminAdmin issue..."

# Run test for 30 seconds and capture output
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  2>&1 | tee thread-isolation-test.log

# Check for AdminAdminAdmin issue
if grep -q "AdminAdminAdmin" thread-isolation-test.log; then
    echo "❌ FAILED: Found AdminAdminAdmin issue - threads are sharing browser instances"
    exit 1
else
    echo "✅ SUCCESS: No AdminAdminAdmin found - thread isolation is working"
fi

# Check thread distribution
echo ""
echo "=== Thread Analysis ==="
echo "Thread IDs found:"
grep "Thread ID" thread-isolation-test.log | sort | uniq
echo ""
echo "Username entries by thread:"
grep "Entering username:" thread-isolation-test.log | head -10

echo ""
echo "=== Thread Isolation Test Complete ==="