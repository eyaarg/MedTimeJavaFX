package esprit.fx.entities;

import java.util.List;

public class Categorie {
    private int id;
    private String nom;
    private String description;
    private String icone;

    private List<Article> articles;

    public Categorie() {}

    public Categorie(String nom, String description, String icone) {
        this.nom = nom;
        this.description = description;
        this.icone = icone;
    }

    public Categorie(int id, String nom, String description, String icone) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.icone = icone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }

    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }

    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                '}';
    }
}