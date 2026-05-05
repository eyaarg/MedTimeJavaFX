package esprit.fx.controllers;

import com.paypal.api.payments.Payment;
import esprit.fx.entities.Order;
import esprit.fx.services.PayPalService;
import esprit.fx.services.ServiceOrder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PaiementController implements Initializable {

    @FXML private WebView webView;
    @FXML private Label lblMontant;
    @FXML private Label lblStatut;
    @FXML private HBox loadingBox;

    private final PayPalService payPalService = new PayPalService();
    private final ServiceOrder serviceOrder = new ServiceOrder();
    private Order order;

    // URLs de redirection PayPal
    private static final String SUCCESS_URL = "https://medtime.app/paypal/success";
    private static final String CANCEL_URL  = "https://medtime.app/paypal/cancel";

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setOrder(Order order) {
        this.order = order;
        lblMontant.setText("— Total : " + String.format("%.2f TND", order.getTotal()));
        lancerPaiement();
    }

    private void lancerPaiement() {
        loadingBox.setVisible(true);
        lblStatut.setText("Initialisation du paiement...");

        new Thread(() -> {
            try {
                // Créer le paiement PayPal
                Payment payment = payPalService.creerPaiement(
                        order.getTotal(), "USD", SUCCESS_URL, CANCEL_URL
                );

                String approvalUrl = payPalService.getApprovalUrl(payment);

                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    lblStatut.setText("En attente d'approbation PayPal...");

                    // Charger l'URL PayPal dans le WebView
                    webView.getEngine().load(approvalUrl);

                    // Écouter les changements d'URL pour détecter succès/annulation
                    webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
                        if (newUrl != null && newUrl.startsWith(SUCCESS_URL)) {
                            // Extraire paymentId et PayerID de l'URL
                            String paymentId = extraireParam(newUrl, "paymentId");
                            String payerId   = extraireParam(newUrl, "PayerID");
                            confirmerPaiement(paymentId, payerId);
                        } else if (newUrl != null && newUrl.startsWith(CANCEL_URL)) {
                            Platform.runLater(() -> {
                                lblStatut.setText("❌ Paiement annulé.");
                                showAlert(Alert.AlertType.WARNING, "Paiement annulé par l'utilisateur.");
                            });
                        }
                    });
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    lblStatut.setText("⚠️ Erreur : " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Erreur PayPal : " + e.getMessage());
                });
            }
        }).start();
    }

    private void confirmerPaiement(String paymentId, String payerId) {
        lblStatut.setText("⏳ Confirmation du paiement...");

        new Thread(() -> {
            try {
                Payment payment = payPalService.executerPaiement(paymentId, payerId);

                if ("approved".equals(payment.getState())) {
                    serviceOrder.updateStatut(order.getId(), "PAYEE");
                    order.setStatus("PAYEE");

                    Platform.runLater(() -> {
                        lblStatut.setText("✅ Paiement confirmé !");
                        // Fermer la fenêtre PayPal
                        Stage stage = (Stage) webView.getScene().getWindow();
                        stage.close();
                        // Ouvrir la facture
                        ouvrirFacture();
                    });
                } else {
                    Platform.runLater(() -> {
                        lblStatut.setText("❌ Paiement refusé.");
                        showAlert(Alert.AlertType.ERROR, "Le paiement a été refusé par PayPal.");
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatut.setText("⚠️ Erreur confirmation : " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Erreur lors de la confirmation : " + e.getMessage());
                });
            }
        }).start();
    }

    private void ouvrirFacture() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Facture.fxml"));
            Parent root = loader.load();
            FactureController controller = loader.getController();
            controller.setOrder(order);

            Stage stage = new Stage();
            stage.setTitle("📄 Facture — Commande #" + order.getId());
            stage.setScene(new Scene(root, 700, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur ouverture facture : " + e.getMessage());
        }
    }

    private String extraireParam(String url, String param) {
        try {
            String[] parts = url.split("[?&]");
            for (String part : parts) {
                if (part.startsWith(param + "=")) {
                    return part.substring(param.length() + 1);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
