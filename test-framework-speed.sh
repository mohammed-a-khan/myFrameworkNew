#!/bin/bash

# Test framework speed improvements
echo "Testing framework simplifications..."

# Test compilation speed
echo "1. Testing compilation speed..."
start_time=$(date +%s%3N)
mvn compile -q > /dev/null 2>&1
end_time=$(date +%s%3N)
compile_time=$((end_time - start_time))
echo "   Compilation time: ${compile_time}ms"

# Test basic class loading
echo "2. Testing class loading..."
start_time=$(date +%s%3N)
java -cp "target/classes:target/dependency/*" com.testforge.cs.reporting.CSReportManager > /dev/null 2>&1
end_time=$(date +%s%3N)
load_time=$((end_time - start_time))
echo "   Class loading time: ${load_time}ms"

echo "Framework simplification test completed!"