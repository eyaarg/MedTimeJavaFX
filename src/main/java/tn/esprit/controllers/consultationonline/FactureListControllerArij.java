package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.entities.consultationonline.FactureArij;
import tn.esprit.services.consultationonline.FactureServiceArij;
import tn.esprit.utils.consultationonline.PdfExporterArij;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FactureListControllerArij {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private FlowPane cardsContainer;
    private final FactureServiceArij service = new FactureServiceArij();

    @FXML
    private void initialize() { loadFactures(); }

    private void loadFactures() {
        cardsContainer.getChildren().clear();
        List<FactureArij> factures = service.getMyFactures();

        if (factures.isEmpty()) {
            cardsContainer.getChildren().add(emptyState());
            return;
        }

        for (FactureArij f : factures) {
            cardsContainer.getChildren().add(buildCard(f));
        }
    }

    private VBox buildCard(FactureArij f) {
        VBox card = new VBox(0);
        card.getStyleClass().add("invoice-card");
        card.setPrefWidth(260);

        // Top accent bar
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color:#2563eb;-fx-background-radius:12 12 0 0;");

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 8, 18));

        // Invoice number header
        HBox numRow = new HBox(8);
        numRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🧾");
        icon.setStyle("-fx-font-size:20px;");
        Label num = new Label("Facture #" + (f.getNumeroFacture() != null ? f.getNumeroFacture() : f.getId()));
        num.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0f172a;");
        numRow.getChildren().addAll(icon, num);

        Label dateLabel = new Label("📅  " + (f.getDateEmission() != null
                ? f.getDateEmission().format(FMT) : "N/A"));
        dateLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;");

        // Amount — big display
        Label amountLabel = new Label(String.format("%.2f TND", f.getMontant()));
        amountLabel.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#2563eb;");

        body.getChildren().addAll(numRow, dateLabel, amountLabel);

        // Action row
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 18, 16, 18));
        Button printBtn = new Button("⬇  Télécharger PDF");
        printBtn.getStyleClass().addAll("action-btn", "btn-action-edit");
        printBtn.setMaxWidth(Double.MAX_VALUE);
        printBtn.setOnAction(e -> {
            String path = System.getProperty("user.home") + "/facture-" + f.getId() + ".pdf";
            PdfExporterArij.exportFacture(f, path);
        });
        HBox.setHgrow(printBtn, Priority.ALWAYS);
        actions.getChildren().add(printBtn);

        card.getChildren().addAll(bar, body, actions);
        return card;
    }

    private VBox emptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        Label icon = new Label("🧾");
        icon.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Aucune facture disponible");
        msg.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        box.getChildren().addAll(icon, msg);
        return box;
    }
}
