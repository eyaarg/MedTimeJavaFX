package esprit.fx.services;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.User;
import esprit.fx.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service pour gérer les consultations.
 * Gère l'acceptation des consultations et la génération de liens Google Meet.
 * Utilise JDBC (MyDB) pour la persistance.
 */
public class ConsultationServiceArij {

    /**
     * Génère un lien Google Meet unique.
     * Format : https://meet.google.com/xxx-xxxx-xxx
     * 
     * @return Lien Google Meet généré
     */
    public String genererLienMeet() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();

        // Groupe 1 : 3 caractères
        for (int i = 0; i < 3; i++) {
            sb.append(chars.charAt(r.nextInt(26)));
        }
        sb.append("-");

        // Groupe 2 : 4 caractères
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(r.nextInt(26)));
        }
        sb.append("-");

        // Groupe 3 : 3 caractères
        for (int i = 0; i < 3; i++) {
            sb.append(chars.charAt(r.nextInt(26)));
        }

        return "https://meet.google.com/" + sb.toString();
    }

    /**
     * Accepte une consultation et génère un lien Google Meet.
     * 
     * @param consultationId ID de la consultation
     * @param dateHeure Date et heure de la consultation
     * @return Consultation mise à jour
     * @throws Exception si erreur lors de l'acceptation
     */
    public ConsultationsArij accepterConsultation(Long consultationId, LocalDateTime dateHeure) throws Exception {
        Connection conn = MyDB.getInstance().getConnection();
        
        try {
            // 1. Charger la consultation
            ConsultationsArij consultation = getConsultationById(consultationId);
            if (consultation == null) {
                throw new Exception("Consultation non trouvée : " + consultationId);
            }

            // 2. Vérifier le statut
            if (!"en_attente".equalsIgnoreCase(consultation.getStatus())) {
                throw new Exception("La consultation n'est pas en attente. Statut actuel : " + consultation.getStatus());
            }

            // 3. Générer le lien Meet
            String lienMeet = genererLienMeet();
            System.out.println("[ConsultationServiceArij] Lien Meet généré : " + lienMeet);

            // 4. Mettre à jour la consultation en BDD
            String updateSql = "UPDATE consultations SET status = ?, consultation_date = ?, lien_meet = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, "acceptee");
                ps.setTimestamp(2, Timestamp.valueOf(dateHeure));
                ps.setString(3, lienMeet);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(5, consultationId);
                ps.executeUpdate();
            }

            // Recharger la consultation mise à jour
            consultation = getConsultationById(consultationId);
            System.out.println("[ConsultationServiceArij] Consultation acceptée : " + consultationId);

            // 5. Envoyer la notification au patient
            try {
                NotificationServiceArij notificationService = NotificationServiceArij.getInstance();
                String nomMedecin = consultation.getDoctor() != null ? consultation.getDoctor().getUsername() : "Dr. Inconnu";
                String dateFormatee = dateHeure.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                
                String message = "✅ Dr " + nomMedecin + " a accepté votre consultation le " + dateFormatee + 
                                ". Rejoignez la consultation ici.";
                
                notificationService.notifier(
                    (long) consultation.getPatientId(),
                    message,
                    "success",
                    lienMeet
                );
                System.out.println("[ConsultationServiceArij] Notification envoyée au patient");
            } catch (Exception e) {
                System.err.println("[ConsultationServiceArij] Erreur lors de l'envoi de la notification : " + e.getMessage());
            }

            // 6. Envoyer email si Brevo est disponible (optionnel)
            // TODO: Implémenter BrevoEmailServiceArij si nécessaire
            /*
            try {
                BrevoEmailServiceArij emailService = new BrevoEmailServiceArij();
                String emailPatient = consultation.getPatient() != null ? consultation.getPatient().getEmail() : "";
                String nomMedecin = consultation.getDoctor() != null ? consultation.getDoctor().getUsername() : "Dr. Inconnu";
                String dateFormatee = dateHeure.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                
                if (!emailPatient.isEmpty()) {
                    emailService.envoyerAcceptation(
                        emailPatient,
                        consultation.getPatient().getUsername(),
                        nomMedecin,
                        dateFormatee,
                        lienMeet
                    );
                    System.out.println("[ConsultationServiceArij] Email d'acceptation envoyé");
                }
            } catch (Exception e) {
                System.err.println("[ConsultationServiceArij] Brevo non disponible ou erreur email : " + e.getMessage());
            }
            */

            return consultation;

        } catch (Exception e) {
            System.err.println("[ConsultationServiceArij] Erreur lors de l'acceptation : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Récupère une consultation par ID avec ses données associées.
     */
    public ConsultationsArij getConsultationById(Long consultationId) throws Exception {
        String sql = "SELECT * FROM consultations WHERE id = ?";
        
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, consultationId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                ConsultationsArij consultation = new ConsultationsArij();
                consultation.setId(rs.getInt("id"));
                consultation.setPatientId(rs.getInt("patient_id"));
                consultation.setDoctorId(rs.getInt("doctor_id"));
                consultation.setConsultationDate(rs.getTimestamp("consultation_date") != null ? 
                    rs.getTimestamp("consultation_date").toLocalDateTime() : null);
                consultation.setType(rs.getString("type"));
                consultation.setStatus(rs.getString("status"));
                consultation.setDeleted(rs.getBoolean("is_deleted"));
                consultation.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
                consultation.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
                consultation.setRejectionReason(rs.getString("rejection_reason"));
                consultation.setConsultationFee(rs.getDouble("consultation_fee"));
                consultation.setLienMeet(rs.getString("lien_meet"));
                
                // Charger les données du patient et du médecin
                consultation.setPatient(getUserById(rs.getInt("patient_id")));
                consultation.setDoctor(getUserById(rs.getInt("doctor_id")));
                
                return consultation;
            }
        } catch (SQLException e) {
            System.err.println("[ConsultationServiceArij] Erreur lecture consultation : " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Récupère les consultations en attente d'un médecin.
     */
    public List<ConsultationsArij> getConsultationsEnAttente(Long doctorId) throws Exception {
        List<ConsultationsArij> list = new ArrayList<>();
        String sql = "SELECT * FROM consultations WHERE doctor_id = ? AND status = 'en_attente' ORDER BY created_at DESC";
        
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, doctorId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ConsultationsArij consultation = new ConsultationsArij();
                consultation.setId(rs.getInt("id"));
                consultation.setPatientId(rs.getInt("patient_id"));
                consultation.setDoctorId(rs.getInt("doctor_id"));
                consultation.setConsultationDate(rs.getTimestamp("consultation_date") != null ? 
                    rs.getTimestamp("consultation_date").toLocalDateTime() : null);
                consultation.setType(rs.getString("type"));
                consultation.setStatus(rs.getString("status"));
                consultation.setDeleted(rs.getBoolean("is_deleted"));
                consultation.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
                consultation.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
                consultation.setRejectionReason(rs.getString("rejection_reason"));
                consultation.setConsultationFee(rs.getDouble("consultation_fee"));
                consultation.setLienMeet(rs.getString("lien_meet"));
                
                // Charger les données du patient et du médecin
                consultation.setPatient(getUserById(rs.getInt("patient_id")));
                consultation.setDoctor(getUserById(rs.getInt("doctor_id")));
                
                list.add(consultation);
            }
        } catch (SQLException e) {
            System.err.println("[ConsultationServiceArij] Erreur lecture consultations : " + e.getMessage());
        }
        
        return list;
    }

    /**
     * Récupère un utilisateur par ID.
     */
    private User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("[ConsultationServiceArij] Erreur lecture utilisateur : " + e.getMessage());
        }
        
        return null;
    }
}
