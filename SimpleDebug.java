import java.lang.reflect.Method;

public class SimpleDebug {
    public static void main(String[] args) {
        try {
            System.out.println("=== TESTING CSReportManager.fail() FLOW ===");
            
            // Load CSReportManager class
            Class<?> reportManagerClass = Class.forName("com.testforge.cs.reporting.CSReportManager");
            
            // Test startStep
            System.out.println("1. Calling startStep...");
            Method startStepMethod = reportManagerClass.getMethod("startStep", String.class, String.class);
            startStepMethod.invoke(null, "Given", "I am on the login page");
            
            // Test fail method
            System.out.println("2. Calling fail...");
            Method failMethod = reportManagerClass.getMethod("fail", String.class);
            failMethod.invoke(null, "Custom user message for I am on the login page");
            
            // Test endStep
            System.out.println("3. Calling endStep...");
            Method endStepMethod = reportManagerClass.getMethod("endStep");
            endStepMethod.invoke(null);
            
            // Test getLastStepActions
            System.out.println("4. Getting actions...");
            Method getActionsMethod = reportManagerClass.getMethod("getLastStepActions");
            Object actions = getActionsMethod.invoke(null);
            
            if (actions != null) {
                System.out.println("SUCCESS: Found actions - " + actions.toString());
            } else {
                System.out.println("PROBLEM: No actions found!");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
