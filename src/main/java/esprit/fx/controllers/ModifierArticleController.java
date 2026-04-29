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
import java.util.HashMap;
import java.util.List;
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

        cbStatut.setItems(FXCollections.observableArrayList("publi├®", "brouillon"));
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
        alert.setTitle("Succ├¿s");
        alert.setContentText("Article modifi├® avec succ├¿s !");
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