package esprit.fx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MyDB {
    private String url;
    private String user;
    private String password;
    private Connection connection;
    private static MyDB instance;

    private MyDB() {
        loadDatabaseConfig();
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion à la base de données 'mediplatform' réussie !");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void loadDatabaseConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/mediplatform");
                this.user = props.getProperty("db.user", "root");
                this.password = props.getProperty("db.password", "");
            } else {
                // Valeurs par défaut si le fichier n'existe pas
                this.url = "jdbc:mysql://localhost:3306/mediplatform_test_test";
                this.user = "root";
                this.password = "";
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
            // Utiliser les valeurs par défaut
            this.url = "jdbc:mysql://localhost:3306/mediplatform_test_test";
            this.user = "root";
            this.password = "";
        }
    }

    public Connection getConnection() {
        try {
            // Vérifier si la connexion est toujours valide
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la reconnexion : " + e.getMessage());
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion fermée.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}
