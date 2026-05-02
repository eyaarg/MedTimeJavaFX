package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceProduit;
import esprit.fx.utils.MyDB;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ListeProduitController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;

    private ServiceProduit serviceProduit;
    private Connection connection;
    private List<Produit> produitsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connectToDatabase();
        try {
            loadProduits();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setupSearchListener();
    }

    private void connectToDatabase() {
        try {
            connection = MyDB.getInstance().getConnection();
            serviceProduit = new ServiceProduit(connection);
        } catch (Exception e) {
            showAlert("Erreur de connexion: " + e.getMessage());
        }
    }

    private void loadProduits() throws SQLException {
        produitsList = serviceProduit.getAll();
        displayCards(produitsList);
        updateStatus(produitsList.size());
    }

    private void displayCards(List<Produit> produits) {
        cardsContainer.getChildren().clear();

        for (Produit p : produits) {
            VBox card = createCard(p);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createCard(Produit p) {
        // Conteneur principal de la carte
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(220);
        card.setPrefHeight(280);
        card.setPadding(new Insets(12));

        // Icône selon catégorie
        Label iconLabel = new Label(getIconForCategorie(p.getCategorieName()));
        iconLabel.setStyle("-fx-font-size: 40px;");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Nom du produit
        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(200);

        // Prix
        Label prixLabel = new Label(String.format("%.2f €", p.getPrix()));
        prixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        // Stock
        Label stockLabel = new Label("📦 Stock: " + p.getStock());
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Disponibilité
        Label dispoLabel = new Label(p.getDisponible() ? "✅ Disponible" : "❌ Indisponible");
        dispoLabel.setStyle(p.getDisponible() ? "-fx-text-fill: #27ae60; -fx-font-size: 11px;" : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        // Boutons
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button detailsBtn = new Button("👁");
        detailsBtn.setTooltip(new Tooltip("Afficher"));
        detailsBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        detailsBtn.setOnAction(e -> handleAfficher(p));

        Button modifierBtn = new Button("✏");
        modifierBtn.setTooltip(new Tooltip("Modifier"));
        modifierBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        modifierBtn.setOnAction(e -> handleModifier(p));

        Button supprimerBtn = new Button("🗑");
        supprimerBtn.setTooltip(new Tooltip("Supprimer"));
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        supprimerBtn.setOnAction(e -> {
            try {
                handleSupprimer(p);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        buttonsBox.getChildren().addAll(detailsBtn, modifierBtn, supprimerBtn);

        // Ajouter tous les éléments à la carte
        card.getChildren().addAll(iconLabel, nomLabel, prixLabel, stockLabel, dispoLabel, buttonsBox);

        return card;
    }

    private String getIconForCategorie(String categorie) {
        if (categorie == null) return "📦";
        switch (categorie.toUpperCase()) {
            case "MEDICAMENT": return "💊";
            case "MATERIEL_MEDICAL": return "🩺";
            case "PARAPHARMACIE": return "🧴";
            case "HYGIENE": return "🧼";
            case "COMPLEMENT_ALIMENTAIRE": return "🥗";
            default: return "📦";
        }
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCards(newValue);
        });
    }

    private void filterCards(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            displayCards(produitsList);
            updateStatus(produitsList.size());
            return;
        }

        List<Produit> filtered = produitsList.stream()
                .filter(p -> p.getNom().toLowerCase().contains(keyword.toLowerCase()) ||
                        (p.getMarque() != null && p.getMarque().toLowerCase().contains(keyword.toLowerCase())))
                .toList();

        displayCards(filtered);
        updateStatus(filtered.size());
    }

    private void updateStatus(int count) {
        lblStatus.setText("📊 " + count + " produit(s)");
    }

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjoutProd.fxml"));
            Parent root = loader.load();

            FormulaireProduitController controller = loader.getController();
            controller.setServiceProduit(serviceProduit);
            controller.setListeController(this);
            controller.setMode("ajouter");

            Stage stage = new Stage();
            stage.setTitle("Ajouter un produit");
            stage.setScene(new Scene(root, 500, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private void handleModifier(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjoutProd.fxml"));
            Parent root = loader.load();

            FormulaireProduitController controller = loader.getController();
            controller.setServiceProduit(serviceProduit);
            controller.setListeController(this);
            controller.setMode("modifier");
            controller.setProduit(produit);

            Stage stage = new Stage();
            stage.setTitle("Modifier le produit");
            stage.setScene(new Scene(root, 500, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private void handleAfficher(Produit produit) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails produit");
        info.setHeaderText(produit.getNom());
        info.setContentText(
                "Catégorie: " + (produit.getCategorieName() != null ? produit.getCategorieName() : "N/A") + "\n" +
                "Prix: " + produit.getPrix() + "\n" +
                "Stock: " + produit.getStock() + "\n" +
                "Marque: " + (produit.getMarque() != null ? produit.getMarque() : "-") + "\n" +
                "Disponible: " + (produit.getDisponible() ? "Oui" : "Non"));
        info.showAndWait();
    }

    private void handleSupprimer(Produit produit) throws SQLException {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer " + produit.getNom() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceProduit.supprimer(produit.getId().intValue());
            loadProduits();
        }
    }

    public void refreshList() throws SQLException {
        loadProduits();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
