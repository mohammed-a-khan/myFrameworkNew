# Lazy Page Initialization in Step Definitions

## The Problem

When creating step definition classes that extend `CSStepDefinitions`, you might encounter instantiation failures if you try to initialize page objects as field declarations:

### ❌ This will cause instantiation failure:
```java
public class MySteps extends CSStepDefinitions {
    // This tries to call getPage() during class instantiation
    // WebDriver is not initialized yet!
    private MyPage myPage = getPage(MyPage.class);  // FAILS!
}
```

### Why It Fails

1. **Timing Issue**: Field initialization happens when the class is instantiated by the framework
2. **WebDriver Not Ready**: At instantiation time, the WebDriver hasn't been initialized yet
3. **Parent Class State**: The parent class `CSStepDefinitions` hasn't fully initialized its state
4. **Test Context Missing**: The test context and thread-local storage aren't set up yet

## The Solutions

### Solution 1: Initialize in Each Method (Works but Repetitive)

```java
public class MySteps extends CSStepDefinitions {
    private MyPage myPage;  // Don't initialize here
    
    @CSStep("I do something")
    public void doSomething() {
        myPage = getPage(MyPage.class);  // Initialize when needed
        myPage.performAction();
    }
    
    @CSStep("I do another thing")
    public void doAnotherThing() {
        myPage = getPage(MyPage.class);  // Repeat in every method
        myPage.performAnotherAction();
    }
}
```

**Pros**: Works reliably
**Cons**: Repetitive code, not DRY principle

### Solution 2: Use Lazy Initialization with getOrCreatePage() (Recommended)

The framework now provides `getOrCreatePage()` method that handles lazy initialization with caching:

```java
public class MySteps extends CSStepDefinitions {
    
    // Create getter methods for clean access
    private MyPage myPage() {
        return getOrCreatePage(MyPage.class);
    }
    
    @CSStep("I do something")
    public void doSomething() {
        myPage().performAction();  // Clean and simple
    }
    
    @CSStep("I do another thing")
    public void doAnotherThing() {
        myPage().performAnotherAction();  // No repetition
    }
}
```

**Pros**: 
- Clean, readable code
- No repetition
- Pages are cached after first creation
- Thread-safe implementation

### Solution 3: Initialize in a Setup Method

```java
public class MySteps extends CSStepDefinitions {
    private MyPage myPage;
    
    @Before  // Or in a specific initialization step
    public void setup() {
        myPage = getPage(MyPage.class);
    }
    
    @CSStep("I do something")
    public void doSomething() {
        myPage.performAction();
    }
}
```

**Pros**: Single initialization point
**Cons**: Requires setup method to be called

## Best Practice Recommendation

Use the **lazy initialization pattern with getter methods** (Solution 2):

```java
@CSFeature(name = "My Feature")
public class MySteps extends CSStepDefinitions {
    
    // Lazy getters for all page objects
    private LoginPage loginPage() {
        return getOrCreatePage(LoginPage.class);
    }
    
    private HomePage homePage() {
        return getOrCreatePage(HomePage.class);
    }
    
    private SearchPage searchPage() {
        return getOrCreatePage(SearchPage.class);
    }
    
    // Clean step definitions
    @CSStep("I login with username {username}")
    public void login(String username) {
        loginPage().enterUsername(username);
        loginPage().clickLogin();
    }
    
    @CSStep("I search for {query}")
    public void search(String query) {
        searchPage().enterSearchQuery(query);
        searchPage().clickSearch();
    }
    
    @CSStep("I should be on home page")
    public void verifyHomePage() {
        homePage().verifyPageDisplayed();
    }
}
```

## Benefits of This Approach

1. **No Initialization Issues**: Pages are created only when needed
2. **Clean Code**: No repetitive initialization in every method
3. **Performance**: Pages are cached after first creation
4. **Thread Safety**: ConcurrentHashMap ensures thread-safe caching
5. **Readable**: Method names clearly indicate what page is being used
6. **Maintainable**: Easy to add new pages or modify existing ones

## How getOrCreatePage() Works

The `getOrCreatePage()` method in `CSStepDefinitions`:

1. Checks if the page is already in the cache
2. If not, creates it using `getPage()` when WebDriver is ready
3. Caches the page instance for future use
4. Returns the cached instance on subsequent calls

```java
protected <T> T getOrCreatePage(Class<T> pageClass) {
    return (T) pageCache.computeIfAbsent(pageClass, clazz -> {
        try {
            return getPage(clazz);  // Only called once per page class
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize page: " + clazz.getName(), e);
        }
    });
}
```

## Migration Guide

To migrate existing step definitions:

### Before:
```java
public class MySteps extends CSStepDefinitions {
    private MyPage myPage = getPage(MyPage.class);  // FAILS
    
    @CSStep("...")
    public void myStep() {
        myPage.doSomething();
    }
}
```

### After:
```java
public class MySteps extends CSStepDefinitions {
    private MyPage myPage() {
        return getOrCreatePage(MyPage.class);
    }
    
    @CSStep("...")
    public void myStep() {
        myPage().doSomething();  // Just add parentheses
    }
}
```

## Troubleshooting

### If you still get instantiation errors:

1. **Check Field Initialization**: Ensure NO page objects are initialized at field declaration
2. **Check Static Fields**: Static page fields will cause issues
3. **Check Constructor**: Don't initialize pages in the constructor
4. **Use Debug Logging**: Enable debug logging to see when pages are created

### Example of what to avoid:

```java
public class MySteps extends CSStepDefinitions {
    // ALL OF THESE WILL CAUSE PROBLEMS:
    
    // ❌ Direct initialization
    private MyPage page1 = getPage(MyPage.class);
    
    // ❌ Static field
    private static MyPage page2;
    
    // ❌ Initialization in constructor
    public MySteps() {
        page3 = getPage(MyPage.class);
    }
    
    // ❌ Initialization in instance initializer
    {
        page4 = getPage(MyPage.class);
    }
}
```

## Summary

- **Never** initialize page objects at field declaration time
- **Always** use lazy initialization when WebDriver is ready
- **Prefer** the `getOrCreatePage()` method with getter pattern
- **Remember** that pages are cached for performance

This pattern ensures your step definitions are robust, maintainable, and free from initialization issues.