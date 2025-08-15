#!/bin/bash

echo "Testing environment-specific configuration loading..."
echo ""

# Test different environments
for env in dev sit qa prod; do
    echo "Testing $env environment:"
    java -cp "target/classes:target/dependency/*" \
         -Denvironment.name=$env \
         -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
         com.testforge.cs.utils.CSConfigTest 2>/dev/null | grep -E "(environment|url|test.plan)"
    echo "---"
done