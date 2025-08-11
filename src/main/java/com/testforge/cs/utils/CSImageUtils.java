package com.testforge.cs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

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
}