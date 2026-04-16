package esprit.fx.services;

import esprit.fx.entities.Patient;
import esprit.fx.utils.MyDB;

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
        String reqUser = "INSERT INTO `users` (`email`, `username`, `password`, `created_at`, `is_active`, `phone_number`, `is_verified`, `email_verification_token`, `email_verification_token_expires_at`, `password_reset_token`, `password_reset_token_expires_at`, `failed_attempts`) " +
                "VALUES ('" + patient.getEmail() + "', '" + patient.getUsername() + "', '" + patient.getPassword() + "', '" + LocalDateTime.now() + "', " + patient.isActive() + ", '" + patient.getPhoneNumber() + "', " + patient.isVerified() + ", null, null, null, null, " + patient.getFailedAttempts() + ")";
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(reqUser, Statement.RETURN_GENERATED_KEYS);

        // 2 - Récupérer l'id généré
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        int userId = 0;
        if (generatedKeys.next()) {
            userId = generatedKeys.getInt(1);
        }


        String reqPatient = "INSERT INTO `patients` (`region`, `allergies`, `medical_history`, `previous_cancellations`, `birth_date`, `created_at`, `user_id`) " +
                "VALUES ('" + patient.getRegion() + "', '" + patient.getAllergies() + "', '" + patient.getMedicalHistory() + "', " + patient.getPreviousCancellations() + ", '" + patient.getBirthDate() + "', '" + LocalDateTime.now() + "', " + userId + ")";
        stmt.executeUpdate(reqPatient);

        //roles , de mm por doctor et user?

    }

    @Override
    public void modifier(Patient patient) throws SQLException {
        String reqUser = "UPDATE `users` SET `email`=?, `username`=?, `password`=?, `phone_number`=? WHERE `id`=?";
        PreparedStatement ps = conn.prepareStatement(reqUser);
        ps.setString(1, patient.getEmail());
        ps.setString(2, patient.getUsername());
        ps.setString(3, patient.getPassword());
        ps.setString(4, patient.getPhoneNumber());
        ps.setInt(5, patient.getId());
        ps.executeUpdate();

        // 2 - UPDATE patients
        String reqPatient = "UPDATE `patients` SET `region`=?, `allergies`=?, `medical_history`=?, `previous_cancellations`=?, `birth_date`=? WHERE `user_id`=?";
        PreparedStatement ps2 = conn.prepareStatement(reqPatient);
        ps2.setString(1, patient.getRegion());
        ps2.setString(2, patient.getAllergies());
        ps2.setString(3, patient.getMedicalHistory());
        ps2.setInt(4, patient.getPreviousCancellations());
        ps2.setDate(5, patient.getBirthDate() != null ? Date.valueOf(patient.getBirthDate()) : null);
        ps2.setInt(6, patient.getId());
        ps2.executeUpdate();

    }

    @Override
    public void supprimer(int patientId) throws SQLException {
        PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `patients` WHERE `user_id`=?");
        ps2.setInt(1, patientId);
        ps2.executeUpdate();

        // 3 - DELETE users en dernier
        PreparedStatement ps3 = conn.prepareStatement("DELETE FROM `users` WHERE `id`=?");
        ps3.setInt(1, patientId);
        ps3.executeUpdate();

    }

    @Override
    public List<Patient> getAll() throws SQLException {
        String req = "SELECT u.*, p.id as patient_id, p.region, p.allergies, p.medical_history, p.previous_cancellations, p.birth_date " +
                "FROM `users` u JOIN `patients` p ON u.id = p.user_id";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(req);
        List<Patient> patients = new ArrayList<>();
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
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
            patients.add(patient);
        }
        return patients;
    }


    @Override
    public Patient afficherParId(int id) throws SQLException {
        Patient patient = null;
        String sql = "SELECT u.*, p.id as patient_id, p.region, p.allergies, p.medical_history, p.previous_cancellations, p.birth_date " +
                "FROM users u JOIN patients p ON u.id = p.user_id WHERE u.id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            patient = new Patient(
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
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        }
        return patient;
    }
}
