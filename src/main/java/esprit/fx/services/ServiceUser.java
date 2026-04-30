package esprit.fx.services;
import esprit.fx.entities.User;
import esprit.fx.entities.Role;
import esprit.fx.utils.MyDB;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
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
        String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());

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
            String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());
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
        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray());

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
        ensureRoleProfiles(user);
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

    public User login(String username, String password) throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name " +
                "FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `roles` r ON ur.role_id = r.id " +
                "WHERE u.username=? OR u.email=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, username);
        ps.setString(2, username);
        ResultSet rs = ps.executeQuery();

        User user = null;
        List<Role> roles = new ArrayList<>();
        boolean passwordValid = false;

        while (rs.next()) {
            if (user == null) {
                String storedPassword = rs.getString("password");
                passwordValid = passwordMatches(password, storedPassword);
                if (!passwordValid) {
                    return null;
                }

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
            ensureRoleProfiles(user);
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

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        try {
            BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), storedPassword);
            return result.verified;
        } catch (IllegalArgumentException ex) {
            // Fallback for existing rows still containing plain-text passwords.
            return rawPassword.equals(storedPassword);
        }
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

    private void ensureRoleProfiles(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            return;
        }

        List<Role> roles = user.getRoles();
        boolean provisioned = false;
        if (roles != null) {
            for (Role role : roles) {
                String normalized = normalizeRoleInput(role != null ? role.getName() : null);
                if ("DOCTOR".equals(normalized)) {
                    ensureDoctorProfile(user.getId());
                    provisioned = true;
                } else if ("PATIENT".equals(normalized)) {
                    ensurePatientProfile(user.getId());
                    provisioned = true;
                }
            }
        }

        if (!provisioned) {
            ensurePatientProfile(user.getId());
        }
    }

    private void ensurePatientProfile(int userId) throws SQLException {
        if (profileExists("SELECT id FROM patients WHERE user_id = ? LIMIT 1", userId)) {
            return;
        }

        String insert = "INSERT INTO patients (created_at, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void ensureDoctorProfile(int userId) throws SQLException {
        if (profileExists("SELECT id FROM doctors WHERE user_id = ? LIMIT 1", userId)) {
            return;
        }

        String insert = "INSERT INTO doctors (created_at, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private boolean profileExists(String sql, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    @Override
    public User afficherParId(int id) throws SQLException {
        User user = null;
        String sql = "SELECT * FROM user WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            user = new User();
            user.setId(rs.getInt("id"));
        }
        return user;
    }

    /**
     * Récupère tous les utilisateurs ayant le rôle DOCTOR
     */
    public List<User> getAllDoctors() throws SQLException {
        List<User> doctors = new ArrayList<>();
        
        String sql = """
            SELECT DISTINCT u.id, u.username, u.email
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            INNER JOIN roles r ON ur.role_id = r.id
            WHERE r.name IN ('DOCTOR', 'ROLE_DOCTOR', 'Medecin', 'MEDECIN')
            AND u.is_active = 1
            ORDER BY u.username
            """;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User doctor = new User();
                doctor.setId(rs.getInt("id"));
                doctor.setUsername(rs.getString("username"));
                doctor.setEmail(rs.getString("email"));
                doctors.add(doctor);
            }
        }
        
        return doctors;
    }

    @Override
    public User afficherParId(int id) throws SQLException {
        User user = null;
        String sql = "SELECT * FROM user WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            user = new User();
            user.setId(rs.getInt("id"));
        }
        return user;
    }


}
