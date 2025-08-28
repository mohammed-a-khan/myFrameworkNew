import com.testforge.cs.config.CSConfigManager;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestLocalDriverSetup {
    public static void main(String[] args) {
        System.setProperty("cs.encryption.key", "LhABO2esT/784OrITVLdNbA+7Slaialzf3SKwsNVCYA=");
        
        CSConfigManager config = CSConfigManager.getInstance();
        
        // Test local driver lookup configuration
        System.out.println("=== Local Driver Configuration Test ===");
        System.out.println("Local lookup enabled: " + config.getBooleanProperty("cs.driver.local.lookup.enabled", false));
        System.out.println("Local path: " + config.getProperty("cs.driver.local.path", "server"));
        System.out.println("Chrome executable: " + config.getProperty("cs.driver.chrome.executable", "chromedriver.exe"));
        System.out.println("Firefox executable: " + config.getProperty("cs.driver.firefox.executable", "geckodriver.exe"));
        System.out.println("Edge executable: " + config.getProperty("cs.driver.edge.executable", "msedgedriver.exe"));
        System.out.println("IE executable: " + config.getProperty("cs.driver.ie.executable", "IEDriverServer.exe"));
        System.out.println("Safari executable: " + config.getProperty("cs.driver.safari.executable", "safaridriver"));
        
        // Test path resolution
        System.out.println("\n=== Driver Path Resolution Test ===");
        String localDriverPath = config.getProperty("cs.driver.local.path", "server");
        
        Path driverDir;
        if (Paths.get(localDriverPath).isAbsolute()) {
            driverDir = Paths.get(localDriverPath);
        } else {
            driverDir = Paths.get(System.getProperty("user.dir"), localDriverPath);
        }
        
        System.out.println("Resolved driver directory: " + driverDir.toAbsolutePath());
        System.out.println("Driver directory exists: " + driverDir.toFile().exists());
        
        // Check for each driver type
        String[] browsers = {"chrome", "firefox", "edge", "ie", "safari"};
        for (String browser : browsers) {
            String executableName = getDriverExecutableName(config, browser);
            if (executableName != null) {
                File driverFile = driverDir.resolve(executableName).toFile();
                System.out.println(browser + " driver (" + executableName + "): " + 
                    (driverFile.exists() ? "FOUND" : "NOT FOUND") + " at " + driverFile.getAbsolutePath());
            }
        }
        
        System.out.println("\n=== WebDriverManager Configuration Test ===");
        System.out.println("Offline first: " + config.getBooleanProperty("cs.webdrivermanager.offline.first", false));
        System.out.println("Avoid browser detection: " + config.getBooleanProperty("cs.webdrivermanager.avoid.browser.detection", false));
        System.out.println("Timeout seconds: " + config.getIntegerProperty("cs.webdrivermanager.timeout.seconds", 30));
    }
    
    private static String getDriverExecutableName(CSConfigManager config, String browserType) {
        switch (browserType.toLowerCase()) {
            case "chrome":
                return config.getProperty("cs.driver.chrome.executable", "chromedriver.exe");
            case "firefox":
                return config.getProperty("cs.driver.firefox.executable", "geckodriver.exe");
            case "edge":
                return config.getProperty("cs.driver.edge.executable", "msedgedriver.exe");
            case "ie":
            case "internet explorer":
                return config.getProperty("cs.driver.ie.executable", "IEDriverServer.exe");
            case "safari":
                return config.getProperty("cs.driver.safari.executable", "safaridriver");
            default:
                return null;
        }
    }
}