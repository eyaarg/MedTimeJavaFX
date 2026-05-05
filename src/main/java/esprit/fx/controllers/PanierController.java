package esprit.fx.controllers;

import esprit.fx.entities.LignePanier;
import esprit.fx.entities.Order;
import esprit.fx.entities.Panier;
import esprit.fx.services.ServiceOrder;
import esprit.fx.services.ServicePanier;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PanierController implements Initializable {

    @FXML private VBox lignesContainer;
    @FXML private Label lblTotal;
    @FXML private Label lblNbArticles;

    private final ServicePanier servicePanier = new ServicePanier();
    private final ServiceOrder serviceOrder = new ServiceOrder();
    private Panier panier;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerPanier();
    }

    private void chargerPanier() {
        try {
            int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
            panier = servicePanier.getPanierByUser(userId);
            List<LignePanier> lignes = servicePanier.getLignesByPanier(panier.getId());
            afficherLignes(lignes);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement panier : " + e.getMessage());
        }
    }

    private void afficherLignes(List<LignePanier> lignes) {
        lignesContainer.getChildren().clear();

        // Vérifier éligibilité réduction
        boolean eligible = false;
        try {
            int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
            eligible = new ServiceOrder().estEligibleReduction(userId);
        } catch (Exception ignored) {}

        if (eligible) {
            Label bandeau = new Label("🎉 Réduction fidélité -20% appliquée sur tous vos produits !");
            bandeau.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; " +
                    "-fx-background-radius: 8;");
            bandeau.setMaxWidth(Double.MAX_VALUE);
            lignesContainer.getChildren().add(bandeau);
        }

        if (lignes.isEmpty()) {
            Label vide = new Label("🛒 Votre panier est vide");
            vide.setStyle("-fx-font-size: 16px; -fx-text-fill: #95a5a6;");
            lignesContainer.getChildren().add(vide);
            lblTotal.setText("0.00 TND");
            lblNbArticles.setText("0 article(s)");
            return;
        }

        double total = 0;
        int nbArticles = 0;

        for (LignePanier ligne : lignes) {
            lignesContainer.getChildren().add(creerLigneCard(ligne));
            total += ligne.getSousTotal();
            nbArticles += ligne.getQuantite();
        }

        lblTotal.setText(String.format("%.2f TND", total));
        lblNbArticles.setText(nbArticles + " article(s)");
    }

    private HBox creerLigneCard(LignePanier ligne) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        // Image produit
        ImageView imageView = new ImageView();
        imageView.setFitWidth(70);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);
        if (ligne.getProduitImage() != null && !ligne.getProduitImage().isBlank()) {
            try {
                URL imgUrl = getClass().getResource(ligne.getProduitImage());
                if (imgUrl != null) imageView.setImage(new Image(imgUrl.toExternalForm()));
            } catch (Exception ignored) {}
        }

        // Infos produit
        VBox infos = new VBox(4);
        Label nomLabel = new Label(ligne.getProduitNom());
        nomLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label prixLabel = new Label(String.format("%.2f TND × %d", ligne.getPrixUnitaire(), ligne.getQuantite()));
        prixLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        infos.getChildren().addAll(nomLabel, prixLabel);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // Sous-total
        Label sousTotalLabel = new Label(String.format("%.2f TND", ligne.getSousTotal()));
        sousTotalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        // Bouton supprimer
        Button supprimerBtn = new Button("🗑");
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                              "-fx-background-radius: 8; -fx-padding: 6 10;");
        supprimerBtn.setOnAction(e -> {
            try {
                servicePanier.supprimerLigne(ligne.getId(), ligne.getPanierId());
                chargerPanier();
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur suppression : " + ex.getMessage());
            }
        });

        card.getChildren().addAll(imageView, infos, sousTotalLabel, supprimerBtn);
        return card;
    }

    @FXML
    private void handleVider() {
        if (panier == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider le panier");
        confirm.setContentText("Voulez-vous vraiment vider votre panier ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                servicePanier.viderPanier(panier.getId());
                chargerPanier();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleValider() {
        if (panier == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Valider la commande");
        confirm.setContentText("Confirmer votre commande ?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : 0;
                Order order = serviceOrder.validerCommande(userId);

                // Ouvrir l'interface de détail commande
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrderDetail.fxml"));
                Parent root = loader.load();
                OrderDetailController controller = loader.getController();
                controller.setOrder(order);

                Stage stage = new Stage();
                stage.setTitle("📦 Détail Commande #" + order.getId());
                stage.setScene(new javafx.scene.Scene(root, 700, 580));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();

                chargerPanier();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur validation : " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
