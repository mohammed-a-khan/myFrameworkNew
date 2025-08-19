import com.testforge.cs.bdd.CSStepRegistry;
import com.testforge.cs.bdd.CSStepDefinition;
import com.testforge.cs.config.CSConfigManager;

public class TestStepMatching {
    public static void main(String[] args) {
        try {
            // Initialize
            CSConfigManager.getInstance();
            CSStepRegistry registry = CSStepRegistry.getInstance();
            
            // Register akhan steps
            System.out.println("Registering Akhan steps...");
            registry.scanPackage("com.akhan.stepdefs");
            
            // Test step matching
            String[] testSteps = {
                "I enter username \"testuser1\"",
                "I enter username \"testuser1@americas.cshare.net\""
            };
            
            for (String step : testSteps) {
                System.out.println("\nTesting step: " + step);
                CSStepDefinition stepDef = registry.findStep(step, CSStepDefinition.StepType.ANY);
                
                if (stepDef != null) {
                    System.out.println("  ✓ Found matching step definition");
                    System.out.println("    Pattern: " + stepDef.getOriginalPattern());
                } else {
                    System.out.println("  ✗ No matching step definition found!");
                    
                    // Try to find similar steps
                    System.out.println("  Available patterns:");
                    registry.getAllSteps().forEach((type, steps) -> {
                        steps.forEach(s -> {
                            if (s.getOriginalPattern().contains("username")) {
                                System.out.println("    - " + s.getOriginalPattern());
                            }
                        });
                    });
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}