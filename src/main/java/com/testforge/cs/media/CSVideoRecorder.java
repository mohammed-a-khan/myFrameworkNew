package com.testforge.cs.media;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.context.CSThreadContext;
import com.testforge.cs.exceptions.CSMediaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-ready video recording system for test execution
 * Records screen activity during test execution with thread isolation
 * Uses native Java capabilities without third-party video libraries
 */
public class CSVideoRecorder {
    private static final Logger logger = LoggerFactory.getLogger(CSVideoRecorder.class);
    
    // Thread-safe recording management
    private static final Map<Long, VideoRecordingSession> activeRecordings = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService captureExecutor = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors(),
        r -> {
            Thread t = new Thread(r, "CS-VideoCapture-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        }
    );
    
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static CSConfigManager config;
    private static boolean recordingEnabled;
    private static int captureIntervalMs;
    private static String videoOutputDir;
    private static int maxVideoSizeMB;
    private static boolean recordOnFailureOnly;
    private static Rectangle captureArea;
    
    /**
     * Initialize video recording system
     */
    public static synchronized void initialize() {
        if (initialized.get()) {
            return;
        }
        
        try {
            config = CSConfigManager.getInstance();
            
            // Load configuration
            recordingEnabled = Boolean.parseBoolean(config.getProperty("video.recording.enabled", "false"));
            captureIntervalMs = Integer.parseInt(config.getProperty("video.capture.interval.ms", "500"));
            videoOutputDir = config.getProperty("video.output.directory", "target/videos");
            maxVideoSizeMB = Integer.parseInt(config.getProperty("video.max.size.mb", "100"));
            recordOnFailureOnly = Boolean.parseBoolean(config.getProperty("video.record.failure.only", "true"));
            
            // Setup capture area (default: full screen)
            String captureAreaConfig = config.getProperty("video.capture.area", "fullscreen");
            setupCaptureArea(captureAreaConfig);
            
            // Create output directory
            Path videoDir = Paths.get(videoOutputDir);
            if (!Files.exists(videoDir)) {
                Files.createDirectories(videoDir);
            }
            
            // Add shutdown hook to cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down video recording system...");
                stopAllRecordings();
                captureExecutor.shutdown();
                try {
                    if (!captureExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        captureExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    captureExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }));
            
            initialized.set(true);
            
            if (recordingEnabled) {
                logger.info("Video recording system initialized - Interval: {}ms, Output: {}, MaxSize: {}MB",
                    captureIntervalMs, videoOutputDir, maxVideoSizeMB);
            } else {
                logger.info("Video recording disabled by configuration");
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize video recording system", e);
            recordingEnabled = false;
        }
    }
    
    /**
     * Start video recording for current test
     */
    public static String startRecording() {
        return startRecording(null);
    }
    
    /**
     * Start video recording with custom name
     */
    public static String startRecording(String testName) {
        if (!recordingEnabled || !initialized.get()) {
            logger.debug("Video recording not enabled or not initialized");
            return null;
        }
        
        long threadId = Thread.currentThread().getId();
        
        // Check if already recording for this thread
        if (activeRecordings.containsKey(threadId)) {
            logger.warn("Video recording already active for thread: {}", threadId);
            return activeRecordings.get(threadId).getVideoFilePath();
        }
        
        try {
            // Generate video file name
            String fileName = generateVideoFileName(testName);
            String videoFilePath = Paths.get(videoOutputDir, fileName).toString();
            
            // Create recording session
            VideoRecordingSession session = new VideoRecordingSession(threadId, videoFilePath, testName);
            activeRecordings.put(threadId, session);
            
            // Start capture task
            session.startCapture();
            
            logger.info("Started video recording for thread {} -> {}", threadId, videoFilePath);
            return videoFilePath;
            
        } catch (Exception e) {
            logger.error("Failed to start video recording for thread: {}", threadId, e);
            throw new CSMediaException("Failed to start video recording", e);
        }
    }
    
    /**
     * Stop video recording for current test
     */
    public static String stopRecording() {
        return stopRecording(false);
    }
    
    /**
     * Stop video recording with option to keep on failure only
     */
    public static String stopRecording(boolean testFailed) {
        long threadId = Thread.currentThread().getId();
        VideoRecordingSession session = activeRecordings.remove(threadId);
        
        if (session == null) {
            logger.debug("No active video recording found for thread: {}", threadId);
            return null;
        }
        
        try {
            session.stopCapture();
            String videoFilePath = session.finalizeVideo();
            
            // Handle failure-only recording policy
            if (recordOnFailureOnly && !testFailed) {
                // Delete video if test passed and we only record failures
                try {
                    Files.deleteIfExists(Paths.get(videoFilePath));
                    logger.debug("Deleted video recording for passed test: {}", videoFilePath);
                    return null;
                } catch (Exception e) {
                    logger.warn("Failed to delete video file: {}", videoFilePath, e);
                }
            }
            
            logger.info("Stopped video recording for thread {} -> {} ({})",
                threadId, videoFilePath, testFailed ? "FAILED" : "PASSED");
            
            return videoFilePath;
            
        } catch (Exception e) {
            logger.error("Failed to stop video recording for thread: {}", threadId, e);
            return session.getVideoFilePath();
        }
    }
    
    /**
     * Stop all active recordings
     */
    public static void stopAllRecordings() {
        logger.info("Stopping all active video recordings...");
        
        for (Map.Entry<Long, VideoRecordingSession> entry : activeRecordings.entrySet()) {
            try {
                VideoRecordingSession session = entry.getValue();
                session.stopCapture();
                session.finalizeVideo();
                logger.info("Stopped recording for thread: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error stopping recording for thread: {}", entry.getKey(), e);
            }
        }
        
        activeRecordings.clear();
    }
    
    /**
     * Get active recording count
     */
    public static int getActiveRecordingCount() {
        return activeRecordings.size();
    }
    
    /**
     * Check if recording is active for current thread
     */
    public static boolean isRecordingActive() {
        return activeRecordings.containsKey(Thread.currentThread().getId());
    }
    
    /**
     * Setup capture area based on configuration
     */
    private static void setupCaptureArea(String captureAreaConfig) {
        try {
            if ("fullscreen".equalsIgnoreCase(captureAreaConfig)) {
                // Get primary screen dimensions
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice[] screens = ge.getScreenDevices();
                if (screens.length > 0) {
                    Rectangle bounds = screens[0].getDefaultConfiguration().getBounds();
                    captureArea = new Rectangle(0, 0, bounds.width, bounds.height);
                } else {
                    captureArea = new Rectangle(0, 0, 1920, 1080); // Fallback
                }
            } else {
                // Parse custom area: "x,y,width,height"
                String[] parts = captureAreaConfig.split(",");
                if (parts.length == 4) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int width = Integer.parseInt(parts[2].trim());
                    int height = Integer.parseInt(parts[3].trim());
                    captureArea = new Rectangle(x, y, width, height);
                } else {
                    throw new IllegalArgumentException("Invalid capture area format: " + captureAreaConfig);
                }
            }
            
            logger.info("Video capture area configured: {}x{} at ({},{})",
                captureArea.width, captureArea.height, captureArea.x, captureArea.y);
                
        } catch (Exception e) {
            logger.error("Failed to setup capture area, using default", e);
            captureArea = new Rectangle(0, 0, 1920, 1080);
        }
    }
    
    /**
     * Generate unique video file name
     */
    private static String generateVideoFileName(String testName) {
        StringBuilder fileName = new StringBuilder();
        
        // Add timestamp
        fileName.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        // Add thread ID for uniqueness in parallel execution
        fileName.append("_T").append(Thread.currentThread().getId());
        
        // Add test name if provided
        if (testName != null && !testName.trim().isEmpty()) {
            String cleanTestName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (cleanTestName.length() > 50) {
                cleanTestName = cleanTestName.substring(0, 50);
            }
            fileName.append("_").append(cleanTestName);
        }
        
        // Add from thread context if available
        try {
            var context = CSThreadContext.getCurrentContext();
            if (context.getCurrentTestMethod() != null) {
                String methodName = context.getCurrentTestMethod().replaceAll("[^a-zA-Z0-9._-]", "_");
                if (methodName.length() > 30) {
                    methodName = methodName.substring(0, 30);
                }
                fileName.append("_").append(methodName);
            }
        } catch (Exception e) {
            // Ignore context errors
        }
        
        fileName.append(".mjpeg");
        return fileName.toString();
    }
    
    /**
     * Video recording session for a single thread/test
     */
    private static class VideoRecordingSession {
        private final long threadId;
        private final String videoFilePath;
        private final String testName;
        private final LocalDateTime startTime;
        private final AtomicBoolean recording;
        private final AtomicInteger frameCount;
        private final AtomicLong totalFileSize;
        
        private Robot robot;
        private FileOutputStream videoOutput;
        private java.util.concurrent.ScheduledFuture<?> captureTask;
        
        public VideoRecordingSession(long threadId, String videoFilePath, String testName) {
            this.threadId = threadId;
            this.videoFilePath = videoFilePath;
            this.testName = testName;
            this.startTime = LocalDateTime.now();
            this.recording = new AtomicBoolean(false);
            this.frameCount = new AtomicInteger(0);
            this.totalFileSize = new AtomicLong(0);
            
            try {
                this.robot = new Robot();
                this.videoOutput = new FileOutputStream(videoFilePath);
                
                // Write MJPEG header
                writeMJPEGHeader();
                
            } catch (Exception e) {
                throw new CSMediaException("Failed to initialize video recording session", e);
            }
        }
        
        /**
         * Start capturing frames
         */
        public void startCapture() {
            if (recording.get()) {
                return;
            }
            
            recording.set(true);
            captureTask = captureExecutor.scheduleAtFixedRate(
                this::captureFrame,
                0,
                captureIntervalMs,
                TimeUnit.MILLISECONDS
            );
            
            logger.debug("Started frame capture for session: {}", videoFilePath);
        }
        
        /**
         * Stop capturing frames
         */
        public void stopCapture() {
            if (!recording.get()) {
                return;
            }
            
            recording.set(false);
            
            if (captureTask != null) {
                captureTask.cancel(false);
            }
            
            logger.debug("Stopped frame capture for session: {} ({} frames captured)",
                videoFilePath, frameCount.get());
        }
        
        /**
         * Capture a single frame
         */
        private void captureFrame() {
            if (!recording.get()) {
                return;
            }
            
            try {
                // Check file size limit
                if (totalFileSize.get() > maxVideoSizeMB * 1024 * 1024) {
                    logger.warn("Video file size limit reached for session: {}", videoFilePath);
                    stopCapture();
                    return;
                }
                
                // Capture screenshot
                BufferedImage screenshot = robot.createScreenCapture(captureArea);
                
                // Convert to JPEG
                ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(screenshot, "JPEG", jpegOutput);
                byte[] jpegData = jpegOutput.toByteArray();
                
                // Write MJPEG frame
                synchronized (videoOutput) {
                    writeMJPEGFrame(jpegData);
                }
                
                frameCount.incrementAndGet();
                totalFileSize.addAndGet(jpegData.length);
                
            } catch (Exception e) {
                if (recording.get()) {
                    logger.error("Error capturing frame for session: {}", videoFilePath, e);
                }
            }
        }
        
        /**
         * Write MJPEG header
         */
        private void writeMJPEGHeader() throws Exception {
            // Simple MJPEG header - just boundary marker
            String boundary = "--FRAME\r\n";
            videoOutput.write(boundary.getBytes());
        }
        
        /**
         * Write MJPEG frame
         */
        private void writeMJPEGFrame(byte[] jpegData) throws Exception {
            String frameHeader = String.format(
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: %d\r\n\r\n",
                jpegData.length
            );
            
            videoOutput.write(frameHeader.getBytes());
            videoOutput.write(jpegData);
            videoOutput.write("\r\n--FRAME\r\n".getBytes());
            videoOutput.flush();
        }
        
        /**
         * Finalize video file
         */
        public String finalizeVideo() {
            try {
                if (videoOutput != null) {
                    synchronized (videoOutput) {
                        // Write final boundary
                        videoOutput.write("--FRAME--\r\n".getBytes());
                        videoOutput.close();
                    }
                }
                
                LocalDateTime endTime = LocalDateTime.now();
                long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
                
                logger.info("Finalized video recording: {} ({} frames, {} seconds, {} bytes)",
                    videoFilePath, frameCount.get(), durationSeconds, totalFileSize.get());
                
                return videoFilePath;
                
            } catch (Exception e) {
                logger.error("Error finalizing video: {}", videoFilePath, e);
                return videoFilePath;
            }
        }
        
        // Getters
        public long getThreadId() { return threadId; }
        public String getVideoFilePath() { return videoFilePath; }
        public String getTestName() { return testName; }
        public LocalDateTime getStartTime() { return startTime; }
        public boolean isRecording() { return recording.get(); }
        public int getFrameCount() { return frameCount.get(); }
        public long getTotalFileSize() { return totalFileSize.get(); }
    }
    
    /**
     * Get recording statistics
     */
    public static class RecordingStats {
        private final int activeRecordings;
        private final long totalFramesCaptured;
        private final long totalFileSizeBytes;
        
        public RecordingStats() {
            this.activeRecordings = CSVideoRecorder.activeRecordings.size();
            this.totalFramesCaptured = CSVideoRecorder.activeRecordings.values().stream()
                .mapToLong(session -> session.getFrameCount())
                .sum();
            this.totalFileSizeBytes = CSVideoRecorder.activeRecordings.values().stream()
                .mapToLong(session -> session.getTotalFileSize())
                .sum();
        }
        
        public int getActiveRecordings() { return activeRecordings; }
        public long getTotalFramesCaptured() { return totalFramesCaptured; }
        public long getTotalFileSizeBytes() { return totalFileSizeBytes; }
        public double getTotalFileSizeMB() { return totalFileSizeBytes / (1024.0 * 1024.0); }
    }
    
    /**
     * Get current recording statistics
     */
    public static RecordingStats getRecordingStats() {
        return new RecordingStats();
    }
}