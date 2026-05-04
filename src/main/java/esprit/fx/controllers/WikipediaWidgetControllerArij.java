package esprit.fx.controllers;

import esprit.fx.entities.WikipediaResult;
import esprit.fx.services.WikipediaService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;

/**
 * Contrôleur pour le widget Wikipedia d'informations santé.
 */
public class WikipediaWidgetControllerArij {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private VBox loadingContainer;
    @FXML private VBox resultsContainer;
    @FXML private Label noResultsLabel;
    @FXML private Label titleLabel;
    @FXML private ImageView imageView;
    @FXML private Label summaryLabel;
    @FXML private Button linkButton;

    private final WikipediaService wikipediaService = new WikipediaService();
    private String currentArticleUrl;

    @FXML
    private void initialize() {
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());
        linkButton.setOnAction(e -> openArticle());
    }

    /**
     * Effectue une recherche Wikipedia.
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un terme de recherche", Alert.AlertType.WARNING);
            return;
        }

        // Afficher loading, cacher le reste
        loadingContainer.setVisible(true);
        loadingContainer.setManaged(true);
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);
        noResultsLabel.setVisible(false);
        noResultsLabel.setManaged(false);

        Task<WikipediaResult> task = new Task<WikipediaResult>() {
            @Override
            protected WikipediaResult call() {
                return wikipediaService.chercherAvecFallback(query);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                WikipediaResult result = task.getValue();
                loadingContainer.setVisible(false);
                loadingContainer.setManaged(false);

                if (result.trouve) {
                    displayResult(result);
                } else {
                    noResultsLabel.setText("Aucun résultat pour \"" + query + "\"");
                    noResultsLabel.setVisible(true);
                    noResultsLabel.setManaged(true);
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                loadingContainer.setVisible(false);
                loadingContainer.setManaged(false);
                noResultsLabel.setText("Erreur lors de la recherche");
                noResultsLabel.setVisible(true);
                noResultsLabel.setManaged(true);
                System.err.println("[WikipediaWidgetControllerArij] Erreur: " + task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    /**
     * Affiche les résultats de la recherche.
     */
    private void displayResult(WikipediaResult result) {
        titleLabel.setText(result.titre);
        summaryLabel.setText(result.resume);
        currentArticleUrl = result.urlComplete;

        // Charger l'image
        if (result.imageUrl != null && !result.imageUrl.isEmpty()) {
            imageView.setVisible(false); // cacher pendant le chargement
            Task<Image> imageTask = new Task<Image>() {
                @Override
                protected Image call() {
                    try {
                        return new Image(result.imageUrl, 320, 160, true, true);
                    } catch (Exception e) {
                        System.err.println("[WikipediaWidgetControllerArij] Erreur image: " + e.getMessage());
                        return null;
                    }
                }
            };
            imageTask.setOnSucceeded(e -> {
                Image image = imageTask.getValue();
                if (image != null && !image.isError()) {
                    imageView.setImage(image);
                    imageView.setVisible(true);
                }
            });
            new Thread(imageTask).start();
        } else {
            imageView.setVisible(false);
        }

        // Afficher les résultats
        resultsContainer.setVisible(true);
        resultsContainer.setManaged(true);
        noResultsLabel.setVisible(false);
        noResultsLabel.setManaged(false);
    }

    /**
     * Ouvre l'article Wikipedia complet.
     */
    private void openArticle() {
        if (currentArticleUrl == null || currentArticleUrl.isEmpty()) {
            showAlert("Erreur", "URL de l'article non disponible", Alert.AlertType.ERROR);
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(currentArticleUrl));
                System.out.println("[WikipediaWidgetControllerArij] Ouverture: " + currentArticleUrl);
            }
        } catch (Exception e) {
            System.err.println("[WikipediaWidgetControllerArij] Erreur ouverture lien: " + e.getMessage());
            showAlert("Erreur", "Impossible d'ouvrir l'article", Alert.AlertType.ERROR);
        }
    }

    /**
     * Affiche une alerte.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
