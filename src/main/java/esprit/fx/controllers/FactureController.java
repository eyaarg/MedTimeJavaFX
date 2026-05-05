package esprit.fx.controllers;

import esprit.fx.entities.Order;
import esprit.fx.entities.OrderItem;
import esprit.fx.services.ServiceOrder;
import esprit.fx.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class FactureController implements Initializable {

    @FXML private Label lblNumeroCommande;
    @FXML private Label lblDateCommande;
    @FXML private Label lblStatut;
    @FXML private Label lblClient;
    @FXML private Label lblTotal;
    @FXML private Label lblReduction;
    @FXML private VBox itemsContainer;

    private final ServiceOrder serviceOrder = new ServiceOrder();
    private Order order;
    private List<OrderItem> items;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setOrder(Order order) {
        this.order = order;
        chargerFacture();
    }

    private void chargerFacture() {
        lblNumeroCommande.setText("Commande #" + order.getId());

        if (order.getDateOrder() != null) {
            lblDateCommande.setText("Date : " +
                order.getDateOrder().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        }

        lblStatut.setText("✅ " + order.getStatus());

        String nomClient = UserSession.getCurrentUser() != null ?
                UserSession.getCurrentUser().getUsername() : "Client";
        lblClient.setText(nomClient);

        lblTotal.setText(String.format("%.2f TND", order.getTotal()));

        // Vérifier réduction fidélité
        try {
            if (serviceOrder.estEligibleReduction(order.getUserId())) {
                lblReduction.setText("🏷️ Réduction fidélité -20% appliquée sur cette commande");
                lblReduction.setVisible(true);
                lblReduction.setManaged(true);
            }
        } catch (SQLException ignored) {}

        // Charger les items
        try {
            items = serviceOrder.getItemsByOrder(order.getId());
            afficherItems();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement facture : " + e.getMessage());
        }
    }

    private void afficherItems() {
        itemsContainer.getChildren().clear();
        for (OrderItem item : items) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 12; -fx-border-color: transparent transparent #ecf0f1 transparent;");

            Label nomLabel = new Label(item.getProductName() != null ? item.getProductName() : "Produit #" + item.getProductId());
            nomLabel.setPrefWidth(200);
            nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
            HBox.setHgrow(nomLabel, Priority.ALWAYS);

            Label qteLabel = new Label(String.valueOf(item.getQuantity()));
            qteLabel.setPrefWidth(60);
            qteLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

            Label prixLabel = new Label(String.format("%.2f TND", item.getPrice()));
            prixLabel.setPrefWidth(90);
            prixLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

            Label sousTotalLabel = new Label(String.format("%.2f TND", item.getPrice() * item.getQuantity()));
            sousTotalLabel.setPrefWidth(90);
            sousTotalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

            row.getChildren().addAll(nomLabel, qteLabel, prixLabel, sousTotalLabel);
            itemsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleTelechargerPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer la facture");
        fileChooser.setInitialFileName("facture_commande_" + order.getId() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        Stage stage = (Stage) lblTotal.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            genererPDF(file);
        }
    }

    private void genererPDF(File file) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = 780;
                float pageWidth = PDRectangle.A4.getWidth();

                // Titre
                cs.beginText();
                cs.setFont(fontBold, 22);
                cs.newLineAtOffset(margin, y);
                cs.showText("MedTime - Facture");
                cs.endText();
                y -= 30;

                // Numéro commande
                cs.beginText();
                cs.setFont(fontNormal, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Commande #" + order.getId());
                cs.endText();
                y -= 18;

                // Date
                if (order.getDateOrder() != null) {
                    cs.beginText();
                    cs.setFont(fontNormal, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Date : " + order.getDateOrder().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    cs.endText();
                    y -= 18;
                }

                // Statut
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Statut : " + order.getStatus());
                cs.endText();
                y -= 30;

                // Ligne séparatrice
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 20;

                // En-tête tableau
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Produit");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(300, y);
                cs.showText("Qte");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(360, y);
                cs.showText("Prix unit.");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(450, y);
                cs.showText("Sous-total");
                cs.endText();
                y -= 20;

                // Items
                if (items != null) {
                    for (OrderItem item : items) {
                        String nom = item.getProductName() != null ? item.getProductName() : "Produit #" + item.getProductId();
                        if (nom.length() > 35) nom = nom.substring(0, 35) + "...";

                        cs.beginText();
                        cs.setFont(fontNormal, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(nom);
                        cs.endText();

                        cs.beginText();
                        cs.setFont(fontNormal, 10);
                        cs.newLineAtOffset(300, y);
                        cs.showText(String.valueOf(item.getQuantity()));
                        cs.endText();

                        cs.beginText();
                        cs.setFont(fontNormal, 10);
                        cs.newLineAtOffset(360, y);
                        cs.showText(String.format("%.2f TND", item.getPrice()));
                        cs.endText();

                        cs.beginText();
                        cs.setFont(fontNormal, 10);
                        cs.newLineAtOffset(450, y);
                        cs.showText(String.format("%.2f TND", item.getPrice() * item.getQuantity()));
                        cs.endText();

                        y -= 18;
                    }
                }

                y -= 10;
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 20;

                // Total
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(350, y);
                cs.showText("TOTAL : " + String.format("%.2f TND", order.getTotal()));
                cs.endText();
            }

            doc.save(file);
            showAlert(Alert.AlertType.INFORMATION, "✅ Facture PDF enregistrée avec succès !");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur génération PDF : " + e.getMessage());
        }
    }

    @FXML
    private void handleFermer() {
        try {
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/ListProd.fxml"));
            javafx.scene.Parent root = loader.load();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("MedTime — Produits");
        } catch (Exception e) {
            // Fallback : juste fermer
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
