package esprit.fx.controllers;

import esprit.fx.entities.WikipediaResult;
import esprit.fx.services.WikipediaService;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;

/**
 * Contrôleur pour le panneau latéral Wikipedia.
 * Affiche les informations sur une maladie depuis Wikipedia.
 */
public class WikipediaSidePanelController {

    @FXML private VBox rootPane;
    @FXML private Button btnFermer;
    @FXML private ImageView imageView;
    @FXML private Label titreLabel;
    @FXML private Separator separator;
    @FXML private ScrollPane scrollPane;
    @FXML private Label resumeLabel;
    @FXML private Hyperlink lienArticle;
    @FXML private Label sourceLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label notFoundLabel;

    private final WikipediaService wikipediaService = new WikipediaService();
    private String urlArticleActuel;

    @FXML
    private void initialize() {
        // Configurer le bouton fermer
        btnFermer.setOnAction(e -> fermerPanneau());

        // Configurer le lien article
        lienArticle.setOnAction(e -> ouvrirArticle());

        // Cacher le panneau par défaut
        rootPane.setVisible(false);
    }

    /**
     * Affiche le panneau avec les informations d'une maladie.
     * 
     * @param maladie Nom de la maladie à afficher
     */
    public void afficherMaladie(String maladie) {
        if (maladie == null || maladie.trim().isEmpty()) {
            fermerPanneau();
            return;
        }

        // Afficher le panneau et l'indicateur de chargement
        rootPane.setVisible(true);
        loadingIndicator.setVisible(true);
        titreLabel.setVisible(false);
        separator.setVisible(false);
        scrollPane.setVisible(false);
        lienArticle.setVisible(false);
        sourceLabel.setVisible(false);
        imageView.setVisible(false);
        notFoundLabel.setVisible(false);

        // Animation d'apparition du panneau
        animerApparition();

        // Lancer la recherche en arrière-plan
        Task<WikipediaResult> task = new Task<WikipediaResult>() {
            @Override
            protected WikipediaResult call() throws Exception {
                return wikipediaService.chercherAvecFallback(maladie);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                WikipediaResult result = task.getValue();
                loadingIndicator.setVisible(false);

                if (result.trouve) {
                    afficherResultat(result);
                } else {
                    notFoundLabel.setVisible(true);
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                notFoundLabel.setText("⚠ Erreur lors de la recherche");
                notFoundLabel.setVisible(true);
            });
        });

        new Thread(task).start();
    }

    /**
     * Affiche les résultats de la recherche Wikipedia.
     */
    private void afficherResultat(WikipediaResult result) {
        // Afficher le titre
        titreLabel.setText(result.titre);
        titreLabel.setVisible(true);

        // Afficher le séparateur
        separator.setVisible(true);

        // Afficher le résumé
        resumeLabel.setText(result.resume);
        scrollPane.setVisible(true);

        // Afficher le lien article
        urlArticleActuel = result.urlComplete;
        lienArticle.setVisible(true);

        // Afficher la source
        sourceLabel.setVisible(true);

        // Charger et afficher l'image si disponible
        if (result.imageUrl != null && !result.imageUrl.isEmpty()) {
            chargerImage(result.imageUrl);
        }
    }

    /**
     * Charge l'image en arrière-plan avec timeout.
     */
    private void chargerImage(String imageUrl) {
        Task<Image> imageTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                try {
                    // Charger l'image avec timeout de 3 secondes
                    return new Image(imageUrl, 200, 120, false, true);
                } catch (Exception e) {
                    System.err.println("[WikipediaSidePanelController] Erreur chargement image: " + e.getMessage());
                    return null;
                }
            }
        };

        imageTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                Image image = imageTask.getValue();
                if (image != null && !image.isError()) {
                    imageView.setImage(image);
                    imageView.setVisible(true);
                }
            });
        });

        imageTask.setOnFailed(e -> {
            System.err.println("[WikipediaSidePanelController] Erreur chargement image");
        });

        new Thread(imageTask).start();
    }

    /**
     * Anime l'apparition du panneau (glisse depuis la droite).
     */
    private void animerApparition() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), rootPane);
        transition.setFromX(280);
        transition.setToX(0);
        transition.play();
    }

    /**
     * Anime la disparition du panneau (glisse vers la droite).
     */
    private void animerDisparition() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), rootPane);
        transition.setFromX(0);
        transition.setToX(280);
        transition.setOnFinished(e -> rootPane.setVisible(false));
        transition.play();
    }

    /**
     * Ferme le panneau.
     */
    public void fermerPanneau() {
        animerDisparition();
    }

    /**
     * Ouvre l'article Wikipedia complet dans le navigateur.
     */
    private void ouvrirArticle() {
        if (urlArticleActuel != null && !urlArticleActuel.isEmpty()) {
            try {
                Desktop.getDesktop().browse(new URI(urlArticleActuel));
            } catch (Exception e) {
                System.err.println("[WikipediaSidePanelController] Erreur ouverture navigateur: " + e.getMessage());
            }
        }
    }
}
