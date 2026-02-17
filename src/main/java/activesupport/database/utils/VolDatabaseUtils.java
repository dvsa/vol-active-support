package activesupport.database.utils;

import activesupport.database.connection.DatabaseConnectionManager;
import activesupport.database.queries.VolSqlQueries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * High-level database utility class that combines connection management and query execution.
 * Provides convenience methods for common database operations across VOL testing projects.
 */
public class VolDatabaseUtils {
    
    private static final Logger LOGGER = LogManager.getLogger(VolDatabaseUtils.class);
    
    /**
     * Executes a SELECT query and returns results as a list of maps.
     * Each map represents a row with column names as keys.
     * 
     * @param environment The target environment
     * @param sqlQuery The SQL SELECT query
     * @return List of maps representing query results
     * @throws SQLException if query execution fails
     */
    public static List<Map<String, Object>> executeQuery(String environment, String sqlQuery) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection(environment);
             PreparedStatement statement = connection.prepareStatement(sqlQuery);
             ResultSet resultSet = statement.executeQuery()) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
            LOGGER.debug("Query returned {} rows for environment: {}", results.size(), environment);
        } catch (SQLException e) {
            LOGGER.error("Failed to execute query in environment: {}", environment, e);
            throw e;
        }
        
        return results;
    }
    
    /**
     * Executes a parameterized SELECT query.
     * 
     * @param environment The target environment
     * @param sqlQuery The SQL SELECT query with parameters (?)
     * @param parameters The parameter values
     * @return List of maps representing query results
     * @throws SQLException if query execution fails
     */
    public static List<Map<String, Object>> executeQuery(String environment, String sqlQuery, Object... parameters) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = DatabaseConnectionManager.getConnection(environment);
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
            LOGGER.debug("Parameterized query returned {} rows for environment: {}", results.size(), environment);
        } catch (SQLException e) {
            LOGGER.error("Failed to execute parameterized query in environment: {}", environment, e);
            throw e;
        }
        
        return results;
    }
    
    /**
     * Executes an UPDATE, INSERT, or DELETE statement.
     * 
     * @param environment The target environment
     * @param sqlStatement The SQL statement
     * @return Number of affected rows
     * @throws SQLException if execution fails
     */
    public static int executeUpdate(String environment, String sqlStatement) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection(environment);
             PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
            
            int affectedRows = statement.executeUpdate();
            LOGGER.info("Statement affected {} rows in environment: {}", affectedRows, environment);
            return affectedRows;
            
        } catch (SQLException e) {
            LOGGER.error("Failed to execute update in environment: {}", environment, e);
            throw e;
        }
    }
    
    /**
     * Executes a parameterized UPDATE, INSERT, or DELETE statement.
     * 
     * @param environment The target environment
     * @param sqlStatement The SQL statement with parameters (?)
     * @param parameters The parameter values
     * @return Number of affected rows
     * @throws SQLException if execution fails
     */
    public static int executeUpdate(String environment, String sqlStatement, Object... parameters) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.getConnection(environment);
             PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            
            int affectedRows = statement.executeUpdate();
            LOGGER.info("Parameterized statement affected {} rows in environment: {}", affectedRows, environment);
            return affectedRows;
            
        } catch (SQLException e) {
            LOGGER.error("Failed to execute parameterized update in environment: {}", environment, e);
            throw e;
        }
    }
    
    // ========================================
    // Convenience Methods for Common Operations
    // ========================================
    
    /**
     * Gets users for performance testing.
     * 
     * @param environment The target environment
     * @param userLimit Maximum number of users to retrieve
     * @return List of user maps
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getUsersForTesting(String environment, int userLimit) throws SQLException {
        return executeQuery(environment, VolSqlQueries.getUsersForTesting(userLimit));
    }
    
    /**
     * Gets users with temporary passwords.
     * 
     * @param environment The target environment
     * @return List of user maps with temp passwords
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getUsersWithTempPasswords(String environment) throws SQLException {
        return executeQuery(environment, VolSqlQueries.getUsersWithTempPasswords());
    }
    
    /**
     * Gets internal (DVSA) users.
     * 
     * @param environment The target environment
     * @return List of internal user maps
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getInternalUsers(String environment) throws SQLException {
        return executeQuery(environment, VolSqlQueries.getInternalUsers());
    }
    
    /**
     * Gets trading names for testing.
     * 
     * @param environment The target environment
     * @return List of trading name maps
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getTradingNames(String environment) throws SQLException {
        return executeQuery(environment, VolSqlQueries.getTradingNames());
    }
    
    /**
     * Gets licence details.
     * 
     * @param environment The target environment
     * @return List of licence detail maps
     * @throws SQLException if query fails
     */
    public static List<Map<String, Object>> getLicenceDetails(String environment) throws SQLException {
        return executeQuery(environment, VolSqlQueries.getLicenceDetails());
    }
    
    /**
     * Creates the temporary password table for performance testing.
     * 
     * @param environment The target environment
     * @throws SQLException if table creation fails
     */
    public static void createTempPasswordTable(String environment) throws SQLException {
        executeUpdate(environment, VolSqlQueries.createTempPasswordTable());
        LOGGER.info("Created temporary password table in environment: {}", environment);
    }
    
    /**
     * Stores a temporary password for a user.
     * 
     * @param environment The target environment
     * @param username The username
     * @param tempPassword The temporary password
     * @throws SQLException if insertion fails
     */
    public static void storeTempPassword(String environment, String username, String tempPassword) throws SQLException {
        executeUpdate(environment, VolSqlQueries.insertTempPassword(), username, tempPassword);
        LOGGER.debug("Stored temp password for user: {} in environment: {}", username, environment);
    }
    
    /**
     * Clears all temporary passwords.
     * 
     * @param environment The target environment
     * @return Number of passwords cleared
     * @throws SQLException if deletion fails
     */
    public static int clearTempPasswords(String environment) throws SQLException {
        int cleared = executeUpdate(environment, VolSqlQueries.clearTempPasswords());
        LOGGER.info("Cleared {} temporary passwords in environment: {}", cleared, environment);
        return cleared;
    }
    
    /**
     * Cancels applications for cleanup purposes.
     * 
     * @param environment The target environment
     * @param applicationLimit Maximum number of applications to cancel
     * @return Number of applications cancelled
     * @throws SQLException if update fails
     */
    public static int cancelApplications(String environment, int applicationLimit) throws SQLException {
        int cancelled = executeUpdate(environment, VolSqlQueries.cancelApplications(applicationLimit));
        LOGGER.info("Cancelled {} applications in environment: {}", cancelled, environment);
        return cancelled;
    }
    
    /**
     * Tests database connectivity for an environment.
     * 
     * @param environment The target environment
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection(String environment) {
        return DatabaseConnectionManager.testConnection(environment);
    }
    
    /**
     * Gets first PSV disc number for a licence.
     * 
     * @param environment The target environment
     * @param licenceId The licence ID
     * @return PSV disc number, or -1 if not found
     * @throws SQLException if query fails
     */
    public static int getFirstPsvDiscNumber(String environment, String licenceId) throws SQLException {
        List<Map<String, Object>> results = executeQuery(environment, VolSqlQueries.getFirstPsvDiscNumber(licenceId));
        if (!results.isEmpty()) {
            Object discNo = results.get(0).get("disc_no");
            return discNo instanceof Number ? ((Number) discNo).intValue() : -1;
        }
        return -1;
    }
}