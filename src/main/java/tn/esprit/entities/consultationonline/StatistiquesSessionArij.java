package tn.esprit.entities.consultationonline;

public class StatistiquesSessionArij {
    private int id;
    private int duree;
    private String qualiteConnexion;
    private int nbMessages;
    private int consultationId;

    public StatistiquesSessionArij() {
    }

    public StatistiquesSessionArij(int id, int duree, String qualiteConnexion, int nbMessages, int consultationId) {
        this.id = id;
        this.duree = duree;
        this.qualiteConnexion = qualiteConnexion;
        this.nbMessages = nbMessages;
        this.consultationId = consultationId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public String getQualiteConnexion() {
        return qualiteConnexion;
    }

    public void setQualiteConnexion(String qualiteConnexion) {
        this.qualiteConnexion = qualiteConnexion;
    }

    public int getNbMessages() {
        return nbMessages;
    }

    public void setNbMessages(int nbMessages) {
        this.nbMessages = nbMessages;
    }

    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    @Override
    public String toString() {
        return "StatistiquesSessionArij{" +
                "id=" + id +
                ", duree=" + duree +
                ", qualiteConnexion='" + qualiteConnexion + '\'' +
                ", nbMessages=" + nbMessages +
                ", consultationId=" + consultationId +
                '}';
    }
}
