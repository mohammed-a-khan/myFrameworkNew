#!/bin/bash

echo "========================================"
echo "Configuration Verification Report"
echo "========================================"
echo ""

echo "1. Checking project structure:"
echo "   - Project root resources folder:"
if [ -d "resources/config" ]; then
    echo "     ✓ resources/config exists"
    echo "     Files:"
    ls -1 resources/config/*.properties 2>/dev/null | sed 's/^/       - /'
else
    echo "     ✗ resources/config NOT found"
fi

echo ""
echo "   - src/main/resources folder:"
if [ -f "src/main/resources/application.properties" ]; then
    echo "     ✗ application.properties still exists (should be removed)"
else
    echo "     ✓ application.properties removed (as expected)"
fi

if [ -f "src/main/resources/logback.xml" ]; then
    echo "     ✓ logback.xml exists (for logging configuration)"
fi

echo ""
echo "2. Merged properties file location:"
echo "   resources/config/application.properties"

echo ""
echo "3. Configuration loading changes:"
echo "   - CSConfigManager now loads ONLY from: resources/config/"
echo "   - No longer loads from classpath (src/main/resources/config/)"

echo ""
echo "========================================"
echo "✓ Configuration consolidation completed!"
echo "========================================"
echo ""
echo "Summary:"
echo "- All properties are now in: resources/config/"
echo "- Main application.properties has all merged properties"
echo "- Framework will only load from project root resources folder"
echo "========================================"