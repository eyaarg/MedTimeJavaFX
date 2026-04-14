package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;

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
        chargerArticles();

        // Bouton Supprimer
        btnSupprimer.setOnAction(e -> {
            Article articleSelectionne = tableArticles.getSelectionModel().getSelectedItem();
            if (articleSelectionne != null) {
                try {
                    articleService.supprimer(articleSelectionne.getId());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                chargerArticles();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setContentText("Veuillez sélectionner un article !");
                alert.show();
            }
        });
    }

    private void chargerArticles() {
        List<Article> articles = articleService.afficherTous();
        ObservableList<Article> observableList = FXCollections.observableArrayList(articles);
        tableArticles.setItems(observableList);
    }

    @FXML
    public void ajouterArticle() {
        // On va connecter cette méthode à la fenêtre AjouterArticle.fxml
        System.out.println("Ouvrir fenêtre Ajouter Article");
    }

    @FXML
    public void modifierArticle() {
        // On va connecter cette méthode à la fenêtre ModifierArticle.fxml
        System.out.println("Ouvrir fenêtre Modifier Article");
    }
}