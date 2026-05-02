package esprit.fx.test;

import esprit.fx.utils.MyDB;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class TestCreateAdmins {

    public static void main(String[] args) {
        try (Connection connection = MyDB.getInstance().getConnection()) {
            createAdmin(connection, "admin1", "admin1@medtimefx.com", "admin12026", "12345678");
            createAdmin(connection, "admin2", "admin2@medtimefx.com", "admin22026", "87654321");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création des admins : " + e.getMessage());
        }
    }

    private static void createAdmin(Connection connection, String username, String email, String password, String phoneNumber) {
        try {
            // Hash the password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert into users table
            String insertUserSQL = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, failed_attempts) " +
                                   "VALUES (?, ?, ?, NOW(), ?, ?, ?, ?)";
            try (PreparedStatement userStmt = connection.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, email);
                userStmt.setString(2, username);
                userStmt.setString(3, hashedPassword);
                userStmt.setBoolean(4, true); // is_active
                userStmt.setString(5, phoneNumber);
                userStmt.setBoolean(6, true); // is_verified
                userStmt.setInt(7, 0);        // failed_attempts
                userStmt.executeUpdate();

                // Retrieve the generated user_id
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);

                    // Get or create the ADMIN role
                    int roleId = getOrCreateAdminRole(connection);

                    // Insert into user_roles table
                    String insertUserRoleSQL = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
                    try (PreparedStatement userRoleStmt = connection.prepareStatement(insertUserRoleSQL)) {
                        userRoleStmt.setInt(1, userId);
                        userRoleStmt.setInt(2, roleId);
                        userRoleStmt.executeUpdate();
                    }

                    System.out.println("Admin créé avec succès : " + username);
                } else {
                    System.err.println("Erreur : impossible de récupérer l'ID généré pour l'utilisateur " + username);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'admin " + username + " : " + e.getMessage());
        }
    }

    private static int getOrCreateAdminRole(Connection connection) throws SQLException {
        String selectRoleSQL = "SELECT id FROM roles WHERE name LIKE ?";
        try (PreparedStatement selectRoleStmt = connection.prepareStatement(selectRoleSQL)) {
            selectRoleStmt.setString(1, "%ADMIN%");
            ResultSet resultSet = selectRoleStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        }

        // Role does not exist, create it
        String insertRoleSQL = "INSERT INTO roles (name) VALUES (?)";
        try (PreparedStatement insertRoleStmt = connection.prepareStatement(insertRoleSQL, Statement.RETURN_GENERATED_KEYS)) {
            insertRoleStmt.setString(1, "ADMIN");
            insertRoleStmt.executeUpdate();

            ResultSet generatedKeys = insertRoleStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Erreur : impossible de récupérer l'ID généré pour le rôle ADMIN");
            }
        }
    }
}
