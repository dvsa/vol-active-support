package activesupport.database;

import activesupport.MissingRequiredArgument;
import activesupport.database.credential.DatabaseCredentialType;
import activesupport.database.exception.UnsupportedDatabaseDriverException;
import activesupport.database.url.DbURL;
import activesupport.system.Properties;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.database.search.ExportedKeysSearchCallback;
import org.dbunit.database.search.TablesDependencyHelper;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.csv.CsvDataSetWriter;
import org.dbunit.dataset.csv.CsvURLDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.util.TableFormatter;
import org.dbunit.util.fileloader.CsvDataFileLoader;
import org.dbunit.util.fileloader.DataFileLoader;
import org.dbunit.util.fileloader.FlatXmlDataFileLoader;
import org.dbunit.util.search.DepthFirstSearch;
import org.dbunit.util.search.SearchException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Set;

import static activesupport.file.Files.createFolder;


public class DBUnit {
    private static final Logger logger = LoggerFactory.getLogger(TablesDependencyHelper.class);
    private static IDatabaseConnection dbUnitConnection;
    private static String getEnv = Properties.get("env");

    private static DbURL dbURL = new DbURL();

    public static IDatabaseConnection getDBUnitConnection() throws UnsupportedDatabaseDriverException {
        return getDBUnitConnection(Driver.MYSQL);
    }

    public static IDatabaseConnection getDBUnitConnection(@NotNull Driver driver) throws UnsupportedDatabaseDriverException {
        if (dbUnitConnection == null) {
            try {
                dbUnitConnection = new DatabaseConnection(JDBConnection(driver));
            } catch (DatabaseUnitException e) {
                e.printStackTrace();
            }
        }

        return dbUnitConnection;
    }

    public static IDataSet readCSV(@NotNull File file) throws DataSetException, MalformedURLException {
        return new CsvURLDataSet(file.toURI().toURL());
    }

    public static Connection JDBConnection(@NotNull Driver driver) throws UnsupportedDatabaseDriverException {
        try {
            Class.forName(driver.toString()).newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            throw new UnsupportedDatabaseDriverException();
        }

        Connection dbConnection = null;

        try {
            dbConnection = DriverManager.getConnection(dbURL.getDBUrl(getEnv));
        } catch (MissingRequiredArgument | SQLException e) {
            e.printStackTrace();
        }

        return dbConnection;
    }

    public static IDataSet queryDatabase(@NotNull String query, @NotNull String fileName) throws Exception {
        QueryDataSet dataSet;

        dataSet = new QueryDataSet(getDBUnitConnection());
        dataSet.addTable(fileName, query);
        return dataSet;
    }

    public static ResultSet checkResult(String sql) throws UnsupportedDatabaseDriverException, SQLException {
        PreparedStatement preparedStatement = getDBUnitConnection().getConnection().prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        return resultSet;
    }

    public static void executeUpdateSQL(String sql) throws UnsupportedDatabaseDriverException, SQLException {
        PreparedStatement preparedStatement = getDBUnitConnection().getConnection().prepareStatement(sql);
        preparedStatement.executeUpdate();
    }

    public static IDataSet readXMLFileAsDataSet(String file) {
        DataFileLoader dataFileLoader = new FlatXmlDataFileLoader();
        IDataSet loadedDataSet = dataFileLoader.load(file);
        return loadedDataSet;
    }

    public static IDataSet readCSVFileAsDataSet(String file) {
        DataFileLoader dataFileLoader = new CsvDataFileLoader();
        IDataSet loadedDataset = dataFileLoader.load(file);
        return loadedDataset;
    }

    public static void writeToXml(IDataSet dataSet, @NotNull String directory, @NotNull String fileName) throws IOException, DataSetException {
        FlatXmlDataSet.write(dataSet, new FileOutputStream(createFolder(directory) + "/" + fileName));
    }

    public static void writeToCsv(@NotNull IDataSet dataSet, @NotNull String path) throws DataSetException {
        CsvDataSetWriter.write(dataSet, createFolder(path));
    }

    public static boolean equals(IDataSet dataSet, IDataSet dataSet1, @NotNull String table) {
        boolean equals = true;

        try {
            Assertion.assertEquals(dataSet.getTable(table), dataSet1.getTable(table));
        } catch (DatabaseUnitException e) {
            equals = false;
        }

        return equals;
    }

    public static String loadDBCredential(@NotNull DatabaseCredentialType databaseCredentialType) throws MissingRequiredArgument {
        return Properties.get(databaseCredentialType.toString(), true);
    }

    private static String getDiffBetweenTables(@NotNull ITable expectedTable, @NotNull ITable actualTable) throws DataSetException {
        TableFormatter tableFormatter = new TableFormatter();
        String newline = System.getProperty("line.separator");
        StringBuilder differences = new StringBuilder();
        String.valueOf(differences.append("---Expected Table---" + newline));
        differences.append(tableFormatter.format(expectedTable) + newline);
        String.valueOf(differences.append("---Actual Table---" + newline));
        differences.append(tableFormatter.format(actualTable) + newline);

        return differences.toString();
    }

    public static IDataSet exportFullDataSet() throws Exception {
        return getDBUnitConnection().createDataSet();
    }

    // dependent tables database export: export table X and all tables that
    // have a PK which is a FK on X, in the right order for insertion
    public static IDataSet dependantTableExport(@NotNull String rootTableName) throws Exception {
        String[] depTableNames = (String[]) dependentTables(rootTableName).toArray();
        return getDBUnitConnection().createDataSet(depTableNames);
    }

    public static Set dependentTables(@NotNull String rootTable) throws UnsupportedDatabaseDriverException, SearchException {
        return TablesDependencyHelper.getDirectDependsOnTables(getDBUnitConnection(), rootTable);
    }

    public static Set dependentTables(@NotNull String rootTable, int depth) throws SearchException, UnsupportedDatabaseDriverException {
        logger.debug("getDirectDependsOnTables(connection={}, tableName={}) - start", getDBUnitConnection(), rootTable);
        ExportedKeysSearchCallback callback = new ExportedKeysSearchCallback(getDBUnitConnection());
        DepthFirstSearch search = new DepthFirstSearch(depth);
        Set tables = search.search(new String[]{rootTable}, callback);
        return tables;
    }

    public static void assertEquals(@NotNull IDataSet expectedDataSet, @NotNull IDataSet actualDataSet, @NotNull String table) throws Exception {
        try {
            Assertion.assertEquals(expectedDataSet.getTable(table), expectedDataSet.getTable(table));
        } catch (AssertionError e) {
            throw new DatabaseUnitException(getDiffBetweenTables(expectedDataSet.getTable(table), expectedDataSet.getTable(table)), e);
        }
    }

    public static boolean include(@NotNull ITable table, @NotNull String column, @NotNull String value) throws DataSetException {
        boolean foundMatch = false;
        int size = table.getRowCount();

        for (int i = 0; i < size; i++) {
            if (String.valueOf(table.getValue(i, column)).equals(value)) {
                foundMatch = true;
            }
        }

        return foundMatch;
    }

}

