package com.testforge.cs.visualization;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.context.CSThreadContext;
import com.testforge.cs.exceptions.CSVisualizationException;
import com.testforge.cs.reporting.CSTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Production-ready timeline visualization for parallel test execution
 * Generates interactive HTML timelines showing test execution patterns, thread utilization, and performance metrics
 */
public class CSTimelineVisualization {
    private static final Logger logger = LoggerFactory.getLogger(CSTimelineVisualization.class);
    
    private static volatile CSTimelineVisualization instance;
    private static final Object instanceLock = new Object();
    
    // Timeline data collection
    private final Map<Long, List<TimelineEvent>> threadTimelines = new ConcurrentHashMap<>();
    private final Map<String, ExecutionPhase> executionPhases = new ConcurrentHashMap<>();
    private final AtomicLong eventIdGenerator = new AtomicLong(0);
    
    // Configuration
    private CSConfigManager config;
    private String outputDirectory;
    private boolean visualizationEnabled;
    private boolean realTimeVisualization;
    private int maxTimelineEvents;
    private String timelineTheme;
    
    /**
     * Get singleton instance
     */
    public static CSTimelineVisualization getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new CSTimelineVisualization();
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private CSTimelineVisualization() {
        initialize();
    }
    
    /**
     * Initialize timeline visualization
     */
    private void initialize() {
        try {
            config = CSConfigManager.getInstance();
            
            // Load configuration
            visualizationEnabled = Boolean.parseBoolean(config.getProperty("timeline.visualization.enabled", "true"));
            realTimeVisualization = Boolean.parseBoolean(config.getProperty("timeline.real.time.enabled", "false"));
            outputDirectory = config.getProperty("timeline.output.directory", "target/timeline");
            maxTimelineEvents = Integer.parseInt(config.getProperty("timeline.max.events", "10000"));
            timelineTheme = config.getProperty("timeline.theme", "modern");
            
            // Create output directory
            Path timelineDir = Paths.get(outputDirectory);
            if (!Files.exists(timelineDir)) {
                Files.createDirectories(timelineDir);
            }
            
            logger.info("Timeline visualization initialized - Enabled: {}, Real-time: {}, Output: {}", 
                visualizationEnabled, realTimeVisualization, outputDirectory);
            
        } catch (Exception e) {
            logger.error("Failed to initialize timeline visualization", e);
            visualizationEnabled = false;
        }
    }
    
    /**
     * Record test start event
     */
    public void recordTestStart(String testName, String className, String methodName) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            EventType.TEST_START,
            System.currentTimeMillis(),
            threadId,
            threadName,
            testName,
            className,
            methodName
        );
        
        recordEvent(event);
        logger.debug("Recorded test start: {} on thread {}", testName, threadId);
    }
    
    /**
     * Record test end event
     */
    public void recordTestEnd(String testName, String className, String methodName, boolean passed, long duration) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            passed ? EventType.TEST_PASS : EventType.TEST_FAIL,
            System.currentTimeMillis(),
            threadId,
            threadName,
            testName,
            className,
            methodName
        );
        
        event.setDuration(duration);
        event.addMetadata("status", passed ? "PASSED" : "FAILED");
        
        recordEvent(event);
        logger.debug("Recorded test end: {} ({}) on thread {} - {}ms", 
            testName, passed ? "PASSED" : "FAILED", threadId, duration);
    }
    
    /**
     * Record suite execution phase
     */
    public void recordSuitePhase(String suiteName, String phase, long startTime, long endTime) {
        if (!visualizationEnabled) return;
        
        ExecutionPhase suitePhase = new ExecutionPhase(
            suiteName + "_" + phase,
            suiteName,
            phase,
            startTime,
            endTime
        );
        
        executionPhases.put(suitePhase.getId(), suitePhase);
        logger.debug("Recorded suite phase: {} - {} ({}ms)", suiteName, phase, endTime - startTime);
    }
    
    /**
     * Record custom timeline event
     */
    public void recordCustomEvent(String eventName, String category, Map<String, Object> metadata) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            EventType.CUSTOM,
            System.currentTimeMillis(),
            threadId,
            threadName,
            eventName,
            category,
            null
        );
        
        if (metadata != null) {
            metadata.forEach(event::addMetadata);
        }
        
        recordEvent(event);
        logger.debug("Recorded custom event: {} in category {} on thread {}", eventName, category, threadId);
    }
    
    /**
     * Record WebDriver action
     */
    public void recordWebDriverAction(String action, String element, long duration) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            EventType.WEBDRIVER_ACTION,
            System.currentTimeMillis(),
            threadId,
            threadName,
            action,
            "WebDriver",
            null
        );
        
        event.setDuration(duration);
        event.addMetadata("element", element);
        event.addMetadata("action", action);
        
        recordEvent(event);
    }
    
    /**
     * Record API call
     */
    public void recordApiCall(String method, String url, int responseCode, long duration) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            EventType.API_CALL,
            System.currentTimeMillis(),
            threadId,
            threadName,
            method + " " + url,
            "API",
            null
        );
        
        event.setDuration(duration);
        event.addMetadata("method", method);
        event.addMetadata("url", url);
        event.addMetadata("responseCode", responseCode);
        event.addMetadata("status", responseCode < 400 ? "SUCCESS" : "ERROR");
        
        recordEvent(event);
    }
    
    /**
     * Record database operation
     */
    public void recordDatabaseOperation(String operation, String query, long duration, int rowsAffected) {
        if (!visualizationEnabled) return;
        
        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();
        
        TimelineEvent event = new TimelineEvent(
            eventIdGenerator.incrementAndGet(),
            EventType.DATABASE_OPERATION,
            System.currentTimeMillis(),
            threadId,
            threadName,
            operation,
            "Database",
            null
        );
        
        event.setDuration(duration);
        event.addMetadata("operation", operation);
        event.addMetadata("query", query.length() > 100 ? query.substring(0, 100) + "..." : query);
        event.addMetadata("rowsAffected", rowsAffected);
        
        recordEvent(event);
    }
    
    /**
     * Record event to timeline
     */
    private void recordEvent(TimelineEvent event) {
        threadTimelines.computeIfAbsent(event.getThreadId(), k -> new ArrayList<>()).add(event);
        
        // Limit timeline events to prevent memory issues
        List<TimelineEvent> timeline = threadTimelines.get(event.getThreadId());
        if (timeline.size() > maxTimelineEvents) {
            timeline.remove(0); // Remove oldest event
        }
        
        // Real-time visualization update
        if (realTimeVisualization) {
            updateRealTimeVisualization();
        }
    }
    
    /**
     * Generate complete timeline visualization
     */
    public String generateTimeline() {
        return generateTimeline("execution_timeline.html");
    }
    
    /**
     * Generate timeline with custom filename
     */
    public String generateTimeline(String filename) {
        if (!visualizationEnabled) {
            logger.warn("Timeline visualization is disabled");
            return null;
        }
        
        try {
            String htmlContent = generateTimelineHTML();
            String outputPath = Paths.get(outputDirectory, filename).toString();
            
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(htmlContent);
            }
            
            logger.info("Generated timeline visualization: {}", outputPath);
            return outputPath;
            
        } catch (Exception e) {
            logger.error("Failed to generate timeline visualization", e);
            throw new CSVisualizationException("Failed to generate timeline", e);
        }
    }
    
    /**
     * Generate timeline HTML
     */
    private String generateTimelineHTML() {
        StringBuilder html = new StringBuilder();
        
        // HTML header with CSS and JavaScript
        html.append(generateHTMLHeader());
        
        // Timeline data
        html.append(generateTimelineData());
        
        // Timeline visualization script
        html.append(generateTimelineScript());
        
        // HTML footer
        html.append(generateHTMLFooter());
        
        return html.toString();
    }
    
    /**
     * Generate HTML header
     */
    private String generateHTMLHeader() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Test Execution Timeline</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #f5f7fa; }
                    .header { background: #2c3e50; color: white; padding: 20px; text-align: center; }
                    .container { max-width: 1400px; margin: 0 auto; padding: 20px; }
                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }
                    .stat-card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .stat-title { font-size: 14px; color: #7f8c8d; margin-bottom: 5px; }
                    .stat-value { font-size: 24px; font-weight: bold; color: #2c3e50; }
                    .timeline-container { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .timeline-controls { margin-bottom: 20px; display: flex; gap: 10px; flex-wrap: wrap; }
                    .control-group { display: flex; align-items: center; gap: 5px; }
                    .control-group label { font-size: 14px; color: #7f8c8d; }
                    .control-group select, .control-group input { padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px; }
                    #timelineCanvas { border: 1px solid #ddd; width: 100%; height: 600px; }
                    .legend { display: flex; flex-wrap: wrap; gap: 15px; margin-top: 15px; }
                    .legend-item { display: flex; align-items: center; gap: 5px; }
                    .legend-color { width: 15px; height: 15px; border-radius: 3px; }
                    .thread-info { margin-top: 20px; }
                    .thread-card { background: #ecf0f1; border-radius: 6px; padding: 15px; margin-bottom: 10px; }
                    .event-details { margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 6px; display: none; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Test Execution Timeline</h1>
                    <p>Generated on """ + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + """
                </div>
                <div class="container">
            """;
    }
    
    /**
     * Generate timeline data as JavaScript
     */
    private String generateTimelineData() {
        StringBuilder data = new StringBuilder();
        data.append("<script>\n");
        
        // Thread timeline data
        data.append("const timelineData = {\n");
        data.append("  threads: [\n");
        
        for (Map.Entry<Long, List<TimelineEvent>> entry : threadTimelines.entrySet()) {
            Long threadId = entry.getKey();
            List<TimelineEvent> events = entry.getValue();
            
            data.append("    {\n");
            data.append("      id: ").append(threadId).append(",\n");
            data.append(String.format("      name: \"%s\",\n", 
                events.isEmpty() ? "Thread-" + threadId : events.get(0).getThreadName()));
            data.append("      events: [\n");
            
            for (TimelineEvent event : events) {
                data.append("        {\n");
                data.append("          id: ").append(event.getId()).append(",\n");
                data.append(String.format("          type: \"%s\",\n", event.getType()));
                data.append("          timestamp: ").append(event.getTimestamp()).append(",\n");
                data.append(String.format("          name: \"%s\",\n", escapeJson(event.getName())));
                data.append(String.format("          className: \"%s\",\n", escapeJson(event.getClassName())));
                data.append(String.format("          methodName: \"%s\",\n", escapeJson(event.getMethodName())));
                data.append("          duration: ").append(event.getDuration()).append(",\n");
                data.append("          metadata: ").append(formatMetadataAsJson(event.getMetadata())).append("\n");
                data.append("        },\n");
            }
            
            data.append("      ]\n");
            data.append("    },\n");
        }
        
        data.append("  ],\n");
        
        // Execution phases
        data.append("  phases: [\n");
        for (ExecutionPhase phase : executionPhases.values()) {
            data.append("    {\n");
            data.append(String.format("      id: \"%s\",\n", phase.getId()));
            data.append(String.format("      name: \"%s\",\n", phase.getName()));
            data.append(String.format("      phase: \"%s\",\n", phase.getPhase()));
            data.append("      startTime: ").append(phase.getStartTime()).append(",\n");
            data.append("      endTime: ").append(phase.getEndTime()).append("\n");
            data.append("    },\n");
        }
        data.append("  ]\n");
        
        data.append("};\n");
        data.append("</script>\n");
        
        return data.toString();
    }
    
    /**
     * Generate timeline visualization script
     */
    private String generateTimelineScript() {
        return """
            <div class="stats-grid" id="statsGrid">
                <!-- Stats will be populated by JavaScript -->
            </div>
            
            <div class="timeline-container">
                <div class="timeline-controls">
                    <div class="control-group">
                        <label>View:</label>
                        <select id="viewMode">
                            <option value="all">All Threads</option>
                            <option value="active">Active Only</option>
                            <option value="failed">Failed Tests</option>
                        </select>
                    </div>
                    <div class="control-group">
                        <label>Zoom:</label>
                        <select id="zoomLevel">
                            <option value="1">1x</option>
                            <option value="2">2x</option>
                            <option value="4">4x</option>
                            <option value="8">8x</option>
                        </select>
                    </div>
                    <div class="control-group">
                        <label>Filter:</label>
                        <input type="text" id="eventFilter" placeholder="Filter events...">
                    </div>
                </div>
                
                <canvas id="timelineCanvas"></canvas>
                
                <div class="legend">
                    <div class="legend-item">
                        <div class="legend-color" style="background: #3498db;"></div>
                        <span>Test Execution</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #2ecc71;"></div>
                        <span>Passed</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #e74c3c;"></div>
                        <span>Failed</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #f39c12;"></div>
                        <span>WebDriver Action</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #9b59b6;"></div>
                        <span>API Call</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #1abc9c;"></div>
                        <span>Database</span>
                    </div>
                </div>
            </div>
            
            <div class="event-details" id="eventDetails">
                <!-- Event details will be shown here -->
            </div>
            
            <script>
                // Timeline visualization implementation
                class TimelineVisualizer {
                    constructor(canvasId, data) {
                        this.canvas = document.getElementById(canvasId);
                        this.ctx = this.canvas.getContext('2d');
                        this.data = data;
                        this.zoomLevel = 1;
                        this.viewMode = 'all';
                        this.filter = '';
                        
                        this.colors = {
                            TEST_START: '#3498db',
                            TEST_PASS: '#2ecc71',
                            TEST_FAIL: '#e74c3c',
                            WEBDRIVER_ACTION: '#f39c12',
                            API_CALL: '#9b59b6',
                            DATABASE_OPERATION: '#1abc9c',
                            CUSTOM: '#95a5a6'
                        };
                        
                        this.init();
                    }
                    
                    init() {
                        this.resizeCanvas();
                        this.bindEvents();
                        this.generateStats();
                        this.render();
                    }
                    
                    resizeCanvas() {
                        const rect = this.canvas.getBoundingClientRect();
                        this.canvas.width = rect.width * window.devicePixelRatio;
                        this.canvas.height = 600 * window.devicePixelRatio;
                        this.ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
                    }
                    
                    bindEvents() {
                        document.getElementById('viewMode').addEventListener('change', (e) => {
                            this.viewMode = e.target.value;
                            this.render();
                        });
                        
                        document.getElementById('zoomLevel').addEventListener('change', (e) => {
                            this.zoomLevel = parseInt(e.target.value);
                            this.render();
                        });
                        
                        document.getElementById('eventFilter').addEventListener('input', (e) => {
                            this.filter = e.target.value.toLowerCase();
                            this.render();
                        });
                        
                        this.canvas.addEventListener('click', (e) => {
                            this.handleCanvasClick(e);
                        });
                    }
                    
                    generateStats() {
                        const stats = this.calculateStats();
                        const statsGrid = document.getElementById('statsGrid');
                        
                        statsGrid.innerHTML = `
                            <div class="stat-card">
                                <div class="stat-title">Total Tests</div>
                                <div class="stat-value">${stats.totalTests}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-title">Passed</div>
                                <div class="stat-value" style="color: #2ecc71;">${stats.passedTests}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-title">Failed</div>
                                <div class="stat-value" style="color: #e74c3c;">${stats.failedTests}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-title">Active Threads</div>
                                <div class="stat-value">${stats.activeThreads}</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-title">Avg Duration</div>
                                <div class="stat-value">${stats.avgDuration}ms</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-title">Total Duration</div>
                                <div class="stat-value">${stats.totalDuration}ms</div>
                            </div>
                        `;
                    }
                    
                    calculateStats() {
                        let totalTests = 0, passedTests = 0, failedTests = 0;
                        let totalDuration = 0, testCount = 0;
                        
                        this.data.threads.forEach(thread => {
                            thread.events.forEach(event => {
                                if (event.type === 'TEST_PASS') {
                                    totalTests++;
                                    passedTests++;
                                    totalDuration += event.duration;
                                    testCount++;
                                } else if (event.type === 'TEST_FAIL') {
                                    totalTests++;
                                    failedTests++;
                                    totalDuration += event.duration;
                                    testCount++;
                                }
                            });
                        });
                        
                        return {
                            totalTests,
                            passedTests,
                            failedTests,
                            activeThreads: this.data.threads.length,
                            avgDuration: testCount > 0 ? Math.round(totalDuration / testCount) : 0,
                            totalDuration: Math.round(totalDuration)
                        };
                    }
                    
                    render() {
                        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
                        
                        const filteredData = this.getFilteredData();
                        if (filteredData.threads.length === 0) return;
                        
                        const timeRange = this.getTimeRange(filteredData);
                        this.drawTimeline(filteredData, timeRange);
                    }
                    
                    getFilteredData() {
                        const filtered = { threads: [], phases: this.data.phases };
                        
                        this.data.threads.forEach(thread => {
                            let includeThread = true;
                            
                            if (this.viewMode === 'active') {
                                includeThread = thread.events.some(e => 
                                    e.type === 'TEST_PASS' || e.type === 'TEST_FAIL'
                                );
                            } else if (this.viewMode === 'failed') {
                                includeThread = thread.events.some(e => e.type === 'TEST_FAIL');
                            }
                            
                            if (includeThread) {
                                const filteredEvents = thread.events.filter(event => 
                                    this.filter === '' || 
                                    event.name.toLowerCase().includes(this.filter) ||
                                    event.className.toLowerCase().includes(this.filter)
                                );
                                
                                if (filteredEvents.length > 0) {
                                    filtered.threads.push({
                                        ...thread,
                                        events: filteredEvents
                                    });
                                }
                            }
                        });
                        
                        return filtered;
                    }
                    
                    getTimeRange(data) {
                        let minTime = Infinity, maxTime = -Infinity;
                        
                        data.threads.forEach(thread => {
                            thread.events.forEach(event => {
                                minTime = Math.min(minTime, event.timestamp);
                                maxTime = Math.max(maxTime, event.timestamp + event.duration);
                            });
                        });
                        
                        return { min: minTime, max: maxTime, range: maxTime - minTime };
                    }
                    
                    drawTimeline(data, timeRange) {
                        const canvas = this.canvas;
                        const ctx = this.ctx;
                        const threadHeight = 40;
                        const threadSpacing = 50;
                        const leftMargin = 150;
                        const topMargin = 30;
                        const timelineWidth = canvas.width / window.devicePixelRatio - leftMargin - 50;
                        
                        // Draw thread labels and lanes
                        data.threads.forEach((thread, index) => {
                            const y = topMargin + index * threadSpacing;
                            
                            // Thread label
                            ctx.fillStyle = '#2c3e50';
                            ctx.font = '12px Arial';
                            ctx.textAlign = 'right';
                            ctx.fillText(`Thread ${thread.id}`, leftMargin - 10, y + 20);
                            
                            // Thread lane
                            ctx.fillStyle = '#ecf0f1';
                            ctx.fillRect(leftMargin, y, timelineWidth, threadHeight);
                            
                            // Draw events
                            thread.events.forEach(event => {
                                const startX = leftMargin + ((event.timestamp - timeRange.min) / timeRange.range) * timelineWidth;
                                const width = Math.max(2, (event.duration / timeRange.range) * timelineWidth * this.zoomLevel);
                                
                                ctx.fillStyle = this.colors[event.type] || this.colors.CUSTOM;
                                ctx.fillRect(startX, y + 5, width, threadHeight - 10);
                                
                                // Event label for significant events
                                if (width > 30 && (event.type === 'TEST_PASS' || event.type === 'TEST_FAIL')) {
                                    ctx.fillStyle = 'white';
                                    ctx.font = '10px Arial';
                                    ctx.textAlign = 'left';
                                    ctx.fillText(event.name.substring(0, 10), startX + 2, y + 18);
                                }
                            });
                        });
                        
                        // Draw time axis
                        this.drawTimeAxis(timeRange, leftMargin, topMargin + data.threads.length * threadSpacing + 20, timelineWidth);
                    }
                    
                    drawTimeAxis(timeRange, x, y, width) {
                        const ctx = this.ctx;
                        const tickCount = 10;
                        const tickSpacing = width / tickCount;
                        
                        ctx.strokeStyle = '#7f8c8d';
                        ctx.lineWidth = 1;
                        
                        // Axis line
                        ctx.beginPath();
                        ctx.moveTo(x, y);
                        ctx.lineTo(x + width, y);
                        ctx.stroke();
                        
                        // Ticks and labels
                        for (let i = 0; i <= tickCount; i++) {
                            const tickX = x + i * tickSpacing;
                            const time = timeRange.min + (i / tickCount) * timeRange.range;
                            const date = new Date(time);
                            
                            ctx.beginPath();
                            ctx.moveTo(tickX, y);
                            ctx.lineTo(tickX, y + 5);
                            ctx.stroke();
                            
                            ctx.fillStyle = '#7f8c8d';
                            ctx.font = '10px Arial';
                            ctx.textAlign = 'center';
                            ctx.fillText(date.toLocaleTimeString(), tickX, y + 18);
                        }
                    }
                    
                    handleCanvasClick(e) {
                        // Implementation for event details on click
                        const rect = this.canvas.getBoundingClientRect();
                        const x = e.clientX - rect.left;
                        const y = e.clientY - rect.top;
                        
                        // Find clicked event and show details
                        // This would be implemented based on the coordinate mapping
                    }
                }
                
                // Initialize timeline when page loads
                document.addEventListener('DOMContentLoaded', function() {
                    if (typeof timelineData !== 'undefined') {
                        new TimelineVisualizer('timelineCanvas', timelineData);
                    }
                });
            </script>
            """;
    }
    
    /**
     * Generate HTML footer
     */
    private String generateHTMLFooter() {
        return """
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * Update real-time visualization
     */
    private void updateRealTimeVisualization() {
        // Implementation for real-time updates would go here
        // This could involve WebSocket connections or periodic file updates
        logger.debug("Real-time visualization update triggered");
    }
    
    /**
     * Clear all timeline data
     */
    public void clearTimeline() {
        threadTimelines.clear();
        executionPhases.clear();
        eventIdGenerator.set(0);
        logger.info("Timeline data cleared");
    }
    
    /**
     * Get timeline statistics
     */
    public TimelineStatistics getStatistics() {
        return new TimelineStatistics(threadTimelines, executionPhases);
    }
    
    /**
     * Utility methods
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    private String formatMetadataAsJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Timeline event types
     */
    public enum EventType {
        TEST_START,
        TEST_PASS,
        TEST_FAIL,
        WEBDRIVER_ACTION,
        API_CALL,
        DATABASE_OPERATION,
        CUSTOM
    }
    
    /**
     * Timeline event class
     */
    public static class TimelineEvent {
        private final long id;
        private final EventType type;
        private final long timestamp;
        private final long threadId;
        private final String threadName;
        private final String name;
        private final String className;
        private final String methodName;
        private long duration;
        private final Map<String, Object> metadata;
        
        public TimelineEvent(long id, EventType type, long timestamp, long threadId, String threadName,
                           String name, String className, String methodName) {
            this.id = id;
            this.type = type;
            this.timestamp = timestamp;
            this.threadId = threadId;
            this.threadName = threadName;
            this.name = name != null ? name : "";
            this.className = className != null ? className : "";
            this.methodName = methodName != null ? methodName : "";
            this.duration = 0;
            this.metadata = new HashMap<>();
        }
        
        // Getters
        public long getId() { return id; }
        public EventType getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public long getThreadId() { return threadId; }
        public String getThreadName() { return threadName; }
        public String getName() { return name; }
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public long getDuration() { return duration; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        // Setters
        public void setDuration(long duration) { this.duration = duration; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }
    
    /**
     * Execution phase class
     */
    public static class ExecutionPhase {
        private final String id;
        private final String name;
        private final String phase;
        private final long startTime;
        private final long endTime;
        
        public ExecutionPhase(String id, String name, String phase, long startTime, long endTime) {
            this.id = id;
            this.name = name;
            this.phase = phase;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhase() { return phase; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDuration() { return endTime - startTime; }
    }
    
    /**
     * Timeline statistics class
     */
    public static class TimelineStatistics {
        private final int totalThreads;
        private final int totalEvents;
        private final long totalDuration;
        private final int testsPassed;
        private final int testsFailed;
        private final Map<EventType, Integer> eventTypeCounts;
        
        public TimelineStatistics(Map<Long, List<TimelineEvent>> threadTimelines, 
                                Map<String, ExecutionPhase> executionPhases) {
            this.totalThreads = threadTimelines.size();
            this.eventTypeCounts = new HashMap<>();
            
            int events = 0, passed = 0, failed = 0;
            long duration = 0;
            
            for (List<TimelineEvent> timeline : threadTimelines.values()) {
                events += timeline.size();
                
                for (TimelineEvent event : timeline) {
                    eventTypeCounts.merge(event.getType(), 1, Integer::sum);
                    duration += event.getDuration();
                    
                    if (event.getType() == EventType.TEST_PASS) passed++;
                    else if (event.getType() == EventType.TEST_FAIL) failed++;
                }
            }
            
            this.totalEvents = events;
            this.totalDuration = duration;
            this.testsPassed = passed;
            this.testsFailed = failed;
        }
        
        // Getters
        public int getTotalThreads() { return totalThreads; }
        public int getTotalEvents() { return totalEvents; }
        public long getTotalDuration() { return totalDuration; }
        public int getTestsPassed() { return testsPassed; }
        public int getTestsFailed() { return testsFailed; }
        public int getTotalTests() { return testsPassed + testsFailed; }
        public double getPassRate() { 
            int total = getTotalTests();
            return total > 0 ? (double) testsPassed / total * 100 : 0; 
        }
        public Map<EventType, Integer> getEventTypeCounts() { return eventTypeCounts; }
    }
}