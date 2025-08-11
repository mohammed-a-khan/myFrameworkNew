#!/bin/bash

echo "===== Testing Thread Count Hierarchy ====="
echo ""

echo "Test 1: Only thread-count=3 specified"
echo "Expected: Should use 3 threads"
echo "----------------------------------------"
timeout 10 mvn test -Dsurefire.suiteXmlFiles=suites/test-thread-count-only.xml 2>&1 | grep -E "(Using thread-count|TestNG is using|source:)" | head -5
echo ""

echo "Test 2: Only data-provider-thread-count=4 specified"
echo "Expected: Should use 4 threads"
echo "----------------------------------------"
timeout 10 mvn test -Dsurefire.suiteXmlFiles=suites/test-data-provider-count-only.xml 2>&1 | grep -E "(Using data-provider-thread-count|TestNG is using|source:)" | head -5
echo ""

echo "Test 3: Both thread-count=5 and data-provider-thread-count=2 specified"
echo "Expected: Should use 5 threads (thread-count takes priority)"
echo "----------------------------------------"
timeout 10 mvn test -Dsurefire.suiteXmlFiles=suites/test-both-counts.xml 2>&1 | grep -E "(Using thread-count|TestNG is using|source:)" | head -5
echo ""

echo "Test 4: Neither specified"
echo "Expected: Should use 1 thread from application.properties"
echo "----------------------------------------"
timeout 10 mvn test -Dsurefire.suiteXmlFiles=suites/test-no-counts.xml 2>&1 | grep -E "(No thread count|TestNG is using|source:|application.properties)" | head -5
echo ""

echo "===== Test Complete ====="