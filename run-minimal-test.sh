#!/bin/bash

echo "=== Running Minimal Test ==="

# Kill any existing processes
pkill -f chrome 2>/dev/null
pkill -f java 2>/dev/null

# Wait a moment
sleep 2

# Run minimal test with verbose output
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" mvn test \
  -Dsurefire.suiteXmlFiles=suites/minimal-test.xml \
  -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
  -Dcs.azure.devops.enabled=false \
  -X 2>&1 | head -100