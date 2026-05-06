package esprit.fx.controllers;

import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.services.UnsplashService;
import esprit.fx.utils.MyDB;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

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

    // Image IA
    @FXML private Button     btnGenererImage;
    @FXML private HBox       previewBox;
    @FXML private Label      lblPreviewInfo;

    // Planification
    @FXML private CheckBox   chkPlanifier;
    @FXML private HBox       panneauPlanification;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbHeure;
    @FXML private ComboBox<String> cbMinutes;
    @FXML private Label      lblPlanifInfo;

    private ArticleService  articleService;
    private UnsplashService unsplashService;
    private Map<String, Integer> specialiteMap;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        articleService  = new ArticleService();
        unsplashService = new UnsplashService();
        specialiteMap   = new HashMap<>();

        cbStatut.setItems(FXCollections.observableArrayList("publie", "brouillon"));

        List<String> heures = new ArrayList<>();
        for (int i = 0; i < 24; i++) heures.add(String.format("%02d", i));
        cbHeure.setItems(FXCollections.observableArrayList(heures));
        cbHeure.setValue("08");

        List<String> minutes = new ArrayList<>();
        for (int i = 0; i < 60; i += 5) minutes.add(String.format("%02d", i));
        cbMinutes.setItems(FXCollections.observableArrayList(minutes));
        cbMinutes.setValue("00");

        if (datePicker != null) {
            datePicker.valueProperty().addListener((o, ov, nv) -> mettreAJourInfoPlanif());
            cbHeure.valueProperty().addListener((o, ov, nv)    -> mettreAJourInfoPlanif());
            cbMinutes.valueProperty().addListener((o, ov, nv)  -> mettreAJourInfoPlanif());
        }

        loadSpecialites();
    }

    @FXML
    public void genererImage() {
        String titre      = txtTitre.getText().trim();
        String specialite = cbCategorie.getValue();

        if (titre.isEmpty() && specialite == null) {
            showWarningAlert("Champs manquants", "Veuillez saisir un titre ou choisir une categorie.");
            return;
        }

        btnGenererImage.setDisable(true);
        btnGenererImage.setText("Generation...");
        if (previewBox != null) { previewBox.setVisible(false); previewBox.setManaged(false); }

        Thread thread = new Thread(() -> {
            String imageUrl = unsplashService.genererImage(titre, specialite);
            Platform.runLater(() -> {
                btnGenererImage.setDisable(false);
                btnGenererImage.setText("Generer image");
                if (imageUrl != null && !imageUrl.isBlank()) {
                    txtImage.setText(imageUrl);
                    if (lblPreviewInfo != null) lblPreviewInfo.setText("Image generee via Unsplash\nBasee sur : " + (titre.isEmpty() ? specialite : titre));
                    if (previewBox != null) { previewBox.setVisible(true); previewBox.setManaged(true); }
                } else {
                    showWarningAlert("Generation echouee", "Impossible de generer une image. Verifiez votre connexion.");
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
            datePicker.setValue(LocalDate.now().plusDays(1));
            cbStatut.setValue("brouillon");
            cbStatut.setDisable(true);
            btnAjouter.setText("Planifier");
            mettreAJourInfoPlanif();
        } else {
            cbStatut.setDisable(false);
            btnAjouter.setText("Publier");
            if (lblPlanifInfo != null) lblPlanifInfo.setText("");
        }
    }

    private void mettreAJourInfoPlanif() {
        if (datePicker == null || datePicker.getValue() == null || lblPlanifInfo == null) return;
        LocalDateTime dt = getDateTimePlanifiee();
        if (dt == null) return;
        if (dt.isBefore(LocalDateTime.now())) {
            lblPlanifInfo.setText("La date choisie est dans le passe !");
        } else {
            lblPlanifInfo.setText(String.format("Publication prevue le %02d/%02d/%d a %s:%s",
                dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(), cbHeure.getValue(), cbMinutes.getValue()));
        }
    }

    private LocalDateTime getDateTimePlanifiee() {
        if (datePicker == null || datePicker.getValue() == null || cbHeure.getValue() == null || cbMinutes.getValue() == null) return null;
        return LocalDateTime.of(datePicker.getValue(), java.time.LocalTime.of(Integer.parseInt(cbHeure.getValue()), Integer.parseInt(cbMinutes.getValue())));
    }

    private void loadSpecialites() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT id, nom FROM specialite ORDER BY nom").executeQuery();
            while (rs.next()) specialiteMap.put(rs.getString("nom"), rs.getInt("id"));
            cbCategorie.setItems(FXCollections.observableArrayList(specialiteMap.keySet()));
        } catch (SQLException e) {
            System.err.println("Erreur chargement specialites: " + e.getMessage());
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
            article.setNbLikes(0); article.setNbVues(0);
            article.setTags(txtTags.getText() != null ? txtTags.getText().trim() : "");
            article.setSpecialiteId(specialiteMap.get(cbCategorie.getValue()));

            if (chkPlanifier != null && chkPlanifier.isSelected()) {
                LocalDateTime dt = getDateTimePlanifiee();
                if (dt == null || dt.isBefore(LocalDateTime.now())) {
                    showWarningAlert("Date invalide", "Veuillez choisir une date future."); return;
                }
                article.setStatut("planifie");
                article.setDatePublication(Date.from(dt.atZone(ZoneId.systemDefault()).toInstant()));
                articleService.ajouter(article);
                showInfoAlert("Article planifie", String.format("Publication prevue le %02d/%02d/%d a %s:%s",
                    dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(), cbHeure.getValue(), cbMinutes.getValue()));
            } else {
                article.setStatut(cbStatut.getValue());
                article.setDatePublication(new Date());
                articleService.ajouter(article);
                showInfoAlert("Succes", "Article ajoute avec succes !");
            }
            closeWindow();
        } catch (SQLException e) {
            showErrorAlert("Erreur", e.getMessage()); e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (txtTitre.getText().trim().isEmpty()) { showWarningAlert("Champ manquant", "Veuillez saisir un titre"); return false; }
        if (txtContenu.getText().trim().isEmpty()) { showWarningAlert("Champ manquant", "Veuillez saisir le contenu"); return false; }
        if (cbCategorie.getValue() == null) { showWarningAlert("Champ manquant", "Veuillez selectionner une categorie"); return false; }
        if ((chkPlanifier == null || !chkPlanifier.isSelected()) && cbStatut.getValue() == null) {
            showWarningAlert("Champ manquant", "Veuillez selectionner un statut"); return false;
        }
        return true;
    }

    private void showWarningAlert(String t, String c) { Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
    private void showErrorAlert(String t, String c)   { Alert a = new Alert(Alert.AlertType.ERROR);   a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
    private void showInfoAlert(String t, String c)    { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); }
    private void closeWindow() { ((Stage) btnAjouter.getScene().getWindow()).close(); }

    @FXML public void annuler() { closeWindow(); }
}
