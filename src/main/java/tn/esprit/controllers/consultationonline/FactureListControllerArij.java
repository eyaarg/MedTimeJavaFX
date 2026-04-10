package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.consultationonline.FactureArij;
import tn.esprit.services.consultationonline.FactureServiceArij;
import tn.esprit.utils.consultationonline.PdfExporterArij;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FactureListControllerArij {
    @FXML
    private FlowPane cardsContainer;

    private final FactureServiceArij factureService = new FactureServiceArij();

    @FXML
    private void initialize() {
        loadFactures();
    }

    private void loadFactures() {
        cardsContainer.getChildren().clear();
        List<FactureArij> factures = factureService.getMyFactures();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (FactureArij f : factures) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");
            card.setPrefWidth(220);

            Label numero = new Label("N°: " + (f.getNumeroFacture() != null ? f.getNumeroFacture() : f.getId()));
            Label date = new Label("Date: " + (f.getDateEmission() != null ? f.getDateEmission().format(formatter) : "N/A"));
            Label montant = new Label("Montant: " + f.getMontant());

            Button printBtn = new Button("Print PDF");
            printBtn.setOnAction(e -> {
                String path = System.getProperty("user.home") + "/facture-" + f.getId() + ".pdf";
                PdfExporterArij.exportFacture(f, path);
            });

            card.getChildren().addAll(numero, date, montant, printBtn);
            cardsContainer.getChildren().add(card);
        }
    }
}
