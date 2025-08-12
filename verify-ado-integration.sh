#!/bin/bash

echo "================================================"
echo "Azure DevOps Integration Verification"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if ADO is enabled in application.properties
echo "1. Checking ADO configuration..."
if grep -q "ado.enabled=true" resources/config/application.properties; then
    echo -e "${GREEN}✓${NC} ADO is enabled in application.properties"
else
    echo -e "${RED}✗${NC} ADO is not enabled in application.properties"
fi

# Check ADO configuration values
echo ""
echo "2. ADO Configuration values:"
grep "^ado\." resources/config/application.properties | while read line; do
    key=$(echo $line | cut -d'=' -f1)
    value=$(echo $line | cut -d'=' -f2)
    if [[ $key == *"pat"* ]]; then
        echo "  $key = [HIDDEN]"
    else
        echo "  $key = $value"
    fi
done

# Check if feature file has ADO tags
echo ""
echo "3. Checking ADO tags in feature file..."
if grep -q "@TestCaseId" features/ado-mapped-tests.feature; then
    echo -e "${GREEN}✓${NC} Feature file has @TestCaseId tags"
    echo "  Found test cases:"
    grep "@TestCaseId" features/ado-mapped-tests.feature | sed 's/.*@TestCaseId:\([0-9]*\).*/    - Test Case \1/'
else
    echo -e "${RED}✗${NC} Feature file missing @TestCaseId tags"
fi

# Check if ADO classes are compiled
echo ""
echo "4. Checking ADO classes compilation..."
if [ -d "target/classes/com/testforge/cs/azuredevops" ]; then
    count=$(find target/classes/com/testforge/cs/azuredevops -name "*.class" | wc -l)
    echo -e "${GREEN}✓${NC} ADO classes compiled: $count class files found"
else
    echo -e "${RED}✗${NC} ADO classes not found in target directory"
fi

# Check last test run results
echo ""
echo "5. Checking last test execution..."
if [ -f "target/surefire-reports/testng-results.xml" ]; then
    total=$(grep 'total="[0-9]*"' target/surefire-reports/testng-results.xml | sed 's/.*total="\([0-9]*\)".*/\1/')
    passed=$(grep 'passed="[0-9]*"' target/surefire-reports/testng-results.xml | sed 's/.*passed="\([0-9]*\)".*/\1/')
    failed=$(grep 'failed="[0-9]*"' target/surefire-reports/testng-results.xml | sed 's/.*failed="\([0-9]*\)".*/\1/')
    
    if [ "$failed" = "0" ]; then
        echo -e "${GREEN}✓${NC} Last test run: $passed/$total tests passed"
    else
        echo -e "${RED}✗${NC} Last test run: $passed passed, $failed failed out of $total tests"
    fi
else
    echo -e "${YELLOW}⚠${NC} No test results found"
fi

# Check if PAT token is set
echo ""
echo "6. Checking ADO PAT token..."
if [ -n "$ADO_PAT_TOKEN" ]; then
    echo -e "${GREEN}✓${NC} ADO_PAT_TOKEN environment variable is set"
else
    echo -e "${YELLOW}⚠${NC} ADO_PAT_TOKEN not set - ADO publishing will be skipped"
    echo "  Set it with: export ADO_PAT_TOKEN=your_token"
fi

echo ""
echo "================================================"
echo "Summary:"
echo "------------------------------------------------"

# Overall status
if [ -n "$ADO_PAT_TOKEN" ] && [ "$failed" = "0" ]; then
    echo -e "${GREEN}✓ Azure DevOps integration is fully configured and working!${NC}"
    echo ""
    echo "You can run tests with ADO integration using:"
    echo "  ./test-ado-bdd.sh"
    echo ""
    echo "Or directly with Maven:"
    echo "  mvn test -Dsurefire.suiteXmlFiles=suites/ado-bdd-suite.xml"
else
    echo -e "${YELLOW}⚠ Azure DevOps integration is configured but needs PAT token${NC}"
    echo ""
    echo "To enable full ADO integration:"
    echo "  1. Set PAT token: export ADO_PAT_TOKEN=your_token"
    echo "  2. Run tests: ./test-ado-bdd.sh"
fi

echo "================================================"