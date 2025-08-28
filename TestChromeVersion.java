import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.config.CSConfigManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

public class TestChromeVersion {
    public static void main(String[] args) {
        System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
        
        try {
            System.out.println("=== Chrome Version Compatibility Test ===");
            
            // Check system Chrome version
            System.out.println("Attempting to detect Chrome version...");
            
            // Try to set up Chrome driver with version detection disabled
            WebDriverManager chromeManager = WebDriverManager.chromedriver();
            chromeManager.avoidBrowserDetection();
            chromeManager.driverVersion("138.0.6613.84"); // Compatible with Chrome 138
            
            try {
                chromeManager.setup();
                System.out.println("WebDriverManager setup successful with version 138.0.6613.84");
            } catch (Exception e) {
                System.out.println("WebDriverManager failed with specific version: " + e.getMessage());
                
                // Try generic setup
                try {
                    WebDriverManager.chromedriver().setup();
                    System.out.println("WebDriverManager setup successful with generic version");
                } catch (Exception e2) {
                    System.out.println("WebDriverManager generic setup also failed: " + e2.getMessage());
                }
            }
            
            // Try to create Chrome driver
            try {
                WebDriver driver = new ChromeDriver();
                System.out.println("Chrome driver created successfully!");
                
                // Get browser info
                String browserName = ((RemoteWebDriver) driver).getCapabilities().getBrowserName();
                String browserVersion = ((RemoteWebDriver) driver).getCapabilities().getBrowserVersion();
                System.out.println("Browser: " + browserName);
                System.out.println("Version: " + browserVersion);
                
                // Navigate to a simple page to test functionality
                driver.get("https://www.google.com");
                System.out.println("Navigation successful - Title: " + driver.getTitle());
                
                driver.quit();
                System.out.println("Chrome driver test completed successfully!");
                
            } catch (Exception e) {
                System.out.println("Chrome driver creation failed: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}