package esprit.fx.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static final String DB_NAME = "mediplatform_test_test";
    private static final String URL =
            "jdbc:mysql://localhost:3306/" + DB_NAME
            + "?useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=UTC"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDB instance;

    private MyDB() {
        connection = createConnection();
    }

    private static Connection createConnection() {
        try {
            Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
            // Explicitly set the catalog so no other DB can bleed in
            c.setCatalog(DB_NAME);
            System.out.println("[MyDB] Connected to " + DB_NAME);
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("[MyDB] Cannot connect: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a valid connection, always pointing to {@value DB_NAME}.
     * If the connection is closed, stale, or pointing to the wrong catalog,
     * a fresh one is created.
     */
    public Connection getConnection() {
        try {
            boolean needsReset = connection == null
                    || connection.isClosed()
                    || !connection.isValid(2)
                    || !DB_NAME.equals(connection.getCatalog());

            if (needsReset) {
                try { if (connection != null) connection.close(); } catch (Exception ignored) {}
                connection = createConnection();
            }
        } catch (SQLException e) {
            // isValid / isClosed threw — force reconnect
            connection = createConnection();
        }
        return connection;
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }
}
