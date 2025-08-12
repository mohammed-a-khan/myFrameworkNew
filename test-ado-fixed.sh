#!/bin/bash

echo "================================================"
echo "Testing Azure DevOps Integration (Fixed)"
echo "================================================"
echo ""

# Check PAT token
if [ -z "$ADO_PAT_TOKEN" ]; then
    echo "WARNING: ADO_PAT_TOKEN not set."
    echo "Set it with: export ADO_PAT_TOKEN=your_token"
    echo ""
    echo "Continuing with tests only (no ADO publishing)..."
    echo ""
fi

echo "Running ADO BDD test suite..."
echo "================================================"

# Run with more detailed logging
mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml \
    -Dtest.verbose=true \
    -Dlogback.debug=false \
    2>&1 | tee test-ado-output.log

# Check if tests passed
if grep -q "BUILD SUCCESS" test-ado-output.log; then
    echo ""
    echo "================================================"
    echo "✅ Tests executed successfully!"
    
    # Check for ADO errors
    if grep -q "Failed to complete ADO test run" test-ado-output.log; then
        echo "⚠️  Warning: ADO test run completion had issues"
        echo "   Check the log for details: test-ado-output.log"
    elif grep -q "Completed Azure DevOps test run" test-ado-output.log; then
        echo "✅ ADO test run completed successfully!"
        
        # Extract test run ID
        RUN_ID=$(grep "Started Azure DevOps test run" test-ado-output.log | grep -oP "ID: \K\d+")
        if [ ! -z "$RUN_ID" ]; then
            echo "   Test Run ID: $RUN_ID"
            echo "   View at: https://dev.azure.com/mdakhan/myproject/_testManagement/Runs?runId=$RUN_ID"
        fi
    fi
else
    echo ""
    echo "================================================"
    echo "❌ Tests failed. Check test-ado-output.log for details"
fi

echo "================================================"