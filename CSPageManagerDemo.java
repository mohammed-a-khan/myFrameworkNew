// DEMONSTRATION: CSPageManager Caching Behavior
public class CSPageManagerDemo {
    
    public void demonstrateCaching() {
        String threadName = Thread.currentThread().getName();
        System.out.println("\n=== " + threadName + " ===");
        
        // First call - creates new instance
        LoginPage login1 = CSPageManager.getPage(LoginPage.class);
        System.out.println("Call 1: " + login1.hashCode() + " (NEW INSTANCE)");
        
        // Second call - returns cached instance
        LoginPage login2 = CSPageManager.getPage(LoginPage.class);
        System.out.println("Call 2: " + login2.hashCode() + " (CACHED)");
        
        // Third call - returns same cached instance
        LoginPage login3 = CSPageManager.getPage(LoginPage.class);
        System.out.println("Call 3: " + login3.hashCode() + " (CACHED)");
        
        // Different page class - creates new instance
        DashboardPage dashboard = CSPageManager.getPage(DashboardPage.class);
        System.out.println("Dashboard: " + dashboard.hashCode() + " (NEW INSTANCE)");
        
        // Same dashboard class - returns cached
        DashboardPage dashboard2 = CSPageManager.getPage(DashboardPage.class);
        System.out.println("Dashboard2: " + dashboard2.hashCode() + " (CACHED)");
        
        // Verify they're the same objects
        System.out.println("login1 == login2: " + (login1 == login2)); // true
        System.out.println("login2 == login3: " + (login2 == login3)); // true
        System.out.println("dashboard == dashboard2: " + (dashboard == dashboard2)); // true
    }
}

/*
EXPECTED OUTPUT FOR SINGLE THREAD:

=== TestNG-PoolService-1 ===
Call 1: 123456789 (NEW INSTANCE)     ← Created
Call 2: 123456789 (CACHED)           ← Same object returned
Call 3: 123456789 (CACHED)           ← Same object returned
Dashboard: 987654321 (NEW INSTANCE)   ← Created (different class)
Dashboard2: 987654321 (CACHED)        ← Same dashboard object
login1 == login2: true
login2 == login3: true
dashboard == dashboard2: true

=== TestNG-PoolService-2 ===
Call 1: 111222333 (NEW INSTANCE)     ← Different instance for Thread-2
Call 2: 111222333 (CACHED)           ← Same object for Thread-2
...
*/