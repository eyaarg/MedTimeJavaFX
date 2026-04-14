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
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ModifierArticleController implements Initializable {

    @FXML
    private TextField txtTitre;

    @FXML
    private TextArea txtContenu;

    @FXML
    private ComboBox<Categorie> cbCategorie;

    @FXML
    private TextField txtTags;

    @FXML
    private ComboBox<String> cbStatut;

    @FXML
    private TextField txtImage;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnAnnuler;

    private ArticleService articleService;
    private CategorieService categorieService;
    private Article articleAModifier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        articleService = new ArticleService();
        categorieService = new CategorieService();

        // Charger les statuts
        cbStatut.setItems(FXCollections.observableArrayList("publié", "brouillon"));

        // Charger les catégories
        List<Categorie> categories = null;
        try {
            categories = categorieService.getAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        cbCategorie.setItems(FXCollections.observableArrayList(categories));

        // Afficher le nom dans ComboBox
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

    // Méthode appelée depuis ArticleController pour passer l'article sélectionné
    public void setArticle(Article article) {
        this.articleAModifier = article;

        // Pré-remplir les champs
        txtTitre.setText(article.getTitre());
        txtContenu.setText(article.getContenu());
        txtTags.setText(article.getTags());
        txtImage.setText(article.getImage());
        cbStatut.setValue(article.getStatut());

        // Sélectionner la catégorie
        if (article.getCategorie() != null) {
            cbCategorie.getItems().forEach(cat -> {
                if (cat.getId() == article.getCategorie().getId()) {
                    cbCategorie.setValue(cat);
                }
            });
        }
    }

    @FXML
    public void modifierArticle() throws SQLException {
        // Validation
        if (txtTitre.getText().isEmpty() || txtContenu.getText().isEmpty()
                || cbCategorie.getValue() == null || cbStatut.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez remplir tous les champs !");
            alert.show();
            return;
        }

        // Modifier l'article
        articleAModifier.setTitre(txtTitre.getText());
        articleAModifier.setContenu(txtContenu.getText());
        articleAModifier.setTags(txtTags.getText());
        articleAModifier.setImage(txtImage.getText());
        articleAModifier.setStatut(cbStatut.getValue());
        articleAModifier.setCategorie(cbCategorie.getValue());

        articleService.modifier(articleAModifier);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText("Article modifié avec succès !");
        alert.show();

        Stage stage = (Stage) btnModifier.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void annuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}