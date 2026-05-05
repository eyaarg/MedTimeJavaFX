package esprit.fx.controllers;

import esprit.fx.entities.CategorieEnum;
import esprit.fx.entities.Produit;
import esprit.fx.services.ServiceFavoris;
import esprit.fx.services.ServicePanier;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class ListeFavorisController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private Label lblStatus;
    @FXML private Label lblVide;

    private final ServiceFavoris serviceFavoris = ServiceFavoris.getInstance();
    private final ServicePanier servicePanier = new ServicePanier();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        afficherFavoris();
    }

    private void afficherFavoris() {
        cardsContainer.getChildren().clear();
        List<Produit> favoris = serviceFavoris.getFavoris();

        if (favoris.isEmpty()) {
            lblVide.setVisible(true);
            lblVide.setManaged(true);
            lblStatus.setText("0 favori(s)");
            return;
        }

        lblVide.setVisible(false);
        lblVide.setManaged(false);

        for (Produit p : favoris) {
            cardsContainer.getChildren().add(createCard(p));
        }
        lblStatus.setText("❤️ " + favoris.size() + " favori(s)");
    }

    private VBox createCard(Produit p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(220);
        card.setPadding(new Insets(12));

        Label iconLabel = new Label(getIconForCategorie(p.getCategorie()));
        iconLabel.setStyle("-fx-font-size: 40px;");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(javafx.geometry.Pos.CENTER);

        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nomLabel.setWrapText(true);

        Label prixLabel = new Label(String.format("%.2f €", p.getPrix()));
        prixLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        Label marqueLabel = new Label(p.getMarque() != null ? p.getMarque() : "");
        marqueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Button retirerBtn = new Button("🗑 Retirer");
        retirerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 16; -fx-padding: 5 12;");
        retirerBtn.setTooltip(new Tooltip("Retirer des favoris"));
        retirerBtn.setOnAction(e -> {
            serviceFavoris.supprimerFavori(p);
            afficherFavoris();
        });

        Button panierBtn = new Button("🛒");
        panierBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; " +
                "-fx-background-radius: 16; -fx-padding: 5 12;");
        panierBtn.setTooltip(new Tooltip("Ajouter au panier"));
        panierBtn.setOnAction(e -> {
            try {
                int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
                servicePanier.ajouterAuPanier(userId, p, 1);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("✅ \"" + p.getNom() + "\" ajouté au panier !");
                alert.showAndWait();
            } catch (SQLException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Erreur panier : " + ex.getMessage());
                alert.showAndWait();
            }
        });

        HBox buttonsBox = new HBox(8);
        buttonsBox.getChildren().addAll(retirerBtn, panierBtn);

        card.getChildren().addAll(iconLabel, nomLabel, prixLabel, marqueLabel, buttonsBox);
        return card;
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
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur ouverture panier : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleRetour() {
        try {
            Node node = cardsContainer;
            javafx.scene.Scene scene = node.getScene();
            if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane bp) {
                Node center = bp.getCenter();
                if (center instanceof javafx.scene.layout.StackPane sp) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListProd.fxml"));
                    sp.getChildren().setAll(Collections.singleton(loader.load()));
                    return;
                }
            }
            // fallback : ouvrir dans une nouvelle fenêtre
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ListProd.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) cardsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
