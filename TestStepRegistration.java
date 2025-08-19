import com.testforge.cs.bdd.CSStepRegistry;
import com.testforge.cs.bdd.CSStepDefinition;
import com.akhan.stepdefs.AkhanSteps;
import java.util.List;
import java.util.Map;

public class TestStepRegistration {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Step Registration ===\n");
            
            // Test 1: Can we instantiate AkhanSteps directly?
            System.out.println("Test 1: Direct instantiation of AkhanSteps");
            try {
                AkhanSteps steps = new AkhanSteps();
                System.out.println("✓ SUCCESS: AkhanSteps instantiated directly\n");
            } catch (Exception e) {
                System.out.println("✗ FAILED: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
            
            // Test 2: Can we register the class?
            System.out.println("Test 2: Register AkhanSteps class");
            try {
                CSStepRegistry registry = CSStepRegistry.getInstance();
                registry.registerStepClass(AkhanSteps.class);
                System.out.println("✓ SUCCESS: AkhanSteps registered\n");
                
                // Check how many steps were registered
                Map<CSStepDefinition.StepType, List<CSStepDefinition>> allSteps = registry.getAllSteps();
                int totalSteps = allSteps.values().stream()
                    .mapToInt(List::size)
                    .sum();
                System.out.println("Total steps registered: " + totalSteps);
                
            } catch (Exception e) {
                System.out.println("✗ FAILED: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
            
            // Test 3: Scan package
            System.out.println("\nTest 3: Scan package com.akhan.stepdefs");
            try {
                CSStepRegistry registry = CSStepRegistry.getInstance();
                registry.clear(); // Clear first
                registry.scanPackage("com.akhan.stepdefs");
                System.out.println("✓ SUCCESS: Package scanned\n");
                
                // Check registered steps
                Map<CSStepDefinition.StepType, List<CSStepDefinition>> allSteps = registry.getAllSteps();
                int totalSteps = allSteps.values().stream()
                    .mapToInt(List::size)
                    .sum();
                System.out.println("Total steps after scan: " + totalSteps);
                
            } catch (Exception e) {
                System.out.println("✗ FAILED: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}