package esprit.fx.services;

import esprit.fx.entities.Patient;
import esprit.fx.utils.MyDB;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicePatient implements IService<Patient> {
    private Connection conn;

    public ServicePatient() {
        conn = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(Patient patient) throws SQLException {
        // NEW #A + #B: use PreparedStatement and hash the password with BCrypt
        String reqUser = "INSERT INTO `users` (`email`, `username`, `password`, `created_at`, " +
                "`is_active`, `phone_number`, `is_verified`, `email_verification_token`, " +
                "`email_verification_token_expires_at`, `password_reset_token`, " +
                "`password_reset_token_expires_at`, `failed_attempts`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?)";

        String hashedPassword = BCrypt.hashpw(patient.getPassword(), BCrypt.gensalt());

        try (PreparedStatement ps = conn.prepareStatement(reqUser, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, patient.getEmail());
            ps.setString(2, patient.getUsername());
            ps.setString(3, hashedPassword);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBoolean(5, patient.isActive());
            ps.setString(6, patient.getPhoneNumber());
            ps.setBoolean(7, patient.isVerified());
            ps.setInt(8, patient.getFailedAttempts());
            ps.executeUpdate();

            int userId = 0;
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
            }

            String reqPatient = "INSERT INTO `patients` (`region`, `allergies`, `medical_history`, " +
                    "`previous_cancellations`, `birth_date`, `created_at`, `user_id`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps2 = conn.prepareStatement(reqPatient)) {
                ps2.setString(1, patient.getRegion());
                ps2.setString(2, patient.getAllergies());
                ps2.setString(3, patient.getMedicalHistory());
                ps2.setInt(4, patient.getPreviousCancellations());
                ps2.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);
                ps2.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps2.setInt(7, userId);
                ps2.executeUpdate();
            }
        }
    }

    @Override
    public void modifier(Patient patient) throws SQLException {
        // Hash password only if a new one is being set
        boolean updatePassword = patient.getPassword() != null && !patient.getPassword().isBlank();
        String reqUser;
        if (updatePassword) {
            reqUser = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `phone_number`=? WHERE `id`=?";
        } else {
            reqUser = "UPDATE `users` SET `email`=?, `username`=?, `phone_number`=? WHERE `id`=?";
        }

        try (PreparedStatement ps = conn.prepareStatement(reqUser)) {
            ps.setString(1, patient.getEmail());
            ps.setString(2, patient.getUsername());
            if (updatePassword) {
                ps.setString(3, BCrypt.hashpw(patient.getPassword(), BCrypt.gensalt()));
                ps.setString(4, patient.getPhoneNumber());
                ps.setInt(5, patient.getId());
            } else {
                ps.setString(3, patient.getPhoneNumber());
                ps.setInt(4, patient.getId());
            }
            ps.executeUpdate();
        }

        String reqPatient = "UPDATE `patients` SET `region`=?, `allergies`=?, `medical_history`=?, " +
                "`previous_cancellations`=?, `birth_date`=? WHERE `user_id`=?";
        try (PreparedStatement ps2 = conn.prepareStatement(reqPatient)) {
            ps2.setString(1, patient.getRegion());
            ps2.setString(2, patient.getAllergies());
            ps2.setString(3, patient.getMedicalHistory());
            ps2.setInt(4, patient.getPreviousCancellations());
            ps2.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);
            ps2.setInt(6, patient.getId());
            ps2.executeUpdate();
        }
    }

    @Override
    public void supprimer(int patientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM `patients` WHERE `user_id`=?")) {
            ps.setInt(1, patientId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM `users` WHERE `id`=?")) {
            ps.setInt(1, patientId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Patient> getAll() throws SQLException {
        String req = "SELECT u.*, p.id as patient_id, p.region, p.allergies, p.medical_history, " +
                "p.previous_cancellations, p.birth_date " +
                "FROM `users` u JOIN `patients` p ON u.id = p.user_id";
        List<Patient> patients = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(req)) {
            while (rs.next()) {
                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password"),
                        (Object) null,
                        rs.getBoolean("is_active"),
                        rs.getString("phone_number"),
                        rs.getBoolean("is_verified"),
                        (String) null,
                        (LocalDateTime) null,
                        (String) null,
                        (LocalDateTime) null,
                        rs.getInt("failed_attempts"),
                        rs.getInt("patient_id"),
                        rs.getInt("id"),
                        rs.getString("region"),
                        rs.getString("allergies"),
                        rs.getString("medical_history"),
                        rs.getInt("previous_cancellations"),
                        rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null,
                        rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime()
                                : LocalDateTime.now()
                );
                patients.add(patient);
            }
        }
        return patients;
    }
}
