package esprit.fx.services;
import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.utils.MyDB;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ServiceUser implements IService<User> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    private Connection conn;
    public ServiceUser() {
        conn = MyDB.getInstance().getConnection();
    }
    @Override
    public void ajouter(User user) throws SQLException {
        validateUserForCreate(user);

        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req);


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
        validateUserForUpdate(user);

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

    public User registerUser(User user, String roleName) throws SQLException {
        validateUserForCreate(user);

        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
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

        int createdUserId;
        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
            if (!generatedKeys.next()) {
                throw new SQLException("Impossible de recuperer l'id du nouvel utilisateur.");
            }
            createdUserId = generatedKeys.getInt(1);
        }

        int roleId = resolveRoleId(roleName);
        String resolvedRoleName = resolveRoleNameById(roleId);

        try (PreparedStatement rolePs = conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            rolePs.setInt(1, createdUserId);
            rolePs.setInt(2, roleId);
            rolePs.executeUpdate();
        }

        user.setId(createdUserId);
        user.setPassword(hashedPassword);
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(roleId, resolvedRoleName));
        user.setRoles(roles);
        return user;
    }

    public void updateUserRole(int userId, String roleName) throws SQLException {
        int roleId = resolveRoleId(roleName);

        try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM user_roles WHERE user_id=?")) {
            deletePs.setInt(1, userId);
            deletePs.executeUpdate();
        }

        try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            insertPs.setInt(1, userId);
            insertPs.setInt(2, roleId);
            insertPs.executeUpdate();
        }
    }

    public List<String> getAvailableRoleNames() throws SQLException {
        List<String> roleNames = new ArrayList<>();
        roleNames.add("Patient");
        roleNames.add("Medecin");
        return roleNames;
    }


    public User login(String email, String rawPassword) throws SQLException {
        String query = "SELECT u.*, r.id as role_id, r.name as role_name FROM users u " +
                "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                "LEFT JOIN roles r ON ur.role_id = r.id " +
                "WHERE u.email = ? AND u.is_active = true";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        User user = null;
        List<Role> roles = new ArrayList<>();
        boolean passwordValid = false;

        while (rs.next()) {
            if (user == null) {
                String storedPassword = rs.getString("password");
                passwordValid = BCrypt.checkpw(rawPassword, storedPassword);
                if (!passwordValid) {
                    incrementFailedAttempts(email);
                    return null;
                }

                resetFailedAttempts(email);

                user = new User(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("username"),
                        storedPassword,
                        null,
                        rs.getBoolean("is_active"),
                        rs.getString("phone_number"),
                        rs.getBoolean("is_verified"),
                        null, null, null, null,
                        rs.getInt("failed_attempts")
                );
            }

            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
        }

        if (user != null) {
            user.setRoles(roles);
        }

        return user;
    }

    private void incrementFailedAttempts(String email) throws SQLException {
        String query = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, email);
        ps.executeUpdate();

        String checkQuery = "SELECT failed_attempts FROM users WHERE email = ?";
        ps = conn.prepareStatement(checkQuery);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next() && rs.getInt("failed_attempts") >= 5) {
            lockAccount(email);
        }
    }

    private void resetFailedAttempts(String email) throws SQLException {
        String query = "UPDATE users SET failed_attempts = 0 WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, email);
        ps.executeUpdate();
    }

    private void lockAccount(String email) throws SQLException {
        String query = "UPDATE users SET is_active = false WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, email);
        ps.executeUpdate();
        sendAccountLockedEmail(email);
    }

    private void sendAccountLockedEmail(String email) {
        // Implementation for sending account locked email
    }

    public boolean verifyEmailToken(String token) throws SQLException {
        String query = "SELECT id, email_verification_token_expires_at FROM users WHERE email_verification_token = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, token);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Timestamp expiresAt = rs.getTimestamp("email_verification_token_expires_at");
            if (expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                String updateQuery = "UPDATE users SET is_verified = true, is_active = true, email_verification_token = NULL, email_verification_token_expires_at = NULL WHERE email_verification_token = ?";
                ps = conn.prepareStatement(updateQuery);
                ps.setString(1, token);
                ps.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void resendVerificationEmail(String email) throws SQLException {
        String token = java.util.UUID.randomUUID().toString();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        String query = "UPDATE users SET email_verification_token = ?, email_verification_token_expires_at = ? WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, token);
        ps.setTimestamp(2, expiresAt);
        ps.setString(3, email);
        ps.executeUpdate();

        sendVerificationEmail(email, token);
    }

    private void sendVerificationEmail(String email, String token) {
        // Implementation for sending verification email
    }

    public void requestPasswordReset(String email) throws SQLException {
        String token = java.util.UUID.randomUUID().toString();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 60 * 60 * 1000);

        String query = "UPDATE users SET password_reset_token = ?, password_reset_token_expires_at = ? WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, token);
        ps.setTimestamp(2, expiresAt);
        ps.setString(3, email);
        ps.executeUpdate();

        sendPasswordResetEmail(email, token);
    }

    private void sendPasswordResetEmail(String email, String token) {
        // Implementation for sending password reset email
    }

    public boolean resetPassword(String token, String newPassword) throws SQLException {
        String query = "SELECT id, password_reset_token_expires_at FROM users WHERE password_reset_token = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, token);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Timestamp expiresAt = rs.getTimestamp("password_reset_token_expires_at");
            if (expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                String updateQuery = "UPDATE users SET password = ?, password_reset_token = NULL, password_reset_token_expires_at = NULL WHERE password_reset_token = ?";
                ps = conn.prepareStatement(updateQuery);
                ps.setString(1, hashedPassword);
                ps.setString(2, token);
                ps.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void unlockAccount(int userId) throws SQLException {
        String query = "UPDATE users SET failed_attempts = 0, is_active = true WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, userId);
        ps.executeUpdate();
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

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, storedPassword);
        } catch (IllegalArgumentException ex) {
            // Fallback for existing rows still containing plain-text passwords.
            return rawPassword.equals(storedPassword);
        }
    }

    private void validateUserForCreate(User user) throws SQLException {
        validateCommonFields(user);
        if (!hasText(user.getPassword())) {
            throw new SQLException("Le mot de passe est obligatoire.");
        }
        if (!PASSWORD_PATTERN.matcher(user.getPassword().trim()).matches()) {
            throw new SQLException("Le mot de passe doit contenir des lettres et des chiffres.");
        }
    }

    private void validateUserForUpdate(User user) throws SQLException {
        validateCommonFields(user);
        if (user.getId() <= 0) {
            throw new SQLException("Identifiant utilisateur invalide.");
        }
        if (hasText(user.getPassword()) && !PASSWORD_PATTERN.matcher(user.getPassword().trim()).matches()) {
            throw new SQLException("Le mot de passe doit contenir des lettres et des chiffres.");
        }
    }

    private void validateCommonFields(User user) throws SQLException {
        if (user == null) {
            throw new SQLException("Utilisateur invalide.");
        }

        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber().trim();

        if (username.length() < 3) {
            throw new SQLException("Le username doit contenir au moins 3 caracteres.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new SQLException("Email invalide (format attendu: exemple@domaine.com).");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new SQLException("Le numero de telephone doit contenir exactement 8 chiffres.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int resolveRoleId(String roleName) throws SQLException {
        String normalized = normalizeRoleInput(roleName);

        for (String alias : roleAliases(normalized)) {
            Integer roleId = findRoleId(alias);
            if (roleId != null) {
                return roleId;
            }
        }

        String roleToCreate = "PATIENT".equals(normalized) ? "PATIENT" : "DOCTOR";
        return createRoleAndReturnId(roleToCreate);
    }

    private String normalizeRoleInput(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "PATIENT";
        }

        String value = roleName.trim().toUpperCase();
        if ("MEDECIN".equals(value) || "DOCTOR".equals(value) || "ROLE_DOCTOR".equals(value)) {
            return "DOCTOR";
        }
        if ("PATIENT".equals(value) || "ROLE_PATIENT".equals(value)) {
            return "PATIENT";
        }
        return value;
    }

    private List<String> roleAliases(String normalizedRole) {
        List<String> aliases = new ArrayList<>();
        if ("DOCTOR".equals(normalizedRole)) {
            aliases.add("DOCTOR");
            aliases.add("ROLE_DOCTOR");
            aliases.add("MEDECIN");
            return aliases;
        }

        aliases.add("PATIENT");
        aliases.add("ROLE_PATIENT");
        return aliases;
    }

    private int createRoleAndReturnId(String roleName) throws SQLException {
        String insert = "INSERT INTO roles (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roleName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        Integer existingId = findRoleId(roleName);
        if (existingId != null) {
            return existingId;
        }

        throw new SQLException("Impossible de creer ou recuperer le role: " + roleName);
    }

    private Integer findRoleId(String roleName) throws SQLException {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        String req = "SELECT id FROM roles WHERE UPPER(name)=UPPER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, roleName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private String resolveRoleNameById(int roleId) throws SQLException {
        String req = "SELECT name FROM roles WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return "PATIENT";
    }


}
