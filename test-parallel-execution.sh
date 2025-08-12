#\!/bin/bash

echo "==========================================="
echo "Testing Parallel Execution with 2 Threads"
echo "==========================================="
echo ""

# Create a test suite for parallel testing
cat > suites/test-parallel.xml << 'XMLEOF'
<\!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Parallel Test Suite" parallel="methods" thread-count="2">
    
    <parameter name="browser.name" value="chrome"/>
    <parameter name="browser.headless" value="false"/>
    <parameter name="browser.reuse.instance" value="true"/>
    
    <test name="Parallel Test">
        <classes>
            <class name="com.testforge.cs.bdd.CSBDDRunner"/>
        </classes>
    </test>
    
</suite>
XMLEOF

echo "Test suite created: suites/test-parallel.xml"
echo "Running parallel test with thread-count=2..."
echo ""

# Run the test
mvn test -DsuiteXmlFile=suites/test-parallel.xml 2>&1 | tee parallel-test.log

echo ""
echo "Test complete. Check parallel-test.log for details."
