package esprit.fx.controllers;

import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.utils.MyDB;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AjouterArticleController implements Initializable {

    @FXML
    private TextField txtTitre;

    @FXML
    private TextArea txtContenu;

    @FXML
    private ComboBox<String> cbCategorie;

    @FXML
    private TextField txtTags;

    @FXML
    private ComboBox<String> cbStatut;

    @FXML
    private TextField txtImage;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnAnnuler;

    private ArticleService articleService;
    private Map<String, Integer> specialiteMap;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();
        specialiteMap = new HashMap<>();

        cbStatut.setItems(FXCollections.observableArrayList("publié", "brouillon"));
        loadSpecialites();
    }

    private void loadSpecialites() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            reloadSpecialites(con);
            if (specialiteMap.isEmpty()) {
                seedDefaultSpecialites(con);
                reloadSpecialites(con);
            }
            cbCategorie.setItems(FXCollections.observableArrayList(specialiteMap.keySet()));
        } catch (SQLException e) {
            System.err.println("Erreur chargement specialites: " + e.getMessage());
        }
    }

    private void reloadSpecialites(Connection con) throws SQLException {
        specialiteMap.clear();
        String sql = "SELECT id, nom FROM specialite ORDER BY nom";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                specialiteMap.put(rs.getString("nom"), rs.getInt("id"));
            }
        }
    }

    private void seedDefaultSpecialites(Connection con) throws SQLException {
        List<String> defaults = Arrays.asList(
                "Cardiologie",
                "Dermatologie",
                "Medecine generale",
                "Neurologie",
                "Pediatrie"
        );
        String sql = "INSERT INTO specialite (nom) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String nom : defaults) {
                ps.setString(1, nom);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @FXML
    public void ajouterArticle() {
        if (!validateInputs()) {
            return;
        }

        try {
            Article article = createArticleFromInputs();
            articleService.ajouter(article);
            showSuccessAlert();
            closeWindow();
        } catch (SQLException e) {
            showErrorAlert("Erreur lors de l'ajout de l'article", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (txtTitre.getText().trim().isEmpty()) {
            showWarningAlert("Champ manquant", "Veuillez saisir un titre");
            return false;
        }

        if (txtContenu.getText().trim().isEmpty()) {
            showWarningAlert("Champ manquant", "Veuillez saisir le contenu");
            return false;
        }

        if (cbCategorie.getValue() == null) {
            showWarningAlert("Champ manquant", "Veuillez sélectionner une catégorie");
            return false;
        }

        if (cbStatut.getValue() == null) {
            showWarningAlert("Champ manquant", "Veuillez sélectionner un statut");
            return false;
        }

        return true;
    }

    private Article createArticleFromInputs() {
        Article article = new Article();
        article.setTitre(txtTitre.getText().trim());
        article.setContenu(txtContenu.getText().trim());
        article.setDatePublication(new Date());
        article.setImage(txtImage.getText() != null ? txtImage.getText().trim() : "");
        article.setNbLikes(0);
        article.setNbVues(0);
        article.setTags(txtTags.getText() != null ? txtTags.getText().trim() : "");
        article.setStatut(cbStatut.getValue());
        article.setSpecialiteId(specialiteMap.get(cbCategorie.getValue()));
        return article;
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("Article ajouté avec succès !");
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        closeWindow();
    }
}
