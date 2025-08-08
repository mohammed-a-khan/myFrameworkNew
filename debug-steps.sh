#!/bin/bash

echo "Debugging step definitions..."

# Run Maven with debug logging for the step registry
mvn test -Dtest=com.testforge.cs.bdd.CSBDDRunner \
  -DsuiteXmlFile=suites/simple-valid-login.xml \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -Dorg.slf4j.simpleLogger.log.com.testforge.cs.bdd.CSStepRegistry=debug \
  -Dorg.slf4j.simpleLogger.log.com.testforge.cs.bdd.CSScenarioRunner=debug \
  2>&1 | grep -E "(Registering step|Found step|Executing step|click.*button)" | head -50