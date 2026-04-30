package esprit.fx.controllers;

import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.utils.MyDB;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.services.UnsplashService;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class AjouterArticleController implements Initializable {

    @FXML private TextField  txtTitre;
    @FXML private TextArea   txtContenu;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField  txtTags;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField  txtImage;
    @FXML private Button     btnAjouter;
    @FXML private Button     btnAnnuler;

    // Planification
    @FXML private CheckBox   chkPlanifier;
    @FXML private HBox       panneauPlanification;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbHeure;
    @FXML private ComboBox<String> cbMinutes;
    @FXML private Label      lblPlanifInfo;

    // Image IA
    @FXML private Button     btnGenererImage;
    @FXML private HBox       previewBox;
    @FXML private Label      lblPreviewInfo;

    private ArticleService articleService;
    private Map<String, Integer> specialiteMap;
    private UnsplashService unsplashService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService  = new ArticleService();
        specialiteMap   = new HashMap<>();
        unsplashService = new UnsplashService();

        cbStatut.setItems(FXCollections.observableArrayList("publié", "brouillon"));

        // Heures 00–23
        List<String> heures = new ArrayList<>();
        for (int i = 0; i < 24; i++) heures.add(String.format("%02d", i));
        cbHeure.setItems(FXCollections.observableArrayList(heures));
        cbHeure.setValue("08");

        // Minutes 00, 05, 10 ... 55
        List<String> minutes = new ArrayList<>();
        for (int i = 0; i < 60; i += 5) minutes.add(String.format("%02d", i));
        cbMinutes.setItems(FXCollections.observableArrayList(minutes));
        cbMinutes.setValue("00");

        // Mettre à jour le label info quand date/heure change
        datePicker.valueProperty().addListener((o, ov, nv) -> mettreAJourInfoPlanif());
        cbHeure.valueProperty().addListener((o, ov, nv)    -> mettreAJourInfoPlanif());
        cbMinutes.valueProperty().addListener((o, ov, nv)  -> mettreAJourInfoPlanif());

        loadSpecialites();
    }

    @FXML
    public void genererImage() {
        String titre      = txtTitre.getText().trim();
        String specialite = cbCategorie.getValue();

        if (titre.isEmpty() && specialite == null) {
            showWarningAlert("Champs manquants",
                "Veuillez saisir un titre ou choisir une catégorie avant de générer une image.");
            return;
        }

        // Désactiver le bouton pendant la génération
        btnGenererImage.setDisable(true);
        btnGenererImage.setText("⏳ Génération...");
        previewBox.setVisible(false);
        previewBox.setManaged(false);

        // Appel dans un thread séparé pour ne pas bloquer l'UI
        Thread thread = new Thread(() -> {
            String imageUrl = unsplashService.genererImage(titre, specialite);

            javafx.application.Platform.runLater(() -> {
                btnGenererImage.setDisable(false);
                btnGenererImage.setText("🎨 Générer");

                if (imageUrl != null && !imageUrl.isBlank()) {
                    txtImage.setText(imageUrl);

                    // Afficher info prévisualisation
                    String source = unsplashService.hasApiKey() ? "Unsplash API" : "Unsplash (mode demo)";
                    lblPreviewInfo.setText(
                        "✅ Image générée via " + source + "\n" +
                        "🔍 Basée sur : " + (titre.isEmpty() ? specialite : titre) + "\n" +
                        "🔗 " + imageUrl.substring(0, Math.min(60, imageUrl.length())) + "..."
                    );
                    previewBox.setVisible(true);
                    previewBox.setManaged(true);
                } else {
                    showWarningAlert("Génération échouée",
                        "Impossible de générer une image. Vérifiez votre connexion internet.");
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void togglePlanification() {
        boolean planifier = chkPlanifier.isSelected();
        panneauPlanification.setVisible(planifier);
        panneauPlanification.setManaged(planifier);

        if (planifier) {
            // Pré-sélectionner demain
            datePicker.setValue(LocalDate.now().plusDays(1));
            // Forcer statut brouillon — sera changé en "planifié" à la sauvegarde
            cbStatut.setValue("brouillon");
            cbStatut.setDisable(true);
            btnAjouter.setText("🗓  Planifier");
            mettreAJourInfoPlanif();
        } else {
            cbStatut.setDisable(false);
            btnAjouter.setText("➕ Publier");
            lblPlanifInfo.setText("");
        }
    }

    private void mettreAJourInfoPlanif() {
        if (datePicker.getValue() == null) return;
        LocalDateTime dt = getDateTimePlanifiee();
        if (dt == null) return;
        if (dt.isBefore(LocalDateTime.now())) {
            lblPlanifInfo.setText("⚠️ La date choisie est dans le passé !");
            lblPlanifInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#dc2626;");
        } else {
            lblPlanifInfo.setText("📅 Publication prévue le\n" +
                String.format("%02d/%02d/%d à %s:%s",
                    dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(),
                    cbHeure.getValue(), cbMinutes.getValue()));
            lblPlanifInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#0369a1;");
        }
    }

    private LocalDateTime getDateTimePlanifiee() {
        if (datePicker.getValue() == null || cbHeure.getValue() == null || cbMinutes.getValue() == null)
            return null;
        return LocalDateTime.of(
            datePicker.getValue(),
            java.time.LocalTime.of(
                Integer.parseInt(cbHeure.getValue()),
                Integer.parseInt(cbMinutes.getValue())
            )
        );
    }

    private void loadSpecialites() {
        try {
            Connection con = esprit.fx.utils.MyDB.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT id, nom FROM specialite").executeQuery();
            while (rs.next()) specialiteMap.put(rs.getString("nom"), rs.getInt("id"));
            cbCategorie.setItems(FXCollections.observableArrayList(specialiteMap.keySet()));
        } catch (SQLException e) {
            System.err.println("Erreur chargement spécialités: " + e.getMessage());
        }
    }

    @FXML
    public void ajouterArticle() {
        if (!validateInputs()) return;

        try {
            Article article = new Article();
            article.setTitre(txtTitre.getText().trim());
            article.setContenu(txtContenu.getText().trim());
            article.setImage(txtImage.getText() != null ? txtImage.getText().trim() : "");
            article.setNbLikes(0);
            article.setNbVues(0);
            article.setTags(txtTags.getText() != null ? txtTags.getText().trim() : "");
            article.setSpecialiteId(specialiteMap.get(cbCategorie.getValue()));

            if (chkPlanifier.isSelected()) {
                // ── Article planifié ──
                LocalDateTime dt = getDateTimePlanifiee();
                if (dt == null || dt.isBefore(LocalDateTime.now())) {
                    showWarningAlert("Date invalide", "Veuillez choisir une date future pour la planification.");
                    return;
                }
                article.setStatut("planifié");
                article.setDatePublication(Date.from(dt.atZone(ZoneId.systemDefault()).toInstant()));

                articleService.ajouter(article);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Article planifié ✅");
                alert.setHeaderText(null);
                alert.setContentText(
                    "Votre article a été planifié avec succès !\n\n" +
                    "📅 Il sera publié automatiquement le\n" +
                    String.format("%02d/%02d/%d à %s:%s",
                        dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(),
                        cbHeure.getValue(), cbMinutes.getValue())
                );
                alert.showAndWait();
            } else {
                // ── Publication immédiate ──
                article.setStatut(cbStatut.getValue());
                article.setDatePublication(new Date());
                articleService.ajouter(article);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Article ajouté avec succès !");
                alert.showAndWait();
            }

            closeWindow();
        } catch (SQLException e) {
            showErrorAlert("Erreur lors de l'ajout de l'article", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (txtTitre.getText().trim().isEmpty()) {
            showWarningAlert("Champ manquant", "Veuillez saisir un titre"); return false;
        }
        if (txtContenu.getText().trim().isEmpty()) {
            showWarningAlert("Champ manquant", "Veuillez saisir le contenu"); return false;
        }
        if (cbCategorie.getValue() == null) {
            showWarningAlert("Champ manquant", "Veuillez sélectionner une catégorie"); return false;
        }
        if (!chkPlanifier.isSelected() && cbStatut.getValue() == null) {
            showWarningAlert("Champ manquant", "Veuillez sélectionner un statut"); return false;
        }
        return true;
    }

    private void showWarningAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
    private void showErrorAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
    private void closeWindow() {
        ((Stage) btnAjouter.getScene().getWindow()).close();
    }

    @FXML
    public void annuler() { closeWindow(); }
}
