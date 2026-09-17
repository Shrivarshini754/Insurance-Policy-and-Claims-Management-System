package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521/FREEPDB1";
    
    // Put your new username and password here:
    private static final String USERNAME = "INSURANCE";
    private static final String PASSWORD = "insurance123";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}