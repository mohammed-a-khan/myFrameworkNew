package com.testforge.cs.core;

import com.testforge.cs.annotations.*;
import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSTestExecutionException;
import com.testforge.cs.listeners.CSTestListener;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.reporting.CSTestResult;
import com.testforge.cs.utils.*;
import com.testforge.cs.waits.CSWaitUtils;
import com.testforge.cs.driver.CSWebDriverManager;
import com.testforge.cs.driver.CSDriver;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Base test class for all test classes
 * Provides setup/teardown, parallel execution support, and test lifecycle management
 */
@Listeners({CSTestListener.class})
public abstract class CSBaseTest {
    protected static final Logger logger = LoggerFactory.getLogger(CSBaseTest.class);
    protected static final CSConfigManager config = CSConfigManager.getInstance();
    
    protected WebDriver driver;
    protected String testName;
    protected String testId;
    protected Map<String, Object> testData;
    protected CSTestResult testResult;
    protected CSReportManager reportManager;
    protected CSWaitUtils waitUtils;
    protected ITestContext testContext;
    
    private static final ThreadLocal<Map<String, Object>> threadLocalData = ThreadLocal.withInitial(HashMap::new);
    private static final Map<String, CSTestResult> testResults = new ConcurrentHashMap<>();
    
    /**
     * Suite level setup
     */
    @BeforeSuite(alwaysRun = true)
    public void setupSuite() {
        logger.info("Starting test suite");
        
        // Initialize report manager
        CSReportManager.getInstance().initializeReport("TestSuite_" + System.currentTimeMillis());
        
        // Execute @CSBeforeSuite methods
        executeAnnotatedMethods(CSBeforeSuite.class);
    }
    
    /**
     * Suite level teardown
     */
    @AfterSuite(alwaysRun = true)
    public void teardownSuite() {
        logger.info("Finishing test suite");
        
        // Execute @CSAfterSuite methods
        executeAnnotatedMethods(CSAfterSuite.class);
        
        // Generate final report
        CSReportManager.getInstance().generateReport();
        
        // Properly close all WebDriver instances managed by the framework
        logger.info("Closing all test browser instances...");
        CSWebDriverManager.quitAllDrivers();
        
        // DO NOT kill browser processes - this would close user's personal browsers
        // The quitAllDrivers() method should properly close all test browsers
        // If browsers remain open, it's a driver management issue that needs fixing
        
        CSDbUtils.closeAllDataSources();
    }
    
    /**
     * Class level setup
     */
    @BeforeClass(alwaysRun = true)
    public void setupClass(ITestContext context) {
        logger.info("Setting up test class: {}", this.getClass().getSimpleName());
        this.testContext = context;
        
        // Set maximum browsers based on suite configuration (do it here since BeforeSuite can't have parameters)
        String parallelMode = context.getSuite().getParallel();
        int threadCount = context.getSuite().getXmlSuite().getThreadCount();
        
        // Check if IE is being used - IE doesn't handle parallel execution well
        String browserName = context.getCurrentXmlTest() != null ? 
            context.getCurrentXmlTest().getParameter("browser.name") : 
            config.getProperty("browser.name", "chrome");
        
        // IMPORTANT: For parallel="methods", limit browsers to thread count
        // This prevents creating extra browser instances that remain idle
        if ("methods".equals(parallelMode) || "tests".equals(parallelMode) || "classes".equals(parallelMode)) {
            logger.info("Parallel mode '{}' detected with {} threads - limiting browsers to thread count", 
                parallelMode, threadCount);
            CSWebDriverManager.setMaxBrowsersAllowed(threadCount);
        }
        
        if ("ie".equalsIgnoreCase(browserName) || "internet explorer".equalsIgnoreCase(browserName)) {
            // Force sequential execution for IE
            CSWebDriverManager.setMaxBrowsersAllowed(1);
            logger.warn("Internet Explorer detected - forcing sequential execution (max 1 browser)");
            logger.warn("IE does not handle parallel execution well. Tests will run sequentially.");
            logger.warn("Consider using Edge or Chrome for parallel test execution.");
        } else if (parallelMode != null && !parallelMode.equals("none") && !parallelMode.equals("false")) {
            // In parallel mode, limit browsers to thread count
            CSWebDriverManager.setMaxBrowsersAllowed(threadCount);
            logger.info("Parallel mode '{}' with thread-count={}: Limiting browsers to {}", 
                parallelMode, threadCount, threadCount);
        } else {
            // In sequential mode, only allow 1 browser
            CSWebDriverManager.setMaxBrowsersAllowed(1);
            logger.info("Sequential mode: Limiting browsers to 1");
        }
        
        // Initialize class-level resources
        initializeClassResources();
    }
    
    /**
     * Class level teardown
     */
    @AfterClass(alwaysRun = true)
    public void teardownClass() {
        logger.info("Tearing down test class: {}", this.getClass().getSimpleName());
        
        // Cleanup class-level resources
        cleanupClassResources();
    }
    
    /**
     * Test method setup
     */
    @BeforeMethod(alwaysRun = true)
    public void setupTest(Method method, Object[] params, ITestContext context) {
        try {
            // Store test context
            this.testContext = context;
            
            // Get test information
            testName = method.getName();
            testId = UUID.randomUUID().toString();
            testData = new HashMap<>();
            
            // Check for @CSTest annotation
            CSTest csTest = method.getAnnotation(CSTest.class);
            if (csTest != null) {
                if (!csTest.name().isEmpty()) {
                    testName = csTest.name();
                }
                if (!csTest.id().isEmpty()) {
                    testId = csTest.id();
                }
                
                // Check if test is enabled
                if (!csTest.enabled()) {
                    throw new CSTestExecutionException("Test is disabled: " + testName);
                }
                
                // Check environment
                String currentEnv = config.getProperty("environment.name", "qa");
                if (csTest.environments().length > 0 && 
                    !Arrays.asList(csTest.environments()).contains(currentEnv)) {
                    throw new CSTestExecutionException("Test not enabled for environment: " + currentEnv);
                }
            }
            
            logger.info("Starting test: {} [{}]", testName, testId);
            
            // Initialize test result
            testResult = new CSTestResult();
            testResult.setTestId(testId);
            testResult.setTestName(testName);
            testResult.setClassName(this.getClass().getName());
            testResult.setMethodName(method.getName());
            testResult.setStartTime(LocalDateTime.now());
            testResult.setStatus(CSTestResult.Status.RUNNING);
            
            testResults.put(testId, testResult);
            
            // Setup browser if needed
            if (shouldInitializeBrowser(method)) {
                String threadName = Thread.currentThread().getName();
                long threadId = Thread.currentThread().getId();
                
                // For parallel execution, ALWAYS create a new driver for each test
                // to ensure proper thread isolation
                String parallelMode = context.getSuite().getParallel();
                boolean isParallel = parallelMode != null && !parallelMode.equals("none") && !parallelMode.equals("false");
                
                if (isParallel) {
                    // In parallel mode, reuse driver per thread
                    logger.info("[{}] Thread ID {} - Parallel mode ({})", 
                        threadName, threadId, parallelMode);
                    
                    // Check if browser should be reused
                    boolean reuseBrowser = config.getBooleanProperty("cs.browser.reuse.instance", true);
                    
                    // Check ThreadLocal for existing driver only if reuse is enabled
                    driver = reuseBrowser ? CSWebDriverManager.getDriver() : null;
                    
                    if (driver == null) {
                        logger.info("[{}] {} - will create new driver", threadName, 
                            reuseBrowser ? "No existing driver in ThreadLocal" : "Browser reuse disabled");
                        // No driver for this thread yet, create a new one
                        String browserType = getBrowserType(method);
                        boolean headless = config.getBooleanProperty("cs.browser.headless", false);
                        logger.info("[{}] Thread ID {} - Creating NEW {} driver (headless: {})", 
                            threadName, threadId, browserType, headless);
                        driver = CSWebDriverManager.createDriver(browserType, headless, null);
                        if (driver == null) {
                            logger.error("[{}] Failed to create driver - browser limit may have been reached", threadName);
                            throw new CSTestExecutionException("Failed to create WebDriver - browser limit reached");
                        }
                        logger.info("[{}] Thread ID {} - New driver created with hashCode: {}", 
                            threadName, threadId, driver.hashCode());
                    } else {
                        // Reuse existing driver for this thread
                        logger.info("[{}] Thread ID {} - Reusing existing driver with hashCode: {}", 
                            threadName, threadId, driver.hashCode());
                        // Clear cookies only
                        try {
                            driver.manage().deleteAllCookies();
                        } catch (Exception e) {
                            logger.warn("Failed to clear browser cookies: {}", e.getMessage());
                        }
                    }
                } else {
                    // Non-parallel mode - check for existing driver
                    boolean reuseBrowser = config.getBooleanProperty("cs.browser.reuse.instance", true);
                    driver = reuseBrowser ? CSWebDriverManager.getDriver() : null;
                    if (driver == null) {
                        String browserType = getBrowserType(method);
                        boolean headless = config.getBooleanProperty("cs.browser.headless", false);
                        logger.info("[{}] Creating NEW {} driver (headless: {})", threadName, browserType, headless);
                        driver = CSWebDriverManager.createDriver(browserType, headless, null);
                        logger.info("[{}] Driver created: {}", threadName, driver);
                    } else {
                        logger.info("[{}] Reusing existing driver for thread (browser.reuse.instance=true)", threadName);
                        // Clear cookies but don't navigate to about:blank as it creates confusion
                        // The test will navigate to the appropriate URL
                        try {
                            driver.manage().deleteAllCookies();
                            // Don't navigate to about:blank - let the test handle navigation
                        } catch (Exception e) {
                            logger.warn("Failed to clear browser cookies: {}", e.getMessage());
                        }
                    }
                }
                
                // Initialize utilities
                waitUtils = new CSWaitUtils(driver);
            } else {
                logger.info("[{}] Browser initialization skipped for method: {}", Thread.currentThread().getName(), method.getName());
            }
            
            // Initialize report manager
            reportManager = CSReportManager.getInstance();
            
            // Store test data
            if (params != null && params.length > 0) {
                testData.put("parameters", params);
            }
            
            // Execute custom setup
            onTestStart(method, params);
            
        } catch (Exception e) {
            logger.error("Failed to setup test: {}", testName, e);
            throw new CSTestExecutionException(testName, this.getClass().getName(), 
                                             "SETUP", "Test setup failed", e);
        }
    }
    
    /**
     * Test method teardown
     */
    @AfterMethod(alwaysRun = true)
    public void teardownTest(Method method, ITestResult result) {
        try {
            logger.info("Finishing test: {} [{}]", testName, testId);
            
            // Update test result
            if (testResult != null) {
                testResult.setEndTime(LocalDateTime.now());
                testResult.setDuration(testResult.calculateDuration());
                
                switch (result.getStatus()) {
                    case ITestResult.SUCCESS:
                        testResult.setStatus(CSTestResult.Status.PASSED);
                        break;
                    case ITestResult.FAILURE:
                        testResult.setStatus(CSTestResult.Status.FAILED);
                        testResult.setErrorMessage(result.getThrowable().getMessage());
                        testResult.setStackTrace(getStackTrace(result.getThrowable()));
                        captureFailureArtifacts();
                        break;
                    case ITestResult.SKIP:
                        testResult.setStatus(CSTestResult.Status.SKIPPED);
                        break;
                }
                
                // Add to report only if not a BDD test
                // BDD tests are managed by CSBDDRunner
                if (!method.getName().equals("executeBDDScenario")) {
                    CSReportManager.getInstance().addTestResult(testResult);
                }
            }
            
            // Execute custom teardown
            onTestEnd(method, result);
            
            // In parallel mode, check if this thread has more tests to run
            String parallelMode = result.getTestContext().getSuite().getParallel();
            boolean isParallel = parallelMode != null && !parallelMode.equals("none") && !parallelMode.equals("false");
            
            // Check if browser should be reused or closed after each test
            boolean reuseBrowser = config.getBooleanProperty("cs.browser.reuse.instance", true);
            
            if (!reuseBrowser) {
                // Close browser after each test when reuse is disabled
                logger.info("[{}] Closing browser after test (browser.reuse.instance=false)", Thread.currentThread().getName());
                CSWebDriverManager.quitDriver();
                driver = null;
            } else if (isParallel) {
                // Keep browser open for thread reuse in parallel mode
                logger.info("[{}] Keeping browser open for thread reuse in parallel mode (browser.reuse.instance=true)", Thread.currentThread().getName());
            } else {
                // In sequential mode, keep browser for next test
                logger.info("[{}] Sequential mode - keeping browser for next test (browser.reuse.instance=true)", Thread.currentThread().getName());
            }
            
            // Clear thread local data
            threadLocalData.remove();
            
        } catch (Exception e) {
            logger.error("Error during test teardown: {}", testName, e);
        }
    }
    
    /**
     * Data provider for @CSDataSource annotation
     */
    @DataProvider(name = "CSDataProvider", parallel = true)
    public Object[][] getTestData(Method method) {
        CSDataSource dataSource = method.getAnnotation(CSDataSource.class);
        if (dataSource == null) {
            return new Object[0][0];
        }
        
        List<Map<String, String>> data = new ArrayList<>();
        
        switch (dataSource.type()) {
            case EXCEL:
                data = CSExcelUtils.readExcelWithKey(
                    dataSource.source(), 
                    dataSource.sheet(), 
                    dataSource.key(), 
                    Arrays.asList(dataSource.keyValues().split(","))
                );
                break;
                
            case CSV:
                data = CSCsvUtils.readCsvWithKey(
                    dataSource.source(),
                    dataSource.key(),
                    Arrays.asList(dataSource.keyValues().split(",")),
                    dataSource.hasHeader(),
                    ","
                );
                break;
                
            case JSON:
                String json = CSFileUtils.readFileAsString(dataSource.source());
                List<Map<String, Object>> jsonData = CSJsonUtils.jsonToListOfMaps(json);
                // Convert to List<Map<String, String>>
                data = jsonData.stream()
                    .map(map -> {
                        Map<String, String> stringMap = new HashMap<>();
                        map.forEach((k, v) -> stringMap.put(k, v != null ? v.toString() : ""));
                        return stringMap;
                    })
                    .collect(Collectors.toList());
                if (!dataSource.key().isEmpty()) {
                    data = filterDataByKey(data, dataSource.key(), dataSource.keyValues());
                }
                break;
                
            case DATABASE:
                String query = config.getProperty(dataSource.source());
                if (query == null) {
                    query = dataSource.source();
                }
                List<Map<String, Object>> dbData = CSDbUtils.executeQuery(dataSource.database(), query);
                // Convert List<Map<String, Object>> to List<Map<String, String>>
                data = dbData.stream()
                    .map(map -> {
                        Map<String, String> stringMap = new HashMap<>();
                        map.forEach((k, v) -> stringMap.put(k, v != null ? v.toString() : ""));
                        return stringMap;
                    })
                    .collect(Collectors.toList());
                break;
        }
        
        // Apply additional filter if specified
        if (!dataSource.filter().isEmpty()) {
            data = applyFilter(data, dataSource.filter());
        }
        
        // Convert to Object array
        return data.stream()
            .map(row -> new Object[]{row})
            .toArray(Object[][]::new);
    }
    
    /**
     * Hook method for custom test start logic
     */
    protected void onTestStart(Method method, Object[] params) {
        // Override in subclasses for custom logic
    }
    
    /**
     * Hook method for custom test end logic
     */
    protected void onTestEnd(Method method, ITestResult result) {
        // Override in subclasses for custom logic
    }
    
    /**
     * Initialize class-level resources
     */
    protected void initializeClassResources() {
        // Override in subclasses
    }
    
    /**
     * Cleanup class-level resources
     */
    protected void cleanupClassResources() {
        // Override in subclasses
    }
    
    /**
     * Store data in thread-local storage
     */
    protected void storeData(String key, Object value) {
        threadLocalData.get().put(key, value);
    }
    
    /**
     * Retrieve data from thread-local storage
     */
    @SuppressWarnings("unchecked")
    protected <T> T getData(String key) {
        return (T) threadLocalData.get().get(key);
    }
    
    /**
     * Get current test result
     */
    protected CSTestResult getCurrentTestResult() {
        return testResult;
    }
    
    /**
     * Add attachment to current test
     */
    protected void addAttachment(String name, String path) {
        if (testResult != null) {
            testResult.addAttachment(name, path);
        }
    }
    
    /**
     * Log test step
     */
    protected void logStep(String step) {
        logger.info("Step: {}", step);
        if (testResult != null) {
            testResult.addStep(step);
        }
    }
    
    /**
     * Take screenshot
     */
    protected String takeScreenshot(String name) {
        if (driver != null) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = name + "_" + timestamp + ".png";
            
            // Use temp directory to avoid redundancy
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "cs-temp-screenshots");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            String filePath = tempDir.getAbsolutePath() + "/" + fileName;
            
            File screenshot = CSWebDriverManager.takeScreenshot(filePath);
            if (screenshot != null && screenshot.exists()) {
                addAttachment(name, screenshot.getAbsolutePath());
                return screenshot.getAbsolutePath();
            }
        }
        return null;
    }
    
    /**
     * Get CSDriver instance
     */
    protected CSDriver getCSDriver() {
        if (driver == null) {
            throw new CSTestExecutionException("WebDriver not initialized. Ensure test is configured for UI testing.");
        }
        return new CSDriver(driver);
    }
    
    /**
     * Check if browser should be initialized
     */
    private boolean shouldInitializeBrowser(Method method) {
        // Check for UI test annotations or indicators
        CSTest csTest = method.getAnnotation(CSTest.class);
        if (csTest != null && csTest.type() == CSTest.TestType.API) {
            return false;
        }
        
        // Default to true for UI tests
        return true;
    }
    
    /**
     * Get browser type for test
     */
    private String getBrowserType(Method method) {
        // First check if there's a CSTest annotation with browser specification
        CSTest csTest = method.getAnnotation(CSTest.class);
        if (csTest != null && csTest.browsers().length > 0) {
            // Return first browser for now (can be enhanced for multi-browser testing)
            return csTest.browsers()[0];
        }
        
        // Then check TestNG parameters from suite XML
        if (testContext != null && testContext.getCurrentXmlTest() != null) {
            String browserParam = testContext.getCurrentXmlTest().getParameter("browser.name");
            if (browserParam != null && !browserParam.isEmpty()) {
                logger.info("Using browser from TestNG parameter: {}", browserParam);
                return browserParam;
            }
        }
        
        // Finally fall back to properties file
        String browser = config.getProperty("browser.name", "chrome");
        logger.debug("Using browser from properties: {}", browser);
        return browser;
    }
    
    /**
     * Capture failure artifacts
     */
    private void captureFailureArtifacts() {
        try {
            // Take screenshot
            if (config.getBooleanProperty("cs.screenshot.on.failure", true)) {
                takeScreenshot("failure_screenshot");
            }
            
            // Capture page source
            if (driver != null) {
                String pageSource = driver.getPageSource();
                String fileName = "page_source_" + System.currentTimeMillis() + ".html";
                
                // Use temp directory to avoid redundancy
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "cs-temp-screenshots");
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                String filePath = tempDir.getAbsolutePath() + "/" + fileName;
                
                CSFileUtils.writeStringToFile(filePath, pageSource);
                addAttachment("Page Source", filePath);
            }
            
        } catch (Exception e) {
            logger.error("Error capturing failure artifacts", e);
        }
    }
    
    /**
     * Execute methods with specific annotation
     */
    private void executeAnnotatedMethods(Class<? extends java.lang.annotation.Annotation> annotationClass) {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(annotationClass)) {
                try {
                    method.invoke(this);
                } catch (Exception e) {
                    logger.error("Error executing annotated method: {}", method.getName(), e);
                }
            }
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Filter data by key values
     */
    private List<Map<String, String>> filterDataByKey(List<Map<String, String>> data, 
                                                      String key, String keyValues) {
        List<String> values = Arrays.asList(keyValues.split(","));
        return data.stream()
            .filter(row -> values.contains(row.get(key)))
            .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * Apply custom filter expression
     * Supports expressions like: "status=active", "age>25", "name~John", "price>=100 AND category=electronics"
     */
    private List<Map<String, String>> applyFilter(List<Map<String, String>> data, String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return data;
        }
        
        try {
            // Parse and apply filter expressions
            String[] conditions = filter.split("\\s+AND\\s+", -1);
            
            return data.stream()
                .filter(row -> {
                    for (String condition : conditions) {
                        if (!evaluateCondition(row, condition.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to apply filter: {}. Returning unfiltered data.", filter, e);
            return data;
        }
    }
    
    /**
     * Evaluate a single filter condition
     */
    private boolean evaluateCondition(Map<String, String> row, String condition) {
        // Support operators: =, !=, >, <, >=, <=, ~(contains), !~(not contains)
        String[] operators = {"!=", ">=", "<=", "!~", "=", ">", "<", "~"};
        
        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(Pattern.quote(op), 2);
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", ""); // Remove quotes
                    String actualValue = row.get(field);
                    
                    if (actualValue == null) {
                        return false;
                    }
                    
                    switch (op) {
                        case "=":
                            return actualValue.equals(value);
                        case "!=":
                            return !actualValue.equals(value);
                        case ">":
                            return compareNumeric(actualValue, value) > 0;
                        case "<":
                            return compareNumeric(actualValue, value) < 0;
                        case ">=":
                            return compareNumeric(actualValue, value) >= 0;
                        case "<=":
                            return compareNumeric(actualValue, value) <= 0;
                        case "~":
                            return actualValue.toLowerCase().contains(value.toLowerCase());
                        case "!~":
                            return !actualValue.toLowerCase().contains(value.toLowerCase());
                    }
                }
                break;
            }
        }
        
        return false;
    }
    
    /**
     * Compare values numerically if possible
     */
    private int compareNumeric(String actual, String expected) {
        try {
            double actualNum = Double.parseDouble(actual);
            double expectedNum = Double.parseDouble(expected);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return actual.compareTo(expected);
        }
    }
    
    /**
     * Convert database data to string maps
     */
    private List<Map<String, String>> convertDbDataToStringMaps(List<Map<String, Object>> dbData) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> row : dbData) {
            Map<String, String> stringRow = new HashMap<>();
            row.forEach((key, value) -> stringRow.put(key, value != null ? value.toString() : ""));
            result.add(stringRow);
        }
        return result;
    }
    
    /**
     * Find element using locator string and description
     */
    protected com.testforge.cs.elements.CSElement findElement(String locatorString, String description) {
        CSDriver csDriver = new CSDriver(driver);
        return csDriver.findElement(locatorString, description);
    }
}