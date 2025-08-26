#!/bin/bash

echo "=== BROWSER CLEANUP VERIFICATION TEST ==="

# Start clean
echo "Killing any existing Chrome processes..."
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Starting browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

# Run the test
echo "Running orangehrm-failure-test.xml..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  > test-output.log 2>&1

echo ""
echo "Test completed. Checking browsers immediately..."
BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
echo "Browsers remaining: $BROWSER_COUNT"

if [ $BROWSER_COUNT -gt 0 ]; then
    echo ""
    echo "❌ PROBLEM: $BROWSER_COUNT browser(s) still running!"
    echo "Browser processes:"
    ps aux | grep chrome | grep -v grep
    
    echo ""
    echo "Checking cleanup calls in logs:"
    grep -i "closing all browsers\|quitalldriver\|afterclass" test-output.log | tail -5
    
    echo ""
    echo "Force killing remaining browsers..."
    pkill -f chrome 2>/dev/null || true
    sleep 2
    echo "After force kill: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"
    
else
    echo "✅ SUCCESS: All browsers properly cleaned up!"
fi

echo ""
echo "Test results summary:"
if grep -q "Tests run:" test-output.log; then
    grep "Tests run:" test-output.log
else
    echo "No test results found in output"
fi