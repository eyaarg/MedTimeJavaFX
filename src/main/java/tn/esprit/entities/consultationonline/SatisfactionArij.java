package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;

public class SatisfactionArij {
    private int id;
    private int score;
    private String commentaire;
    private LocalDateTime createdAt;
    private int consultationId;
    private int patientId;

    public SatisfactionArij() {
    }

    public SatisfactionArij(int id, int score, String commentaire, LocalDateTime createdAt, int consultationId,
                            int patientId) {
        this.id = id;
        this.score = score;
        this.commentaire = commentaire;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
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
        return "SatisfactionArij{" +
                "id=" + id +
                ", score=" + score +
                ", commentaire='" + commentaire + '\'' +
                ", createdAt=" + createdAt +
                ", consultationId=" + consultationId +
                ", patientId=" + patientId +
                '}';
    }
}
