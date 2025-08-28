import com.testforge.cs.driver.CSWebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

public class TestEdgeBrowserType {
    public static void main(String[] args) {
        try {
            System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
            
            // Create Edge browser
            WebDriver edgeDriver = CSWebDriverManager.createDriver("edge", true, null);
            
            // Check what browser name is reported
            String browserName = ((RemoteWebDriver) edgeDriver).getCapabilities().getBrowserName();
            System.out.println("Edge browser reports name as: " + browserName);
            System.out.println("Current browser type from manager: " + CSWebDriverManager.getCurrentBrowserType());
            
            // Close browser
            CSWebDriverManager.quitDriver();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}