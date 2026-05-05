package esprit.fx.services;
import esprit.fx.entities.Role;
import esprit.fx.entities.User;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.EmailService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ServiceUser implements IService<User> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\d{8}|\\+[1-9]\\d{6,14})$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}0-9_.\\-]{3,80}$");

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(User user) throws SQLException {
        validateUserForCreate(user);
        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(req);
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
        PreparedStatement ps = conn().prepareStatement(req);
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
        ps.setTimestamp(index++, user.getEmailVerificationTokenExpiresAt() != null ? Timestamp.valueOf(user.getEmailVerificationTokenExpiresAt()) : null);
        ps.setString(index++, user.getPasswordResetToken());
        ps.setTimestamp(index++, user.getPasswordResetTokenExpiresAt() != null ? Timestamp.valueOf(user.getPasswordResetTokenExpiresAt()) : null);
        ps.setInt(index++, user.getFailedAttempts());
        ps.setInt(index, user.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("DELETE FROM `users` WHERE `id`=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public User registerUser(User user, String roleName) throws SQLException {
        validateUserForCreate(user);
        String normalizedRole = normalizeRoleInput(roleName);
        boolean isDoctor = "ROLE_PHYSICIAN".equals(normalizedRole);

        // Pour les patients : g├®n├®rer token de v├®rification, is_active=false, is_verified=false
        // Pour les m├®decins : is_active=false (en attente admin), is_verified=true (pas besoin email verif)
        String verificationToken = null;
        Timestamp tokenExpiry = null;
        boolean isActive = false;
        boolean isVerified;
        if (isDoctor) {
            isVerified = true; // m├®decin : pas de v├®rif email, bloqu├® par is_active=false
        } else {
            isVerified = false;
            verificationToken = java.util.UUID.randomUUID().toString();
            tokenExpiry = new Timestamp(System.currentTimeMillis() + 24L * 60 * 60 * 1000);
        }

        String req = "INSERT INTO users (email, username, password, created_at, is_active, phone_number, is_verified, email_verification_token, email_verification_token_expires_at, password_reset_token, password_reset_token_expires_at, failed_attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getUsername());
        ps.setString(3, hashedPassword);
        ps.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));
        ps.setBoolean(5, isActive);
        ps.setString(6, user.getPhoneNumber());
        ps.setBoolean(7, isVerified);
        ps.setString(8, verificationToken);
        ps.setTimestamp(9, tokenExpiry);
        ps.setString(10, null);
        ps.setTimestamp(11, null);
        ps.setInt(12, 0);
        ps.executeUpdate();
        int createdUserId;
        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
            if (!generatedKeys.next()) throw new SQLException("Impossible de recuperer l'id du nouvel utilisateur.");
            createdUserId = generatedKeys.getInt(1);
        }
        int roleId = resolveRoleId(roleName);
        String resolvedRoleName = resolveRoleNameById(roleId);
        try (PreparedStatement rolePs = conn().prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            rolePs.setInt(1, createdUserId);
            rolePs.setInt(2, roleId);
            rolePs.executeUpdate();
        }
        user.setId(createdUserId);
        user.setPassword(hashedPassword);
        user.setActive(isActive);
        user.setVerified(isVerified);
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(roleId, resolvedRoleName));
        user.setRoles(roles);

        // Envoyer email de v├®rification pour les patients
        if (!isDoctor && verificationToken != null) {
            final String tokenFinal    = verificationToken;
            final String emailFinal    = user.getEmail();
            final String usernameFinal = user.getUsername();
            new Thread(() -> {
                System.out.println("Envoi email ├á : " + emailFinal);
                try {
                    EmailService.sendVerificationEmail(emailFinal, usernameFinal, tokenFinal);
                    System.out.println("[ServiceUser] Email v├®rification envoy├® ├á : " + emailFinal);
                } catch (Exception e) {
                    System.err.println("[ServiceUser] ERREUR envoi email v├®rification : " + e.getMessage());
                    e.printStackTrace();
                }
            }, "email-verification-thread").start();
        }
        return user;
    }

    public void updateUserRole(int userId, String roleName) throws SQLException {
        int roleId = resolveRoleId(roleName);
        try (PreparedStatement deletePs = conn().prepareStatement("DELETE FROM user_roles WHERE user_id=?")) {
            deletePs.setInt(1, userId);
            deletePs.executeUpdate();
        }
        try (PreparedStatement insertPs = conn().prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
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

    public User login(String identifier, String password) throws SQLException {
        String query = "SELECT u.*, r.id as role_id, r.name as role_name FROM users u " +
                "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                "LEFT JOIN roles r ON ur.role_id = r.id " +
                "WHERE (u.email = ? OR u.username = ?)";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setString(1, identifier);
        ps.setString(2, identifier);
        ResultSet rs = ps.executeQuery();

        User user = null;
        List<Role> roles = new ArrayList<>();

        while (rs.next()) {
            if (user == null) {
                String storedPassword = rs.getString("password");
                if (!passwordMatches(password, storedPassword)) {
                    incrementFailedAttempts(identifier);
                    return null; // mot de passe incorrect
                }
                // Mot de passe correct : construire le user avec tous ses flags
                boolean isActive   = rs.getBoolean("is_active");
                boolean isVerified = rs.getBoolean("is_verified");
                user = new User(
                        rs.getInt("id"), rs.getString("email"), rs.getString("username"),
                        storedPassword, null, isActive,
                        rs.getString("phone_number"), isVerified,
                        rs.getString("email_verification_token"), null,
                        null, null, rs.getInt("failed_attempts")
                );
                user.setCreatedAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            }
            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
        }

        if (user == null) return null; // identifiant introuvable

        user.setRoles(roles);

        // R├®initialiser les tentatives seulement si le compte est actif et v├®rifi├®
        if (user.isActive() && user.isVerified()) {
            resetFailedAttempts(identifier);
        }

        return user; // retourner le user dans tous les cas (controller g├¿re les cas)
    }

    private void incrementFailedAttempts(String identifier) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE email=? OR username=?");
        ps.setString(1, identifier);
        ps.setString(2, identifier);
        ps.executeUpdate();
        PreparedStatement checkPs = conn().prepareStatement(
                "SELECT failed_attempts FROM users WHERE email=? OR username=?");
        checkPs.setString(1, identifier);
        checkPs.setString(2, identifier);
        ResultSet rs = checkPs.executeQuery();
        if (rs.next() && rs.getInt("failed_attempts") >= 5) {
            lockAccount(identifier);
        }
    }

    private void resetFailedAttempts(String identifier) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET failed_attempts = 0 WHERE email=? OR username=?");
        ps.setString(1, identifier);
        ps.setString(2, identifier);
        ps.executeUpdate();
    }

    private void lockAccount(String identifier) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET is_active = false WHERE email=? OR username=?");
        ps.setString(1, identifier);
        ps.setString(2, identifier);
        ps.executeUpdate();
        sendAccountLockedEmail(identifier);
    }

    private void sendAccountLockedEmail(String identifier) {
        try {
            String username = getUsernameByEmail(identifier);
            String email    = getEmailByIdentifier(identifier);
            System.out.println("Envoi email ├á : " + email);
            EmailService.sendAccountLockedEmail(email, username);
            System.out.println("[ServiceUser] Email compte bloqu├® envoy├® ├á : " + email);
        } catch (Exception e) {
            System.err.println("[ServiceUser] ERREUR envoi email compte bloqu├® : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean verifyEmailToken(String token) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "SELECT id, email_verification_token_expires_at FROM users WHERE email_verification_token = ?");
        ps.setString(1, token);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Timestamp expiresAt = rs.getTimestamp("email_verification_token_expires_at");
            if (expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                PreparedStatement updatePs = conn().prepareStatement(
                        "UPDATE users SET is_verified = true, is_active = true, email_verification_token = NULL, email_verification_token_expires_at = NULL WHERE email_verification_token = ?");
                updatePs.setString(1, token);
                updatePs.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void resendVerificationEmail(String email) throws SQLException {
        String token = java.util.UUID.randomUUID().toString();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET email_verification_token = ?, email_verification_token_expires_at = ? WHERE email = ?");
        ps.setString(1, token);
        ps.setTimestamp(2, expiresAt);
        ps.setString(3, email);
        ps.executeUpdate();
        sendVerificationEmail(email, token);
    }

    private void sendVerificationEmail(String email, String token) {
        try {
            String username = getUsernameByEmail(email);
            System.out.println("[ServiceUser] Envoi email v├®rification ├á : " + email);
            EmailService.sendVerificationEmail(email, username, token);
            System.out.println("[ServiceUser] Email v├®rification envoy├® ├á : " + email);
        } catch (Exception e) {
            System.err.println("[ServiceUser] ERREUR envoi email v├®rification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void requestPasswordReset(String email) throws SQLException {
        String token = java.util.UUID.randomUUID().toString();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 60 * 60 * 1000);
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET password_reset_token = ?, password_reset_token_expires_at = ? WHERE email = ?");
        ps.setString(1, token);
        ps.setTimestamp(2, expiresAt);
        ps.setString(3, email);
        ps.executeUpdate();
        sendPasswordResetEmail(email, token);
    }

    private void sendPasswordResetEmail(String email, String token) {
        try {
            String username = getUsernameByEmail(email);
            System.out.println("Envoi email ├á : " + email);
            EmailService.sendPasswordResetEmail(email, username, token);
            System.out.println("[ServiceUser] Email reset password envoy├® ├á : " + email);
        } catch (Exception e) {
            System.err.println("[ServiceUser] ERREUR envoi email reset password : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean resetPassword(String token, String newPassword) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "SELECT id, password_reset_token_expires_at FROM users WHERE password_reset_token = ?");
        ps.setString(1, token);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Timestamp expiresAt = rs.getTimestamp("password_reset_token_expires_at");
            if (expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                PreparedStatement updatePs = conn().prepareStatement(
                        "UPDATE users SET password = ?, password_reset_token = NULL, password_reset_token_expires_at = NULL WHERE password_reset_token = ?");
                updatePs.setString(1, hashedPassword);
                updatePs.setString(2, token);
                updatePs.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void unlockAccount(int userId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET failed_attempts = 0, is_active = true WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    /**
     * Met ├á jour username, email, t├®l├®phone d'un utilisateur connect├®.
     */
    public void updateProfile(int userId, String username, String email, String phone) throws SQLException {
        // Validations
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches())
            throw new SQLException("Le username doit contenir entre 3 et 80 caract├¿res (lettres, chiffres, point, tiret, underscore).");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new SQLException("Email invalide.");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches())
            throw new SQLException("Le num├®ro de t├®l├®phone doit contenir 8 chiffres ou ├¬tre au format international (ex: +21629110800).");

        PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET username=?, email=?, phone_number=? WHERE id=?");
        ps.setString(1, username.trim());
        ps.setString(2, email.trim());
        ps.setString(3, phone.trim());
        ps.setInt(4, userId);
        ps.executeUpdate();
    }

    /**
     * Change le mot de passe apr├¿s v├®rification de l'ancien.
     * Retourne true si succ├¿s, false si ancien mot de passe incorrect.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.trim().length() < 8)
            throw new SQLException("Le nouveau mot de passe doit contenir au moins 8 caract├¿res.");
        if (!PASSWORD_PATTERN.matcher(newPassword).matches())
            throw new SQLException("Le nouveau mot de passe doit contenir des lettres et des chiffres.");

        // R├®cup├®rer le hash actuel
        PreparedStatement ps = conn().prepareStatement(
                "SELECT password FROM users WHERE id=?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) throw new SQLException("Utilisateur introuvable.");

        String storedHash = rs.getString("password");
        if (!passwordMatches(oldPassword, storedHash)) {
            return false; // ancien mot de passe incorrect
        }

        // Mettre ├á jour avec le nouveau hash
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        PreparedStatement updatePs = conn().prepareStatement(
                "UPDATE users SET password=? WHERE id=?");
        updatePs.setString(1, newHash);
        updatePs.setInt(2, userId);
        updatePs.executeUpdate();
        return true;
    }

    @Override
    public List<User> getAll() throws SQLException {
        String req = "SELECT u.*, r.id as role_id, r.name as role_name FROM `users` u " +
                "LEFT JOIN `user_roles` ur ON u.id = ur.user_id " +
                "LEFT JOIN `roles` r ON ur.role_id = r.id " +
                "ORDER BY u.id";
        Statement statement = conn().createStatement();
        ResultSet rs = statement.executeQuery(req);
        // D├®dupliquer : un user peut avoir plusieurs r├┤les ÔåÆ plusieurs lignes
        java.util.LinkedHashMap<Integer, User> userMap = new java.util.LinkedHashMap<>();
        while (rs.next()) {
            int uid = rs.getInt("id");
            User user = userMap.get(uid);
            if (user == null) {
                user = new User(
                        uid, rs.getString("email"), rs.getString("username"),
                        rs.getString("password"), null, rs.getBoolean("is_active"),
                        rs.getString("phone_number"), rs.getBoolean("is_verified"),
                        rs.getString("email_verification_token"),
                        rs.getTimestamp("email_verification_token_expires_at") != null ?
                                rs.getTimestamp("email_verification_token_expires_at").toLocalDateTime() : null,
                        rs.getString("password_reset_token"),
                        rs.getTimestamp("password_reset_token_expires_at") != null ?
                                rs.getTimestamp("password_reset_token_expires_at").toLocalDateTime() : null,
                        rs.getInt("failed_attempts")
                );
                user.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                user.setRoles(new ArrayList<>());
                userMap.put(uid, user);
            }
            if (rs.getString("role_name") != null) {
                user.getRoles().add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
        }
        return new ArrayList<>(userMap.values());
    }


    public User afficherParId(int id) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
                "SELECT u.*, r.id as role_id, r.name as role_name FROM users u " +
                        "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                        "LEFT JOIN roles r ON ur.role_id = r.id WHERE u.id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        User user = null;
        List<Role> roles = new ArrayList<>();
        while (rs.next()) {
            if (user == null) {
                user = new User(
                        rs.getInt("id"), rs.getString("email"), rs.getString("username"),
                        rs.getString("password"), null, rs.getBoolean("is_active"),
                        rs.getString("phone_number"), rs.getBoolean("is_verified"),
                        null, null, null, null, rs.getInt("failed_attempts")
                );
            }
            if (rs.getString("role_name") != null) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
        }
        if (user != null) user.setRoles(roles);
        return user;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        try {
            return BCrypt.checkpw(rawPassword, storedPassword);
        } catch (IllegalArgumentException ex) {
            return rawPassword.equals(storedPassword);
        }
    }

    private String getUsernameByEmail(String identifier) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                    "SELECT username FROM users WHERE email=? OR username=?");
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (SQLException e) {
            System.err.println("Erreur getUsernameByEmail: " + e.getMessage());
        }
        return identifier;
    }

    private String getEmailByIdentifier(String identifier) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                    "SELECT email FROM users WHERE email=? OR username=?");
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            System.err.println("Erreur getEmailByIdentifier: " + e.getMessage());
        }
        return identifier;
    }

    private void validateUserForCreate(User user) throws SQLException {
        validateCommonFields(user);
        if (!hasText(user.getPassword())) throw new SQLException("Le mot de passe est obligatoire.");
        if (user.getPassword().trim().length() < 8)
            throw new SQLException("Le mot de passe doit contenir au moins 8 caract├¿res.");
        if (!PASSWORD_PATTERN.matcher(user.getPassword().trim()).matches())
            throw new SQLException("Le mot de passe doit contenir des lettres et des chiffres.");
    }

    private void validateUserForUpdate(User user) throws SQLException {
        validateCommonFields(user);
        if (user.getId() <= 0) throw new SQLException("Identifiant utilisateur invalide.");
        if (hasText(user.getPassword())) {
            if (user.getPassword().trim().length() < 8)
                throw new SQLException("Le mot de passe doit contenir au moins 8 caract├¿res.");
            if (!PASSWORD_PATTERN.matcher(user.getPassword().trim()).matches())
                throw new SQLException("Le mot de passe doit contenir des lettres et des chiffres.");
        }
    }

    private void validateCommonFields(User user) throws SQLException {
        if (user == null) throw new SQLException("Utilisateur invalide.");
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber().trim();
        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new SQLException("Le username doit contenir entre 3 et 80 caract├¿res (lettres, chiffres, point, tiret, underscore).");
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new SQLException("Email invalide.");
        if (!PHONE_PATTERN.matcher(phone).matches()) throw new SQLException("Le num├®ro de t├®l├®phone doit contenir 8 chiffres ou ├¬tre au format international (ex: +21629110800).");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int resolveRoleId(String roleName) throws SQLException {
        String normalized = normalizeRoleInput(roleName);
        for (String alias : roleAliases(normalized)) {
            Integer roleId = findRoleId(alias);
            if (roleId != null) return roleId;
        }
        String roleToCreate = "ROLE_PATIENT".equals(normalized) ? "ROLE_PATIENT" : "ROLE_PHYSICIAN";
        return createRoleAndReturnId(roleToCreate);
    }

    private String normalizeRoleInput(String roleName) {
        if (roleName == null || roleName.isBlank()) return "ROLE_PATIENT";
        String value = roleName.trim().toUpperCase();
        if ("MEDECIN".equals(value) || "DOCTOR".equals(value) || "PHYSICIAN".equals(value)
                || "ROLE_DOCTOR".equals(value) || "ROLE_PHYSICIAN".equals(value)) return "ROLE_PHYSICIAN";
        if ("PATIENT".equals(value) || "ROLE_PATIENT".equals(value)) return "ROLE_PATIENT";
        return value;
    }

    private List<String> roleAliases(String normalizedRole) {
        List<String> aliases = new ArrayList<>();
        if ("ROLE_PHYSICIAN".equals(normalizedRole)) {
            aliases.add("ROLE_PHYSICIAN"); aliases.add("PHYSICIAN"); aliases.add("DOCTOR"); aliases.add("ROLE_DOCTOR"); aliases.add("MEDECIN");
        } else {
            aliases.add("ROLE_PATIENT"); aliases.add("PATIENT");
        }
        return aliases;
    }

    private int createRoleAndReturnId(String roleName) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO roles (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roleName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        Integer existingId = findRoleId(roleName);
        if (existingId != null) return existingId;
        throw new SQLException("Impossible de creer ou recuperer le role: " + roleName);
    }

    private Integer findRoleId(String roleName) throws SQLException {
        if (roleName == null || roleName.isBlank()) return null;
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT id FROM roles WHERE UPPER(name)=UPPER(?) LIMIT 1")) {
            ps.setString(1, roleName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }

    private String resolveRoleNameById(int roleId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT name FROM roles WHERE id=?")) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        }
        return "ROLE_PATIENT";
    }

    /**
     * Récupère tous les utilisateurs ayant le rôle médecin.
     */
    public List<User> getAllDoctors() throws SQLException {
        List<User> doctors = new ArrayList<>();
        String sql = "SELECT u.*, r.id as role_id, r.name as role_name FROM users u " +
                "JOIN user_roles ur ON u.id = ur.user_id " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE UPPER(r.name) IN ('ROLE_PHYSICIAN','ROLE_DOCTOR','DOCTOR','MEDECIN','PHYSICIAN')";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"), rs.getString("email"), rs.getString("username"),
                        rs.getString("password"), null, rs.getBoolean("is_active"),
                        rs.getString("phone_number"), rs.getBoolean("is_verified"),
                        null, null, null, null, rs.getInt("failed_attempts")
                );
                doctors.add(user);
            }
        }
        return doctors;
    }
}

