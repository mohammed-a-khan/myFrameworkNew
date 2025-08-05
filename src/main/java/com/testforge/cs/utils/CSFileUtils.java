package com.testforge.cs.utils;

import com.testforge.cs.exceptions.CSFrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for file operations
 */
public class CSFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSFileUtils.class);
    
    private CSFileUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Read file content as string
     */
    public static String readFileAsString(String filePath) {
        return readFileAsString(filePath, StandardCharsets.UTF_8);
    }
    
    /**
     * Read file content as string with specific charset
     */
    public static String readFileAsString(String filePath, Charset charset) {
        try {
            logger.debug("Reading file: {}", filePath);
            return Files.readString(Paths.get(filePath), charset);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to read file: " + filePath, e);
        }
    }
    
    /**
     * Read text file content (alias for readFileAsString)
     */
    public static String readTextFile(String filePath) {
        return readFileAsString(filePath);
    }
    
    /**
     * Read file lines
     */
    public static List<String> readFileLines(String filePath) {
        return readFileLines(filePath, StandardCharsets.UTF_8);
    }
    
    /**
     * Read file lines with specific charset
     */
    public static List<String> readFileLines(String filePath, Charset charset) {
        try {
            logger.debug("Reading file lines: {}", filePath);
            return Files.readAllLines(Paths.get(filePath), charset);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to read file lines: " + filePath, e);
        }
    }
    
    /**
     * Write string to file
     */
    public static void writeStringToFile(String filePath, String content) {
        writeStringToFile(filePath, content, StandardCharsets.UTF_8, false);
    }
    
    /**
     * Write string to file with options
     */
    public static void writeStringToFile(String filePath, String content, Charset charset, boolean append) {
        try {
            logger.debug("Writing to file: {}", filePath);
            Path path = Paths.get(filePath);
            createParentDirectories(path);
            
            OpenOption[] options = append 
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
                
            Files.writeString(path, content, charset, options);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to write to file: " + filePath, e);
        }
    }
    
    /**
     * Write lines to file
     */
    public static void writeLinesToFile(String filePath, List<String> lines) {
        writeLinesToFile(filePath, lines, StandardCharsets.UTF_8, false);
    }
    
    /**
     * Write lines to file with options
     */
    public static void writeLinesToFile(String filePath, List<String> lines, Charset charset, boolean append) {
        try {
            logger.debug("Writing lines to file: {}", filePath);
            Path path = Paths.get(filePath);
            createParentDirectories(path);
            
            OpenOption[] options = append 
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
                
            Files.write(path, lines, charset, options);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to write lines to file: " + filePath, e);
        }
    }
    
    /**
     * Copy file
     */
    public static void copyFile(String source, String destination) {
        try {
            logger.debug("Copying file from {} to {}", source, destination);
            Path sourcePath = Paths.get(source);
            Path destPath = Paths.get(destination);
            createParentDirectories(destPath);
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to copy file from " + source + " to " + destination, e);
        }
    }
    
    /**
     * Move file
     */
    public static void moveFile(String source, String destination) {
        try {
            logger.debug("Moving file from {} to {}", source, destination);
            Path sourcePath = Paths.get(source);
            Path destPath = Paths.get(destination);
            createParentDirectories(destPath);
            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to move file from " + source + " to " + destination, e);
        }
    }
    
    /**
     * Delete file
     */
    public static boolean deleteFile(String filePath) {
        try {
            logger.debug("Deleting file: {}", filePath);
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * Delete directory recursively
     */
    public static void deleteDirectory(String directoryPath) {
        try {
            logger.debug("Deleting directory: {}", directoryPath);
            Path path = Paths.get(directoryPath);
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to delete directory: " + directoryPath, e);
        }
    }
    
    /**
     * Create directory
     */
    public static void createDirectory(String directoryPath) {
        try {
            logger.debug("Creating directory: {}", directoryPath);
            Files.createDirectories(Paths.get(directoryPath));
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to create directory: " + directoryPath, e);
        }
    }
    
    /**
     * Create parent directories
     */
    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
    
    /**
     * Check if file exists
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * Check if directory exists
     */
    public static boolean directoryExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        return Files.exists(path) && Files.isDirectory(path);
    }
    
    /**
     * Get file size
     */
    public static long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to get file size: " + filePath, e);
        }
    }
    
    /**
     * Get absolute path from relative path
     */
    public static String getAbsolutePath(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath().toString();
    }
    
    /**
     * Get file extension
     */
    public static String getFileExtension(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * Get file name without extension
     */
    public static String getFileNameWithoutExtension(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
    }
    
    /**
     * List files in directory
     */
    public static List<String> listFiles(String directoryPath) {
        return listFiles(directoryPath, false);
    }
    
    /**
     * List files in directory with option for recursive
     */
    public static List<String> listFiles(String directoryPath, boolean recursive) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.isDirectory(path)) {
                throw new CSFrameworkException("Path is not a directory: " + directoryPath);
            }
            
            try (Stream<Path> stream = recursive ? Files.walk(path) : Files.list(path)) {
                return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to list files in directory: " + directoryPath, e);
        }
    }
    
    /**
     * List files with extension
     */
    public static List<String> listFilesWithExtension(String directoryPath, String extension) {
        return listFilesWithExtension(directoryPath, extension, false);
    }
    
    /**
     * List files with extension and recursive option
     */
    public static List<String> listFilesWithExtension(String directoryPath, String extension, boolean recursive) {
        String ext = extension.startsWith(".") ? extension : "." + extension;
        return listFiles(directoryPath, recursive).stream()
            .filter(file -> file.endsWith(ext))
            .collect(Collectors.toList());
    }
    
    /**
     * Zip files
     */
    public static void zipFiles(List<String> files, String zipFilePath) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            logger.debug("Creating zip file: {}", zipFilePath);
            
            for (String filePath : files) {
                File file = new File(filePath);
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);
                        
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to create zip file: " + zipFilePath, e);
        }
    }
    
    /**
     * Unzip file
     */
    public static void unzipFile(String zipFilePath, String destinationDir) {
        try {
            logger.debug("Extracting zip file: {} to {}", zipFilePath, destinationDir);
            createDirectory(destinationDir);
            
            byte[] buffer = new byte[1024];
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = new File(destinationDir, zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        createParentDirectories(newFile.toPath());
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int length;
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to extract zip file: " + zipFilePath, e);
        }
    }
    
    /**
     * Get file creation time
     */
    public static long getFileCreationTime(String filePath) {
        try {
            BasicFileAttributes attr = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class);
            return attr.creationTime().toMillis();
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to get file creation time: " + filePath, e);
        }
    }
    
    /**
     * Get file last modified time
     */
    public static long getFileLastModifiedTime(String filePath) {
        try {
            return Files.getLastModifiedTime(Paths.get(filePath)).toMillis();
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to get file last modified time: " + filePath, e);
        }
    }
    
    /**
     * Create temporary file
     */
    public static String createTempFile(String prefix, String suffix) {
        try {
            Path tempFile = Files.createTempFile(prefix, suffix);
            logger.debug("Created temporary file: {}", tempFile);
            return tempFile.toString();
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to create temporary file", e);
        }
    }
    
    /**
     * Create temporary directory
     */
    public static String createTempDirectory(String prefix) {
        try {
            Path tempDir = Files.createTempDirectory(prefix);
            logger.debug("Created temporary directory: {}", tempDir);
            return tempDir.toString();
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to create temporary directory", e);
        }
    }
    
    /**
     * Compare two files
     */
    public static boolean compareFiles(String file1, String file2) {
        try {
            return Files.mismatch(Paths.get(file1), Paths.get(file2)) == -1;
        } catch (IOException e) {
            logger.error("Failed to compare files: {} and {}", file1, file2, e);
            return false;
        }
    }
    
    /**
     * Get available disk space
     */
    public static long getAvailableDiskSpace(String path) {
        File file = new File(path);
        return file.getUsableSpace();
    }
    
    /**
     * Clean directory (delete all contents but keep directory)
     */
    public static void cleanDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                Files.walk(path)
                    .filter(p -> !p.equals(path))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new CSFrameworkException("Failed to clean directory: " + directoryPath, e);
        }
    }
}