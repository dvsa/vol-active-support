package activesupport.database.queries;

/**
 * Consolidated SQL queries for VOL testing projects.
 * Contains commonly used SQL statements for test data management, user operations,
 * and licence/application queries across performance, functional, and security testing.
 */
public class VolSqlQueries {

    // ========================================
    // User Management Queries
    // ========================================

    /**
     * Gets users for performance testing with specified limit.
     * 
     * @param userLimit The maximum number of users to retrieve
     * @return SQL query string
     */
    public static String getUsersForTesting(int userLimit) {
        return "SELECT DISTINCT\n" +
                "  u.login_id as Username, p.family_name as Forename\n" +
                "FROM\n" +
                "   OLCS_RDS_OLCSDB.licence l\n" +
                "       JOIN\n" +
                "   OLCS_RDS_OLCSDB.organisation_user ou ON ou.organisation_id = l.organisation_id\n" +
                "       JOIN\n" +
                "   OLCS_RDS_OLCSDB.user u ON u.id = ou.user_id\n" +
                "       JOIN\n" +
                "   OLCS_RDS_OLCSDB.user_role ur ON u.id = ur.user_id\n" +
                "       JOIN\n" +
                "   OLCS_RDS_OLCSDB.contact_details AS con ON u.contact_details_id = con.id\n" +
                "       JOIN\n" +
                "   OLCS_RDS_OLCSDB.person AS p ON con.person_id = p.id\n" +
                "WHERE\n" +
                "       u.login_id NOT REGEXP '_[0-9]'\n" +
                "       AND u.login_id NOT LIKE '% %'\n" +
                "       AND u.account_disabled = '0'\n" +
                "       AND l.status IN ('lsts_valid', 'lsts_curtailed', 'lsts_suspended')\n" +
                "       AND l.goods_or_psv IN ('lcat_gv','lcat_psv')\n" +
                "       AND l.licence_type IN ('ltyp_sn')\n" +
                "       AND ur.role_id IN (25, 26)\n" +
                "       AND u.login_id REGEXP '^[A-Za-z0-9]+$'\n" +
                String.format("LIMIT %d;", userLimit);
    }

    /**
     * Gets users with their temporary passwords from the temp_user_passwords table.
     * Used for performance testing with pre-created users.
     * 
     * @return SQL query string
     */
    public static String getUsersWithTempPasswords() {
        return "SELECT u.login_id as Username, p.forename as Forename, u.id as userId,\n" +
                "       cd.email_address as emailAddress, p.family_name as familyName,\n" +
                "       tmp.temp_password as Password\n" +
                "FROM OLCS_RDS_OLCSDB.user u\n" +
                "JOIN OLCS_RDS_OLCSDB.contact_details cd ON u.contact_details_id = cd.id\n" +
                "JOIN OLCS_RDS_OLCSDB.person p ON cd.person_id = p.id\n" +
                "JOIN OLCS_RDS_OLCSDB.temp_user_passwords tmp ON u.login_id = tmp.user_id\n" +
                "WHERE u.account_disabled = 0\n" +
                "ORDER BY tmp.created_at DESC;";
    }

    /**
     * Gets internal users (DVSA staff) for testing.
     * 
     * @return SQL query string
     */
    public static String getInternalUsers() {
        return """
        SELECT login_id 
        FROM OLCS_RDS_OLCSDB.user 
        WHERE id IN (
            SELECT user_id 
            FROM OLCS_RDS_OLCSDB.user_role 
            WHERE role_id IN (23, 24, 33)
        )
        AND deleted_date IS NULL 
        AND created_on < 20161201
        ORDER BY RAND()
        LIMIT 100
        """;
    }

    // ========================================
    // Temporary Password Management
    // ========================================

    /**
     * Creates the temporary password table for performance testing.
     * 
     * @return SQL query string
     */
    public static String createTempPasswordTable() {
        return "CREATE TABLE IF NOT EXISTS OLCS_RDS_OLCSDB.temp_user_passwords (\n" +
                "    user_id VARCHAR(255) PRIMARY KEY,\n" +
                "    temp_password VARCHAR(255) NOT NULL,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    INDEX idx_created_at (created_at)\n" +
                ");";
    }

    /**
     * Inserts or updates a temporary password for a user.
     * 
     * @return SQL query string (parameterized)
     */
    public static String insertTempPassword() {
        return "INSERT INTO OLCS_RDS_OLCSDB.temp_user_passwords (user_id, temp_password) " +
                "VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE temp_password = VALUES(temp_password), created_at = CURRENT_TIMESTAMP;";
    }

    /**
     * Clears all temporary passwords.
     * 
     * @return SQL query string
     */
    public static String clearTempPasswords() {
        return "DELETE FROM OLCS_RDS_OLCSDB.temp_user_passwords " +
                "WHERE created_at <= NOW();";
    }

    /**
     * Drops the temporary password table.
     * 
     * @return SQL query string
     */
    public static String dropTempPasswordTable() {
        return "DROP TABLE IF EXISTS OLCS_RDS_OLCSDB.temp_user_passwords;";
    }

    // ========================================
    // Organisation and Licence Queries
    // ========================================

    /**
     * Gets trading names for testing purposes.
     * 
     * @return SQL query string
     */
    public static String getTradingNames() {
        return """
        SELECT name 
        FROM OLCS_RDS_OLCSDB.trading_name 
        WHERE licence_id IN (
            SELECT id 
            FROM OLCS_RDS_OLCSDB.licence 
            WHERE status = 'lsts_valid'
        )
        AND deleted_date IS NULL
        ORDER BY RAND() 
        LIMIT 100
        """;
    }

    /**
     * Gets licence details with organisation, type, status, and traffic area information.
     * 
     * @return SQL query string
     */
    public static String getLicenceDetails() {
        return """
        SELECT 
            l.lic_no as 'Licence number', 
            ro.description as 'Organisation', 
            rlt.description as 'Licence type', 
            rls.description as 'Licence status', 
            ta.name as 'Traffic Area', 
            rgp.description as 'Goods or PSV'
        FROM OLCS_RDS_OLCSDB.licence l 
        JOIN OLCS_RDS_OLCSDB.organisation o ON l.organisation_id = o.id
        JOIN OLCS_RDS_OLCSDB.ref_data ro ON o.type = ro.id
        JOIN OLCS_RDS_OLCSDB.ref_data rlt ON l.licence_type = rlt.id
        JOIN OLCS_RDS_OLCSDB.ref_data rls ON l.status = rls.id
        JOIN OLCS_RDS_OLCSDB.traffic_area ta ON l.traffic_area_id = ta.id
        JOIN OLCS_RDS_OLCSDB.ref_data rgp ON l.goods_or_psv = rgp.id
        WHERE l.deleted_date IS NULL
        AND l.status NOT IN ('lsts_cancelled', 'lsts_not_submitted', 'lsts_unlicenced', 'lsts_withdrawn')
        AND l.created_on > DATE_SUB(SYSDATE(), INTERVAL 1 YEAR)
        ORDER BY RAND()
        LIMIT 1;
        """;
    }

    // ========================================
    // Application Management Queries  
    // ========================================

    /**
     * Cancels applications created today (for cleanup purposes).
     * 
     * @param applicationLimit The maximum number of applications to cancel
     * @return SQL query string
     */
    public static String cancelApplications(int applicationLimit) {
        return "UPDATE OLCS_RDS_OLCSDB.application \n " +
                "SET application.status = 'apsts_cancelled' \n " +
                "WHERE\n" +
                "  licence_id is not null\n" +
                "  AND created_on >= cast(sysdate() AS Date)\n" +
                "  ORDER BY last_modified_on\n" +
                String.format("DESC LIMIT %d;", applicationLimit);
    }

    // ========================================
    // PSV (Public Service Vehicle) Queries
    // ========================================

    /**
     * Gets the first PSV disc number for a licence.
     * 
     * @param licenceId The licence ID
     * @return SQL query string (parameterized)
     */
    public static String getFirstPsvDiscNumber(String licenceId) {
        return String.format(
                "SELECT disc_no FROM OLCS_RDS_OLCSDB.psv_disc WHERE licence_id = '%s' AND ceased_date IS NULL;", 
                licenceId
        );
    }

    // ========================================
    // Utility Queries
    // ========================================

    /**
     * Gets database connection test query.
     * 
     * @return SQL query string
     */
    public static String connectionTest() {
        return "SELECT 1 as test_result;";
    }

    /**
     * Gets current timestamp from database.
     * 
     * @return SQL query string
     */
    public static String getCurrentTimestamp() {
        return "SELECT NOW() as current_timestamp;";
    }

    /**
     * Gets database version information.
     * 
     * @return SQL query string
     */
    public static String getDatabaseVersion() {
        return "SELECT VERSION() as db_version;";
    }
}