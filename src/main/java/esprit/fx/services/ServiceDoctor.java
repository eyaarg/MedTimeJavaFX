package esprit.fx.services;

import esprit.fx.entities.Doctor;
import esprit.fx.utils.EmailService;
import esprit.fx.utils.MyDB;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceDoctor implements IService<Doctor> {

    private Connection conn() {
        return MyDB.getInstance().getConnection();
    }

    public ServiceDoctor() {
    }

    @Override
    public void ajouter(Doctor doctor) throws SQLException {
        // The user row is already created by ServiceUser.registerUser.
        // Insert the doctors row including the location fields (issue #8).
        String reqDoctor = "INSERT INTO `doctors` (`license_code`, `is_certified`, `created_at`, " +
                "`updated_at`, `user_id`, `city`, `latitude`, `longitude`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(reqDoctor, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, doctor.getLicenseCode() != null ? doctor.getLicenseCode() : "");
            ps.setBoolean(2, false);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, doctor.getUserId());
            ps.setString(6, doctor.getCity());
            ps.setObject(7, doctor.getLatitude());   // nullable Double
            ps.setObject(8, doctor.getLongitude());  // nullable Double
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    doctor.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void modifier(Doctor doctor) throws SQLException {
        // NEW #C: hash the password with BCrypt only when a new password is provided
        boolean updatePassword = doctor.getPassword() != null && !doctor.getPassword().isBlank();
        String reqUser;
        if (updatePassword) {
            reqUser = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `phone_number`=? WHERE `id`=?";
        } else {
            reqUser = "UPDATE `users` SET `email`=?, `username`=?, `phone_number`=? WHERE `id`=?";
        }

        try (PreparedStatement ps = conn().prepareStatement(reqUser)) {
            ps.setString(1, doctor.getEmail());
            ps.setString(2, doctor.getUsername());
            if (updatePassword) {
                ps.setString(3, BCrypt.hashpw(doctor.getPassword(), BCrypt.gensalt()));
                ps.setString(4, doctor.getPhoneNumber());
                ps.setInt(5, doctor.getUserId());
            } else {
                ps.setString(3, doctor.getPhoneNumber());
                ps.setInt(4, doctor.getUserId());
            }
            ps.executeUpdate();
        }

        // Update doctors row including location fields (issue #8)
        String reqDoctor = "UPDATE `doctors` SET `license_code`=?, `is_certified`=?, `updated_at`=?, " +
                "`city`=?, `latitude`=?, `longitude`=? WHERE `user_id`=?";
        try (PreparedStatement ps2 = conn().prepareStatement(reqDoctor)) {
            ps2.setString(1, doctor.getLicenseCode());
            ps2.setBoolean(2, doctor.isCertified());
            ps2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps2.setString(4, doctor.getCity());
            ps2.setObject(5, doctor.getLatitude());
            ps2.setObject(6, doctor.getLongitude());
            ps2.setInt(7, doctor.getUserId());
            ps2.executeUpdate();
        }
    }

    @Override
    public void supprimer(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM `user_roles` WHERE `user_id`=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM `doctors` WHERE `user_id`=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM `users` WHERE `id`=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Doctor> getAll() throws SQLException {
        // Issue #8: include location columns in SELECT
        String req = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at, " +
                "d.city, d.latitude, d.longitude, d.adresse " +
                "FROM `users` u JOIN `doctors` d ON u.id = d.user_id";
        try (PreparedStatement ps = conn().prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {
            List<Doctor> doctors = new ArrayList<>();
            while (rs.next()) {
                doctors.add(mapDoctor(rs));
            }
            return doctors;
        }
    }

    public List<Doctor> getDoctorsPendingVerification() throws SQLException {
        // Issue #8: include location columns in SELECT
        String query = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at, " +
                "d.city, d.latitude, d.longitude, d.adresse " +
                "FROM users u JOIN doctors d ON u.id = d.user_id " +
                "WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        try (PreparedStatement ps = conn().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            List<Doctor> pendingDoctors = new ArrayList<>();
            while (rs.next()) {
                pendingDoctors.add(mapDoctor(rs));
            }
            return pendingDoctors;
        }
    }

    public void approveDoctorCertification(int userId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE doctors SET is_certified = true, updated_at = ? WHERE user_id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET is_active = true WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE doctor_documents SET status = 'approved' WHERE doctor_id = (SELECT id FROM doctors WHERE user_id = ?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        sendApprovalEmail(userId);
    }

    public void rejectDoctorCertification(int userId, String reason) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE doctor_documents SET status = 'rejected' WHERE doctor_id = (SELECT id FROM doctors WHERE user_id = ?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        sendRejectionEmail(userId, reason);
    }

    private void sendApprovalEmail(int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT email, username FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email    = rs.getString("email");
                    String username = rs.getString("username");
                    System.out.println("Envoi email ├á : " + email);
                    EmailService.sendDoctorApprovedEmail(email, username);
                    System.out.println("[ServiceDoctor] Email approbation envoy├® ├á : " + email);
                }
            }
        } catch (Exception e) {
            System.err.println("[ServiceDoctor] ERREUR envoi email approbation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRejectionEmail(int userId, String reason) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT email, username FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email    = rs.getString("email");
                    String username = rs.getString("username");
                    System.out.println("Envoi email ├á : " + email);
                    EmailService.sendDoctorRejectedEmail(email, username, reason);
                    System.out.println("[ServiceDoctor] Email refus envoy├® ├á : " + email);
                }
            }
        } catch (Exception e) {
            System.err.println("[ServiceDoctor] ERREUR envoi email refus : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPendingDoctorsCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id " +
                "WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        try (PreparedStatement ps = conn().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public File getDoctorPdf(int userId) throws SQLException {
        String query = "SELECT dd.folder_name, dd.stored_name FROM doctor_documents dd " +
                "JOIN doctors d ON dd.doctor_id = d.id " +
                "WHERE d.user_id = ? ORDER BY dd.uploaded_at DESC LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String folder = rs.getString("folder_name");
                    String stored = rs.getString("stored_name");
                    File pdfFile = Paths.get(folder, stored).toFile();
                    if (pdfFile.exists()) return pdfFile;
                }
            }
        }
        return null;
    }

    public int getActiveDoctorsCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id " +
                "WHERE d.is_certified = true AND u.is_active = true";
        try (PreparedStatement ps = conn().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Doctor mapDoctor(ResultSet rs) throws SQLException {
        Doctor d = new Doctor(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password"),
                null,
                rs.getBoolean("is_active"),
                rs.getString("phone_number"),
                rs.getBoolean("is_verified"),
                null, null, null, null,
                rs.getInt("failed_attempts"),
                rs.getInt("doctor_id"),
                rs.getInt("id"),
                rs.getString("license_code"),
                rs.getBoolean("is_certified"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now(),
                rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toLocalDateTime()
                        : null
        );
        // Issue #8: populate location fields (city/lat/lng, no country)
        d.setCity(rs.getString("city"));
        double lat = rs.getDouble("latitude");
        d.setLatitude(rs.wasNull() ? null : lat);
        double lng = rs.getDouble("longitude");
        d.setLongitude(rs.wasNull() ? null : lng);
        // adresse pour la carte OpenStreetMap (moduleB_farah)
        try { d.setAdresse(rs.getString("adresse")); } catch (Exception ignored) {}
        return d;
    }
}
