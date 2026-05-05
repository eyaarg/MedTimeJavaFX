package esprit.fx.controllers;

import esprit.fx.entities.Order;
import esprit.fx.entities.OrderItem;
import esprit.fx.services.ServiceOrder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the order detail view.
 * Displays order summary and items, with option to proceed to PayPal payment.
 */
public class OrderDetailController implements Initializable {

    @FXML private Label lblNumeroCommande;
    @FXML private Label lblStatut;
    @FXML private Label lblTotal;
    @FXML private VBox itemsContainer;

    private final ServiceOrder serviceOrder = new ServiceOrder();
    private Order order;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setOrder(Order order) {
        this.order = order;
        chargerDetail();
    }

    private void chargerDetail() {
        if (order == null) return;

        if (lblNumeroCommande != null) {
            lblNumeroCommande.setText("Commande #" + order.getId());
        }
        if (lblStatut != null) {
            lblStatut.setText(order.getStatus() != null ? order.getStatus() : "EN_ATTENTE");
        }
        if (lblTotal != null) {
            lblTotal.setText(String.format("%.2f TND", order.getTotal()));
        }

        try {
            List<OrderItem> items = serviceOrder.getItemsByOrder(order.getId());
            afficherItems(items);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement commande : " + e.getMessage());
        }
    }

    private void afficherItems(List<OrderItem> items) {
        if (itemsContainer == null) return;
        itemsContainer.getChildren().clear();

        for (OrderItem item : items) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 12; -fx-border-color: transparent transparent #ecf0f1 transparent;");

            String nom = item.getProductName() != null ? item.getProductName() : "Produit #" + item.getProductId();
            Label nomLabel = new Label(nom);
            nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
            HBox.setHgrow(nomLabel, Priority.ALWAYS);

            Label qteLabel = new Label("x" + item.getQuantity());
            qteLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

            Label prixLabel = new Label(String.format("%.2f TND", item.getPrice() * item.getQuantity()));
            prixLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

            row.getChildren().addAll(nomLabel, qteLabel, prixLabel);
            itemsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handlePayer() {
        if (order == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Paiement.fxml"));
            Parent root = loader.load();
            PaiementController controller = loader.getController();
            controller.setOrder(order);

            Stage stage = new Stage();
            stage.setTitle("💳 Paiement PayPal — Commande #" + order.getId());
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh status after payment
            chargerDetail();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur ouverture paiement : " + e.getMessage());
        }
    }

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) (lblTotal != null ? lblTotal.getScene().getWindow() : null);
        if (stage != null) stage.close();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
