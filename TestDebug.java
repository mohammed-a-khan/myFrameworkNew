
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class TestDebug {
    public static void main(String[] args) {
        Map<String, Object> resultData = new HashMap<>();
        
        // Test point
        Map<String, Object> testPoint = new HashMap<>();
        testPoint.put("id", 3);
        resultData.put("testPoint", testPoint);
        
        // Test case
        Map<String, Object> testCase = new HashMap<>();
        testCase.put("id", 419);
        resultData.put("testCase", testCase);
        
        resultData.put("testCaseTitle", "Test Title");
        resultData.put("automatedTestName", "com.test.MyTest");
        resultData.put("automatedTestStorage", "com.test");
        resultData.put("outcome", "Passed");
        resultData.put("state", "Completed");
        resultData.put("startedDate", "2025-08-14T05:35:00");
        resultData.put("completedDate", "2025-08-14T05:35:10");
        resultData.put("durationInMs", 10000);
        
        List<Map<String, Object>> results = Arrays.asList(resultData);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

