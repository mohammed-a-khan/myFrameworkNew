package com.testforge.cs.dataprovider;

import com.testforge.cs.annotations.CSDataSource;
import com.testforge.cs.utils.*;
import com.testforge.cs.database.CSQueryManager;
import org.testng.annotations.DataProvider;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central data provider for all data-driven tests
 */
public class CSDataProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CSDataProvider.class);
    
    @DataProvider(name = "excelData", parallel = true)
    public static Object[][] provideExcelData(Method method, ITestContext context) {
        return provideData(method, CSDataSource.Type.EXCEL);
    }
    
    @DataProvider(name = "csvData", parallel = true)
    public static Object[][] provideCsvData(Method method, ITestContext context) {
        return provideData(method, CSDataSource.Type.CSV);
    }
    
    @DataProvider(name = "jsonData", parallel = true)
    public static Object[][] provideJsonData(Method method, ITestContext context) {
        return provideData(method, CSDataSource.Type.JSON);
    }
    
    @DataProvider(name = "dbData", parallel = true)
    public static Object[][] provideDatabaseData(Method method, ITestContext context) {
        return provideData(method, CSDataSource.Type.DATABASE);
    }
    
    @DataProvider(name = "loginData", parallel = true)
    public static Object[][] provideLoginData(Method method, ITestContext context) {
        // Generic data provider that determines type from annotation
        CSDataSource dataSource = method.getAnnotation(CSDataSource.class);
        if (dataSource != null) {
            return provideData(method, dataSource.type());
        }
        return new Object[0][0];
    }
    
    private static Object[][] provideData(Method method, CSDataSource.Type expectedType) {
        CSDataSource dataSource = method.getAnnotation(CSDataSource.class);
        if (dataSource == null) {
            throw new IllegalArgumentException("Method " + method.getName() + 
                " must have @CSDataSource annotation");
        }
        
        if (dataSource.type() != expectedType) {
            throw new IllegalArgumentException("Expected data source type " + expectedType + 
                " but found " + dataSource.type());
        }
        
        try {
            List<Map<String, Object>> data = loadData(dataSource);
            
            // Apply filtering if specified
            if (!dataSource.filter().isEmpty()) {
                data = filterData(data, dataSource.filter());
            }
            
            // Convert to Object[][] for TestNG
            return convertToArray(data);
            
        } catch (Exception e) {
            logger.error("Failed to load data for method: " + method.getName(), e);
            throw new RuntimeException("Data loading failed", e);
        }
    }
    
    private static List<Map<String, Object>> loadData(CSDataSource dataSource) {
        String source = getDataSourcePath(dataSource);
        
        switch (dataSource.type()) {
            case EXCEL:
                return loadExcelData(source, dataSource);
                
            case CSV:
                return loadCsvData(source, dataSource);
                
            case JSON:
                return loadJsonData(source, dataSource);
                
            case DATABASE:
                return loadDatabaseData(dataSource);
                
            default:
                throw new UnsupportedOperationException(
                    "Data source type not supported: " + dataSource.type());
        }
    }
    
    private static String getDataSourcePath(CSDataSource dataSource) {
        // Priority: path > source
        if (!dataSource.path().isEmpty()) {
            return dataSource.path();
        }
        return dataSource.source();
    }
    
    private static List<Map<String, Object>> loadExcelData(String path, CSDataSource dataSource) {
        List<Map<String, String>> excelData;
        if (dataSource.sheet().isEmpty()) {
            excelData = CSExcelUtils.readExcel(path);
        } else {
            excelData = CSExcelUtils.readExcel(path, dataSource.sheet(), true);
        }
        
        // Convert to Map<String, Object>
        List<Map<String, Object>> allData = new ArrayList<>();
        for (Map<String, String> row : excelData) {
            Map<String, Object> objectRow = new HashMap<>(row);
            allData.add(objectRow);
        }
        
        // Filter by key field if specified
        if (!dataSource.keyField().isEmpty() && !dataSource.keyValues().isEmpty()) {
            String keyField = dataSource.keyField();
            Set<String> keyValues = new HashSet<>(
                Arrays.asList(dataSource.keyValues().split(","))
            );
            
            allData = allData.stream()
                .filter(row -> keyValues.contains(String.valueOf(row.get(keyField))))
                .collect(Collectors.toList());
        }
        
        return allData;
    }
    
    private static List<Map<String, Object>> loadCsvData(String path, CSDataSource dataSource) {
        List<Map<String, String>> csvData = CSCsvUtils.readCsv(path, dataSource.hasHeader());
        
        // Convert to Map<String, Object>
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, String> row : csvData) {
            Map<String, Object> objectRow = new HashMap<>(row);
            result.add(objectRow);
        }
        
        return result;
    }
    
    private static List<Map<String, Object>> loadJsonData(String path, CSDataSource dataSource) {
        String jsonContent = CSFileUtils.readTextFile(path);
        
        // Try to parse as array first with automatic decryption
        try {
            // Use the new method that automatically decrypts encrypted values
            List<Map<String, Object>> jsonArray = CSJsonUtils.parseJsonArrayWithDecryption(jsonContent);
            return jsonArray;
        } catch (Exception e) {
            // If not array, try as single object with automatic decryption
            Map<String, Object> singleObject = CSJsonUtils.parseJsonWithDecryption(jsonContent);
            return Collections.singletonList(singleObject);
        }
    }
    
    private static List<Map<String, Object>> loadDatabaseData(CSDataSource dataSource) {
        String query;
        Map<String, Object> params = new HashMap<>();
        
        if (!dataSource.query().isEmpty()) {
            query = dataSource.query();
        } else if (!dataSource.queryKey().isEmpty()) {
            query = CSQueryManager.getInstance().getQuery(dataSource.queryKey());
            
            // Parse query parameters
            for (String param : dataSource.queryParams()) {
                String[] parts = param.split(":");
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        } else {
            throw new IllegalArgumentException("Database data source must specify query or queryKey");
        }
        
        // Execute query
        List<Map<String, Object>> results = CSDbUtils.executeQuery(
            dataSource.database(), 
            query, 
            params
        );
        
        return results;
    }
    
    private static List<Map<String, Object>> filterData(List<Map<String, Object>> data, String filter) {
        // Simple filter implementation
        // Format: "field=value" or "field!=value" or "field>value" etc.
        
        String[] parts = filter.split("(=|!=|>|<|>=|<=)");
        if (parts.length != 2) {
            return data;
        }
        
        String field = parts[0].trim();
        String value = parts[1].trim();
        String operator = filter.substring(parts[0].length(), filter.length() - parts[1].length());
        
        return data.stream()
            .filter(row -> evaluateFilter(row.get(field), operator, value))
            .collect(Collectors.toList());
    }
    
    private static boolean evaluateFilter(Object fieldValue, String operator, String compareValue) {
        if (fieldValue == null) {
            return false;
        }
        
        String fieldStr = String.valueOf(fieldValue);
        
        switch (operator) {
            case "=":
                return fieldStr.equals(compareValue);
            case "!=":
                return !fieldStr.equals(compareValue);
            case ">":
                return compareNumeric(fieldStr, compareValue) > 0;
            case "<":
                return compareNumeric(fieldStr, compareValue) < 0;
            case ">=":
                return compareNumeric(fieldStr, compareValue) >= 0;
            case "<=":
                return compareNumeric(fieldStr, compareValue) <= 0;
            default:
                return true;
        }
    }
    
    private static int compareNumeric(String val1, String val2) {
        try {
            double d1 = Double.parseDouble(val1);
            double d2 = Double.parseDouble(val2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return val1.compareTo(val2);
        }
    }
    
    private static Object[][] convertToArray(List<Map<String, Object>> data) {
        Object[][] result = new Object[data.size()][1];
        
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        
        return result;
    }
}