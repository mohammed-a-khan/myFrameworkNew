#!/bin/bash

echo "Testing Chrome browser..."

# Check if Chrome is installed
which google-chrome || which chromium-browser || which chrome

# Check Chrome version
google-chrome --version || chromium-browser --version || chrome --version

# Check if ChromeDriver is available
which chromedriver

# Check ChromeDriver version
chromedriver --version

echo "Running simple Chrome test..."
# Run a simple test
mvn test -Dtest=com.testforge.cs.core.CSBaseTest -DsuiteXmlFile=suites/simple-valid-login.xml -Dbrowser.headless=true