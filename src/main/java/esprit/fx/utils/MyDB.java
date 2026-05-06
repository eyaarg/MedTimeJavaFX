package esprit.fx.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    private final String url="jdbc:mysql://localhost:3306/mediplatform_test_test";
    private final String user="root";
    private final String password="";
    private Connection connection;
    private static MyDB instance;

    private MyDB() {
        try {
            connection= DriverManager.getConnection(url,user,password);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("Reconnected to database successfully");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se reconnecter : " + e.getMessage());
        }
        return connection;
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance=new MyDB();
        }
        return instance;
    }
}
