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
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class ListeProduitController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField txtSearch;
    @FXML private Label lblStatus;
    @FXML private Button btnAjouter;
    @FXML private Button btnAnalyse;

    private ServiceProduit serviceProduit;
    private Connection connection;
    private List<Produit> produitsList;

    private final SmartSearchService aiService = new SmartSearchService();
    private final ServiceFavoris serviceFavoris = ServiceFavoris.getInstance();
    private final ServicePanier servicePanier = new ServicePanier();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connectToDatabase();
        configureRoleUi();
        try {
            loadProduits();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setupSearchListener();
    }

    @FXML
    private void onSmartSearch() {
        if (produitsList == null || produitsList.isEmpty()) {
            showAlert("Aucun produit disponible pour l'analyse.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Analyse produit");
        dialog.setHeaderText("Décrire le symptôme");
        dialog.setContentText("Symptôme :");
        dialog.getEditor().setPromptText("Exemple : j'ai mal à la tête");

        String currentSearch = txtSearch.getText();
        if (currentSearch != null && !currentSearch.isBlank()) {
            dialog.getEditor().setText(currentSearch.trim());
        }

        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            return;
        }

        String query = input.get().trim();
        if (query.isEmpty()) {
            showAlert("Veuillez saisir un symptôme.");
            return;
        }

        lblStatus.setText("Analyse IA en cours...");
        if (btnAnalyse != null) {
            btnAnalyse.setDisable(true);
        }

        new Thread(() -> {
            try {
                List<String> productNames = produitsList.stream()
                        .map(Produit::getNom)
                        .collect(Collectors.toList());

                String suggestedName = aiService.askGroq(query, productNames);

                Platform.runLater(() -> {
                    if (btnAnalyse != null) {
                        btnAnalyse.setDisable(false);
                    }

                    List<Produit> smartResult = findSuggestedProducts(suggestedName);
                    if (!smartResult.isEmpty()) {
                        displayCards(smartResult);
                        lblStatus.setText("Produit conseillé : " + smartResult.get(0).getNom());
                        showAlert("Produit conseillé : " + smartResult.get(0).getNom());
                    } else {
                        displayCards(List.of());
                        lblStatus.setText("Aucun produit conseillé pour : " + query);
                        showAlert("Aucun produit adapté n'a été trouvé dans la liste.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnAnalyse != null) {
                        btnAnalyse.setDisable(false);
                    }
                    lblStatus.setText("Erreur analyse : " + e.getMessage());
                    showAlert("Erreur analyse : " + e.getMessage());
                });
            }
        }).start();
    }

    private void configureRoleUi() {
        boolean patient = isPatient();
        if (btnAjouter != null) {
            btnAjouter.setVisible(!patient);
            btnAjouter.setManaged(!patient);
        }
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
        nomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(200);

        // Prix avec réduction éventuelle
        Label prixLabel = new Label(String.format("%.2f €", p.getPrix()));
        prixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");

        // Vérifier éligibilité réduction
        boolean eligible = false;
        try {
            int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
            eligible = new ServiceOrder().estEligibleReduction(userId);
        } catch (Exception ignored) {}

        if (eligible) {
            Label prixBarre = new Label(String.format("%.2f €", p.getPrix()));
            prixBarre.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-strikethrough: true;");
            double prixReduit = p.getPrix() * 0.80;
            prixLabel.setText(String.format("%.2f €", prixReduit));
            Label badge = new Label("🏷️ -20% Fidélité");
            badge.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; " +
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 2 6;");
            card.getChildren().addAll(prixBarre, badge);
        }

        // Stock avec alerte
        HBox stockBox = new HBox(6);
        stockBox.setAlignment(Pos.CENTER_LEFT);
        int stock = p.getStock() != null ? p.getStock() : 0;
        if (stock < 5) {
            Label stockLabel = new Label("🔴 Stock: " + stock);
            stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
            Label alertLabel = new Label("⚠️ Stock faible !");
            alertLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #1d4ed8; -fx-background-color: #eff6ff;" +
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
                "-fx-text-fill: #1d4ed8; -fx-font-size: 11px;" : "-fx-text-fill: #64748b; -fx-font-size: 11px;");

        // Boutons
        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER);

        Button detailsBtn = new Button("👁");
        detailsBtn.setTooltip(new Tooltip("Afficher"));
        detailsBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-border-color: #bfdbfe; -fx-border-width: 1; -fx-border-radius: 16; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        detailsBtn.setOnAction(e -> handleAfficher(p));

        Button modifierBtn = new Button("✏");
        modifierBtn.setTooltip(new Tooltip("Modifier"));
        modifierBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-border-color: #bfdbfe; -fx-border-width: 1; -fx-border-radius: 16; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        modifierBtn.setOnAction(e -> handleModifier(p));

        Button supprimerBtn = new Button("🗑");
        supprimerBtn.setTooltip(new Tooltip("Supprimer"));
        supprimerBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-border-color: #bfdbfe; -fx-border-width: 1; -fx-border-radius: 16; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        supprimerBtn.setOnAction(e -> {
            try { handleSupprimer(p); } catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        Button favoriBtn = new Button(serviceFavoris.estFavori(p) ? "♥" : "♡");
        favoriBtn.setTooltip(new Tooltip("Favori"));
        favoriBtn.setStyle(favoriteButtonStyle(serviceFavoris.estFavori(p)));
        favoriBtn.setOnAction(e -> {
            if (serviceFavoris.estFavori(p)) {
                serviceFavoris.supprimerFavori(p);
                favoriBtn.setText("♡");
                favoriBtn.setStyle(favoriteButtonStyle(false));
            } else {
                serviceFavoris.ajouterFavori(p);
                favoriBtn.setText("♥");
                favoriBtn.setStyle(favoriteButtonStyle(true));
            }
        });

        Button panierBtn = new Button("🛒");
        panierBtn.setTooltip(new Tooltip("Ajouter au panier"));
        panierBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-border-color: #bfdbfe; -fx-border-width: 1; -fx-border-radius: 16; -fx-font-size: 12px; -fx-background-radius: 16; -fx-padding: 5 10;");
        panierBtn.setOnAction(e -> handleAjouterAuPanier(p));

        if (isPatient()) {
            buttonsBox.getChildren().addAll(detailsBtn, favoriBtn, panierBtn);
        } else {
            buttonsBox.getChildren().addAll(detailsBtn, modifierBtn, supprimerBtn, favoriBtn, panierBtn);
        }
        card.getChildren().addAll(nomLabel, prixLabel, stockBox, dispoLabel, buttonsBox);

        return card;
    }

    private String favoriteButtonStyle(boolean selected) {
        String textColor = selected ? "#dc2626" : "#1d4ed8";
        String background = selected ? "#fff1f2" : "#eff6ff";
        String border = selected ? "#fecdd3" : "#bfdbfe";
        return "-fx-background-color: " + background + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-border-color: " + border + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 16; " +
                "-fx-font-size: 15px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 16; " +
                "-fx-padding: 4 10;";
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
        lblStatus.setText(count + " produit(s)");
    }

    private List<Produit> findSuggestedProducts(String suggestedName) {
        if (suggestedName == null || suggestedName.isBlank() || suggestedName.equalsIgnoreCase("NONE")) {
            return List.of();
        }

        String normalizedSuggestion = normalizeProductName(suggestedName);
        List<Produit> exactMatches = produitsList.stream()
                .filter(p -> normalizeProductName(p.getNom()).equals(normalizedSuggestion))
                .toList();
        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }

        return produitsList.stream()
                .filter(p -> {
                    String normalizedProduct = normalizeProductName(p.getNom());
                    return normalizedSuggestion.contains(normalizedProduct)
                            || normalizedProduct.contains(normalizedSuggestion);
                })
                .toList();
    }

    private String normalizeProductName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized;
    }

    private boolean isPatient() {
        String role = UserSession.getCurrentRole();
        return role == null || role.equalsIgnoreCase("PATIENT") || role.equalsIgnoreCase("ROLE_PATIENT");
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
