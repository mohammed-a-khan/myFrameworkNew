import java.util.regex.Pattern;

public class TestPatternMatching {
    public static void main(String[] args) {
        // Test the regex pattern matching issue
        
        // Original pattern from step definition
        String originalPattern = "I enter username {username}";
        
        // Convert to regex (simulating what CSStepDefinition does)
        String regex = originalPattern
            .replaceAll("\\{", "\\\\{")
            .replaceAll("\\}", "\\\\}");
        
        // The actual replacement that happens in CSStepDefinition
        regex = regex.replaceAll("\\\\\\{(\\w+)\\\\\\}", "(?:\"([^\"]*)\"|([^\\s]+))");
        
        System.out.println("Original pattern: " + originalPattern);
        System.out.println("Generated regex: " + regex);
        System.out.println();
        
        // Compile the pattern
        Pattern pattern = Pattern.compile("^" + regex + "$");
        
        // Test cases
        String[] testCases = {
            "I enter username \"testuser1\"",
            "I enter username \"testuser1@americas.cshare.net\"",
            "I enter username testuser1",
            "I enter username testuser1@americas.cshare.net"
        };
        
        for (String test : testCases) {
            boolean matches = pattern.matcher(test).matches();
            System.out.println("Test: " + test);
            System.out.println("  Matches: " + matches);
            
            if (matches) {
                var matcher = pattern.matcher(test);
                matcher.matches();
                System.out.println("  Groups:");
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    System.out.println("    Group " + i + ": " + matcher.group(i));
                }
            }
            System.out.println();
        }
    }
}