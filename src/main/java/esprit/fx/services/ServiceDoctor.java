package esprit.fx.services;

import esprit.fx.entities.Doctor;
import esprit.fx.utils.MyDB;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceDoctor implements IService<Doctor>{


    private Connection conn;
    public ServiceDoctor() {
        conn = MyDB.getInstance().getConnection();
    }
    @Override
    public void ajouter(Doctor doctor) throws SQLException {
        String reqUser = "INSERT INTO `users` (`email`, `username`, `password`, `created_at`, `is_active`, `phone_number`, `is_verified`, `email_verification_token`, `email_verification_token_expires_at`, `password_reset_token`, `password_reset_token_expires_at`, `failed_attempts`) " +
                "VALUES ('" + doctor.getEmail() + "', '" + doctor.getUsername() + "', '" + doctor.getPassword() + "', '" + LocalDateTime.now() + "', " + doctor.isActive() + ", '" + doctor.getPhoneNumber() + "', " + doctor.isVerified() + ", null, null, null, null, " + doctor.getFailedAttempts() + ")";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(reqUser, Statement.RETURN_GENERATED_KEYS);
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        int userId = 0;
        if (generatedKeys.next()) {
            userId = generatedKeys.getInt(1);
        }
        String reqDoctor = "INSERT INTO `doctors` (`license_code`, `is_certified`, `created_at`, `updated_at`, `user_id`) " +
                "VALUES ('" + doctor.getLicenseCode() + "', " + doctor.isCertified() + ", '" + LocalDateTime.now() + "', '" + LocalDateTime.now() + "', " + userId + ")";
        stmt.executeUpdate(reqDoctor);
    }
    public void modifier(Doctor doctor) throws SQLException {
        // 1 - UPDATE users
        String reqUser = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `phone_number`=? WHERE `id`=?";
        PreparedStatement ps = conn.prepareStatement(reqUser);
        ps.setString(1, doctor.getEmail());
        ps.setString(2, doctor.getUsername());
        ps.setString(3, doctor.getPassword());
        ps.setString(4, doctor.getPhoneNumber());
        ps.setInt(5, doctor.getId());
        ps.executeUpdate();

        // 2 - UPDATE doctors
        String reqDoctor = "UPDATE `doctors` SET `license_code`=?, `is_certified`=?, `updated_at`=? WHERE `user_id`=?";
        PreparedStatement ps2 = conn.prepareStatement(reqDoctor);
        ps2.setString(1, doctor.getLicenseCode());
        ps2.setBoolean(2, doctor.isCertified());
        ps2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        ps2.setInt(4, doctor.getId());
        ps2.executeUpdate();
    }
    @Override
    public void supprimer(int userId) throws SQLException {
        // 1 - DELETE user_roles d'abord
        PreparedStatement ps = conn.prepareStatement("DELETE FROM `user_roles` WHERE `user_id`=?");
        ps.setInt(1, userId);
        ps.executeUpdate();

        // 2 - DELETE doctors
        PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `doctors` WHERE `user_id`=?");
        ps2.setInt(1, userId);
        ps2.executeUpdate();

        // 3 - DELETE users en dernier
        PreparedStatement ps3 = conn.prepareStatement("DELETE FROM `users` WHERE `id`=?");
        ps3.setInt(1, userId);
        ps3.executeUpdate();
    }

    @Override
    public List<Doctor> getAll() throws SQLException {
        String req = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at " +
                "FROM `users` u JOIN `doctors` d ON u.id = d.user_id";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(req);
        List<Doctor> doctors = new ArrayList<>();
        while (rs.next()) {
            Doctor doctor = new Doctor(
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
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
            );
            doctors.add(doctor);
        }
        return doctors;
    }

    public List<Doctor> getDoctorsPendingVerification() throws SQLException {
        String query = "SELECT u.*, d.* FROM users u JOIN doctors d ON u.id = d.user_id WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        List<Doctor> pendingDoctors = new ArrayList<>();
        while (rs.next()) {
            Doctor doctor = new Doctor(
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
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
            );
            pendingDoctors.add(doctor);
        }
        return pendingDoctors;
    }

    public void approveDoctorCertification(int doctorId) throws SQLException {
        String updateDoctorQuery = "UPDATE doctors SET is_certified = true WHERE user_id = ?";
        PreparedStatement psDoctor = conn.prepareStatement(updateDoctorQuery);
        psDoctor.setInt(1, doctorId);
        psDoctor.executeUpdate();

        String updateUserQuery = "UPDATE users SET is_active = true WHERE id = ?";
        PreparedStatement psUser = conn.prepareStatement(updateUserQuery);
        psUser.setInt(1, doctorId);
        psUser.executeUpdate();

        sendDoctorApprovedEmail(doctorId);
    }

    public void rejectDoctorCertification(int doctorId, String reason) throws SQLException {
        String updateDocumentsQuery = "UPDATE doctor_documents SET status = 'rejected' WHERE doctor_id = ?";
        PreparedStatement ps = conn.prepareStatement(updateDocumentsQuery);
        ps.setInt(1, doctorId);
        ps.executeUpdate();

        sendDoctorRejectedEmail(doctorId, reason);
    }

    private void sendDoctorApprovedEmail(int doctorId) {
        // Implementation for sending approval email
    }

    private void sendDoctorRejectedEmail(int doctorId, String reason) {
        // Implementation for sending rejection email
    }

    public int getPendingDoctorsCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM doctors d JOIN users u ON d.user_id = u.id WHERE d.is_certified = false AND u.is_active = false AND u.is_verified = true";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    public File getDoctorPdf(int doctorId) throws SQLException {
        // Exemple : récupérer le chemin du fichier PDF depuis la base de données
        String query = "SELECT pdf_path FROM doctor_documents WHERE doctor_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, doctorId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String pdfPath = rs.getString("pdf_path");
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                return pdfFile;
            }
        }
        return null;
    }
}
