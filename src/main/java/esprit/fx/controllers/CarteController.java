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

        // Désactiver les messages d'erreur JavaScript dans la console
        webEngine.setOnError(event -> {
            System.err.println("[CarteController] Erreur WebEngine: " + event.getMessage());
        });

        // Charger la page HTML de base
        URL htmlUrl = getClass().getResource("/carte.html");
        if (htmlUrl != null) {
            System.out.println("[CarteController] Chargement de carte.html depuis: " + htmlUrl.toExternalForm());
            webEngine.load(htmlUrl.toExternalForm());
        } else {
            System.err.println("[CarteController] ERREUR: carte.html introuvable dans /carte.html");
            System.err.println("[CarteController] Chargement du HTML de secours");
            webEngine.loadContent(buildFallbackHtml());
        }
        
        // Ajouter un listener pour détecter les erreurs de chargement
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("[CarteController] Etat de chargement: " + newState);
            
            if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("[CarteController] Echec du chargement de la page");
                System.err.println("[CarteController] Exception: " + webEngine.getLoadWorker().getException());
                webEngine.loadContent(buildFallbackHtml());
            } else if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("[CarteController] Page chargee avec succes");
            } else if (newState == javafx.concurrent.Worker.State.RUNNING) {
                System.out.println("[CarteController] Chargement en cours...");
            }
        });
        
        // Ajouter un timeout pour détecter si le chargement prend trop de temps
        new Thread(() -> {
            try {
                Thread.sleep(10000); // 10 secondes
                Platform.runLater(() -> {
                    if (webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.RUNNING) {
                        System.err.println("[CarteController] Timeout: le chargement prend trop de temps");
                        webEngine.getLoadWorker().cancel();
                        webEngine.loadContent(buildFallbackHtml());
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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

        System.out.println("[CarteController] Initialisation carte pour: " + doctorNom);
        System.out.println("[CarteController] Adresse: " + adresse);

        labelChargement.setText("Recherche de l'adresse : " + adresse + "...");
        loadingBar.setVisible(true);
        loadingBar.setManaged(true);

        // Géocodage dans un thread séparé
        Thread thread = new Thread(() -> {
            OpenStreetMapService service = new OpenStreetMapService();
            String adresseRecherche = service.buildAdresseRecherche(doctorNom, adresse);
            System.out.println("[CarteController] Adresse de recherche: " + adresseRecherche);
            
            OpenStreetMapService.Coordonnees coords = service.getCoordonnees(adresseRecherche);

            Platform.runLater(() -> {
                loadingBar.setVisible(false);
                loadingBar.setManaged(false);

                if (coords != null) {
                    System.out.println("[CarteController] Coordonnees obtenues: " + coords.latitude + ", " + coords.longitude);
                    updateCarte(coords.latitude, coords.longitude,
                                "Cabinet Dr. " + doctorNom,
                                coords.adresseComplete);
                } else {
                    System.out.println("[CarteController] Utilisation des coordonnees par defaut (Tunis)");
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
        
        System.out.println("[CarteController] Execution script JS: " + script);
        
        try {
            webEngine.executeScript(script);
            System.out.println("[CarteController] Carte mise a jour avec succes");
        } catch (Exception e) {
            System.err.println("[CarteController] Erreur lors de l'execution du script JS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }

    private String buildFallbackHtml() {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<style>" +
               "* { margin: 0; padding: 0; box-sizing: border-box; }" +
               "body { font-family: Arial, sans-serif; background: #f8fafc; display: flex; flex-direction: column; height: 100vh; }" +
               ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 20px; }" +
               ".header h2 { font-size: 18px; margin-bottom: 5px; }" +
               ".header p { font-size: 13px; opacity: 0.9; }" +
               ".content { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; }" +
               ".icon { font-size: 64px; margin-bottom: 20px; }" +
               ".message { text-align: center; max-width: 500px; }" +
               ".message h3 { color: #1f2937; font-size: 20px; margin-bottom: 10px; }" +
               ".message p { color: #6b7280; font-size: 14px; line-height: 1.6; margin-bottom: 20px; }" +
               ".info-box { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 15px; margin-top: 20px; }" +
               ".info-box strong { color: #1d4ed8; display: block; margin-bottom: 5px; }" +
               ".info-box span { color: #374151; font-size: 13px; }" +
               ".btn { background: #3b82f6; color: white; padding: 10px 20px; border-radius: 6px; text-decoration: none; display: inline-block; margin-top: 15px; }" +
               "</style></head><body>" +
               "<div class='header'>" +
               "<h2>🗺️ Localisation du Cabinet</h2>" +
               "<p>Carte OpenStreetMap</p>" +
               "</div>" +
               "<div class='content'>" +
               "<div class='icon'>📍</div>" +
               "<div class='message'>" +
               "<h3>Carte temporairement indisponible</h3>" +
               "<p>La carte OpenStreetMap ne peut pas être chargée pour le moment. " +
               "Cela peut être dû à une connexion Internet limitée ou à un problème de chargement des ressources.</p>" +
               "<div class='info-box' id='location-info'>" +
               "<strong>📌 Adresse du cabinet:</strong>" +
               "<span id='address-text'>Chargement...</span>" +
               "</div>" +
               "<p style='margin-top: 20px; font-size: 12px; color: #9ca3af;'>" +
               "💡 Astuce: Vous pouvez copier l'adresse ci-dessus et la rechercher dans Google Maps ou une autre application de cartographie." +
               "</p>" +
               "</div>" +
               "</div>" +
               "<script>" +
               "function updateMap(lat, lon, nom, adresse) {" +
               "  document.getElementById('address-text').textContent = adresse;" +
               "  console.log('Coordonnées:', lat, lon);" +
               "}" +
               "</script>" +
               "</body></html>";
    }
}
