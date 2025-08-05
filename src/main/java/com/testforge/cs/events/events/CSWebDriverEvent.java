package com.testforge.cs.events.events;

import com.testforge.cs.events.CSEvent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * WebDriver lifecycle and operation events
 */
public class CSWebDriverEvent extends CSEvent {
    
    public static final String DRIVER_CREATED = "webdriver.created";
    public static final String DRIVER_QUIT = "webdriver.quit";
    public static final String BROWSER_OPENED = "browser.opened";
    public static final String BROWSER_CLOSED = "browser.closed";
    public static final String NAVIGATION = "webdriver.navigation";
    public static final String ELEMENT_FOUND = "webdriver.element.found";
    public static final String ELEMENT_NOT_FOUND = "webdriver.element.not_found";
    public static final String ELEMENT_CLICK = "webdriver.element.click";
    public static final String ELEMENT_SEND_KEYS = "webdriver.element.send_keys";
    public static final String SCREENSHOT_TAKEN = "webdriver.screenshot";
    public static final String PAGE_LOAD_TIMEOUT = "webdriver.page_load_timeout";
    public static final String ELEMENT_TIMEOUT = "webdriver.element_timeout";
    
    private final String browserType;
    private final String driverVersion;
    private final String url;
    private final String locator;
    private final WebElement element;
    private final String action;
    private final Object value;
    private final long executionTime;
    private final Exception exception;
    
    private CSWebDriverEvent(Builder builder) {
        super(builder.eventType, builder.source != null ? builder.source : "WebDriver");
        this.browserType = builder.browserType;
        this.driverVersion = builder.driverVersion;
        this.url = builder.url;
        this.locator = builder.locator;
        this.element = builder.element;
        this.action = builder.action;
        this.value = builder.value;
        this.executionTime = builder.executionTime;
        this.exception = builder.exception;
        
        // Add WebDriver-specific context
        addContext("browser.type", browserType);
        addContext("driver.version", driverVersion);
        addContext("page.url", url);
        addContext("element.locator", locator);
        addContext("action", action);
        addContext("execution.time.ms", executionTime);
        
        if (value != null) {
            addContext("action.value", value.toString());
        }
        
        if (exception != null) {
            addContext("exception.message", exception.getMessage());
            addContext("exception.type", exception.getClass().getSimpleName());
        }
        
        if (element != null) {
            try {
                addContext("element.tag", element.getTagName());
                addContext("element.text", element.getText());
                addContext("element.displayed", element.isDisplayed());
                addContext("element.enabled", element.isEnabled());
            } catch (Exception e) {
                // Element might be stale, ignore
            }
        }
    }
    
    public String getBrowserType() {
        return browserType;
    }
    
    public String getDriverVersion() {
        return driverVersion;
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getLocator() {
        return locator;
    }
    
    public WebElement getElement() {
        return element;
    }
    
    public String getAction() {
        return action;
    }
    
    public Object getValue() {
        return value;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public Exception getException() {
        return exception;
    }
    
    @Override
    public EventSeverity getSeverity() {
        if (exception != null) {
            return EventSeverity.ERROR;
        }
        
        switch (getEventType()) {
            case ELEMENT_NOT_FOUND:
            case PAGE_LOAD_TIMEOUT:
            case ELEMENT_TIMEOUT:
                return EventSeverity.WARN;
            case DRIVER_CREATED:
            case DRIVER_QUIT:
            case BROWSER_OPENED:
            case BROWSER_CLOSED:
                return EventSeverity.INFO;
            default:
                return EventSeverity.DEBUG;
        }
    }
    
    @Override
    public EventCategory getCategory() {
        return EventCategory.WEB_DRIVER;
    }
    
    public static Builder builder(String eventType) {
        return new Builder(eventType);
    }
    
    /**
     * Builder for WebDriver events
     */
    public static class Builder {
        private final String eventType;
        private String source;
        private String browserType;
        private String driverVersion;
        private String url;
        private String locator;
        private WebElement element;
        private String action;
        private Object value;
        private long executionTime;
        private Exception exception;
        
        private Builder(String eventType) {
            this.eventType = eventType;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder browserType(String browserType) {
            this.browserType = browserType;
            return this;
        }
        
        public Builder driverVersion(String driverVersion) {
            this.driverVersion = driverVersion;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder locator(String locator) {
            this.locator = locator;
            return this;
        }
        
        public Builder element(WebElement element) {
            this.element = element;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder value(Object value) {
            this.value = value;
            return this;
        }
        
        public Builder executionTime(long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder exception(Exception exception) {
            this.exception = exception;
            return this;
        }
        
        public CSWebDriverEvent build() {
            return new CSWebDriverEvent(this);
        }
    }
    
    // Factory methods for common events
    public static CSWebDriverEvent driverCreated(String browserType, String driverVersion) {
        return builder(DRIVER_CREATED)
            .browserType(browserType)
            .driverVersion(driverVersion)
            .build();
    }
    
    public static CSWebDriverEvent navigation(String url, long executionTime) {
        return builder(NAVIGATION)
            .url(url)
            .action("navigate")
            .executionTime(executionTime)
            .build();
    }
    
    public static CSWebDriverEvent elementFound(String locator, WebElement element, long executionTime) {
        return builder(ELEMENT_FOUND)
            .locator(locator)
            .element(element)
            .executionTime(executionTime)
            .build();
    }
    
    public static CSWebDriverEvent elementNotFound(String locator, Exception exception, long executionTime) {
        return builder(ELEMENT_NOT_FOUND)
            .locator(locator)
            .exception(exception)
            .executionTime(executionTime)
            .build();
    }
    
    public static CSWebDriverEvent elementClick(String locator, WebElement element, long executionTime) {
        return builder(ELEMENT_CLICK)
            .locator(locator)
            .element(element)
            .action("click")
            .executionTime(executionTime)
            .build();
    }
    
    public static CSWebDriverEvent screenshotTaken(String filePath, long executionTime) {
        return builder(SCREENSHOT_TAKEN)
            .action("screenshot")
            .value(filePath)
            .executionTime(executionTime)
            .build();
    }
}