package esprit.fx.services;

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

    @Override
    public List<User> getAll() throws SQLException {
        String req="SELECT * FROM `users` ";
        Statement statement = conn.createStatement();
        statement.executeQuery(req);
        ResultSet resultSet = statement.getResultSet();
        List<User> users = new ArrayList<User>();
        while (resultSet.next()) {
            User user = new User(resultSet.getInt("id"),
                    resultSet.getString("email"),
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                    resultSet.getBoolean("is_active"),
                    resultSet.getString("phone_number"),
                    resultSet.getBoolean("is_verified"),
                    resultSet.getString("email_verification_token"),
                    resultSet.getTimestamp("email_verification_token_expires_at") != null ? resultSet.getTimestamp("email_verification_token_expires_at").toLocalDateTime() : null,
                    resultSet.getString("password_reset_token"),
                    resultSet.getTimestamp("password_reset_token_expires_at") != null ? resultSet.getTimestamp("password_reset_token_expires_at").toLocalDateTime() : null,
                    resultSet.getInt("failed_attempts")
            );
            users.add(user);
        }
        return users;
    }

}
