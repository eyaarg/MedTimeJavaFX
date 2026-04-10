package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class ConsultationArij {
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

    public ConsultationArij() {
    }

    public ConsultationArij(int id, int patientId, int doctorId, LocalDateTime consultationDate, String type,
                            String status, boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt,
                            String rejectionReason, double consultationFee, String lienMeet) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;
        this.type = type;
        this.status = status;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.rejectionReason = rejectionReason;
        this.consultationFee = consultationFee;
        this.lienMeet = lienMeet;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getConsultationDate() {
        return consultationDate;
    }

    public void setConsultationDate(LocalDateTime consultationDate) {
        this.consultationDate = consultationDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getLienMeet() {
        return lienMeet;
    }

    public void setLienMeet(String lienMeet) {
        this.lienMeet = lienMeet;
    }

    @Override
    public String toString() {
        return "ConsultationArij{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", consultationDate=" + consultationDate +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", consultationFee=" + consultationFee +
                ", lienMeet='" + lienMeet + '\'' +
                '}';
    }
}
