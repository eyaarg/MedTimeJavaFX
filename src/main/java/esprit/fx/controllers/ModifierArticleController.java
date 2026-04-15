package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import esprit.fx.entities.Article;
import esprit.fx.services.ArticleService;
import esprit.fx.utils.MyDB;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ModifierArticleController implements Initializable {

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
    private Button btnModifier;

    @FXML
    private Button btnAnnuler;

    private ArticleService articleService;
    private Article articleAModifier;
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
            String sql = "SELECT id, nom FROM specialite";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                specialiteMap.put(nom, id);
            }

            cbCategorie.setItems(FXCollections.observableArrayList(specialiteMap.keySet()));
        } catch (SQLException e) {
            System.err.println("Erreur chargement spécialités: " + e.getMessage());
        }
    }

    public void setArticle(Article article) {
        this.articleAModifier = article;

        txtTitre.setText(article.getTitre());
        txtContenu.setText(article.getContenu());
        txtTags.setText(article.getTags());
        txtImage.setText(article.getImage());
        cbStatut.setValue(article.getStatut());

        for (String nom : specialiteMap.keySet()) {
            if (specialiteMap.get(nom) == article.getSpecialiteId()) {
                cbCategorie.setValue(nom);
                break;
            }
        }
    }

    @FXML
    public void modifierArticle() throws SQLException {
        if (txtTitre.getText().isEmpty() || txtContenu.getText().isEmpty()
                || cbCategorie.getValue() == null || cbStatut.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez remplir tous les champs !");
            alert.show();
            return;
        }

        articleAModifier.setTitre(txtTitre.getText());
        articleAModifier.setContenu(txtContenu.getText());
        articleAModifier.setTags(txtTags.getText());
        articleAModifier.setImage(txtImage.getText());
        articleAModifier.setStatut(cbStatut.getValue());
        articleAModifier.setSpecialiteId(specialiteMap.get(cbCategorie.getValue()));

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