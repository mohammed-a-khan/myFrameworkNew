import com.testforge.cs.bdd.CSStepRegistry;
import com.testforge.cs.config.CSConfigManager;

public class TestAkhanStepLoading {
    public static void main(String[] args) {
        try {
            System.out.println("Testing Akhan Step Definitions Loading...");
            
            // Initialize config
            CSConfigManager.getInstance();
            
            // Get registry instance
            CSStepRegistry registry = CSStepRegistry.getInstance();
            
            // Try to scan the akhan package
            System.out.println("Scanning package: com.akhan.stepdefs");
            registry.scanPackage("com.akhan.stepdefs");
            
            // Check registered steps
            int totalSteps = registry.getAllSteps().values().stream()
                .mapToInt(list -> list.size())
                .sum();
            
            System.out.println("Total steps registered: " + totalSteps);
            
            // List all registered steps
            registry.getAllSteps().forEach((type, steps) -> {
                steps.forEach(step -> {
                    System.out.println("  " + type + ": " + step.getOriginalPattern());
                });
            });
            
            System.out.println("SUCCESS: Akhan steps loaded!");
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load Akhan steps");
            e.printStackTrace();
        }
    }
}