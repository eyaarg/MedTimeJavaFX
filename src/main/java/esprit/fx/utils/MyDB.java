package esprit.fx.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    private final String url = "jdbc:mysql://localhost:3306/mediplatform_test_test?connectTimeout=5000&socketTimeout=5000";
    private final String user = "root";
    private final String password = "";
    private static MyDB instance;

    private MyDB() {
        // Validate connection at startup
        try (Connection test = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a fresh connection every time.
     * Callers are responsible for closing it (use try-with-resources).
     * This avoids stale result sets from a shared connection.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se connecter : " + e.getMessage(), e);
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }
}
