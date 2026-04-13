package esprit.fx.entities;

import java.util.Date;
import java.util.List;

public class Article {
    private int id;
    private String titre;
    private String contenu;
    private Date datePublication;
    private String image;
    private int nbLikes;
    private int nbVues;
    private String tags;
    private String statut;

    private Categorie categorie;
    private List<Commentaire> commentaires;

    public Article() {}

    public Article(String titre, String contenu, Date datePublication,
                   String image, int nbLikes, int nbVues, String tags,
                   String statut, Categorie categorie) {
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.image = image;
        this.nbLikes = nbLikes;
        this.nbVues = nbVues;
        this.tags = tags;
        this.statut = statut;
        this.categorie = categorie;
    }

    public Article(int id, String titre, String contenu, Date datePublication,
                   String image, int nbLikes, int nbVues, String tags,
                   String statut, Categorie categorie) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.image = image;
        this.nbLikes = nbLikes;
        this.nbVues = nbVues;
        this.tags = tags;
        this.statut = statut;
        this.categorie = categorie;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Date getDatePublication() { return datePublication; }
    public void setDatePublication(Date datePublication) { this.datePublication = datePublication; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getNbLikes() { return nbLikes; }
    public void setNbLikes(int nbLikes) { this.nbLikes = nbLikes; }

    public int getNbVues() { return nbVues; }
    public void setNbVues(int nbVues) { this.nbVues = nbVues; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) { this.commentaires = commentaires; }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", statut='" + statut + '\'' +
                ", datePublication=" + datePublication +
                ", tags='" + tags + '\'' +
                '}';
    }
}