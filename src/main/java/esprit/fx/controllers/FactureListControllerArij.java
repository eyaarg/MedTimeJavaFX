package esprit.fx.controllers;

import esprit.fx.entities.FactureArij;
import esprit.fx.services.ServiceFactureArij;
import esprit.fx.services.StripeServiceArij;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactureListControllerArij {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private FlowPane cardsContainer;
    private final ServiceFactureArij service = new ServiceFactureArij();
    private final StripeServiceArij stripeService = new StripeServiceArij();
    private HttpServer stripeCallbackServer;
    private int patientId = 0; // patients.id (provided by MainControllerArij)

    @FXML private void initialize() { /* context is set after load */ }

    private void loadFactures() {
        cardsContainer.getChildren().clear();
        List<FactureArij> factures = service.getFacturesByPatient(patientId);
        if (factures.isEmpty()) { cardsContainer.getChildren().add(emptyState()); return; }
        for (FactureArij f : factures) cardsContainer.getChildren().add(buildCard(f));
    }

    private VBox buildCard(FactureArij f) {
        VBox card = new VBox(0);
        card.getStyleClass().add("invoice-card");
        card.setPrefWidth(260);

        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color:#2563eb;-fx-background-radius:12 12 0 0;");

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 8, 18));

        HBox numRow = new HBox(8);
        numRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🧾"); icon.setStyle("-fx-font-size:20px;");
        Label num = new Label("Facture de consultation");
        num.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        numRow.getChildren().addAll(icon, num);

        Label dateLabel = new Label("📅  " + (f.getDateEmission() != null ? f.getDateEmission().format(FMT) : "N/A"));
        dateLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");
        Label amountLabel = new Label(String.format("%.2f TND", f.getMontant()));
        amountLabel.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#2563eb;");
        Label statusLabel = buildStatusBadge(f);

        body.getChildren().addAll(numRow, dateLabel, amountLabel, statusLabel);

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 18, 16, 18));
        if (isPaid(f)) {
            Label paidInfo = new Label("Paiement confirme");
            paidInfo.setMaxWidth(Double.MAX_VALUE);
            paidInfo.setAlignment(Pos.CENTER);
            paidInfo.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 12;");
            HBox.setHgrow(paidInfo, Priority.ALWAYS);
            actions.getChildren().add(paidInfo);
        } else {
            Button payBtn = new Button("Payer Stripe");
            payBtn.getStyleClass().addAll("action-btn", "btn-action-edit");
            payBtn.setMaxWidth(Double.MAX_VALUE);
            payBtn.setOnAction(e -> payerFacture(f));
            HBox.setHgrow(payBtn, Priority.ALWAYS);
            actions.getChildren().add(payBtn);

        }

        card.getChildren().addAll(bar, body, actions);
        return card;
    }

    private VBox emptyState() {
        VBox box = new VBox(12); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(60));
        Label icon = new Label("🧾"); icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Aucune facture disponible"); msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        box.getChildren().addAll(icon, msg);
        return box;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
        loadFactures();
    }

    private void payerFacture(FactureArij facture) {
        if (facture == null || facture.getMontant() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Paiement impossible", "Montant de facture invalide.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                startStripeCallbackServer();
                stripeService.payerConsultation(
                        facture.getMontant(),
                        "Facture consultation MedTime",
                        (long) facture.getId()
                );
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Stripe", ex.getMessage()));
            }
        }, "stripe-payment-arij");
        t.setDaemon(true);
        t.start();
    }

    private Label buildStatusBadge(FactureArij facture) {
        boolean paid = isPaid(facture);
        Label badge = new Label(paid ? "Payee via Stripe" : "En attente de paiement");
        String bg = paid ? "#dcfce7" : "#fff7ed";
        String fg = paid ? "#166534" : "#9a3412";
        String border = paid ? "#86efac" : "#fed7aa";
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-border-color:" + border + ";-fx-border-width:1;" +
                "-fx-background-radius:999;-fx-border-radius:999;" +
                "-fx-padding:4 10;-fx-font-size:11px;-fx-font-weight:bold;");
        return badge;
    }

    private boolean isPaid(FactureArij facture) {
        if (facture == null || facture.getPaiementStatus() == null) {
            return false;
        }
        String status = facture.getPaiementStatus().trim().toUpperCase();
        return status.equals("PAYE") || status.equals("PAYEE") || status.equals("PAID") || status.equals("SUCCEEDED");
    }

    private synchronized void startStripeCallbackServer() throws IOException {
        if (stripeCallbackServer != null) {
            return;
        }

        stripeCallbackServer = HttpServer.create(new InetSocketAddress("localhost", 8765), 0);
        stripeCallbackServer.createContext("/stripe/success", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            int factureId = parseInt(params.get("factureId"));
            boolean updated = factureId > 0 && service.confirmerPaiement(factureId);

            String html = updated
                    ? "<html><body><h2>Paiement confirme</h2><p>Vous pouvez revenir a MedTime.</p></body></html>"
                    : "<html><body><h2>Paiement recu</h2><p>Impossible de mettre a jour la facture.</p></body></html>";
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(updated ? 200 : 500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }

            if (updated) {
                Platform.runLater(() -> {
                    loadFactures();
                    showAlert(Alert.AlertType.INFORMATION, "Paiement confirme", "La facture est marquee automatiquement comme payee.");
                });
            }
        });
        stripeCallbackServer.setExecutor(null);
        stripeCallbackServer.start();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
