package com.testforge.cs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Iterator;

/**
 * Utility class for image processing operations
 */
public class CSImageUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSImageUtils.class);
    
    /**
     * Convert an image file to Base64 encoded data URI
     * 
     * @param imagePath Path to the image file
     * @return Base64 encoded data URI string
     */
    public static String imageToBase64DataUri(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            // Try with cs-reports prefix
            imageFile = new File("cs-reports/" + imagePath);
            if (!imageFile.exists()) {
                logger.warn("Image file not found: {}", imagePath);
                return null;
            }
        }
        
        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            // Determine MIME type based on file extension
            String mimeType = "image/png"; // default
            String fileName = imageFile.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (fileName.endsWith(".gif")) {
                mimeType = "image/gif";
            } else if (fileName.endsWith(".webp")) {
                mimeType = "image/webp";
            }
            
            return "data:" + mimeType + ";base64," + base64;
        } catch (IOException e) {
            logger.error("Failed to convert image to Base64: " + imagePath, e);
            return null;
        }
    }
    
    /**
     * Check if a string is a Base64 data URI
     * 
     * @param str String to check
     * @return true if the string is a data URI
     */
    public static boolean isDataUri(String str) {
        return str != null && str.startsWith("data:image/");
    }
    
    /**
     * Get file size in human readable format
     * 
     * @param filePath Path to the file
     * @return File size as string
     */
    public static String getFileSize(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            file = new File("cs-reports/" + filePath);
        }
        
        if (!file.exists()) {
            return "N/A";
        }
        
        long size = file.length();
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Compress and optimize image for web display
     * Reduces file size significantly while maintaining reasonable quality
     * 
     * @param originalImagePath Path to the original image
     * @param maxWidth Maximum width for the optimized image (default: 800px)
     * @param quality JPEG quality from 0.1 to 1.0 (default: 0.7)
     * @return Optimized image as Base64 data URI, or null if failed
     */
    public static String compressImageToBase64(String originalImagePath, int maxWidth, float quality) {
        if (originalImagePath == null || originalImagePath.isEmpty()) {
            return null;
        }
        
        File imageFile = new File(originalImagePath);
        if (!imageFile.exists()) {
            logger.warn("Image file not found for compression: {}", originalImagePath);
            return null;
        }
        
        try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                logger.warn("Could not read image: {}", originalImagePath);
                return null;
            }
            
            // Calculate new dimensions maintaining aspect ratio
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            
            if (originalWidth > maxWidth) {
                newWidth = maxWidth;
                newHeight = (int) ((double) originalHeight * maxWidth / originalWidth);
            }
            
            // Create optimized image
            BufferedImage optimizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = optimizedImage.createGraphics();
            
            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Fill background with white (for transparency)
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);
            
            // Draw the scaled image
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            // Convert to JPEG with specified quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                logger.warn("No JPEG writers available");
                return null;
            }
            
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(optimizedImage, null, null), param);
            writer.dispose();
            ios.close();
            
            // Convert to base64
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            // Calculate size reduction
            long originalSize = imageFile.length();
            long optimizedSize = imageBytes.length;
            double reductionPercent = ((double)(originalSize - optimizedSize) / originalSize) * 100;
            
            logger.info("Image compressed: {} -> {} bytes ({}% reduction)", 
                       originalSize, optimizedSize, String.format("%.1f", reductionPercent));
            
            return "data:image/jpeg;base64," + base64;
            
        } catch (IOException e) {
            logger.error("Failed to compress image: " + originalImagePath, e);
            return null;
        }
    }
    
    /**
     * Compress image with default settings (800px width, 70% quality)
     * 
     * @param originalImagePath Path to the original image
     * @return Optimized image as Base64 data URI
     */
    public static String compressImageToBase64(String originalImagePath) {
        return compressImageToBase64(originalImagePath, 800, 0.7f);
    }
    
    /**
     * Create a thumbnail version of the image for quick preview
     * 
     * @param originalImagePath Path to the original image
     * @param thumbnailSize Size of the thumbnail (both width and height)
     * @return Thumbnail as Base64 data URI
     */
    public static String createThumbnail(String originalImagePath, int thumbnailSize) {
        return compressImageToBase64(originalImagePath, thumbnailSize, 0.6f);
    }
    
    /**
     * Create a thumbnail with default size (150px)
     * 
     * @param originalImagePath Path to the original image
     * @return Thumbnail as Base64 data URI
     */
    public static String createThumbnail(String originalImagePath) {
        return createThumbnail(originalImagePath, 150);
    }
    
    /**
     * Get Base64 string length in human readable format
     * 
     * @param base64String Base64 encoded string
     * @return Size description
     */
    public static String getBase64Size(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return "0 B";
        }
        
        // Remove data URI prefix to get actual base64 content
        String base64Content = base64String;
        if (base64Content.startsWith("data:")) {
            int commaIndex = base64Content.indexOf(',');
            if (commaIndex > 0) {
                base64Content = base64Content.substring(commaIndex + 1);
            }
        }
        
        // Base64 size approximation (4/3 of original)
        long approximateBytes = (long) (base64Content.length() * 0.75);
        
        if (approximateBytes < 1024) {
            return approximateBytes + " B";
        } else if (approximateBytes < 1024 * 1024) {
            return String.format("%.1f KB", approximateBytes / 1024.0);
        } else {
            return String.format("%.1f MB", approximateBytes / (1024.0 * 1024.0));
        }
    }
}