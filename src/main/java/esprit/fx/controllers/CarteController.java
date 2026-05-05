package esprit.fx.controllers;

import esprit.fx.services.OpenStreetMapService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CarteController implements Initializable {

    @FXML private WebView webViewCarte;
    @FXML private Button  btnFermer;
    @FXML private HBox    loadingBar;
    @FXML private Label   labelChargement;

    private WebEngine webEngine;

    // Données passées depuis DisponibiliteController
    private String doctorNom;
    private String adresse;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webViewCarte.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Charger la page HTML de base
        URL htmlUrl = getClass().getResource("/carte.html");
        if (htmlUrl != null) {
            webEngine.load(htmlUrl.toExternalForm());
        } else {
            webEngine.loadContent(buildFallbackHtml());
        }
    }

    /**
     * Appelé depuis DisponibiliteController pour initialiser la carte.
     *
     * @param doctorNom nom du médecin
     * @param adresse   adresse du cabinet (peut être null)
     */
    public void initCarte(String doctorNom, String adresse) {
        this.doctorNom = doctorNom;
        this.adresse   = adresse;

        labelChargement.setText("Recherche de l'adresse : " + adresse + "...");
        loadingBar.setVisible(true);
        loadingBar.setManaged(true);

        // Géocodage dans un thread séparé
        Thread thread = new Thread(() -> {
            OpenStreetMapService service = new OpenStreetMapService();
            String adresseRecherche = service.buildAdresseRecherche(doctorNom, adresse);
            OpenStreetMapService.Coordonnees coords = service.getCoordonnees(adresseRecherche);

            Platform.runLater(() -> {
                loadingBar.setVisible(false);
                loadingBar.setManaged(false);

                if (coords != null) {
                    updateCarte(coords.latitude, coords.longitude,
                                "Cabinet Dr. " + doctorNom,
                                coords.adresseComplete);
                } else {
                    // Fallback : centre de Tunis (36.8065, 10.1815)
                    updateCarte(36.8065, 10.1815,
                                "Cabinet Dr. " + doctorNom,
                                adresse != null && !adresse.isEmpty()
                                    ? adresse + ", Tunis, Tunisie" : "Tunis, Tunisie");
                    labelChargement.setText("⚠️ Adresse approximative — Centre de Tunis");
                    labelChargement.setStyle(
                        "-fx-font-size: 13px; -fx-text-fill: #92400e; -fx-font-weight: 600;");
                    loadingBar.setStyle(
                        "-fx-background-color: #fef3c7; -fx-padding: 10;" +
                        "-fx-border-color: #fde68a; -fx-border-width: 0 0 1 0;");
                    loadingBar.setVisible(true);
                    loadingBar.setManaged(true);
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    /** Injecte les coordonnées dans la page HTML via JavaScript. */
    private void updateCarte(double lat, double lon, String nom, String adresseAffichee) {
        // Attendre que la page soit chargée avant d'exécuter le script
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                executeMapUpdate(lat, lon, nom, adresseAffichee);
            }
        });

        // Si la page est déjà chargée
        if (webEngine.getLoadWorker().getState()
                == javafx.concurrent.Worker.State.SUCCEEDED) {
            executeMapUpdate(lat, lon, nom, adresseAffichee);
        }
    }

    private void executeMapUpdate(double lat, double lon, String nom, String adresse) {
        // Forcer le point comme séparateur décimal (pas la virgule française)
        String latStr = String.format(java.util.Locale.US, "%.6f", lat);
        String lonStr = String.format(java.util.Locale.US, "%.6f", lon);

        // Échapper les apostrophes pour éviter les erreurs JS
        String nomSafe     = nom.replace("'", "\\'").replace("\"", "\\\"");
        String adresseSafe = adresse.replace("'", "\\'").replace("\"", "\\\"");

        String script = String.format(java.util.Locale.US,
            "updateMap(%s, %s, '%s', '%s');",
            latStr, lonStr, nomSafe, adresseSafe
        );
        webEngine.executeScript(script);
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }

    private String buildFallbackHtml() {
        return "<html><body style='font-family:sans-serif;text-align:center;padding:40px;'>"
             + "<h2>⚠️ Impossible de charger la carte</h2>"
             + "<p>Le fichier carte.html est introuvable.</p>"
             + "</body></html>";
    }
}
