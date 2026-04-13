package esprit.fx.services;
import org.mindrot.jbcrypt.BCrypt;

import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.utils.MyDB;
import org.mindrot.jbcrypt.BCrypt;

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

        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);

        // 🔐 Hash du mot de passe uniquement à la création
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());
        ps.setString(3, hashedPassword);
        ps.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));
        ps.setBoolean(5, user.isActive());
        ps.setString(6, user.getPhoneNumber());
        ps.setBoolean(7, user.isVerified());
        ps.setString(8, null);
        ps.setTimestamp(9, null);
        ps.setString(10, null);
        ps.setTimestamp(11, null);
        ps.setInt(12, user.getFailedAttempts());

        ps.executeUpdate();
    }
    //insert into esem ll column kima fel bd php bedhabt

    @Override
    public void modifier(User user) throws SQLException {

        boolean updatePassword = (user.getPassword() != null && !user.getPassword().isEmpty());

        String req;

        if (updatePassword) {
            req = "UPDATE users SET email=?, username=?, password=?, created_at=?, is_active=?, phone_number=?, is_verified=?, email_verification_token=?, email_verification_token_expires_at=?, password_reset_token=?, password_reset_token_expires_at=?, failed_attempts=? WHERE id=?";
        } else {
            req = "UPDATE users SET email=?, username=?, created_at=?, is_active=?, phone_number=?, is_verified=?, email_verification_token=?, email_verification_token_expires_at=?, password_reset_token=?, password_reset_token_expires_at=?, failed_attempts=? WHERE id=?";
        }

        PreparedStatement ps = conn.prepareStatement(req);

        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());

        int index = 3;

        if (updatePassword) {
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            ps.setString(index++, hashedPassword);
        }

        ps.setTimestamp(index++, Timestamp.valueOf(user.getCreatedAt()));
        ps.setBoolean(index++, user.isActive());
        ps.setString(index++, user.getPhoneNumber());
        ps.setBoolean(index++, user.isVerified());
        ps.setString(index++, user.getEmailVerificationToken());
        ps.setTimestamp(index++, user.getEmailVerificationTokenExpiresAt() != null
                ? Timestamp.valueOf(user.getEmailVerificationTokenExpiresAt()) : null);
        ps.setString(index++, user.getPasswordResetToken());
        ps.setTimestamp(index++, user.getPasswordResetTokenExpiresAt() != null
                ? Timestamp.valueOf(user.getPasswordResetTokenExpiresAt()) : null);
        ps.setInt(index++, user.getFailedAttempts());

        ps.setInt(index, user.getId());

        ps.executeUpdate();
    }


    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM `users` WHERE `id`=?";
        PreparedStatement preparedStatement = conn.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }
    public User login(String username, String password) throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name " +
                "FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `roles` r ON ur.role_id = r.id " +
                "WHERE u.username=? AND u.password=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, username);
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
            List<Role> roles = new ArrayList<>();
            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
            user.setRoles(roles);
            return user;
        }
        return null;
    }

    @Override
    public List<User> getAll() throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name " +
                "FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `roles` r ON ur.role_id = r.id";
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
