package tn.esprit.utils.consultationonline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionArij {
    private static final String URL = "jdbc:mysql://localhost:3306/mediplatform";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static Connection instance;

    private DatabaseConnectionArij() {
    }

    public static synchronized Connection getConnection() {
        try {
            if (instance == null || instance.isClosed()) {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
        return instance;
    }
}
