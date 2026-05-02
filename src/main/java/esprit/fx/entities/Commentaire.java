package esprit.fx.entities;

import java.util.Date;

public class Commentaire {
    private int id;
    private String contenu;
    private Date dateCommentaire;
    private int nbLikes;
    private int utilisateurId;
    private String username;

    private Article article;

    public Commentaire() {}

    public Commentaire(String contenu, Date dateCommentaire,
                       int nbLikes, Article article) {
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.nbLikes = nbLikes;
        this.article = article;
    }

    public Commentaire(int id, String contenu, Date dateCommentaire,
                       int nbLikes, Article article) {
        this.id = id;
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.nbLikes = nbLikes;
        this.article = article;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Date getDateCommentaire() { return dateCommentaire; }
    public void setDateCommentaire(Date dateCommentaire) { this.dateCommentaire = dateCommentaire; }

    public int getNbLikes() { return nbLikes; }
    public void setNbLikes(int nbLikes) { this.nbLikes = nbLikes; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateCommentaire=" + dateCommentaire +
                ", nbLikes=" + nbLikes +
                '}';
    }
}