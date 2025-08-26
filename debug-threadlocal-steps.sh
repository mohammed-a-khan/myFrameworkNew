#!/bin/bash

echo "=== DEBUGGING THREADLOCAL STEP EXECUTION ==="

# Run a single test scenario with detailed logging
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 45s mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  2>&1 | tee threadlocal-debug.log

echo ""
echo "=== ThreadLocal Registry Activity ==="
echo "ThreadLocal step registrations:"
grep "Building step definitions for thread" threadlocal-debug.log

echo ""
echo "ThreadLocal step executions:"
grep "ThreadLocal registry" threadlocal-debug.log | head -5

echo ""
echo "Step execution errors:"
grep -A2 -B2 "No matching step definition found" threadlocal-debug.log | head -10

echo ""
echo "Thread step instance creation:"
grep "Creating new step instance" threadlocal-debug.log | head -3

echo ""
echo "Page injection activities:"
grep "Successfully injected page instance" threadlocal-debug.log | head -3

echo ""
echo "Username entries (to check thread isolation):"
grep "Entering username:" threadlocal-debug.log | head -5