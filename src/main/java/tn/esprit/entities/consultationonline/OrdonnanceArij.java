package tn.esprit.entities.consultationonline;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceArij {
    private int id;
    private int consultationId;
    private int doctorId;
    private String content;
    private String diagnosis;
    private String numeroOrdonnance;
    private LocalDateTime dateEmission;
    private LocalDateTime dateValidite;
    private String signaturePath;
    private String instructions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tokenVerification;
    private String documentNom;
    private int documentSize;
    private String documentMimeType;
    private String documentOriginalName;
    private List<LigneOrdonnanceArij> lignes = new ArrayList<>();

    public OrdonnanceArij() {
    }

    public OrdonnanceArij(int id, int consultationId, int doctorId, String content, String diagnosis,
                          String numeroOrdonnance, LocalDateTime dateEmission, LocalDateTime dateValidite,
                          String signaturePath, String instructions, LocalDateTime createdAt,
                          LocalDateTime updatedAt, String tokenVerification, String documentNom,
                          int documentSize, String documentMimeType, String documentOriginalName,
                          List<LigneOrdonnanceArij> lignes) {
        this.id = id;
        this.consultationId = consultationId;
        this.doctorId = doctorId;
        this.content = content;
        this.diagnosis = diagnosis;
        this.numeroOrdonnance = numeroOrdonnance;
        this.dateEmission = dateEmission;
        this.dateValidite = dateValidite;
        this.signaturePath = signaturePath;
        this.instructions = instructions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tokenVerification = tokenVerification;
        this.documentNom = documentNom;
        this.documentSize = documentSize;
        this.documentMimeType = documentMimeType;
        this.documentOriginalName = documentOriginalName;
        if (lignes != null) {
            this.lignes = lignes;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNumeroOrdonnance() {
        return numeroOrdonnance;
    }

    public void setNumeroOrdonnance(String numeroOrdonnance) {
        this.numeroOrdonnance = numeroOrdonnance;
    }

    public LocalDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDateTime getDateValidite() {
        return dateValidite;
    }

    public void setDateValidite(LocalDateTime dateValidite) {
        this.dateValidite = dateValidite;
    }

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String signaturePath) {
        this.signaturePath = signaturePath;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
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

    public String getTokenVerification() {
        return tokenVerification;
    }

    public void setTokenVerification(String tokenVerification) {
        this.tokenVerification = tokenVerification;
    }

    public String getDocumentNom() {
        return documentNom;
    }

    public void setDocumentNom(String documentNom) {
        this.documentNom = documentNom;
    }

    public int getDocumentSize() {
        return documentSize;
    }

    public void setDocumentSize(int documentSize) {
        this.documentSize = documentSize;
    }

    public String getDocumentMimeType() {
        return documentMimeType;
    }

    public void setDocumentMimeType(String documentMimeType) {
        this.documentMimeType = documentMimeType;
    }

    public String getDocumentOriginalName() {
        return documentOriginalName;
    }

    public void setDocumentOriginalName(String documentOriginalName) {
        this.documentOriginalName = documentOriginalName;
    }

    public List<LigneOrdonnanceArij> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneOrdonnanceArij> lignes) {
        if (lignes == null) {
            this.lignes = new ArrayList<>();
        } else {
            this.lignes = lignes;
        }
    }

    @Override
    public String toString() {
        return "OrdonnanceArij{" +
                "id=" + id +
                ", consultationId=" + consultationId +
                ", doctorId=" + doctorId +
                ", content='" + content + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                ", numeroOrdonnance='" + numeroOrdonnance + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateValidite=" + dateValidite +
                ", signaturePath='" + signaturePath + '\'' +
                ", instructions='" + instructions + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", tokenVerification='" + tokenVerification + '\'' +
                ", documentNom='" + documentNom + '\'' +
                ", documentSize=" + documentSize +
                ", documentMimeType='" + documentMimeType + '\'' +
                ", documentOriginalName='" + documentOriginalName + '\'' +
                ", lignes=" + lignes +
                '}';
    }
}
