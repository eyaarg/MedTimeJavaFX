package esprit.fx.entities;

import java.time.LocalDateTime;

public class PaiementConsultationsArij {
    private int id;
    private double montant;
    private String methode;
    private String status;
    private LocalDateTime createdAt;
    private int consultationId;
    private int patientId;

    public PaiementConsultationsArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public String getMethode() { return methode; }
    public void setMethode(String methode) { this.methode = methode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getConsultationId() { return consultationId; }
    public void setConsultationId(int consultationId) { this.consultationId = consultationId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
}
