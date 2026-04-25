package esprit.fx.controllers;

import esprit.fx.entities.Produit;
import esprit.fx.entities.CategorieEnum;
import esprit.fx.services.ServiceFavoris;
import esprit.fx.services.ServiceProduit;
import esprit.fx.utils.MyDB;
// NOUVEAUX IMPORTS POUR L'IA
import esprit.fx.service.SmartSearchService;
import javafx.application.Platform;
import java.util.stream.Collectors;
// ---
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

    // AJOUT : Instance du service IA
    private final SmartSearchService aiService = new SmartSearchService();

    private final ServiceFavoris serviceFavoris = ServiceFavoris.getInstance();

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

    // --- NOUVELLE MÉTHODE : RECHERCHE INTELLIGENTE ---
    @FXML
    private void onSmartSearch() {
        String query = txtSearch.getText();

        if (query == null || query.trim().isEmpty()) {
            displayCards(produitsList);
            return;
        }

        lblStatus.setText("🤖 Groq réfléchit...");

        // On utilise un Thread séparé pour ne pas "figer" l'interface JavaFX pendant l'appel API
        new Thread(() -> {
            try {
                // 1. On prépare la liste des noms disponibles
                List<String> productNames = produitsList.stream()
                        .map(Produit::getNom)
                        .collect(Collectors.toList());

                // 2. Appel au service Groq
                String suggestedName = aiService.askGroq(query, productNames);

                // 3. Retour à l'interface graphique (UI Thread)
                Platform.runLater(() -> {
                    if (suggestedName != null && !suggestedName.equalsIgnoreCase("NONE")) {
                        List<Produit> smartResult = produitsList.stream()
                                .filter(p -> p.getNom().equalsIgnoreCase(suggestedName))
                                .toList();

                        displayCards(smartResult);
                        lblStatus.setText("✨ IA a trouvé : " + suggestedName);
                    } else {
                        displayCards(List.of()); // Liste vide
                        lblStatus.setText("❌ IA n'a rien trouvé pour : " + query);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("⚠️ Erreur IA : " + e.getMessage());
                });
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

        // Icône selon catégorie (fallback si pas d'image)
        String imagePath = p.getImage();
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) {
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(imgUrl.toExternalForm())
                    );
                    imageView.setFitWidth(100);
                    imageView.setFitHeight(80);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
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

        // Bouton favori
        boolean isFavori = serviceFavoris.estFavori(p);
        Button favoriBtn = new Button(isFavori ? "❤️" : "🤍");
        favoriBtn.setTooltip(new Tooltip(isFavori ? "Retirer des favoris" : "Ajouter aux favoris"));
        favoriBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-padding: 5 6;");
        favoriBtn.setOnAction(e -> {
            if (serviceFavoris.estFavori(p)) {
                serviceFavoris.supprimerFavori(p);
                favoriBtn.setText("🤍");
                favoriBtn.setTooltip(new Tooltip("Ajouter aux favoris"));
            } else {
                serviceFavoris.ajouterFavori(p);
                favoriBtn.setText("❤️");
                favoriBtn.setTooltip(new Tooltip("Retirer des favoris"));
            }
        });

        buttonsBox.getChildren().addAll(detailsBtn, modifierBtn, supprimerBtn, favoriBtn);

        // Ajouter tous les éléments à la carte (l'image/icône est déjà ajoutée en index 0)
        card.getChildren().addAll(nomLabel, prixLabel, stockLabel, dispoLabel, buttonsBox);

        return card;
    }

    private Label makeIconLabel(Produit p) {
        Label iconLabel = new Label(getIconForCategorie(p.getCategorie()));
        iconLabel.setStyle("-fx-font-size: 40px;");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(javafx.geometry.Pos.CENTER);
        return iconLabel;
    }

    private String getIconForCategorie(CategorieEnum categorie) {
        if (categorie == null) return "📦";
        switch (categorie) {
            case MEDICAMENT: return "💊";
            case MATERIEL_MEDICAL: return "🩺";
            case PARAPHARMACIE: return "🧴";
            case HYGIENE: return "🧼";
            case COMPLEMENT_ALIMENTAIRE: return "🥗";
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
                "Catégorie: " + (produit.getCategorie() != null ? produit.getCategorie().name() : "N/A") + "\n" +
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
            e.printStackTrace();
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}