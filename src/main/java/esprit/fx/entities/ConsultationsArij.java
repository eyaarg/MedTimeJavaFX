package esprit.fx.entities;

import esprit.fx.services.ChiffrementServiceArij;
import java.time.LocalDateTime;

public class ConsultationsArij {
    private int id;
    private int patientId;
    private int doctorId;
    private LocalDateTime consultationDate;
    private String type;
    private String status;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectionReason;
    private double consultationFee;
    private String lienMeet;
    private boolean smsSuiviEnvoye;
    
    // Références aux entités (pour faciliter l'accès aux données)
    private User patient;
    private User doctor;

    public ConsultationsArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getConsultationDate() { return consultationDate; }
    public void setConsultationDate(LocalDateTime consultationDate) { this.consultationDate = consultationDate; }
    
    // Alias pour setDateConsultation (utilisé par ConsultationServiceArij)
    public void setDateConsultation(LocalDateTime dateConsultation) { 
        this.consultationDate = dateConsultation; 
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }
    public String getLienMeet() { return lienMeet; }
    public void setLienMeet(String lienMeet) { this.lienMeet = lienMeet; }
    public boolean isSmsSuiviEnvoye() { return smsSuiviEnvoye; }
    public void setSmsSuiviEnvoye(boolean smsSuiviEnvoye) { this.smsSuiviEnvoye = smsSuiviEnvoye; }
    
    // Getters pour les entités
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    /**
     * Déchiffre les données après chargement depuis la BDD.
     */
    public void dechiffrerApresChargement() {
        ChiffrementServiceArij cs = ChiffrementServiceArij.getInstance();
        this.type = cs.dechiffrer(this.type);
        this.rejectionReason = cs.dechiffrer(this.rejectionReason);
        System.out.println("[ConsultationsArij] Données déchiffrées après chargement");
    }

    /**
     * Chiffre les données avant sauvegarde en BDD.
     */
    public void chiffrerAvantSauvegarde() {
        ChiffrementServiceArij cs = ChiffrementServiceArij.getInstance();
        this.type = cs.chiffrer(this.type);
        this.rejectionReason = cs.chiffrer(this.rejectionReason);
        System.out.println("[ConsultationsArij] Données chiffrées avant sauvegarde");
    }
}
