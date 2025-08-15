package com.testforge.cs.utils;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSDataException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for database operations
 * Supports multiple database types and connection pooling
 */
public class CSDbUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSDbUtils.class);
    private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private static final CSConfigManager config = CSConfigManager.getInstance();
    
    private CSDbUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get data source for default database
     */
    public static DataSource getDataSource() {
        return getDataSource("default");
    }
    
    /**
     * Get data source for specific database
     */
    public static DataSource getDataSource(String dbName) {
        return dataSources.computeIfAbsent(dbName, CSDbUtils::createDataSource);
    }
    
    /**
     * Create data source
     */
    private static HikariDataSource createDataSource(String dbName) {
        try {
            logger.info("Creating data source for database: {}", dbName);
            
            HikariConfig hikariConfig = new HikariConfig();
            
            // Get database configuration
            String dbType = config.getProperty("cs.db." + dbName + ".type", "postgresql");
            String host = config.getProperty("cs.db." + dbName + ".host", "localhost");
            String port = config.getProperty("cs.db." + dbName + ".port", getDefaultPort(dbType));
            String database = config.getProperty("cs.db." + dbName + ".name", "testdb");
            String username = config.getProperty("cs.db." + dbName + ".username", "testuser");
            String password = config.getProperty("cs.db." + dbName + ".password", "testpass");
            
            // Build JDBC URL
            String jdbcUrl = buildJdbcUrl(dbType, host, port, database);
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            
            // Set driver class
            hikariConfig.setDriverClassName(getDriverClass(dbType));
            
            // Connection pool settings
            hikariConfig.setPoolName("CS-Pool-" + dbName);
            hikariConfig.setMaximumPoolSize(config.getIntProperty("cs.db.connection.pool.size", 10));
            hikariConfig.setConnectionTimeout(config.getLongProperty("cs.db.connection.timeout", 30000));
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setAutoCommit(true);
            
            // Create data source
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            logger.info("Data source created successfully for: {}", dbName);
            
            return dataSource;
            
        } catch (Exception e) {
            throw new CSDataException("Failed to create data source for: " + dbName, e);
        }
    }
    
    /**
     * Execute query and return results
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        return executeQuery("default", sql, params);
    }
    
    /**
     * Execute query on specific database
     */
    public static List<Map<String, Object>> executeQuery(String dbName, String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = getDataSource(dbName).getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params);
             ResultSet resultSet = statement.executeQuery()) {
            
            logger.debug("Executing query: {}", sql);
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
            logger.debug("Query returned {} rows", results.size());
            return results;
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to execute query: " + sql, e);
        }
    }
    
    /**
     * Execute update (INSERT, UPDATE, DELETE)
     */
    public static int executeUpdate(String sql, Object... params) {
        return executeUpdate("default", sql, params);
    }
    
    /**
     * Execute update on specific database
     */
    public static int executeUpdate(String dbName, String sql, Object... params) {
        try (Connection connection = getDataSource(dbName).getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, params)) {
            
            logger.debug("Executing update: {}", sql);
            int rowsAffected = statement.executeUpdate();
            logger.debug("Update affected {} rows", rowsAffected);
            
            return rowsAffected;
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to execute update: " + sql, e);
        }
    }
    
    /**
     * Execute insert and return generated keys
     */
    public static List<Long> executeInsert(String sql, Object... params) {
        return executeInsert("default", sql, params);
    }
    
    /**
     * Execute insert on specific database
     */
    public static List<Long> executeInsert(String dbName, String sql, Object... params) {
        List<Long> generatedKeys = new ArrayList<>();
        
        try (Connection connection = getDataSource(dbName).getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setParameters(statement, params);
            
            logger.debug("Executing insert: {}", sql);
            statement.executeUpdate();
            
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                while (resultSet.next()) {
                    generatedKeys.add(resultSet.getLong(1));
                }
            }
            
            logger.debug("Insert generated {} keys", generatedKeys.size());
            return generatedKeys;
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to execute insert: " + sql, e);
        }
    }
    
    /**
     * Execute batch update
     */
    public static int[] executeBatch(String sql, List<Object[]> paramsList) {
        return executeBatch("default", sql, paramsList);
    }
    
    /**
     * Execute batch update on specific database
     */
    public static int[] executeBatch(String dbName, String sql, List<Object[]> paramsList) {
        try (Connection connection = getDataSource(dbName).getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            logger.debug("Executing batch update: {} with {} sets of parameters", sql, paramsList.size());
            
            for (Object[] params : paramsList) {
                setParameters(statement, params);
                statement.addBatch();
            }
            
            int[] results = statement.executeBatch();
            logger.debug("Batch update completed, affected rows: {}", Arrays.stream(results).sum());
            
            return results;
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to execute batch: " + sql, e);
        }
    }
    
    /**
     * Execute stored procedure
     */
    public static Map<String, Object> executeStoredProcedure(String procedureName, Object... params) {
        return executeStoredProcedure("default", procedureName, params);
    }
    
    /**
     * Execute stored procedure on specific database
     */
    public static Map<String, Object> executeStoredProcedure(String dbName, String procedureName, Object... params) {
        Map<String, Object> results = new HashMap<>();
        
        try (Connection connection = getDataSource(dbName).getConnection()) {
            
            String sql = "{call " + procedureName + "(" + "?,".repeat(params.length).replaceAll(",$", "") + ")}";
            
            try (CallableStatement statement = connection.prepareCall(sql)) {
                
                // Set input parameters
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                
                logger.debug("Executing stored procedure: {}", procedureName);
                boolean hasResultSet = statement.execute();
                
                // Process result sets
                int resultSetIndex = 0;
                while (hasResultSet) {
                    try (ResultSet resultSet = statement.getResultSet()) {
                        List<Map<String, Object>> resultSetData = new ArrayList<>();
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        while (resultSet.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                            }
                            resultSetData.add(row);
                        }
                        
                        results.put("ResultSet" + resultSetIndex, resultSetData);
                        resultSetIndex++;
                    }
                    
                    hasResultSet = statement.getMoreResults();
                }
                
                // Get update count
                int updateCount = statement.getUpdateCount();
                if (updateCount != -1) {
                    results.put("UpdateCount", updateCount);
                }
                
                return results;
            }
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to execute stored procedure: " + procedureName, e);
        }
    }
    
    /**
     * Get single value from query
     */
    public static <T> T getSingleValue(String sql, Class<T> type, Object... params) {
        return getSingleValue("default", sql, type, params);
    }
    
    /**
     * Get single value from specific database
     */
    public static <T> T getSingleValue(String dbName, String sql, Class<T> type, Object... params) {
        List<Map<String, Object>> results = executeQuery(dbName, sql, params);
        
        if (results.isEmpty()) {
            return null;
        }
        
        Map<String, Object> firstRow = results.get(0);
        if (firstRow.isEmpty()) {
            return null;
        }
        
        Object value = firstRow.values().iterator().next();
        return castValue(value, type);
    }
    
    /**
     * Check if record exists
     */
    public static boolean exists(String sql, Object... params) {
        return exists("default", sql, params);
    }
    
    /**
     * Check if record exists in specific database
     */
    public static boolean exists(String dbName, String sql, Object... params) {
        Long count = getSingleValue(dbName, "SELECT COUNT(*) FROM (" + sql + ") AS subquery", Long.class, params);
        return count != null && count > 0;
    }
    
    /**
     * Execute query from properties file
     */
    public static List<Map<String, Object>> executeQueryFromProperties(String queryKey, Object... params) {
        String sql = config.getProperty(queryKey);
        if (sql == null) {
            throw new CSDataException("Query not found in properties: " + queryKey);
        }
        return executeQuery(sql, params);
    }
    
    /**
     * Execute update from properties file
     */
    public static int executeUpdateFromProperties(String queryKey, Object... params) {
        String sql = config.getProperty(queryKey);
        if (sql == null) {
            throw new CSDataException("Query not found in properties: " + queryKey);
        }
        return executeUpdate(sql, params);
    }
    
    /**
     * Begin transaction
     */
    public static Connection beginTransaction(String dbName) {
        try {
            Connection connection = getDataSource(dbName).getConnection();
            connection.setAutoCommit(false);
            logger.debug("Transaction started for database: {}", dbName);
            return connection;
        } catch (SQLException e) {
            throw new CSDataException("Failed to begin transaction", e);
        }
    }
    
    /**
     * Commit transaction
     */
    public static void commitTransaction(Connection connection) {
        try {
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            logger.debug("Transaction committed");
        } catch (SQLException e) {
            throw new CSDataException("Failed to commit transaction", e);
        }
    }
    
    /**
     * Rollback transaction
     */
    public static void rollbackTransaction(Connection connection) {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
            logger.debug("Transaction rolled back");
        } catch (SQLException e) {
            throw new CSDataException("Failed to rollback transaction", e);
        }
    }
    
    /**
     * Close all data sources
     */
    public static void closeAllDataSources() {
        logger.info("Closing all data sources");
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
    
    /**
     * Get metadata for table
     */
    public static List<Map<String, String>> getTableMetadata(String tableName) {
        return getTableMetadata("default", tableName);
    }
    
    /**
     * Get metadata for table in specific database
     */
    public static List<Map<String, String>> getTableMetadata(String dbName, String tableName) {
        List<Map<String, String>> metadata = new ArrayList<>();
        
        try (Connection connection = getDataSource(dbName).getConnection()) {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            
            try (ResultSet columns = dbMetaData.getColumns(null, null, tableName, null)) {
                while (columns.next()) {
                    Map<String, String> columnInfo = new LinkedHashMap<>();
                    columnInfo.put("COLUMN_NAME", columns.getString("COLUMN_NAME"));
                    columnInfo.put("DATA_TYPE", columns.getString("TYPE_NAME"));
                    columnInfo.put("SIZE", columns.getString("COLUMN_SIZE"));
                    columnInfo.put("NULLABLE", columns.getString("IS_NULLABLE"));
                    metadata.add(columnInfo);
                }
            }
            
            return metadata;
            
        } catch (SQLException e) {
            throw new CSDataException("Failed to get table metadata: " + tableName, e);
        }
    }
    
    /**
     * Prepare statement with parameters
     */
    private static PreparedStatement prepareStatement(Connection connection, String sql, Object... params) 
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        setParameters(statement, params);
        return statement;
    }
    
    /**
     * Set parameters on statement
     */
    private static void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Build JDBC URL based on database type
     */
    private static String buildJdbcUrl(String dbType, String host, String port, String database) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
            case "postgres":
                return "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case "mysql":
                return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
            case "h2":
                return "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1";
            case "sqlserver":
                return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database;
            case "oracle":
                return "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            default:
                throw new CSDataException("Unsupported database type: " + dbType);
        }
    }
    
    /**
     * Get driver class name
     */
    private static String getDriverClass(String dbType) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
            case "postgres":
                return "org.postgresql.Driver";
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "h2":
                return "org.h2.Driver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            default:
                throw new CSDataException("Unsupported database type: " + dbType);
        }
    }
    
    /**
     * Get default port for database type
     */
    private static String getDefaultPort(String dbType) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
            case "postgres":
                return "5432";
            case "mysql":
                return "3306";
            case "sqlserver":
                return "1433";
            case "oracle":
                return "1521";
            default:
                return "5432";
        }
    }
    
    /**
     * Cast value to specified type
     */
    @SuppressWarnings("unchecked")
    private static <T> T castValue(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        // Handle common conversions
        if (type == String.class) {
            return (T) value.toString();
        } else if (type == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        }
        
        throw new CSDataException("Cannot cast " + value.getClass() + " to " + type);
    }
}