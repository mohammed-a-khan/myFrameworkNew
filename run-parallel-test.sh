#!/bin/bash

echo "Running parallel test with orangehrm-failure-test.xml..."
mvn clean test -Dsuite=suites/orangehrm-failure-test.xml