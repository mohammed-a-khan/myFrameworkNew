package com.testforge.cs.utils;

import com.testforge.cs.exceptions.CSDataException;
import com.testforge.cs.security.CSEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for CSV operations
 * Supports key field filtering and advanced operations
 */
public class CSCsvUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSCsvUtils.class);
    private static final String DEFAULT_DELIMITER = ",";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    
    private CSCsvUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Read CSV file with default settings
     */
    public static List<Map<String, String>> readCsv(String filePath) {
        return readCsv(filePath, true, DEFAULT_DELIMITER, DEFAULT_CHARSET);
    }
    
    /**
     * Read CSV file with header option
     */
    public static List<Map<String, String>> readCsv(String filePath, boolean hasHeader) {
        return readCsv(filePath, hasHeader, DEFAULT_DELIMITER, DEFAULT_CHARSET);
    }
    
    /**
     * Read CSV file with custom delimiter
     */
    public static List<Map<String, String>> readCsv(String filePath, boolean hasHeader, String delimiter) {
        return readCsv(filePath, hasHeader, delimiter, DEFAULT_CHARSET);
    }
    
    /**
     * Read CSV file with all options
     */
    public static List<Map<String, String>> readCsv(String filePath, boolean hasHeader, 
                                                   String delimiter, Charset charset) {
        List<Map<String, String>> data = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), charset)) {
            logger.debug("Reading CSV file: {}", filePath);
            
            String line;
            List<String> headers = null;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                List<String> values = parseCsvLine(line, delimiter);
                
                if (hasHeader && headers == null) {
                    headers = values;
                    continue;
                }
                
                if (headers == null) {
                    // Generate default headers
                    headers = new ArrayList<>();
                    for (int i = 0; i < values.size(); i++) {
                        headers.add("Column" + (i + 1));
                    }
                }
                
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < values.size(); i++) {
                    String value = values.get(i);
                    // Decrypt encrypted values
                    if (CSEncryptionUtils.isEncrypted(value)) {
                        value = CSEncryptionUtils.decrypt(value);
                    }
                    row.put(headers.get(i), value);
                }
                
                // Add empty values for missing columns
                for (int i = values.size(); i < headers.size(); i++) {
                    row.put(headers.get(i), "");
                }
                
                data.add(row);
            }
            
            logger.debug("Read {} rows from CSV file", data.size());
            return data;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to read CSV file", e);
        }
    }
    
    /**
     * Read CSV with key field filtering
     */
    public static List<Map<String, String>> readCsvWithKey(String filePath, String keyField, String keyValue) {
        return readCsvWithKey(filePath, keyField, Collections.singletonList(keyValue), true, DEFAULT_DELIMITER);
    }
    
    /**
     * Read CSV with key field filtering for multiple values
     */
    public static List<Map<String, String>> readCsvWithKey(String filePath, String keyField, 
                                                          List<String> keyValues, boolean hasHeader, String delimiter) {
        List<Map<String, String>> allData = readCsv(filePath, hasHeader, delimiter);
        
        return allData.stream()
            .filter(row -> keyValues.contains(row.get(keyField)))
            .collect(Collectors.toList());
    }
    
    /**
     * Write CSV file with default settings
     */
    public static void writeCsv(String filePath, List<Map<String, String>> data) {
        writeCsv(filePath, data, true, DEFAULT_DELIMITER, DEFAULT_CHARSET);
    }
    
    /**
     * Write CSV file with header option
     */
    public static void writeCsv(String filePath, List<Map<String, String>> data, boolean writeHeader) {
        writeCsv(filePath, data, writeHeader, DEFAULT_DELIMITER, DEFAULT_CHARSET);
    }
    
    /**
     * Write CSV file with all options
     */
    public static void writeCsv(String filePath, List<Map<String, String>> data, boolean writeHeader,
                               String delimiter, Charset charset) {
        try {
            logger.debug("Writing CSV file: {}", filePath);
            
            // Create parent directories if needed
            Files.createDirectories(Paths.get(filePath).getParent());
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), charset,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                
                if (data.isEmpty()) {
                    return;
                }
                
                // Write header
                if (writeHeader) {
                    List<String> headers = new ArrayList<>(data.get(0).keySet());
                    writer.write(formatCsvLine(headers, delimiter));
                    writer.newLine();
                }
                
                // Write data
                for (Map<String, String> row : data) {
                    List<String> values = new ArrayList<>(row.values());
                    writer.write(formatCsvLine(values, delimiter));
                    writer.newLine();
                }
            }
            
            logger.debug("Wrote {} rows to CSV file", data.size());
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to write CSV file", e);
        }
    }
    
    /**
     * Append data to CSV file
     */
    public static void appendToCsv(String filePath, List<Map<String, String>> data, String delimiter) {
        try {
            logger.debug("Appending to CSV file: {}", filePath);
            
            boolean fileExists = Files.exists(Paths.get(filePath));
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), DEFAULT_CHARSET,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                
                for (Map<String, String> row : data) {
                    List<String> values = new ArrayList<>(row.values());
                    writer.write(formatCsvLine(values, delimiter));
                    writer.newLine();
                }
            }
            
            logger.debug("Appended {} rows to CSV file", data.size());
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to append to CSV file", e);
        }
    }
    
    /**
     * Filter CSV data by column value
     */
    public static List<Map<String, String>> filterCsv(String filePath, String columnName, String value) {
        List<Map<String, String>> data = readCsv(filePath);
        return data.stream()
            .filter(row -> value.equals(row.get(columnName)))
            .collect(Collectors.toList());
    }
    
    /**
     * Sort CSV data by column
     */
    public static List<Map<String, String>> sortCsv(String filePath, String columnName, boolean ascending) {
        List<Map<String, String>> data = readCsv(filePath);
        
        Comparator<Map<String, String>> comparator = (r1, r2) -> {
            String v1 = r1.get(columnName);
            String v2 = r2.get(columnName);
            if (v1 == null || v2 == null) return 0;
            return ascending ? v1.compareTo(v2) : v2.compareTo(v1);
        };
        
        return data.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }
    
    /**
     * Get column values from CSV
     */
    public static List<String> getColumnValues(String filePath, String columnName) {
        List<Map<String, String>> data = readCsv(filePath);
        return data.stream()
            .map(row -> row.get(columnName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get unique values from column
     */
    public static Set<String> getUniqueColumnValues(String filePath, String columnName) {
        return new LinkedHashSet<>(getColumnValues(filePath, columnName));
    }
    
    /**
     * Group CSV data by column
     */
    public static Map<String, List<Map<String, String>>> groupByColumn(String filePath, String columnName) {
        List<Map<String, String>> data = readCsv(filePath);
        return data.stream()
            .collect(Collectors.groupingBy(row -> row.getOrDefault(columnName, "")));
    }
    
    /**
     * Convert CSV to list of lists
     */
    public static List<List<String>> readCsvAsList(String filePath, String delimiter) {
        List<List<String>> data = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), DEFAULT_CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    data.add(parseCsvLine(line, delimiter));
                }
            }
            return data;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to read CSV file as list", e);
        }
    }
    
    /**
     * Parse CSV line handling quotes and escapes
     */
    private static List<String> parseCsvLine(String line, String delimiter) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentValue.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter.charAt(0) && !inQuotes && delimiter.length() == 1) {
                // End of field
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(ch);
            }
        }
        
        // Add last field
        values.add(currentValue.toString().trim());
        
        return values;
    }
    
    /**
     * Format CSV line with proper escaping
     */
    private static String formatCsvLine(List<String> values, String delimiter) {
        return values.stream()
            .map(value -> {
                if (value == null) value = "";
                
                // Check if value needs quoting
                if (value.contains(delimiter) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                    // Escape quotes
                    value = value.replace("\"", "\"\"");
                    // Wrap in quotes
                    return "\"" + value + "\"";
                }
                return value;
            })
            .collect(Collectors.joining(delimiter));
    }
    
    /**
     * Get row count
     */
    public static int getRowCount(String filePath, boolean hasHeader) {
        try {
            long count = Files.lines(Paths.get(filePath), DEFAULT_CHARSET)
                .filter(line -> !line.trim().isEmpty())
                .count();
            
            return (int) (hasHeader && count > 0 ? count - 1 : count);
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to get row count", e);
        }
    }
    
    /**
     * Get column count
     */
    public static int getColumnCount(String filePath, String delimiter) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), DEFAULT_CHARSET)) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                return parseCsvLine(firstLine, delimiter).size();
            }
            return 0;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to get column count", e);
        }
    }
    
    /**
     * Validate CSV file structure
     */
    public static boolean validateCsvStructure(String filePath, List<String> expectedHeaders, String delimiter) {
        try {
            List<String> actualHeaders = getHeaders(filePath, delimiter);
            return actualHeaders.equals(expectedHeaders);
            
        } catch (Exception e) {
            logger.error("Failed to validate CSV structure", e);
            return false;
        }
    }
    
    /**
     * Get headers from CSV file
     */
    public static List<String> getHeaders(String filePath, String delimiter) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), DEFAULT_CHARSET)) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                return parseCsvLine(firstLine, delimiter);
            }
            return new ArrayList<>();
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "CSV", "Failed to get headers", e);
        }
    }
    
    /**
     * Merge multiple CSV files
     */
    public static void mergeCsvFiles(List<String> inputFiles, String outputFile, boolean hasHeader, String delimiter) {
        try {
            logger.debug("Merging {} CSV files into: {}", inputFiles.size(), outputFile);
            
            List<Map<String, String>> mergedData = new ArrayList<>();
            boolean firstFile = true;
            
            for (String inputFile : inputFiles) {
                List<Map<String, String>> data = readCsv(inputFile, hasHeader, delimiter);
                mergedData.addAll(data);
            }
            
            writeCsv(outputFile, mergedData, hasHeader, delimiter, DEFAULT_CHARSET);
            logger.info("Merged {} rows into: {}", mergedData.size(), outputFile);
            
        } catch (Exception e) {
            throw new CSDataException("Failed to merge CSV files", e);
        }
    }
}