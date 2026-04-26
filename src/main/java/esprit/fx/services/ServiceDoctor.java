package esprit.fx.services;

import esprit.fx.entities.Doctor;
import esprit.fx.utils.EmailService;
import esprit.fx.utils.MyDB;

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
        // Créer l'entrée dans doctors (le user est déjà créé par ServiceUser.registerUser)
        String reqDoctor = "INSERT INTO `doctors` (`license_code`, `is_certified`, `created_at`, `updated_at`, `user_id`) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(reqDoctor, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, doctor.getLicenseCode() != null ? doctor.getLicenseCode() : "");
        ps.setBoolean(2, false);
        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(5, doctor.getUserId());
        ps.executeUpdate();

        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                doctor.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void modifier(Doctor doctor) throws SQLException {
        String reqUser = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `phone_number`=? WHERE `id`=?";
        PreparedStatement ps = conn().prepareStatement(reqUser);
        ps.setString(1, doctor.getEmail());
        ps.setString(2, doctor.getUsername());
        ps.setString(3, doctor.getPassword());
        ps.setString(4, doctor.getPhoneNumber());
        ps.setInt(5, doctor.getUserId());
        ps.executeUpdate();

        String reqDoctor = "UPDATE `doctors` SET `license_code`=?, `is_certified`=?, `updated_at`=? WHERE `user_id`=?";
        PreparedStatement ps2 = conn().prepareStatement(reqDoctor);
        ps2.setString(1, doctor.getLicenseCode());
        ps2.setBoolean(2, doctor.isCertified());
        ps2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        ps2.setInt(4, doctor.getUserId());
        ps2.executeUpdate();
    }

    @Override
    public void supprimer(int userId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("DELETE FROM `user_roles` WHERE `user_id`=?");
        ps.setInt(1, userId);
        ps.executeUpdate();

        PreparedStatement ps2 = conn().prepareStatement("DELETE FROM `doctors` WHERE `user_id`=?");
        ps2.setInt(1, userId);
        ps2.executeUpdate();

        PreparedStatement ps3 = conn().prepareStatement("DELETE FROM `users` WHERE `id`=?");
        ps3.setInt(1, userId);
        ps3.executeUpdate();
    }

    @Override
    public List<Doctor> getAll() throws SQLException {
        String req = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at " +
                "FROM `users` u JOIN `doctors` d ON u.id = d.user_id";
        PreparedStatement ps = conn().prepareStatement(req);
        ResultSet rs = ps.executeQuery();
        List<Doctor> doctors = new ArrayList<>();
        while (rs.next()) {
            doctors.add(mapDoctor(rs));
        }
        return doctors;
    }

    public List<Doctor> getDoctorsPendingVerification() throws SQLException {
        String query = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at " +
                "FROM users u JOIN doctors d ON u.id = d.user_id " +
                "WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        PreparedStatement ps = conn().prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        List<Doctor> pendingDoctors = new ArrayList<>();
        while (rs.next()) {
            pendingDoctors.add(mapDoctor(rs));
        }
        return pendingDoctors;
    }

    public void approveDoctorCertification(int userId) throws SQLException {
        PreparedStatement psDoctor = conn().prepareStatement(
                "UPDATE doctors SET is_certified = true, updated_at = ? WHERE user_id = ?");
        psDoctor.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        psDoctor.setInt(2, userId);
        psDoctor.executeUpdate();

        PreparedStatement psUser = conn().prepareStatement(
                "UPDATE users SET is_active = true WHERE id = ?");
        psUser.setInt(1, userId);
        psUser.executeUpdate();

        // Mettre à jour le statut des documents
        PreparedStatement psDoc = conn().prepareStatement(
                "UPDATE doctor_documents SET status = 'approved' WHERE doctor_id = (SELECT id FROM doctors WHERE user_id = ?)");
        psDoc.setInt(1, userId);
        psDoc.executeUpdate();

        // Envoyer email d'approbation
        sendApprovalEmail(userId);
    }

    public void rejectDoctorCertification(int userId, String reason) throws SQLException {
        PreparedStatement psDoc = conn().prepareStatement(
                "UPDATE doctor_documents SET status = 'rejected' WHERE doctor_id = (SELECT id FROM doctors WHERE user_id = ?)");
        psDoc.setInt(1, userId);
        psDoc.executeUpdate();

        // Envoyer email de refus
        sendRejectionEmail(userId, reason);
    }

    private void sendApprovalEmail(int userId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                    "SELECT email, username FROM users WHERE id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String email    = rs.getString("email");
                String username = rs.getString("username");
                System.out.println("Envoi email à : " + email);
                EmailService.sendDoctorApprovedEmail(email, username);
                System.out.println("[ServiceDoctor] Email approbation envoyé à : " + email);
            }
        } catch (Exception e) {
            System.err.println("[ServiceDoctor] ERREUR envoi email approbation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRejectionEmail(int userId, String reason) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                    "SELECT email, username FROM users WHERE id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String email    = rs.getString("email");
                String username = rs.getString("username");
                System.out.println("Envoi email à : " + email);
                EmailService.sendDoctorRejectedEmail(email, username, reason);
                System.out.println("[ServiceDoctor] Email refus envoyé à : " + email);
            }
        } catch (Exception e) {
            System.err.println("[ServiceDoctor] ERREUR envoi email refus : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPendingDoctorsCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id " +
                "WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        PreparedStatement ps = conn().prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    public File getDoctorPdf(int userId) throws SQLException {
        // Récupérer le document via doctor_id (doctors.id, pas user_id)
        String query = "SELECT dd.folder_name, dd.stored_name FROM doctor_documents dd " +
                "JOIN doctors d ON dd.doctor_id = d.id " +
                "WHERE d.user_id = ? ORDER BY dd.uploaded_at DESC LIMIT 1";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String folder = rs.getString("folder_name");
            String stored = rs.getString("stored_name");
            File pdfFile = Paths.get(folder, stored).toFile();
            if (pdfFile.exists()) return pdfFile;
        }
        return null;
    }

    public int getActiveDoctorsCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id " +
                "WHERE d.is_certified = true AND u.is_active = true";
        PreparedStatement ps = conn().prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    private Doctor mapDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
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
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now(),
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
        );
    }
}
