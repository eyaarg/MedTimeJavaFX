package esprit.fx.controllers;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ListeProduitController implements Initializable {

    // Composants FXML
    @FXML private TableView<Produit> tableViewProduits;
    @FXML private TableColumn<Produit, Long> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, String> colMarque;
    @FXML private TableColumn<Produit, Boolean> colDisponible;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    // Variables
    private ServiceProduit serviceProduit;
    private Connection connection;
    private ObservableList<Produit> produitsList = FXCollections.observableArrayList();
    @FXML
    private Label lblCategorie;
    @FXML
    private Label lblDisponible;
    @FXML
    private Label lblPrescription;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblDateExpiration;
    @FXML
    private Label lblNom;
    @FXML
    private Label lblPrix;
    @FXML
    private Label lblStock;
    @FXML
    private Label lblImagePreview;
    @FXML
    private Label lblMarque;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        connectToDatabase();
        try {
            loadProduits();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setupSearchListener();
        updateStatus();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategorie.setCellValueFactory(cellData -> {
            CategorieEnum cat = cellData.getValue().getCategorie();
            return new SimpleStringProperty(cat != null ? cat.name() : "");});
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colMarque.setCellValueFactory(new PropertyValueFactory<>("marque"));
        colDisponible.setCellValueFactory(new PropertyValueFactory<>("disponible"));
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mediplatform_test_test",
                    "root",
                    ""
            );
            serviceProduit = new ServiceProduit(connection);
        } catch (SQLException e) {
            showAlert("Erreur de connexion: " + e.getMessage());
        }
    }

    private void loadProduits() throws SQLException {
        List<Produit> produits = serviceProduit.getAll();
        produitsList.setAll(produits);
        tableViewProduits.setItems(produitsList);
        updateStatus();
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProduits(newValue);
        });
    }

    private void filterProduits(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tableViewProduits.setItems(produitsList);
            return;
        }

        ObservableList<Produit> filtered = FXCollections.observableArrayList();
        for (Produit p : produitsList) {
            if (p.getNom().toLowerCase().contains(keyword.toLowerCase()) ||
                    p.getMarque().toLowerCase().contains(keyword.toLowerCase()) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(keyword.toLowerCase()))) {
                filtered.add(p);
            }
        }
        tableViewProduits.setItems(filtered);
        updateStatus(filtered.size());
    }

    private void updateStatus() {
        lblStatus.setText("Total: " + produitsList.size() + " produits");
    }

    private void updateStatus(int count) {
        lblStatus.setText("Affichage: " + count + " / " + produitsList.size() + " produits");
    }

    // ==================== ACTIONS ====================

    @FXML
    private void handleAjouter() {
        try {
            // Charger le formulaire d'ajout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutProd.fxml"));
            Parent root = loader.load();

            // Récupérer le controller
            FormulaireProduitController controller = loader.getController();
            controller.setServiceProduit(serviceProduit);
            controller.setListeController(this);
            controller.setMode("ajouter");

            // Créer la fenêtre
            Stage stage = new Stage();
            stage.setTitle("Ajouter un produit");
            stage.setScene(new Scene(root, 600, 550));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        Produit selected = tableViewProduits.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner un produit à modifier !");
            return;
        }

        try {
            // Charger le formulaire de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutProd.fxml"));
            Parent root = loader.load();

            // Récupérer le controller et passer les données
            FormulaireProduitController controller = loader.getController();
            controller.setServiceProduit(serviceProduit);
            controller.setListeController(this);
            controller.setMode("modifier");
            controller.setProduit(selected);

            // Créer la fenêtre
            Stage stage = new Stage();
            stage.setTitle("Modifier le produit");
            stage.setScene(new Scene(root, 600, 550));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() throws SQLException {
        Produit selected = tableViewProduits.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner un produit à supprimer !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer : " + selected.getNom() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceProduit.supprimer(selected.getId().intValue());
            showAlert("Produit supprimé avec succès !", Alert.AlertType.INFORMATION);
            loadProduits();
        }
    }

    @FXML
    private void handleRefresh() throws SQLException {
        loadProduits();
        txtSearch.clear();
        showAlert("Liste actualisée !", Alert.AlertType.INFORMATION);
    }

    // Méthode pour rafraîchir la liste après ajout/modification
    public void refreshList() throws SQLException {
        loadProduits();
    }

    private void showAlert(String message) {
        showAlert(message, Alert.AlertType.WARNING);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erreur" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}