package esprit.fx.entities;

/**
 * DTO représentant le résultat d'une recherche Wikipedia.
 * Contient les informations essentielles sur une maladie.
 */
public class WikipediaResult {
    public String titre;
    public String resume;        // Extrait tronqué à 400 caractères
    public String imageUrl;      // URL de la miniature (peut être null)
    public String urlComplete;   // Lien vers l'article complet
    public boolean trouve;       // Indique si la maladie a été trouvée

    /**
     * Constructeur pour un résultat trouvé.
     */
    public WikipediaResult(String titre, String resume, String imageUrl, String urlComplete) {
        this.titre = titre;
        this.resume = resume;
        this.imageUrl = imageUrl;
        this.urlComplete = urlComplete;
        this.trouve = true;
    }

    /**
     * Constructeur pour un résultat non trouvé.
     */
    public WikipediaResult() {
        this.titre = "";
        this.resume = "";
        this.imageUrl = null;
        this.urlComplete = "";
        this.trouve = false;
    }

    @Override
    public String toString() {
        return "WikipediaResult{" +
                "titre='" + titre + '\'' +
                ", trouve=" + trouve +
                '}';
    }
}
