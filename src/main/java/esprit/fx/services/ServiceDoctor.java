package esprit.fx.services;

import esprit.fx.entities.Doctor;
import esprit.fx.utils.MyDB;

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
        String reqDoctor = "INSERT INTO `doctors` (`license_code`, `is_certified`, `created_at`, `updated_at`, `user_id`, `adresse`) " +
                "VALUES ('" + doctor.getLicenseCode() + "', " + doctor.isCertified() + ", '" + LocalDateTime.now() + "', '" + LocalDateTime.now() + "', " + userId + ", '" + (doctor.getAdresse() != null ? doctor.getAdresse() : "") + "')";
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
        String reqDoctor = "UPDATE `doctors` SET `license_code`=?, `is_certified`=?, `updated_at`=?, `adresse`=? WHERE `user_id`=?";
        PreparedStatement ps2 = conn.prepareStatement(reqDoctor);
        ps2.setString(1, doctor.getLicenseCode());
        ps2.setBoolean(2, doctor.isCertified());
        ps2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        ps2.setString(4, doctor.getAdresse() != null ? doctor.getAdresse() : "");
        ps2.setInt(5, doctor.getId());
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
        String req = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at, d.adresse " +
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
            doctor.setAdresse(rs.getString("adresse"));
            doctors.add(doctor);
        }
        return doctors;
    }





    @Override
    public Doctor afficherParId(int id) throws SQLException {
        Doctor doctor = null;
        String sql = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at, d.adresse " +
                "FROM users u JOIN doctors d ON u.id = d.user_id WHERE u.id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            doctor = new Doctor(
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
                    rs.getTimestamp("updated_at") != null ?
                            rs.getTimestamp("updated_at").toLocalDateTime() : null
            );
            doctor.setAdresse(rs.getString("adresse"));
        }
        return doctor;
    }

    /**
     * Récupère les médecins en attente de vérification (is_active = false).
     */
    public List<Doctor> getDoctorsPendingVerification() throws SQLException {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT u.*, d.id as doctor_id, d.license_code, d.is_certified, d.updated_at, d.adresse " +
                "FROM users u JOIN doctors d ON u.id = d.user_id WHERE u.is_active = false";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            Doctor doctor = new Doctor(
                    rs.getInt("id"), rs.getString("email"), rs.getString("username"),
                    rs.getString("password"), null, rs.getBoolean("is_active"),
                    rs.getString("phone_number"), rs.getBoolean("is_verified"),
                    null, null, null, null, rs.getInt("failed_attempts"),
                    rs.getInt("doctor_id"), rs.getInt("id"),
                    rs.getString("license_code"), rs.getBoolean("is_certified"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
            );
            doctor.setAdresse(rs.getString("adresse"));
            doctors.add(doctor);
        }
        return doctors;
    }

    /** Compte les médecins en attente de vérification. */
    public int getPendingDoctorsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users u JOIN doctors d ON u.id = d.user_id WHERE u.is_active = false";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    /** Approuve la certification d'un médecin. */
    public void approveDoctorCertification(int userId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_active = true WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
        PreparedStatement ps2 = conn.prepareStatement(
                "UPDATE doctors SET is_certified = true, updated_at = ? WHERE user_id = ?");
        ps2.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        ps2.setInt(2, userId);
        ps2.executeUpdate();
    }

    /** Rejette la certification d'un médecin. */
    public void rejectDoctorCertification(int userId, String reason) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_active = false WHERE id = ?");
        ps.setInt(1, userId);
        ps.executeUpdate();
        System.out.println("[ServiceDoctor] Médecin refusé userId=" + userId + " raison=" + reason);
    }
}

