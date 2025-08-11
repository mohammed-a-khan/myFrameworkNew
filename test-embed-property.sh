#!/bin/bash

echo "Testing property reading..."

# Create a simple Java test to verify property reading
cat > TestProperty.java << 'EOF'
import com.testforge.cs.config.CSConfigManager;

public class TestProperty {
    public static void main(String[] args) {
        CSConfigManager config = CSConfigManager.getInstance();
        
        // Test with system property
        System.setProperty("cs.report.screenshots.embed", "true");
        String value = config.getProperty("cs.report.screenshots.embed", "false");
        System.out.println("Property value (with system prop): " + value);
        
        // Test without system property
        System.clearProperty("cs.report.screenshots.embed");
        value = config.getProperty("cs.report.screenshots.embed", "false");
        System.out.println("Property value (from file): " + value);
    }
}
EOF

# Compile and run
javac -cp "target/classes:lib/*" TestProperty.java
java -cp "target/classes:lib/*:." TestProperty

rm TestProperty.java TestProperty.class