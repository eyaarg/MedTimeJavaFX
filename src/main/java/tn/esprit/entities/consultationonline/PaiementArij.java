package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class PaiementArij {
    private int id;
    private double montant;
    private String methode;
    private String status;
    private LocalDateTime createdAt;
    private int consultationId;
    private int patientId;

    public PaiementArij() {
    }

    public PaiementArij(int id, double montant, String methode, String status, LocalDateTime createdAt,
                        int consultationId, int patientId) {
        this.id = id;
        this.montant = montant;
        this.methode = methode;
        this.status = status;
        this.createdAt = createdAt;
        this.consultationId = consultationId;
        this.patientId = patientId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    @Override
    public String toString() {
        return "PaiementArij{" +
                "id=" + id +
                ", montant=" + montant +
                ", methode='" + methode + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", consultationId=" + consultationId +
                ", patientId=" + patientId +
                '}';
    }
}
