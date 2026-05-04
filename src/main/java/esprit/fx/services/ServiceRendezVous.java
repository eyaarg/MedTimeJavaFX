package esprit.fx.services;

import esprit.fx.entities.RendezVous;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous implements IService<RendezVous> {
    private Connection conn;

    public ServiceRendezVous() {
        conn = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(RendezVous rendezVous) throws SQLException {
        String sql = "INSERT INTO rendez_vous (patient_id, doctor_id, appointment_date_time, duration, consultation_type, reason, status, notes, reminder_sent, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rendezVous.getPatientId());
            ps.setInt(2, rendezVous.getDoctorId());
            ps.setTimestamp(3, Timestamp.valueOf(rendezVous.getDateHeure()));
            ps.setInt(4, 40); // Durée par défaut: 40 minutes
            ps.setString(5, "IN_PERSON"); // Type de consultation: en personne
            ps.setString(6, rendezVous.getMotif());
            ps.setString(7, rendezVous.getStatut());
            ps.setString(8, rendezVous.getNotes());
            ps.setBoolean(9, false); // Rappel non envoyé
            ps.setTimestamp(10, Timestamp.valueOf(rendezVous.getDateCreation()));
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rendezVous.setId(rs.getInt(1));
                }
            }
            
            System.out.println("✓ Rendez-vous ajouté avec succès - ID: " + rendezVous.getId());
        }
    }

    @Override
    public void modifier(RendezVous rendezVous) throws SQLException {
        String sql = "UPDATE rendez_vous SET patient_id=?, doctor_id=?, appointment_date_time=?, reason=?, status=?, notes=?, updated_at=? WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rendezVous.getPatientId());
            ps.setInt(2, rendezVous.getDoctorId());
            ps.setTimestamp(3, Timestamp.valueOf(rendezVous.getDateHeure()));
            ps.setString(4, rendezVous.getMotif());
            ps.setString(5, rendezVous.getStatut());
            ps.setString(6, rendezVous.getNotes());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(8, rendezVous.getId());
            
            ps.executeUpdate();
            System.out.println("✓ Rendez-vous modifié avec succès - ID: " + rendezVous.getId());
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM rendez_vous WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<RendezVous> getAll() throws SQLException {
        String sql = "SELECT * FROM rendez_vous ORDER BY id DESC";
        
        List<RendezVous> rendezVousList = new ArrayList<>();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                RendezVous rv = mapResultSetToRendezVousSimple(rs);
                rendezVousList.add(rv);
            }
        }
        
        return rendezVousList;
    }

    @Override
    public RendezVous afficherParId(int id) throws SQLException {
        String sql = """
            SELECT rv.*, 
                   up.username as patient_nom, up.email as patient_email,
                   ud.username as doctor_nom, ud.email as doctor_email
            FROM rendez_vous rv
            LEFT JOIN users up ON rv.patient_id = up.id
            LEFT JOIN users ud ON rv.doctor_id = ud.id
            WHERE rv.id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRendezVous(rs);
                }
            }
        }
        
        return null;
    }

    public List<RendezVous> getRendezVousParPatient(int patientId) throws SQLException {
        String sql = """
            SELECT rv.*, 
                   up.username as patient_nom, up.email as patient_email,
                   ud.username as doctor_nom, ud.email as doctor_email
            FROM rendez_vous rv
            LEFT JOIN users up ON rv.patient_id = up.id
            LEFT JOIN users ud ON rv.doctor_id = ud.id
            WHERE rv.patient_id = ?
            ORDER BY rv.appointment_date_time DESC
            """;
        
        List<RendezVous> rendezVousList = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RendezVous rv = mapResultSetToRendezVous(rs);
                    rendezVousList.add(rv);
                }
            }
            
            System.out.println("✓ " + rendezVousList.size() + " rendez-vous chargés pour le patient " + patientId);
        }
        
        return rendezVousList;
    }

    public List<RendezVous> getRendezVousParDocteur(int doctorId) throws SQLException {
        String sql = """
            SELECT rv.*, 
                   up.username as patient_nom, up.email as patient_email,
                   ud.username as doctor_nom, ud.email as doctor_email
            FROM rendez_vous rv
            LEFT JOIN users up ON rv.patient_id = up.id
            LEFT JOIN users ud ON rv.doctor_id = ud.id
            WHERE rv.doctor_id = ?
            ORDER BY rv.appointment_date_time DESC
            """;
        
        List<RendezVous> rendezVousList = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RendezVous rv = mapResultSetToRendezVous(rs);
                    rendezVousList.add(rv);
                }
            }
        }
        
        return rendezVousList;
    }

    public void changerStatut(int rendezVousId, String nouveauStatut) throws SQLException {
        String sql = "UPDATE rendez_vous SET status=?, updated_at=? WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, rendezVousId);
            
            ps.executeUpdate();
        }
    }

    /**
     * Récupère tous les utilisateurs ayant le rôle DOCTOR.
     * Méthode ajoutée dans ServiceRendezVous pour ne pas modifier ServiceUser (module Eya).
     */
    public List<esprit.fx.entities.User> getAllDoctors() throws SQLException {
        List<esprit.fx.entities.User> doctors = new java.util.ArrayList<>();
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
                esprit.fx.entities.User doctor = new esprit.fx.entities.User();
                doctor.setId(rs.getInt("id"));
                doctor.setUsername(rs.getString("username"));
                doctor.setEmail(rs.getString("email"));
                doctors.add(doctor);
            }
        }
        return doctors;
    }

    private RendezVous mapResultSetToRendezVous(ResultSet rs) throws SQLException {
        RendezVous rv = new RendezVous();
        rv.setId(rs.getInt("id"));
        rv.setPatientId(rs.getInt("patient_id"));
        rv.setDoctorId(rs.getInt("doctor_id"));
        
        // Utiliser le vrai nom de colonne: appointment_date_time
        Timestamp dateHeure = rs.getTimestamp("appointment_date_time");
        if (dateHeure != null) {
            rv.setDateHeure(dateHeure.toLocalDateTime());
        }
        
        // Utiliser reason au lieu de motif
        rv.setMotif(rs.getString("reason"));
        
        // Utiliser status au lieu de statut
        rv.setStatut(rs.getString("status"));
        
        rv.setNotes(rs.getString("notes"));
        
        // Utiliser created_at au lieu de date_creation
        Timestamp dateCreation = rs.getTimestamp("created_at");
        if (dateCreation != null) {
            rv.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        // Utiliser updated_at au lieu de date_modification
        try {
            Timestamp dateModification = rs.getTimestamp("updated_at");
            if (dateModification != null) {
                rv.setDateModification(dateModification.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Colonne peut ne pas exister
        }
        
        // Informations supplémentaires
        try {
            rv.setPatientNom(rs.getString("patient_nom"));
            rv.setPatientEmail(rs.getString("patient_email"));
            rv.setDoctorNom(rs.getString("doctor_nom"));
            rv.setDoctorEmail(rs.getString("doctor_email"));
        } catch (SQLException e) {
            // Ces colonnes viennent du JOIN, peuvent ne pas exister
            rv.setPatientNom("Patient " + rv.getPatientId());
            rv.setDoctorNom("Médecin " + rv.getDoctorId());
        }
        
        return rv;
    }

    private RendezVous mapResultSetToRendezVousSimple(ResultSet rs) throws SQLException {
        RendezVous rv = new RendezVous();
        rv.setId(rs.getInt("id"));
        rv.setPatientId(rs.getInt("patient_id"));
        rv.setDoctorId(rs.getInt("doctor_id"));
        
        // Utiliser appointment_date_time
        try {
            Timestamp dateHeure = rs.getTimestamp("appointment_date_time");
            if (dateHeure != null) {
                rv.setDateHeure(dateHeure.toLocalDateTime());
            }
        } catch (SQLException e) {
            System.err.println("Colonne appointment_date_time non trouvée: " + e.getMessage());
        }
        
        // Utiliser reason
        try {
            rv.setMotif(rs.getString("reason"));
        } catch (SQLException e) {
            rv.setMotif("Consultation");
        }
        
        // Utiliser status
        try {
            rv.setStatut(rs.getString("status"));
        } catch (SQLException e) {
            rv.setStatut("DEMANDE");
        }
        
        try {
            rv.setNotes(rs.getString("notes"));
        } catch (SQLException e) {
            rv.setNotes("");
        }
        
        // Noms temporaires
        rv.setPatientNom("Patient " + rv.getPatientId());
        rv.setDoctorNom("Médecin " + rv.getDoctorId());
        
        return rv;
    }
}
