#!/bin/bash

echo "=== Validating Screenshot Fix ==="
echo "This test will run a shorter scenario to verify screenshots are different"
echo ""

# Clean up temp directories first
rm -rf /tmp/cs-temp-screenshots 2>/dev/null
mkdir -p /tmp/cs-temp-screenshots

echo "Running test with soft fail screenshot capture enabled..."

# Run test with shorter timeout
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" timeout 10s mvn test \
    -Dtest=CSBDDRunner \
    -Dsurefire.suiteXmlFiles=suites/orangehrm-simple-only.xml \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.soft.fail.capture.screenshot=true \
    -q > /dev/null 2>&1

echo ""
echo "=== Checking Screenshots ==="

# Check for soft fail screenshots
SOFT_FAIL_COUNT=$(find /tmp -name "*SOFT_FAIL*" 2>/dev/null | wc -l)
echo "Soft fail screenshots found: $SOFT_FAIL_COUNT"

# Check for hard failure screenshots
HARD_FAIL_COUNT=$(find /tmp -name "*HARD_FAILURE*" 2>/dev/null | wc -l)
echo "Hard failure screenshots found: $HARD_FAIL_COUNT"

# Check for actual step failure screenshots
ACTUAL_FAIL_COUNT=$(find /tmp -name "*ACTUAL*" 2>/dev/null | wc -l)
echo "Actual failure screenshots found: $ACTUAL_FAIL_COUNT"

echo ""
echo "=== Analysis ==="
if [ $SOFT_FAIL_COUNT -gt 0 ] && [ $HARD_FAIL_COUNT -gt 0 ]; then
    echo "✓ SUCCESS: Both soft fail and hard failure screenshots are being captured separately!"
elif [ $SOFT_FAIL_COUNT -gt 0 ]; then
    echo "⚠ Partial: Soft fail screenshots captured but no hard failure screenshots"
elif [ $HARD_FAIL_COUNT -gt 0 ]; then
    echo "⚠ Partial: Hard failure screenshots captured but no soft fail screenshots"
else
    echo "✗ Issue: No distinct screenshots found"
fi

echo ""
echo "Test completed."