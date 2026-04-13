package esprit.fx.services;

import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements IService<User> {
    private Connection conn;
    public ServiceUser() {
        conn = MyDB.getInstance().getConnection();
    }
    @Override
    public void ajouter(User user) throws SQLException {
        String req = "INSERT INTO `users` (`email`, `username`, `password`, `created_at`, `is_active`, `phone_number`, `is_verified`, `email_verification_token`, `email_verification_token_expires_at`, `password_reset_token`, `password_reset_token_expires_at`, `failed_attempts`) " +
                "VALUES ('" + user.getEmail() + "', '" + user.getUsername() + "', '" + user.getPassword() + "', '" + user.getCreatedAt() + "', " + user.isActive() + ", '" + user.getPhoneNumber() + "', " + user.isVerified() + ", null, null, null, null, " + user.getFailedAttempts() + ")";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(req);
    }
    //insert into esem ll column kima fel bd php bedhabt

    @Override
    public void modifier(User objet) throws SQLException {
        String req = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `created_at`=?, `is_active`=?, `phone_number`=?, `is_verified`=?, `email_verification_token`=?, `email_verification_token_expires_at`=?, `password_reset_token`=?, `password_reset_token_expires_at`=?, `failed_attempts`=? WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setString(1, objet.getEmail());
        preparedStatement.setString(2, objet.getUsername());
        preparedStatement.setString(3, objet.getPassword());
        preparedStatement.setTimestamp(4, Timestamp.valueOf(objet.getCreatedAt()));
        preparedStatement.setBoolean(5, objet.isActive());
        preparedStatement.setString(6, objet.getPhoneNumber());
        preparedStatement.setBoolean(7, objet.isVerified());
        preparedStatement.setString(8, objet.getEmailVerificationToken());
        preparedStatement.setTimestamp(9, objet.getEmailVerificationTokenExpiresAt() != null ? Timestamp.valueOf(objet.getEmailVerificationTokenExpiresAt()) : null);
        preparedStatement.setString(10, objet.getPasswordResetToken());
        preparedStatement.setTimestamp(11, objet.getPasswordResetTokenExpiresAt() != null ? Timestamp.valueOf(objet.getPasswordResetTokenExpiresAt()) : null);
        preparedStatement.setInt(12, objet.getFailedAttempts());
        preparedStatement.setInt(13, objet.getId());
        preparedStatement.executeUpdate();
    }


    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM `users` WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }
    public User login(String email, String password) throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name " +
                "FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `role` r ON ur.role_id = r.id " +
                "WHERE u.email=? AND u.password=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, email);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User user = new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    null,
                    rs.getBoolean("is_active"),
                    rs.getString("phone_number"),
                    rs.getBoolean("is_verified"),
                    null, null, null, null,
                    rs.getInt("failed_attempts")
            );
            // récupérer le rôle
            List<Role> roles = new ArrayList<>();
            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
            user.setRoles(roles);
            return user; // login réussi
        }
        return null; // email ou password incorrect
    }

    @Override
    public List<User> getAll() throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name " +
                "FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `role` r ON ur.role_id = r.id";
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery(req);
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User user = new User(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    null,
                    rs.getBoolean("is_active"),
                    rs.getString("phone_number"),
                    rs.getBoolean("is_verified"),
                    rs.getString("email_verification_token"),
                    rs.getTimestamp("email_verification_token_expires_at") != null ?
                            rs.getTimestamp("email_verification_token_expires_at").toLocalDateTime() : null,
                    rs.getString("password_reset_token"),
                    rs.getTimestamp("password_reset_token_expires_at") != null ?
                            rs.getTimestamp("password_reset_token_expires_at").toLocalDateTime() : null,
                    rs.getInt("failed_attempts")
            );
            // récupérer le rôle du user
            List<Role> roles = new ArrayList<>();
            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
            user.setRoles(roles);
            users.add(user);
        }
        return users;
    }


}
