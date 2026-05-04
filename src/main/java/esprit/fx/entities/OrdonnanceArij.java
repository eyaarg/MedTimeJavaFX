package esprit.fx.entities;

import esprit.fx.services.ChiffrementServiceArij;
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
    private String accessToken;
    private List<LigneOrdonnanceArij> lignes = new ArrayList<>();

    public OrdonnanceArij() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConsultationId() { return consultationId; }
    public void setConsultationId(int consultationId) { this.consultationId = consultationId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNumeroOrdonnance() { return numeroOrdonnance; }
    public void setNumeroOrdonnance(String numeroOrdonnance) { this.numeroOrdonnance = numeroOrdonnance; }
    public LocalDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDateTime dateEmission) { this.dateEmission = dateEmission; }
    public LocalDateTime getDateValidite() { return dateValidite; }
    public void setDateValidite(LocalDateTime dateValidite) { this.dateValidite = dateValidite; }
    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTokenVerification() { return tokenVerification; }
    public void setTokenVerification(String tokenVerification) { this.tokenVerification = tokenVerification; }
    public String getDocumentNom() { return documentNom; }
    public void setDocumentNom(String documentNom) { this.documentNom = documentNom; }
    public int getDocumentSize() { return documentSize; }
    public void setDocumentSize(int documentSize) { this.documentSize = documentSize; }
    public String getDocumentMimeType() { return documentMimeType; }
    public void setDocumentMimeType(String documentMimeType) { this.documentMimeType = documentMimeType; }
    public String getDocumentOriginalName() { return documentOriginalName; }
    public void setDocumentOriginalName(String documentOriginalName) { this.documentOriginalName = documentOriginalName; }
    public List<LigneOrdonnanceArij> getLignes() { return lignes; }
    public void setLignes(List<LigneOrdonnanceArij> lignes) { this.lignes = lignes != null ? lignes : new ArrayList<>(); }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    /**
     * Construit l'URL de scan du QR Code pour cette ordonnance.
     * @param baseUrl URL de base (ex: http://localhost:8000)
     * @return URL complète de scan
     */
    public String buildScanUrl(String baseUrl) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return baseUrl + "/ordonnance/scan/" + accessToken;
    }

    /**
     * Déchiffre les données après chargement depuis la BDD.
     */
    public void dechiffrerApresChargement() {
        ChiffrementServiceArij cs = ChiffrementServiceArij.getInstance();
        this.diagnosis = cs.dechiffrer(this.diagnosis);
        this.content = cs.dechiffrer(this.content);
        this.instructions = cs.dechiffrer(this.instructions);
        System.out.println("[OrdonnanceArij] Données déchiffrées après chargement");
    }

    /**
     * Chiffre les données avant sauvegarde en BDD.
     */
    public void chiffrerAvantSauvegarde() {
        ChiffrementServiceArij cs = ChiffrementServiceArij.getInstance();
        this.diagnosis = cs.chiffrer(this.diagnosis);
        this.content = cs.chiffrer(this.content);
        this.instructions = cs.chiffrer(this.instructions);
        System.out.println("[OrdonnanceArij] Données chiffrées avant sauvegarde");
    }
}
