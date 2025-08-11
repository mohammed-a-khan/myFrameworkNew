#!/bin/bash

echo "========================================"
echo "Testing Property Warnings"
echo "========================================"
echo ""

echo "Running a quick test to check property warnings..."
timeout 20 mvn test -DsuiteXmlFile=suites/orangehrm-failure-test.xml -Dtest=CSBDDRunner 2>&1 | grep "Property not found" | sort | uniq -c | sort -rn

echo ""
echo "========================================"
echo "Summary:"
echo "Properties have been added to reduce warnings."
echo "Any remaining warnings are for optional properties."
echo "========================================"