# Database Utilities Migration Guide

This guide shows how to migrate from existing database utilities in vol-app-performance-suite and vol-functional-tests to the new consolidated utilities in vol-active-support.

## Overview

The new utilities provide three levels of abstraction:

1. **DatabaseConnectionManager** - Low-level connection management
2. **VolSqlQueries** - SQL query definitions
3. **VolDatabaseUtils** - High-level utility methods combining both

## Migration Examples

### From vol-app-performance-suite TestSetup

**Old code:**
```java
// From TestSetup.java
public static void createTempPasswordTable() {
    String tempPasswordTableQuery = "CREATE TABLE IF NOT EXISTS temp_user_passwords ...";
    try (Connection connection = getConnection()) {
        Statement statement = connection.createStatement();
        statement.execute(tempPasswordTableQuery);
    }
}

public static void storePassword(String username, String password) {
    String insertQuery = "INSERT INTO temp_user_passwords ...";
    try (Connection connection = getConnection()) {
        PreparedStatement statement = connection.prepareStatement(insertQuery);
        statement.setString(1, username);
        statement.setString(2, password);
        statement.executeUpdate();
    }
}
```

**New code:**
```java
// Using VolDatabaseUtils
import activesupport.database.utils.VolDatabaseUtils;

public static void createTempPasswordTable() {
    VolDatabaseUtils.createTempPasswordTable("int"); // or "qa", "local"
}

public static void storePassword(String username, String password) {
    VolDatabaseUtils.storeTempPassword("int", username, password);
}
```

### From vol-functional-tests DBUtils

**Old code:**
```java
// From DBUtils.java
public List<Map<String, Object>> getUsers() {
    String query = "SELECT login_id, email, family_name FROM user ...";
    List<Map<String, Object>> results = new ArrayList<>();
    try (Connection connection = getConnection()) {
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        // Manual result set processing...
    }
    return results;
}
```

**New code:**
```java
// Using VolDatabaseUtils
import activesupport.database.utils.VolDatabaseUtils;

public List<Map<String, Object>> getUsers() {
    return VolDatabaseUtils.getUsersForTesting("qa", 100);
}

// Or using the lower-level query method:
public List<Map<String, Object>> getUsers() {
    return VolDatabaseUtils.executeQuery("qa", VolSqlQueries.getUsersForTesting(100));
}
```

## Environment Configuration

Set these environment variables or system properties:

```bash
# For different environments
export VOL_DB_ENVIRONMENT=int   # or "qa", "local", "preprod"

# For local development (optional, will use Secrets Manager if not set)
export VOL_DB_HOST=localhost
export VOL_DB_PORT=5432
export VOL_DB_NAME=vol
export VOL_DB_USERNAME=voluser
export VOL_DB_PASSWORD=password
```

## Dependency Updates

Add to your pom.xml:

```xml
<dependency>
    <groupId>org.dvsa.testing.lib</groupId>
    <artifactId>active-support</artifactId>
    <version>2.11.4</version> <!-- Use latest version -->
</dependency>
```

## Migration Steps

1. **Update dependencies** - Add active-support dependency
2. **Replace connection logic** - Use `DatabaseConnectionManager.getConnection(environment)`
3. **Replace SQL queries** - Use methods from `VolSqlQueries` 
4. **Use high-level methods** - Replace manual query execution with `VolDatabaseUtils` methods
5. **Update environment configuration** - Set `VOL_DB_ENVIRONMENT` variable
6. **Remove old code** - Delete old TestSetup and DBUtils classes

## Benefits

- **Consistency** - Same database logic across all VOL testing projects
- **Maintainability** - Single place to update SQL queries and connection logic
- **Environment management** - Unified environment-based configuration
- **Error handling** - Consistent logging and exception handling
- **Security** - Centralized credential management via AWS Secrets Manager

## Advanced Usage

For custom queries not covered by the convenience methods:

```java
// Custom parameterized query
List<Map<String, Object>> results = VolDatabaseUtils.executeQuery(
    "qa", 
    "SELECT * FROM licence WHERE status = ? AND created > ?",
    "Valid",
    LocalDateTime.now().minusDays(30)
);

// Custom update
int affected = VolDatabaseUtils.executeUpdate(
    "qa",
    "UPDATE application SET status = ? WHERE id = ?",
    "cancelled",
    12345
);
```