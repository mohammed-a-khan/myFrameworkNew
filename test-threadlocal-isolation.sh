#!/bin/bash

echo "=== Testing ThreadLocal Step Registry Implementation ==="
echo "Running parallel test to verify each thread has its own step instances..."

# Run test for 30 seconds and capture output
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 30s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  2>&1 | tee threadlocal-test.log

echo ""
echo "=== Analysis Results ==="

# Check for ThreadLocal initialization logs
echo "ThreadLocal step instances created:"
grep "Creating new step instance" threadlocal-test.log | head -5

echo ""
echo "Page injection per thread:"
grep "Initializing ThreadLocal pages" threadlocal-test.log

echo ""
echo "Step execution with thread names:"
grep "Executing step:" threadlocal-test.log | head -5

echo ""
echo "Browser creation per thread:"
grep "BROWSER #" threadlocal-test.log

echo ""
echo "Thread-to-browser mapping:"
grep "Thread ID.*Creating NEW" threadlocal-test.log

echo ""
echo "Final browser count:"
BROWSER_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)
echo "Chrome processes still running: $BROWSER_COUNT"

if [ $BROWSER_COUNT -eq 0 ]; then
    echo "✅ SUCCESS: All browsers properly cleaned up"
else
    echo "❌ WARNING: $BROWSER_COUNT browsers still running"
fi

echo ""
echo "=== ThreadLocal Implementation Test Complete ==="