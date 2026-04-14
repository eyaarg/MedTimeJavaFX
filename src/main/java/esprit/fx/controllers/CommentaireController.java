package esprit.fx.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import esprit.fx.entities.Commentaire;
import esprit.fx.services.CommentaireService;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class CommentaireController implements Initializable {

    @FXML
    private TableView<Commentaire> tableCommentaires;

    @FXML
    private TableColumn<Commentaire, String> colContenu;

    @FXML
    private TableColumn<Commentaire, String> colDate;

    @FXML
    private TableColumn<Commentaire, Integer> colLikes;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    private CommentaireService commentaireService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();

        // Configurer les colonnes
        colContenu.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getContenu()));

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDateCommentaire() != null ?
                                data.getValue().getDateCommentaire().toString() : ""));

        colLikes.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getNbLikes()).asObject());

        // Charger les données
        try {
            chargerCommentaires();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Bouton Supprimer
        btnSupprimer.setOnAction(e -> {
            Commentaire commentaireSelectionne = tableCommentaires
                    .getSelectionModel().getSelectedItem();
            if (commentaireSelectionne != null) {
                try {
                    commentaireService.supprimer(commentaireSelectionne.getId());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                try {
                    chargerCommentaires();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setContentText("Veuillez sélectionner un commentaire !");
                alert.show();
            }
        });
    }

    private void chargerCommentaires() throws SQLException {
        List<Commentaire> commentaires = commentaireService.getAll();
        ObservableList<Commentaire> observableList =
                FXCollections.observableArrayList(commentaires);
        tableCommentaires.setItems(observableList);
    }

    @FXML
    public void ajouterCommentaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterCommentaire.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Commentaire");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void modifierCommentaire() {
        Commentaire commentaireSelectionne = tableCommentaires.getSelectionModel().getSelectedItem();
        if (commentaireSelectionne != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierCommentaire.fxml"));
                Parent root = loader.load();
                ModifierCommentaireController controller = loader.getController();
                controller.setCommentaire(commentaireSelectionne);
                Stage stage = new Stage();
                stage.setTitle("Modifier Commentaire");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un commentaire !");
            alert.show();
        }
    }
}