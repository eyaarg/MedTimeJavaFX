package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ArticleController implements Initializable {

    @FXML
    private TableView<Article> tableArticles;

    @FXML
    private TableColumn<Article, String> colTitre;

    @FXML
    private TableColumn<Article, String> colCategorie;

    @FXML
    private TableColumn<Article, String> colDate;

    @FXML
    private TableColumn<Article, Integer> colLikes;

    @FXML
    private TableColumn<Article, String> colStatut;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    @FXML
    private Button btnAnnuler;

    private ArticleService articleService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();

        // Configurer les colonnes
        colTitre.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTitre()));

        colCategorie.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategorie() != null ?
                                data.getValue().getCategorie().getNom() : ""));

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDatePublication() != null ?
                                data.getValue().getDatePublication().toString() : ""));

        colLikes.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getNbLikes()).asObject());

        colStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStatut()));

        // Charger les données
        try {
            chargerArticles();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Bouton Supprimer
        btnSupprimer.setOnAction(e -> {
            Article articleSelectionne = tableArticles.getSelectionModel().getSelectedItem();
            if (articleSelectionne != null) {
                try {
                    articleService.supprimer(articleSelectionne.getId());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                try {
                    chargerArticles();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setContentText("Veuillez sélectionner un article !");
                alert.show();
            }
        });
    }

    private void chargerArticles() throws SQLException {
        List<Article> articles = articleService.getAll();
        ObservableList<Article> observableList = FXCollections.observableArrayList(articles);
        tableArticles.setItems(observableList);
    }

    @FXML
    public void ajouterArticle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterArticle.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Article");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void modifierArticle() {
        Article articleSelectionne = tableArticles.getSelectionModel().getSelectedItem();
        if (articleSelectionne != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierArticle.fxml"));
                Parent root = loader.load();
                ModifierArticleController controller = loader.getController();
                controller.setArticle(articleSelectionne);
                Stage stage = new Stage();
                stage.setTitle("Modifier Article");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un article !");
            alert.show();
        }
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}