package com.testforge.cs.database;

import com.testforge.cs.exceptions.CSDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Database abstraction for test operations
 * Provides methods for querying, updating, and validating database data
 */
public class CSDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(CSDatabase.class);
    
    private final DataSource dataSource;
    private final String databaseName;
    
    public CSDatabase(String databaseName, DataSource dataSource) {
        this.databaseName = databaseName;
        this.dataSource = dataSource;
    }
    
    /**
     * Execute a query and return results as list of maps
     */
    public List<Map<String, Object>> query(String sql, Object... parameters) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
            logger.debug("Query executed successfully: {} rows returned", results.size());
            
        } catch (SQLException e) {
            logger.error("Database query failed: {}", sql, e);
            throw new CSDataException("Database query failed", e);
        }
        
        return results;
    }
    
    /**
     * Execute a query and return single result as map
     */
    public Map<String, Object> queryForMap(String sql, Object... parameters) {
        List<Map<String, Object>> results = query(sql, parameters);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
    
    /**
     * Execute a query and return single value
     */
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> type, Object... parameters) {
        Map<String, Object> result = queryForMap(sql, parameters);
        if (result == null || result.isEmpty()) {
            return null;
        }
        
        Object value = result.values().iterator().next();
        return (T) value;
    }
    
    /**
     * Execute an update/insert/delete statement
     */
    public int update(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            setParameters(statement, parameters);
            int rowsAffected = statement.executeUpdate();
            
            logger.debug("Update executed successfully: {} rows affected", rowsAffected);
            return rowsAffected;
            
        } catch (SQLException e) {
            logger.error("Database update failed: {}", sql, e);
            throw new CSDataException("Database update failed", e);
        }
    }
    
    /**
     * Execute an insert statement and return generated key
     */
    public Object insertAndReturnKey(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setParameters(statement, parameters);
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getObject(1);
                    }
                }
            }
            
            logger.debug("Insert executed successfully: {} rows affected", rowsAffected);
            return null;
            
        } catch (SQLException e) {
            logger.error("Database insert failed: {}", sql, e);
            throw new CSDataException("Database insert failed", e);
        }
    }
    
    /**
     * Assert that a row exists in the table
     */
    public void assertRowExists(String tableName, String whereClause, Object... parameters) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
        Integer count = queryForObject(sql, Integer.class, parameters);
        
        Assert.assertTrue(count != null && count > 0, 
            "Expected row not found in table " + tableName + " with condition: " + whereClause);
        
        logger.info("Row existence verified in table: {}", tableName);
    }
    
    /**
     * Assert that a column has specific value
     */
    public void assertColumnValue(String tableName, String columnName, Object expectedValue, 
                                 String whereClause, Object... parameters) {
        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE " + whereClause;
        Object actualValue = queryForObject(sql, Object.class, parameters);
        
        Assert.assertEquals(actualValue, expectedValue, 
            "Column value mismatch in table " + tableName + ", column " + columnName);
        
        logger.info("Column value verified in table: {} column: {}", tableName, columnName);
    }
    
    /**
     * Get row count for a table
     */
    public int getRowCount(String tableName, String whereClause, Object... parameters) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        
        Integer count = queryForObject(sql, Integer.class, parameters);
        return count != null ? count : 0;
    }
    
    /**
     * Check if table exists
     */
    public boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                return tables.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check table existence: {}", tableName, e);
            return false;
        }
    }
    
    /**
     * Execute a batch of statements
     */
    public int[] executeBatch(String sql, List<Object[]> parametersList) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            for (Object[] parameters : parametersList) {
                setParameters(statement, parameters);
                statement.addBatch();
            }
            
            int[] results = statement.executeBatch();
            logger.debug("Batch executed successfully: {} statements", results.length);
            return results;
            
        } catch (SQLException e) {
            logger.error("Database batch execution failed: {}", sql, e);
            throw new CSDataException("Database batch execution failed", e);
        }
    }
    
    /**
     * Execute within a transaction
     */
    public <T> T executeInTransaction(DatabaseTransaction<T> transaction) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                T result = transaction.execute(connection);
                connection.commit();
                logger.debug("Transaction executed successfully");
                return result;
            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction rolled back due to error", e);
                throw new CSDataException("Transaction failed", e);
            }
            
        } catch (SQLException e) {
            logger.error("Database transaction failed", e);
            throw new CSDataException("Database transaction failed", e);
        }
    }
    
    /**
     * Set parameters for prepared statement
     */
    private void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
    
    /**
     * Get database name
     */
    public String getDatabaseName() {
        return databaseName;
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Database connection test failed for: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Functional interface for database transactions
     */
    @FunctionalInterface
    public interface DatabaseTransaction<T> {
        T execute(Connection connection) throws Exception;
    }
}