import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestExactPattern {
    public static void main(String[] args) {
        // Exactly replicate what CSStepDefinition.compilePattern does
        String originalPattern = "I enter username {username}";
        
        System.out.println("Original pattern: " + originalPattern);
        
        // Check if pattern contains regex symbols (it doesn't)
        if (originalPattern.contains("^") || originalPattern.contains("$") || originalPattern.contains("\\")) {
            System.out.println("Pattern contains regex symbols - using as-is");
        } else {
            System.out.println("Converting placeholder-based pattern to regex");
        }
        
        String regex = originalPattern;
        
        // First, escape special regex characters (except our placeholders)
        System.out.println("Before escaping: " + regex);
        regex = regex.replaceAll("([\\[\\]().*+?])", "\\\\$1");
        System.out.println("After escaping: " + regex);
        
        // Replace placeholders with regex groups
        regex = regex
            .replaceAll("\\{string\\}", "\"([^\"]*)\"")      
            .replaceAll("\\{int\\}", "(\\d+)")                
            .replaceAll("\\{float\\}", "(\\d+\\.\\d+)")       
            .replaceAll("\\{number\\}", "(\\d+(?:\\.\\d+)?)")
            .replaceAll("\\{word\\}", "(\\w+)")               
            .replaceAll("\\{(\\w+)\\}", "(?:\"([^\"]*)\"|([^\\s]+))");
            
        System.out.println("After replacements: " + regex);
        System.out.println("Final regex: ^" + regex + "$");
        System.out.println();
        
        Pattern pattern = Pattern.compile("^" + regex + "$");
        
        // Test cases
        String[] testCases = {
            "I enter username \"testuser1\"",
            "I enter username \"testuser1@americas.cshare.net\"",
            "I enter username testuser1",
            "I enter username testuser1@americas.cshare.net"
        };
        
        for (String test : testCases) {
            System.out.println("Testing: " + test);
            Matcher matcher = pattern.matcher(test);
            boolean matches = matcher.matches();
            System.out.println("  Matches: " + matches);
            
            if (matches) {
                System.out.println("  Groups found: " + matcher.groupCount());
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    System.out.println("    Group " + i + ": '" + matcher.group(i) + "'");
                }
            }
            System.out.println();
        }
    }
}