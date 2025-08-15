package com.testforge.cs.events;

import com.testforge.cs.config.CSConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Central event management system
 * Provides enterprise-grade event handling with async processing, filtering, and metrics
 */
public class CSEventManager {
    private static final Logger logger = LoggerFactory.getLogger(CSEventManager.class);
    private static volatile CSEventManager instance;
    
    private final Map<String, CSEventListener> listeners;
    private final ExecutorService eventExecutor;
    private final ExecutorService asyncEventExecutor;
    private final BlockingQueue<CSEvent> eventQueue;
    private final Set<String> enabledEventTypes;
    private final Map<String, Object> eventFilters;
    private final CSEventMetrics metrics;
    private final ScheduledExecutorService metricsScheduler;
    
    private volatile boolean running;
    private volatile boolean asyncProcessing;
    private Thread eventProcessorThread;
    
    private CSEventManager() {
        this.listeners = new ConcurrentHashMap<>();
        this.eventQueue = new LinkedBlockingQueue<>(10000);
        this.enabledEventTypes = ConcurrentHashMap.newKeySet();
        this.eventFilters = new ConcurrentHashMap<>();
        this.metrics = new CSEventMetrics();
        
        // Create thread pools
        this.eventExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "CSEvent-Processor-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        
        this.asyncEventExecutor = Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "CSEvent-Async-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        
        this.metricsScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "CSEvent-Metrics");
                t.setDaemon(true);
                return t;
            }
        );
        
        initialize();
    }
    
    public static CSEventManager getInstance() {
        if (instance == null) {
            synchronized (CSEventManager.class) {
                if (instance == null) {
                    instance = new CSEventManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize event manager
     */
    private void initialize() {
        try {
            CSConfigManager config = CSConfigManager.getInstance();
            
            // Load configuration
            asyncProcessing = Boolean.parseBoolean(
                config.getProperty("cs.events.async.enabled", "true")
            );
            
            // Load enabled event types
            String enabledTypes = config.getProperty("cs.events.enabled.types", "*");
            if ("*".equals(enabledTypes)) {
                enabledEventTypes.add("*"); // All types enabled
            } else {
                enabledEventTypes.addAll(Arrays.asList(enabledTypes.split(",")));
            }
            
            // Start event processor
            start();
            
            // Schedule metrics reporting
            int metricsInterval = Integer.parseInt(
                config.getProperty("cs.events.metrics.interval.seconds", "300")
            );
            metricsScheduler.scheduleAtFixedRate(
                this::reportMetrics, 
                metricsInterval, 
                metricsInterval, 
                TimeUnit.SECONDS
            );
            
            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            logger.info("Event manager initialized with async={}, enabled_types={}", 
                asyncProcessing, enabledEventTypes);
            
        } catch (Exception e) {
            logger.error("Failed to initialize event manager", e);
        }
    }
    
    /**
     * Start event processing
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        
        if (asyncProcessing) {
            eventProcessorThread = new Thread(this::processEvents, "CSEvent-MainProcessor");
            eventProcessorThread.setDaemon(true);
            eventProcessorThread.start();
            logger.info("Started async event processing");
        } else {
            logger.info("Using synchronous event processing");
        }
    }
    
    /**
     * Stop event processing
     */
    public void stop() {
        running = false;
        
        if (eventProcessorThread != null) {
            eventProcessorThread.interrupt();
        }
        
        // Process remaining events
        processRemainingEvents();
        
        logger.info("Stopped event processing");
    }
    
    /**
     * Shutdown event manager
     */
    public void shutdown() {
        logger.info("Shutting down event manager...");
        
        stop();
        
        // Shutdown executors
        shutdownExecutor(eventExecutor, "Event Executor");
        shutdownExecutor(asyncEventExecutor, "Async Event Executor");
        shutdownExecutor(metricsScheduler, "Metrics Scheduler");
        
        // Cleanup listeners
        listeners.values().forEach(listener -> {
            try {
                listener.cleanup();
            } catch (Exception e) {
                logger.warn("Error cleaning up listener: {}", listener.getListenerName(), e);
            }
        });
        
        logger.info("Event manager shutdown complete");
    }
    
    /**
     * Add event listener (simple interface)
     */
    public void addEventListener(CSSimpleEventListener listener) {
        // Create adapter for simple listener
        CSEventListener adapter = new CSEventListener() {
            @Override
            public String getListenerName() {
                return "SimpleListener-" + listener.hashCode();
            }
            
            @Override
            public Set<String> getSupportedEventTypes() {
                return Set.of("*"); // Support all event types
            }
            
            @Override
            public Set<CSEvent.EventCategory> getSupportedCategories() {
                return Set.of(CSEvent.EventCategory.values()); // Support all categories
            }
            
            @Override
            public CSEvent.EventSeverity getMinimumSeverity() {
                return CSEvent.EventSeverity.DEBUG; // Support all severities
            }
            
            @Override
            public void handleEvent(CSEvent event) {
                // Convert event type string to EventType enum if possible
                try {
                    EventType eventType = EventType.valueOf(event.getEventType().toUpperCase().replace(".", "_"));
                    Object eventData = event.getContext().get("data");
                    if (eventData == null) {
                        eventData = event.getMetadata();
                    }
                    listener.onEvent(eventType, eventData);
                } catch (Exception e) {
                    // Ignore if cannot convert to EventType
                }
            }
        };
        registerListener(adapter);
    }
    
    /**
     * Add event listener (full interface)
     */
    public void addEventListener(CSEventListener listener) {
        registerListener(listener);
    }
    
    /**
     * Register event listener
     */
    public void registerListener(CSEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        String name = listener.getListenerName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Listener name cannot be null or empty");
        }
        
        try {
            listener.initialize();
            listeners.put(name, listener);
            logger.info("Registered event listener: {} (priority: {}, enabled: {})", 
                name, listener.getPriority(), listener.isEnabled());
        } catch (Exception e) {
            logger.error("Failed to register event listener: {}", name, e);
            throw new RuntimeException("Failed to register listener: " + name, e);
        }
    }
    
    /**
     * Unregister event listener
     */
    public void unregisterListener(String listenerName) {
        CSEventListener listener = listeners.remove(listenerName);
        if (listener != null) {
            try {
                listener.cleanup();
                logger.info("Unregistered event listener: {}", listenerName);
            } catch (Exception e) {
                logger.warn("Error cleaning up listener: {}", listenerName, e);
            }
        }
    }
    
    /**
     * Fire event
     */
    public void fireEvent(CSEvent event) {
        if (event == null) {
            return;
        }
        
        metrics.incrementEventsReceived();
        
        // Check if event type is enabled
        if (!isEventTypeEnabled(event.getEventType())) {
            metrics.incrementEventsFiltered();
            return;
        }
        
        // Apply filters
        if (!passesFilters(event)) {
            metrics.incrementEventsFiltered();
            return;
        }
        
        if (asyncProcessing) {
            // Queue for async processing
            if (!eventQueue.offer(event)) {
                metrics.incrementEventsDropped();
                logger.warn("Event queue full, dropping event: {}", event.getEventId());
            }
        } else {
            // Process synchronously
            processEvent(event);
        }
    }
    
    /**
     * Fire event asynchronously (always async regardless of config)
     */
    public CompletableFuture<Void> fireEventAsync(CSEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                processEvent(event);
            } catch (Exception e) {
                logger.error("Error processing async event: {}", event.getEventId(), e);
            }
        }, asyncEventExecutor);
    }
    
    /**
     * Process events from queue
     */
    private void processEvents() {
        while (running) {
            try {
                CSEvent event = eventQueue.poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    processEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in event processing loop", e);
            }
        }
    }
    
    /**
     * Process single event
     */
    private void processEvent(CSEvent event) {
        long startTime = System.nanoTime();
        
        try {
            // Get matching listeners sorted by priority
            List<CSEventListener> matchingListeners = listeners.values().stream()
                .filter(listener -> listener.isEnabled() && listener.shouldHandle(event))
                .sorted((l1, l2) -> Integer.compare(l2.getPriority(), l1.getPriority()))
                .collect(Collectors.toList());
            
            if (matchingListeners.isEmpty()) {
                metrics.incrementEventsWithNoListeners();
                return;
            }
            
            // Process with each listener
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (CSEventListener listener : matchingListeners) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        listener.handleEvent(event);
                        metrics.incrementEventsProcessed();
                    } catch (Exception e) {
                        metrics.incrementEventErrors();
                        try {
                            listener.handleError(event, e);
                        } catch (Exception errorHandlingError) {
                            logger.error("Error in error handler for listener: {}", 
                                listener.getListenerName(), errorHandlingError);
                        }
                    }
                }, eventExecutor);
                
                futures.add(future);
            }
            
            // Wait for all listeners to complete (with timeout)
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Event processing timeout for event: {}", event.getEventId());
                metrics.incrementEventTimeouts();
            }
            
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getEventId(), e);
            metrics.incrementEventErrors();
        } finally {
            long processingTime = System.nanoTime() - startTime;
            metrics.addProcessingTime(processingTime);
        }
    }
    
    /**
     * Process remaining events in queue
     */
    private void processRemainingEvents() {
        int processed = 0;
        CSEvent event;
        while ((event = eventQueue.poll()) != null) {
            processEvent(event);
            processed++;
        }
        
        if (processed > 0) {
            logger.info("Processed {} remaining events", processed);
        }
    }
    
    /**
     * Check if event type is enabled
     */
    private boolean isEventTypeEnabled(String eventType) {
        return enabledEventTypes.contains("*") || enabledEventTypes.contains(eventType);
    }
    
    /**
     * Check if event passes filters
     */
    private boolean passesFilters(CSEvent event) {
        // Add custom filtering logic here
        return true;
    }
    
    /**
     * Report metrics
     */
    private void reportMetrics() {
        try {
            CSEventMetrics.MetricsSnapshot snapshot = metrics.getSnapshot();
            
            logger.info("Event Metrics - Received: {}, Processed: {}, Filtered: {}, Dropped: {}, " +
                       "Errors: {}, NoListeners: {}, AvgProcessingTime: {}ms", 
                snapshot.getEventsReceived(),
                snapshot.getEventsProcessed(),
                snapshot.getEventsFiltered(),
                snapshot.getEventsDropped(),
                snapshot.getEventErrors(),
                snapshot.getEventsWithNoListeners(),
                snapshot.getAverageProcessingTimeMs()
            );
            
        } catch (Exception e) {
            logger.error("Error reporting event metrics", e);
        }
    }
    
    /**
     * Auto-discover and register listeners from classpath
     */
    public void autoDiscoverListeners(String packageName) {
        try {
            logger.info("Auto-discovering event listeners in package: {}", packageName);
            
            List<Class<?>> classes = getClassesInPackage(packageName);
            int registered = 0;
            
            for (Class<?> clazz : classes) {
                if (CSEventListener.class.isAssignableFrom(clazz) && 
                    !clazz.isInterface() && 
                    !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    
                    try {
                        CSEventListener listener = (CSEventListener) clazz.getDeclaredConstructor().newInstance();
                        registerListener(listener);
                        registered++;
                    } catch (Exception e) {
                        logger.warn("Failed to instantiate listener: {}", clazz.getName(), e);
                    }
                }
            }
            
            logger.info("Auto-discovered and registered {} event listeners", registered);
            
        } catch (Exception e) {
            logger.error("Error during listener auto-discovery", e);
        }
    }
    
    /**
     * Get classes in package
     */
    private List<Class<?>> getClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<java.net.URL> resources = classLoader.getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    classes.addAll(findClassesInDirectory(new java.io.File(resource.toURI()), packageName));
                } else if (resource.getProtocol().equals("jar")) {
                    classes.addAll(findClassesInJar(resource, packagePath));
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning package: {}", packageName, e);
        }
        
        return classes;
    }
    
    /**
     * Find classes in directory
     */
    private List<Class<?>> findClassesInDirectory(java.io.File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        logger.debug("Could not load class: {}", className);
                    }
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Find classes in JAR file
     */
    private List<Class<?>> findClassesInJar(java.net.URL jarUrl, String packagePath) {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))) {
                Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.startsWith(packagePath) && name.endsWith(".class")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            logger.debug("Could not load class: {}", className);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning JAR: {}", jarUrl, e);
        }
        
        return classes;
    }
    
    /**
     * Shutdown executor gracefully
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("{} did not terminate gracefully, forcing shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while shutting down {}", name);
            executor.shutdownNow();
        }
    }
    
    // Getters for testing and monitoring
    public int getQueueSize() {
        return eventQueue.size();
    }
    
    public int getListenerCount() {
        return listeners.size();
    }
    
    public CSEventMetrics getMetrics() {
        return metrics;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public Map<String, CSEventListener> getListeners() {
        return new HashMap<>(listeners);
    }
}