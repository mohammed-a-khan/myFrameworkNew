#!/bin/bash

echo "=== DEBUGGING CSReportManager.fail() STEP EXECUTION ==="

# Create a simple test with detailed logging
cat > src/test/java/DebugStepExecution.java << 'JEOF'
import com.testforge.cs.reporting.CSReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugStepExecution {
    private static final Logger logger = LoggerFactory.getLogger(DebugStepExecution.class);
    
    public static void main(String[] args) {
        try {
            logger.info("=== STARTING DEBUG TEST ===");
            
            // Simulate step execution flow
            logger.info("1. Starting step...");
            CSReportManager.startStep("Given", "I am on the login page");
            
            logger.info("2. Calling fail method...");
            CSReportManager.fail("Custom user message for I am on the login page");
            
            logger.info("3. Ending step...");
            CSReportManager.endStep();
            
            logger.info("4. Getting last step actions...");
            var actions = CSReportManager.getLastStepActions();
            if (actions != null) {
                logger.info("Found {} actions:", actions.size());
                for (var action : actions) {
                    logger.info("  - Type: {}, Description: {}, Status: {}", 
                        action.get("type"), action.get("description"), action.get("status"));
                }
            } else {
                logger.warn("No actions found!");
            }
            
            logger.info("=== DEBUG TEST COMPLETED ===");
            
        } catch (Exception e) {
            logger.error("Debug test failed: {}", e.getMessage(), e);
        }
    }
}
JEOF

echo "1. Compiling debug test..."
javac -cp "target/classes:target/dependency/*" src/test/java/DebugStepExecution.java -d target/test-classes

echo "2. Running debug test..."
java -cp "target/classes:target/test-classes:target/dependency/*" DebugStepExecution

echo "=== DEBUG COMPLETED ==="
