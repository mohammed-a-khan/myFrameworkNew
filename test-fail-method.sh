#!/bin/bash

echo "Testing CSReportManager.fail() functionality..."

# Run a quick test to see if our fix works
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 15s mvn test -Dtest=CSBDDRunner -Dsurefire.suiteXmlFiles=suites/orangehrm-simple-only.xml -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" -q > test-output.log 2>&1

if [ $? -ne 0 ]; then
    echo "Test completed (may have timed out - that's expected)"
    echo "Checking for FAIL actions in logs..."
    
    # Check if our fail method is being called
    grep -i "Step marked as failed" test-output.log && echo "✓ Step marked as failed detected" || echo "✗ Step not marked as failed"
    grep -i "Action added.*FAIL" test-output.log && echo "✓ FAIL action added detected" || echo "✗ FAIL action not added"
    
    echo "Test output preview:"
    tail -20 test-output.log
else
    echo "Test completed successfully"
    echo "Checking logs for fail functionality..."
    grep -i "fail\|action" test-output.log | head -10
fi

echo "CSReportManager.fail() test completed!"