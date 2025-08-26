#!/bin/bash

echo "=== FINAL AGGRESSIVE CLEANUP TEST ==="

# Clean start
pkill -f chrome 2>/dev/null || true
pkill -f java 2>/dev/null || true
sleep 3

echo "Pre-test browser count: $(ps aux | grep chrome | grep -v grep | wc -l)"

# Run the test
echo "Running test with aggressive cleanup..."
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
  -Dsurefire.suiteXmlFiles=suites/orangehrm-failure-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  > aggressive-cleanup-test.log 2>&1

echo ""
echo "Test completed. Checking browsers at different intervals..."

echo "Immediate check: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"
sleep 2
echo "After 2 seconds: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"
sleep 3
echo "After 5 seconds: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"
sleep 5
echo "After 10 seconds: $(ps aux | grep chrome | grep -v grep | wc -l) browsers"

FINAL_COUNT=$(ps aux | grep chrome | grep -v grep | wc -l)

if [ $FINAL_COUNT -gt 0 ]; then
    echo ""
    echo "❌ STILL HAVE $FINAL_COUNT BROWSERS REMAINING:"
    ps aux | grep chrome | grep -v grep
    
    echo ""
    echo "Cleanup activities from logs:"
    grep -i "closing all browsers\|aggressive cleanup\|force cleanup\|waited.*seconds" aggressive-cleanup-test.log
    
else
    echo ""
    echo "✅ PERFECT: All browsers completely eliminated!"
    echo ""
    echo "Cleanup activities that worked:"
    grep -i "closing all browsers\|aggressive cleanup\|force cleanup\|waited.*seconds" aggressive-cleanup-test.log
fi

echo ""
echo "Test results:"
if grep -q "Tests run:" aggressive-cleanup-test.log; then
    grep "Tests run:" aggressive-cleanup-test.log
fi