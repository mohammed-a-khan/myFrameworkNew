#!/bin/bash
# Direct test of soft fail with screenshot

echo "=== Testing Soft Fail Screenshots Directly ==="

# Create a simple test that definitely calls CSReportManager.fail()
cat > TestSoftFailDirect.java << 'EOF'
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.driver.CSWebDriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

public class TestSoftFailDirect {
    @Test
    public void testSoftFailWithScreenshot() {
        System.out.println("Starting direct soft fail test...");
        
        try {
            // Create a WebDriver instance for screenshots
            WebDriver driver = CSWebDriverManager.createDriver("chrome", false);
            driver.get("https://www.google.com");
            
            // This should trigger screenshot capture
            CSReportManager.fail("This is a test soft fail message");
            
            System.out.println("Soft fail executed, continuing test...");
            
            driver.quit();
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# Compile and run the direct test
CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
javac -cp "target/classes:target/dependency/*" TestSoftFailDirect.java

CS_ENCRYPTION_KEY="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
java -cp ".:target/classes:target/dependency/*" \
    -Dcs.encryption.key="LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=" \
    -Dcs.soft.fail.capture.screenshot=true \
    -Dcs.screenshot.compression.enabled=false \
    org.testng.TestNG -testclass TestSoftFailDirect

# Clean up
rm -f TestSoftFailDirect.java TestSoftFailDirect.class

echo "=== Checking Results ==="
if [ -d "/tmp/cs-temp-screenshots" ]; then
    echo "Found temp screenshots directory:"
    ls -la /tmp/cs-temp-screenshots/
else
    echo "No temp screenshots directory found"
fi