package com.testforge.cs.database;

import com.testforge.cs.config.CSConfigManager;
import com.testforge.cs.exceptions.CSDataException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database manager for handling multiple named database connections
 * Supports connection pooling and configuration-driven setup
 */
public class CSDatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CSDatabaseManager.class);
    private static volatile CSDatabaseManager instance;
    
    private final Map<String, CSDatabase> databases = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final CSConfigManager config;
    
    private CSDatabaseManager() {
        this.config = CSConfigManager.getInstance();
        initializeDatabases();
    }
    
    public static CSDatabaseManager getInstance() {
        if (instance == null) {
            synchronized (CSDatabaseManager.class) {
                if (instance == null) {
                    instance = new CSDatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize databases from configuration
     */
    private void initializeDatabases() {
        // Get all database configurations
        String[] databaseNames = config.getString("cs.databases", "default").split(",");
        
        for (String dbName : databaseNames) {
            dbName = dbName.trim();
            if (!dbName.isEmpty()) {
                try {
                    initializeDatabase(dbName);
                } catch (Exception e) {
                    logger.error("Failed to initialize database: {}", dbName, e);
                    if (config.getBoolean("cs.database.strict.mode", true)) {
                        throw new CSDataException("Database initialization failed: " + dbName, e);
                    }
                }
            }
        }
        
        logger.info("Databases initialized: {}", databases.keySet());
    }
    
    /**
     * Initialize a specific database
     */
    private void initializeDatabase(String databaseName) {
        String prefix = databaseName + ".db.";
        
        // Check if database is enabled
        if (!config.getBoolean(prefix + "enabled", false)) {
            logger.debug("Database {} is disabled, skipping initialization", databaseName);
            return;
        }
        
        // Get database configuration
        String url = config.getString(prefix + "url");
        String username = config.getString(prefix + "username");
        String password = config.getString(prefix + "password");
        String driverClass = config.getString(prefix + "driver");
        
        if (url == null || username == null || password == null) {
            logger.warn("Incomplete database configuration for: {}", databaseName);
            return;
        }
        
        // Create HikariCP configuration
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        
        if (driverClass != null) {
            hikariConfig.setDriverClassName(driverClass);
        }
        
        // Connection pool settings
        hikariConfig.setMaximumPoolSize(config.getInt(prefix + "max.pool.size", 10));
        hikariConfig.setMinimumIdle(config.getInt(prefix + "min.idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong(prefix + "connection.timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong(prefix + "idle.timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong(prefix + "max.lifetime", 1800000));
        hikariConfig.setLeakDetectionThreshold(config.getLong(prefix + "leak.detection.threshold", 60000));
        
        // Pool name
        hikariConfig.setPoolName(databaseName + "-pool");
        
        // Additional properties
        String additionalProps = config.getString(prefix + "additional.properties");
        if (additionalProps != null) {
            String[] props = additionalProps.split(";");
            for (String prop : props) {
                String[] keyValue = prop.split("=");
                if (keyValue.length == 2) {
                    hikariConfig.addDataSourceProperty(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        
        // Create data source
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        
        // Test connection
        CSDatabase database = new CSDatabase(databaseName, dataSource);
        if (!database.testConnection()) {
            dataSource.close();
            throw new CSDataException("Failed to establish connection to database: " + databaseName);
        }
        
        // Store database and data source
        databases.put(databaseName, database);
        dataSources.put(databaseName, dataSource);
        
        logger.info("Database initialized successfully: {} -> {}", databaseName, url);
    }
    
    /**
     * Get database by name
     */
    public CSDatabase getDatabase(String databaseName) {
        CSDatabase database = databases.get(databaseName);
        if (database == null) {
            throw new CSDataException("Database not found or not configured: " + databaseName);
        }
        return database;
    }
    
    /**
     * Get default database
     */
    public CSDatabase getDatabase() {
        return getDatabase("default");
    }
    
    /**
     * Check if database exists
     */
    public boolean hasDatabase(String databaseName) {
        return databases.containsKey(databaseName);
    }
    
    /**
     * Get all database names
     */
    public java.util.Set<String> getDatabaseNames() {
        return databases.keySet();
    }
    
    /**
     * Add database at runtime
     */
    public void addDatabase(String databaseName, String url, String username, String password, String driverClass) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        
        if (driverClass != null) {
            hikariConfig.setDriverClassName(driverClass);
        }
        
        // Default pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setPoolName(databaseName + "-runtime-pool");
        
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        CSDatabase database = new CSDatabase(databaseName, dataSource);
        
        if (!database.testConnection()) {
            dataSource.close();
            throw new CSDataException("Failed to establish connection to database: " + databaseName);
        }
        
        databases.put(databaseName, database);
        dataSources.put(databaseName, dataSource);
        
        logger.info("Database added at runtime: {} -> {}", databaseName, url);
    }
    
    /**
     * Remove database
     */
    public void removeDatabase(String databaseName) {
        CSDatabase database = databases.remove(databaseName);
        HikariDataSource dataSource = dataSources.remove(databaseName);
        
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database removed: {}", databaseName);
        }
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStatistics getStatistics(String databaseName) {
        HikariDataSource dataSource = dataSources.get(databaseName);
        if (dataSource == null) {
            return null;
        }
        
        return new DatabaseStatistics(
            databaseName,
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    /**
     * Test all database connections
     */
    public Map<String, Boolean> testAllConnections() {
        Map<String, Boolean> results = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, CSDatabase> entry : databases.entrySet()) {
            boolean connected = entry.getValue().testConnection();
            results.put(entry.getKey(), connected);
        }
        
        return results;
    }
    
    /**
     * Shutdown all databases
     */
    public void shutdown() {
        logger.info("Shutting down database manager...");
        
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().close();
                logger.debug("Closed database connection pool: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error closing database connection pool: {}", entry.getKey(), e);
            }
        }
        
        databases.clear();
        dataSources.clear();
        
        logger.info("Database manager shutdown completed");
    }
    
    /**
     * Database statistics class
     */
    public static class DatabaseStatistics {
        private final String databaseName;
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int threadsAwaitingConnection;
        
        public DatabaseStatistics(String databaseName, int totalConnections, int activeConnections, 
                                int idleConnections, int threadsAwaitingConnection) {
            this.databaseName = databaseName;
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
        }
        
        // Getters
        public String getDatabaseName() { return databaseName; }
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        
        @Override
        public String toString() {
            return String.format("DatabaseStatistics{database='%s', total=%d, active=%d, idle=%d, waiting=%d}",
                databaseName, totalConnections, activeConnections, idleConnections, threadsAwaitingConnection);
        }
    }
}