import com.testforge.cs.bdd.CSBDDRunner;
import com.testforge.cs.bdd.CSFeatureParser;
import com.testforge.cs.bdd.CSFeatureFile;
import com.testforge.cs.bdd.CSStepRegistry;
import com.testforge.cs.config.CSConfigManager;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RunAkhanTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Akhan Feature File Loading ===\n");
            
            // Initialize config
            CSConfigManager config = CSConfigManager.getInstance();
            
            // 1. Check if feature file exists
            String featurePath = "features/akhan.feature";
            System.out.println("1. Checking feature file: " + featurePath);
            if (Files.exists(Paths.get(featurePath))) {
                System.out.println("   ✓ Feature file exists");
            } else {
                System.out.println("   ✗ Feature file NOT found!");
                return;
            }
            
            // 2. Parse the feature file
            System.out.println("\n2. Parsing feature file...");
            CSFeatureParser parser = new CSFeatureParser();
            CSFeatureFile feature = parser.parseFeatureFile(featurePath);
            
            System.out.println("   Feature name: " + feature.getName());
            System.out.println("   Number of scenarios: " + feature.getScenarios().size());
            System.out.println("   Feature tags: " + feature.getTags());
            
            // 3. List scenarios
            System.out.println("\n3. Scenarios in feature file:");
            for (CSFeatureFile.Scenario scenario : feature.getScenarios()) {
                System.out.println("   - " + scenario.getName());
                System.out.println("     Tags: " + scenario.getTags());
                System.out.println("     Steps: " + scenario.getSteps().size());
            }
            
            // 4. Register step definitions
            System.out.println("\n4. Registering step definitions...");
            CSStepRegistry registry = CSStepRegistry.getInstance();
            registry.scanPackage("com.akhan.stepdefs");
            
            int totalSteps = registry.getAllSteps().values().stream()
                .mapToInt(list -> list.size())
                .sum();
            System.out.println("   Total steps registered: " + totalSteps);
            
            // 5. Check if steps match
            System.out.println("\n5. Checking if scenario steps have matching definitions:");
            for (CSFeatureFile.Scenario scenario : feature.getScenarios()) {
                System.out.println("\n   Scenario: " + scenario.getName());
                for (CSFeatureFile.Step step : scenario.getSteps()) {
                    String stepText = step.getText();
                    var stepDef = registry.findStep(stepText, null);
                    if (stepDef != null) {
                        System.out.println("     ✓ " + step.getKeyword() + " " + stepText);
                    } else {
                        System.out.println("     ✗ " + step.getKeyword() + " " + stepText + " [NO MATCH]");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}