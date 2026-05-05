package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.entities.CategorieEnum;
import esprit.fx.services.ServiceFavoris;
import esprit.fx.services.ServiceOrder;
import esprit.fx.services.ServicePanier;
import esprit.fx.services.ServiceProduit;
import esprit.fx.services.SmartSearchService;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.UserSession;
import javafx.application.Platform;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private final SmartSearchService aiService = new SmartSearchService();
    private final ServiceFavoris serviceFavoris = ServiceFavoris.getInstance();
    private final ServicePanier servicePanier = new ServicePanier();

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

    @FXML
    private void onSmartSearch() {
        String query = txtSearch.getText();

        if (query == null || query.trim().isEmpty()) {
            displayCards(produitsList);
            return;
        }

        lblStatus.setText("🤖 Groq réfléchit...");

        new Thread(() -> {
            try {
                List<String> productNames = produitsList.stream()
                        .map(Produit::getNom)
                        .collect(Collectors.toList());

                String suggestedName = aiService.askGroq(query, productNames);

                Platform.runLater(() -> {
                    if (suggestedName != null && !suggestedName.equalsIgnoreCase("NONE")) {
                        List<Produit> smartResult = produitsList.stream()
                                .filter(p -> p.getNom().equalsIgnoreCase(suggestedName))
                                .toList();
                        displayCards(smartResult);
                        lblStatus.setText("✨ IA a trouvé : " + suggestedName);
                    } else {
                        displayCards(List.of());
                        lblStatus.setText("❌ IA n'a rien trouvé pour : " + query);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("⚠️ Erreur IA : " + e.getMessage()));
            }
        }).start();
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
            cardsContainer.getChildren().add(createCard(p));
        }
    }

    private VBox createCard(Produit p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(220);
        card.setPrefHeight(280);
        card.setPadding(new Insets(12));

        // Image ou icône
        String imagePath = p.getImage();
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(imgUrl.toExternalForm()));
                    imageView.setFitWidth(100);
                    imageView.setFitHeight(80);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(0, imageView);
                } else {
                    card.getChildren().add(0, makeIconLabel(p));
                }
            } catch (Exception e) {
                card.getChildren().add(0, makeIconLabel(p));
            }
        } else {
            card.getChildren().add(0, makeIconLabel(p));
        }

        // Nom
        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(200);

        // Prix avec réduction éventuelle
        Label prixLabel = new Label(String.format("%.2f €", p.getPrix()));
        prixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        // Vérifier éligibilité réduction
        boolean eligible = false;
        try {
            int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
            eligible = new ServiceOrder().estEligibleReduction(userId);
        } catch (Exception ignored) {}

        if (eligible) {
            Label prixBarre = new Label(String.format("%.2f €", p.getPrix()));
            prixBarre.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-strikethrough: true;");
            double prixReduit = p.getPrix() * 0.80;
            prixLabel.setText(String.format("%.2f €", prixReduit));
            Label badge = new Label("🏷️ -20% Fidélité");
            badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 2 6;");
            card.getChildren().addAll(prixBarre, badge);
        }

        // Stock avec alerte
        HBox stockBox = new HBox(6);
        stockBox.setAlignment(Pos.CENTER_LEFT);
        int stock = p.getStock() != null ? p.getStock() : 0;
        if (stock < 5) {
            Label stockLabel = new Label("🔴 Stock: " + stock);
            stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            Label alertLabel = new Label("⚠️ Stock faible !");
            alertLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #e74c3c;" +
                    "-fx-background-radius: 8; -fx-padding: 2 6;");
            stockBox.getChildren().addAll(stockLabel, alertLabel);
        } else {
            Label stockLabel = new Label("📦 Stock: " + stock);
            stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            stockBox.getChildren().add(stockLabel);
        }

        // Disponibilité
        Label dispoLabel = new Label(Boolean.TRUE.equals(p.getDisponible()) ? "✅ Disponible" : "❌ Indisponible");
        dispoLabel.setStyle(Boolean.TRUE.equals(p.getDisponible()) ?
                "-fx-text-fill: #27ae60; -fx-font-size: 11px;" : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        // Boutons
        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER);

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
            try { handleSupprimer(p); } catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        Button favoriBtn = new Button(serviceFavoris.estFavori(p) ? "❤️" : "🤍");
        favoriBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-padding: 5 6;");
        favoriBtn.setOnAction(e -> {
            if (serviceFavoris.estFavori(p)) {
                serviceFavoris.supprimerFavori(p);
                favoriBtn.setText("🤍");
            } else {
                serviceFavoris.ajouterFavori(p);
                favoriBtn.setText("❤️");
            }
        });

        Button panierBtn = new Button("🛒");
        panierBtn.setTooltip(new Tooltip("Ajouter au panier"));
        panierBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        panierBtn.setOnAction(e -> handleAjouterAuPanier(p));

        buttonsBox.getChildren().addAll(detailsBtn, modifierBtn, supprimerBtn, favoriBtn, panierBtn);
        card.getChildren().addAll(nomLabel, prixLabel, stockBox, dispoLabel, buttonsBox);

        return card;
    }

    private Label makeIconLabel(Produit p) {
        Label iconLabel = new Label(getIconForCategorie(p.getCategorie()));
        iconLabel.setStyle("-fx-font-size: 40px;");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(Pos.CENTER);
        return iconLabel;
    }

    private String getIconForCategorie(CategorieEnum categorie) {
        if (categorie == null) return "📦";
        return switch (categorie) {
            case MEDICAMENT -> "💊";
            case MATERIEL_MEDICAL -> "🩺";
            case PARAPHARMACIE -> "🧴";
            case HYGIENE -> "🧼";
            case COMPLEMENT_ALIMENTAIRE -> "🥗";
            default -> "📦";
        };
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterCards(newVal));
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
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private void handleAfficher(Produit produit) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails produit");
        info.setHeaderText(produit.getNom());
        info.setContentText(
                "Catégorie: " + (produit.getCategorie() != null ? produit.getCategorie().name() :
                        (produit.getCategorieName() != null ? produit.getCategorieName() : "N/A")) + "\n" +
                        "Prix: " + produit.getPrix() + "\n" +
                        "Stock: " + produit.getStock() + "\n" +
                        "Marque: " + (produit.getMarque() != null ? produit.getMarque() : "-") + "\n" +
                        "Disponible: " + (Boolean.TRUE.equals(produit.getDisponible()) ? "Oui" : "Non"));
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

    private void handleAjouterAuPanier(Produit produit) {
        try {
            int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
            servicePanier.ajouterAuPanier(userId, produit, 1);
            showAlert("✅ \"" + produit.getNom() + "\" ajouté au panier !");
        } catch (SQLException e) {
            showAlert("Erreur panier : " + e.getMessage());
        }
    }

    @FXML
    private void handleMonPanier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Panier.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🛒 Mon Panier");
            stage.setScene(new Scene(root, 700, 550));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            try { loadProduits(); } catch (SQLException ignored) {}
        } catch (Exception e) {
            showAlert("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleMesFavoris() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListeFavoris.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("❤️ Mes Favoris");
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert("Erreur: " + e.getMessage());
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
