package com.testforge.cs.screenshot;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.driver.CSDriver;
import com.testforge.cs.reporting.CSReportManager;
import com.testforge.cs.utils.CSFileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Production-ready screenshot utility
 */
public class CSScreenshotUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSScreenshotUtils.class);
    
    private static final CSConfigManager config = CSConfigManager.getInstance();
    private static final CSReportManager reportManager = CSReportManager.getInstance();
    
    private static final String SCREENSHOT_DIR = config.getString("cs.screenshot.dir", "target/screenshots");
    private static final String SCREENSHOT_FORMAT = config.getString("cs.screenshot.format", "png");
    private static final float SCREENSHOT_QUALITY = config.getFloat("cs.screenshot.quality", 0.9f);
    private static final boolean HIGHLIGHT_ELEMENT = config.getBoolean("cs.screenshot.highlight.element", true);
    
    /**
     * Capture screenshot of entire page
     */
    public static byte[] captureScreenshot(WebDriver driver) {
        try {
            if (driver == null) {
                logger.warn("WebDriver is null, creating placeholder screenshot");
                return createPlaceholderScreenshot("WebDriver not initialized");
            }
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            return screenshot.getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            logger.error("Failed to capture screenshot", e);
            return createPlaceholderScreenshot("Screenshot capture failed: " + e.getMessage());
        }
    }
    
    /**
     * Capture screenshot of entire page (CSDriver)
     */
    public static byte[] captureScreenshot(CSDriver driver) {
        return driver.takeScreenshot();
    }
    
    /**
     * Capture and save screenshot
     */
    public static String captureAndSave(WebDriver driver, String screenshotName) {
        try {
            byte[] screenshotData = captureScreenshot(driver);
            return saveScreenshot(screenshotData, screenshotName);
        } catch (Exception e) {
            logger.error("Failed to capture and save screenshot", e);
            return null;
        }
    }
    
    /**
     * Capture and attach screenshot to report
     */
    public static void captureAndAttach(WebDriver driver, String screenshotName) {
        try {
            byte[] screenshotData = captureScreenshot(driver);
            String path = saveScreenshot(screenshotData, screenshotName);
            reportManager.attachScreenshot(screenshotData, screenshotName);
            logger.info("Screenshot attached to report: {}", path);
        } catch (Exception e) {
            logger.error("Failed to capture and attach screenshot", e);
        }
    }
    
    /**
     * Capture and attach screenshot to report (CSDriver)
     */
    public static void captureAndAttach(CSDriver driver, String screenshotName) {
        try {
            byte[] screenshotData = driver.takeScreenshot();
            String path = saveScreenshot(screenshotData, screenshotName);
            reportManager.attachScreenshot(screenshotData, screenshotName);
            logger.info("Screenshot attached to report: {}", path);
        } catch (Exception e) {
            logger.error("Failed to capture and attach screenshot", e);
        }
    }
    
    /**
     * Capture screenshot of specific element
     */
    public static byte[] captureElement(WebDriver driver, WebElement element) {
        try {
            // Get full page screenshot
            byte[] fullScreenshot = captureScreenshot(driver);
            
            // Get element location and size
            org.openqa.selenium.Point location = element.getLocation();
            org.openqa.selenium.Dimension size = element.getSize();
            
            // Crop to element
            return cropImage(fullScreenshot, location.x, location.y, size.width, size.height);
        } catch (Exception e) {
            logger.error("Failed to capture element screenshot", e);
            return new byte[0];
        }
    }
    
    /**
     * Capture viewport screenshot (visible area only)
     */
    public static byte[] captureViewport(WebDriver driver) {
        try {
            // Execute JavaScript to get viewport dimensions
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long viewportWidth = (Long) js.executeScript("return window.innerWidth");
            Long viewportHeight = (Long) js.executeScript("return window.innerHeight");
            
            byte[] fullScreenshot = captureScreenshot(driver);
            return cropImage(fullScreenshot, 0, 0, viewportWidth.intValue(), viewportHeight.intValue());
        } catch (Exception e) {
            logger.error("Failed to capture viewport screenshot", e);
            return captureScreenshot(driver);
        }
    }
    
    /**
     * Capture full page screenshot (including scrollable area)
     */
    public static byte[] captureFullPage(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Get page dimensions
            Long pageHeight = (Long) js.executeScript("return document.body.scrollHeight");
            Long viewportHeight = (Long) js.executeScript("return window.innerHeight");
            Long viewportWidth = (Long) js.executeScript("return window.innerWidth");
            
            // Calculate number of screenshots needed
            int screenshotCount = (int) Math.ceil(pageHeight.doubleValue() / viewportHeight.doubleValue());
            
            BufferedImage fullImage = new BufferedImage(
                viewportWidth.intValue(),
                pageHeight.intValue(),
                BufferedImage.TYPE_INT_RGB
            );
            
            Graphics2D graphics = fullImage.createGraphics();
            
            // Capture screenshots while scrolling
            for (int i = 0; i < screenshotCount; i++) {
                // Scroll to position
                int scrollY = i * viewportHeight.intValue();
                js.executeScript("window.scrollTo(0, " + scrollY + ")");
                Thread.sleep(200); // Wait for scroll
                
                // Capture screenshot
                byte[] screenshotData = captureScreenshot(driver);
                BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(screenshotData));
                
                // Draw on full image
                graphics.drawImage(screenshot, 0, scrollY, null);
            }
            
            graphics.dispose();
            
            // Scroll back to top
            js.executeScript("window.scrollTo(0, 0)");
            
            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(fullImage, SCREENSHOT_FORMAT, baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to capture full page screenshot", e);
            return captureScreenshot(driver);
        }
    }
    
    /**
     * Compare two screenshots
     */
    public static double compareScreenshots(byte[] screenshot1, byte[] screenshot2) {
        try {
            BufferedImage img1 = ImageIO.read(new ByteArrayInputStream(screenshot1));
            BufferedImage img2 = ImageIO.read(new ByteArrayInputStream(screenshot2));
            
            if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
                return 0.0; // Different dimensions
            }
            
            long difference = 0;
            for (int y = 0; y < img1.getHeight(); y++) {
                for (int x = 0; x < img1.getWidth(); x++) {
                    int rgb1 = img1.getRGB(x, y);
                    int rgb2 = img2.getRGB(x, y);
                    
                    int r1 = (rgb1 >> 16) & 0xFF;
                    int g1 = (rgb1 >> 8) & 0xFF;
                    int b1 = rgb1 & 0xFF;
                    
                    int r2 = (rgb2 >> 16) & 0xFF;
                    int g2 = (rgb2 >> 8) & 0xFF;
                    int b2 = rgb2 & 0xFF;
                    
                    difference += Math.abs(r1 - r2);
                    difference += Math.abs(g1 - g2);
                    difference += Math.abs(b1 - b2);
                }
            }
            
            long maxDifference = (long) img1.getWidth() * img1.getHeight() * 255 * 3;
            double similarity = 1.0 - (difference / (double) maxDifference);
            
            return similarity * 100; // Return percentage
            
        } catch (Exception e) {
            logger.error("Failed to compare screenshots", e);
            return 0.0;
        }
    }
    
    /**
     * Save screenshot to file
     */
    private static String saveScreenshot(byte[] screenshotData, String screenshotName) {
        try {
            // Create screenshot directory
            Path screenshotDir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(screenshotDir);
            
            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("%s_%s.%s", screenshotName, timestamp, SCREENSHOT_FORMAT);
            
            // Save file
            Path filePath = screenshotDir.resolve(fileName);
            Files.write(filePath, screenshotData);
            
            logger.debug("Screenshot saved: {}", filePath);
            return filePath.toString();
            
        } catch (IOException e) {
            logger.error("Failed to save screenshot", e);
            return null;
        }
    }
    
    /**
     * Crop image
     */
    private static byte[] cropImage(byte[] imageData, int x, int y, int width, int height) {
        try {
            BufferedImage fullImage = ImageIO.read(new ByteArrayInputStream(imageData));
            BufferedImage croppedImage = fullImage.getSubimage(x, y, width, height);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, SCREENSHOT_FORMAT, baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to crop image", e);
            return imageData;
        }
    }
    
    /**
     * Add watermark to screenshot
     */
    public static byte[] addWatermark(byte[] screenshotData, String watermarkText) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotData));
            Graphics2D g2d = image.createGraphics();
            
            // Configure rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            
            // Set font
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.setColor(Color.RED);
            
            // Calculate position
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int x = image.getWidth() - fontMetrics.stringWidth(watermarkText) - 10;
            int y = image.getHeight() - 10;
            
            // Draw watermark
            g2d.drawString(watermarkText, x, y);
            g2d.dispose();
            
            // Convert back to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, SCREENSHOT_FORMAT, baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to add watermark", e);
            return screenshotData;
        }
    }
    
    /**
     * Create a placeholder screenshot with error message
     */
    private static byte[] createPlaceholderScreenshot(String message) {
        try {
            // Create a placeholder image
            int width = 800;
            int height = 600;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Fill background
            g2d.setColor(new Color(245, 245, 245));
            g2d.fillRect(0, 0, width, height);
            
            // Draw border
            g2d.setColor(new Color(220, 53, 69));
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(2, 2, width - 4, height - 4);
            
            // Draw error icon
            g2d.setColor(new Color(220, 53, 69));
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            String icon = "⚠";
            FontMetrics fm = g2d.getFontMetrics();
            int iconX = (width - fm.stringWidth(icon)) / 2;
            g2d.drawString(icon, iconX, 200);
            
            // Draw title
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String title = "Screenshot Not Available";
            fm = g2d.getFontMetrics();
            int titleX = (width - fm.stringWidth(title)) / 2;
            g2d.drawString(title, titleX, 300);
            
            // Draw message
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            g2d.setColor(new Color(108, 117, 125));
            fm = g2d.getFontMetrics();
            
            // Word wrap the message
            String[] words = message.split(" ");
            StringBuilder line = new StringBuilder();
            int y = 380;
            int lineHeight = 25;
            int maxWidth = width - 100;
            
            for (String word : words) {
                String testLine = line + word + " ";
                int lineWidth = fm.stringWidth(testLine);
                if (lineWidth > maxWidth) {
                    if (line.length() > 0) {
                        String currentLine = line.toString().trim();
                        int x = (width - fm.stringWidth(currentLine)) / 2;
                        g2d.drawString(currentLine, x, y);
                        y += lineHeight;
                        line = new StringBuilder(word + " ");
                    }
                } else {
                    line.append(word).append(" ");
                }
            }
            if (line.length() > 0) {
                String currentLine = line.toString().trim();
                int x = (width - fm.stringWidth(currentLine)) / 2;
                g2d.drawString(currentLine, x, y);
            }
            
            // Add timestamp
            g2d.setFont(new Font("Arial", Font.ITALIC, 14));
            g2d.setColor(new Color(173, 181, 189));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            fm = g2d.getFontMetrics();
            int timestampX = (width - fm.stringWidth(timestamp)) / 2;
            g2d.drawString(timestamp, timestampX, height - 30);
            
            g2d.dispose();
            
            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, SCREENSHOT_FORMAT, baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to create placeholder screenshot", e);
            // Return a minimal valid PNG if all else fails
            return new byte[0];
        }
    }
}