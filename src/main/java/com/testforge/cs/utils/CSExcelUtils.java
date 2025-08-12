package com.testforge.cs.utils;

import com.testforge.cs.exceptions.CSDataException;
import com.testforge.cs.security.CSEncryptionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for Excel operations using Apache POI
 * Supports key field filtering and advanced operations
 */
public class CSExcelUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSExcelUtils.class);
    
    private CSExcelUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Read Excel file and return all data
     */
    public static List<Map<String, String>> readExcel(String filePath) {
        return readExcel(filePath, 0, true);
    }
    
    /**
     * Read Excel file with sheet index
     */
    public static List<Map<String, String>> readExcel(String filePath, int sheetIndex, boolean hasHeader) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(inputStream, filePath)) {
            
            logger.debug("Reading Excel file: {}", filePath);
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            return readSheet(sheet, hasHeader);
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to read Excel file", e);
        }
    }
    
    /**
     * Read Excel file with sheet name
     */
    public static List<Map<String, String>> readExcel(String filePath, String sheetName, boolean hasHeader) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(inputStream, filePath)) {
            
            logger.debug("Reading Excel file: {} - Sheet: {}", filePath, sheetName);
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new CSDataException("Sheet not found: " + sheetName);
            }
            return readSheet(sheet, hasHeader);
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to read Excel file", e);
        }
    }
    
    /**
     * Read Excel with key field filtering
     */
    public static List<Map<String, String>> readExcelWithKey(String filePath, String sheetName, 
                                                             String keyField, String keyValue) {
        return readExcelWithKey(filePath, sheetName, keyField, Collections.singletonList(keyValue));
    }
    
    /**
     * Read Excel with key field filtering for multiple values
     */
    public static List<Map<String, String>> readExcelWithKey(String filePath, String sheetName, 
                                                             String keyField, List<String> keyValues) {
        List<Map<String, String>> allData = readExcel(filePath, sheetName, true);
        
        return allData.stream()
            .filter(row -> keyValues.contains(row.get(keyField)))
            .collect(Collectors.toList());
    }
    
    /**
     * Write data to Excel file
     */
    public static void writeExcel(String filePath, List<Map<String, String>> data) {
        writeExcel(filePath, data, "Sheet1", true);
    }
    
    /**
     * Write data to Excel file with options
     */
    public static void writeExcel(String filePath, List<Map<String, String>> data, 
                                 String sheetName, boolean writeHeader) {
        try {
            logger.debug("Writing Excel file: {}", filePath);
            Workbook workbook = filePath.endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);
            
            if (data.isEmpty()) {
                workbook.write(new FileOutputStream(filePath));
                workbook.close();
                return;
            }
            
            // Write header
            int rowNum = 0;
            if (writeHeader) {
                Row headerRow = sheet.createRow(rowNum++);
                List<String> headers = new ArrayList<>(data.get(0).keySet());
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    
                    // Style header
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font font = workbook.createFont();
                    font.setBold(true);
                    headerStyle.setFont(font);
                    cell.setCellStyle(headerStyle);
                }
            }
            
            // Write data
            for (Map<String, String> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                List<String> headers = new ArrayList<>(rowData.keySet());
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.createCell(i);
                    setCellValue(cell, rowData.get(headers.get(i)));
                }
            }
            
            // Auto-size columns
            List<String> headers = new ArrayList<>(data.get(0).keySet());
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
            workbook.close();
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to write Excel file", e);
        }
    }
    
    /**
     * Append data to existing Excel file
     */
    public static void appendToExcel(String filePath, List<Map<String, String>> data, String sheetName) {
        try {
            logger.debug("Appending to Excel file: {}", filePath);
            File file = new File(filePath);
            Workbook workbook;
            Sheet sheet;
            
            if (file.exists()) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    workbook = createWorkbook(inputStream, filePath);
                    sheet = workbook.getSheet(sheetName);
                    if (sheet == null) {
                        sheet = workbook.createSheet(sheetName);
                    }
                }
            } else {
                workbook = filePath.endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
                sheet = workbook.createSheet(sheetName);
            }
            
            int lastRowNum = sheet.getLastRowNum();
            int startRow = lastRowNum > 0 ? lastRowNum + 1 : 0;
            
            // Write data
            for (Map<String, String> rowData : data) {
                Row row = sheet.createRow(startRow++);
                List<String> headers = new ArrayList<>(rowData.keySet());
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.createCell(i);
                    setCellValue(cell, rowData.get(headers.get(i)));
                }
            }
            
            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
            workbook.close();
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to append to Excel file", e);
        }
    }
    
    /**
     * Update Excel cell value
     */
    public static void updateCell(String filePath, String sheetName, int row, int col, String value) {
        try {
            logger.debug("Updating cell in Excel file: {} - Sheet: {} - Row: {} - Col: {}", 
                        filePath, sheetName, row, col);
            
            File file = new File(filePath);
            Workbook workbook;
            
            try (InputStream inputStream = new FileInputStream(file)) {
                workbook = createWorkbook(inputStream, filePath);
            }
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new CSDataException("Sheet not found: " + sheetName);
            }
            
            Row sheetRow = sheet.getRow(row);
            if (sheetRow == null) {
                sheetRow = sheet.createRow(row);
            }
            
            Cell cell = sheetRow.getCell(col);
            if (cell == null) {
                cell = sheetRow.createCell(col);
            }
            
            setCellValue(cell, value);
            
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
            workbook.close();
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to update cell", e);
        }
    }
    
    /**
     * Get sheet names from Excel file
     */
    public static List<String> getSheetNames(String filePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(inputStream, filePath)) {
            
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
            return sheetNames;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to get sheet names", e);
        }
    }
    
    /**
     * Get row count for a sheet
     */
    public static int getRowCount(String filePath, String sheetName) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(inputStream, filePath)) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new CSDataException("Sheet not found: " + sheetName);
            }
            return sheet.getLastRowNum() + 1;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to get row count", e);
        }
    }
    
    /**
     * Get column count for a sheet
     */
    public static int getColumnCount(String filePath, String sheetName) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(inputStream, filePath)) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new CSDataException("Sheet not found: " + sheetName);
            }
            
            Row firstRow = sheet.getRow(0);
            return firstRow != null ? firstRow.getLastCellNum() : 0;
            
        } catch (IOException e) {
            throw new CSDataException(filePath, "Excel", "Failed to get column count", e);
        }
    }
    
    /**
     * Convert Excel to CSV
     */
    public static void excelToCsv(String excelPath, String csvPath, String sheetName) {
        List<Map<String, String>> data = readExcel(excelPath, sheetName, true);
        CSCsvUtils.writeCsv(csvPath, data, true);
        logger.info("Converted Excel to CSV: {} -> {}", excelPath, csvPath);
    }
    
    /**
     * Convert CSV to Excel
     */
    public static void csvToExcel(String csvPath, String excelPath, String sheetName) {
        List<Map<String, String>> data = CSCsvUtils.readCsv(csvPath, true);
        writeExcel(excelPath, data, sheetName, true);
        logger.info("Converted CSV to Excel: {} -> {}", csvPath, excelPath);
    }
    
    /**
     * Filter Excel data by column value
     */
    public static List<Map<String, String>> filterExcel(String filePath, String sheetName, 
                                                       String columnName, String value) {
        List<Map<String, String>> data = readExcel(filePath, sheetName, true);
        return data.stream()
            .filter(row -> value.equals(row.get(columnName)))
            .collect(Collectors.toList());
    }
    
    /**
     * Sort Excel data by column
     */
    public static List<Map<String, String>> sortExcel(String filePath, String sheetName, 
                                                      String columnName, boolean ascending) {
        List<Map<String, String>> data = readExcel(filePath, sheetName, true);
        
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
     * Create workbook based on file type
     */
    private static Workbook createWorkbook(InputStream inputStream, String filePath) throws IOException {
        if (filePath.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (filePath.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new CSDataException("Unsupported Excel format: " + filePath);
        }
    }
    
    /**
     * Read sheet data
     */
    private static List<Map<String, String>> readSheet(Sheet sheet, boolean hasHeader) {
        List<Map<String, String>> data = new ArrayList<>();
        
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return data;
        }
        
        // Get headers
        List<String> headers = new ArrayList<>();
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        
        if (hasHeader) {
            Row headerRow = sheet.getRow(firstRow);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValueAsString(cell));
                }
                firstRow++;
            }
        } else {
            // Generate default headers
            Row firstDataRow = sheet.getRow(firstRow);
            if (firstDataRow != null) {
                headers = IntStream.range(0, firstDataRow.getLastCellNum())
                    .mapToObj(i -> "Column" + (i + 1))
                    .collect(Collectors.toList());
            }
        }
        
        // Read data rows
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    rowData.put(headers.get(i), getCellValueAsString(cell));
                }
                data.add(rowData);
            }
        }
        
        return data;
    }
    
    /**
     * Get cell value as string
     * Automatically decrypts encrypted values (wrapped in ENC())
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        String value = "";
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    LocalDateTime localDateTime = date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                    value = localDateTime.toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        value = String.valueOf((long) numValue);
                    } else {
                        value = String.valueOf(numValue);
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                try {
                    value = String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    value = cell.getStringCellValue();
                }
                break;
            case BLANK:
                return "";
            default:
                value = "";
        }
        
        // Check if the value is encrypted and decrypt it
        if (CSEncryptionUtils.isEncrypted(value)) {
            value = CSEncryptionUtils.decrypt(value);
        }
        
        return value;
    }
    
    /**
     * Set cell value with type detection
     */
    private static void setCellValue(Cell cell, String value) {
        if (value == null || value.isEmpty()) {
            cell.setBlank();
            return;
        }
        
        // Try to parse as number
        try {
            double numValue = Double.parseDouble(value);
            cell.setCellValue(numValue);
            return;
        } catch (NumberFormatException e) {
            // Not a number
        }
        
        // Try to parse as boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            cell.setCellValue(Boolean.parseBoolean(value));
            return;
        }
        
        // Default to string
        cell.setCellValue(value);
    }
}