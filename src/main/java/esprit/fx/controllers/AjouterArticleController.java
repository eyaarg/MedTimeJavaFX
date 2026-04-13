package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import esprit.fx.entities.Article;
import esprit.fx.entities.Categorie;
import esprit.fx.services.ArticleService;
import esprit.fx.services.CategorieService;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterArticleController implements Initializable {

    @FXML
    private TextField txtTitre;

    @FXML
    private TextField txtContenu;

    @FXML
    private ComboBox<Categorie> cbCategorie;

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
    private CategorieService categorieService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();
        categorieService = new CategorieService();

        // Charger les statuts
        cbStatut.setItems(FXCollections.observableArrayList("publié", "brouillon"));

        // Charger les catégories
        List<Categorie> categories = categorieService.getAll();
        cbCategorie.setItems(FXCollections.observableArrayList(categories));

        // Afficher le nom de la catégorie dans le ComboBox
        cbCategorie.setCellFactory(param -> new ListCell<Categorie>() {
            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        cbCategorie.setButtonCell(new ListCell<Categorie>() {
            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
    }

    @FXML
    public void ajouterArticle() {
        // Validation
        if (txtTitre.getText().isEmpty() || txtContenu.getText().isEmpty()
                || cbCategorie.getValue() == null || cbStatut.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez remplir tous les champs obligatoires !");
            alert.show();
            return;
        }

        // Créer l'article
        Article article = new Article();
        article.setTitre(txtTitre.getText());
        article.setContenu(txtContenu.getText());
        article.setDatePublication(new Date());
        article.setImage(txtImage.getText());
        article.setNbLikes(0);
        article.setNbVues(0);
        article.setTags(txtTags.getText());
        article.setStatut(cbStatut.getValue());
        article.setCategorie(cbCategorie.getValue());

        // Ajouter en BD
        articleService.ajouter(article);

        // Message succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText("Article ajouté avec succès !");
        alert.show();

        // Fermer la fenêtre
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}