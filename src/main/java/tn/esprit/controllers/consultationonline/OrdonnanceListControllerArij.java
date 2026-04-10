package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.consultationonline.ConsultationArij;
import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;
import tn.esprit.repositories.consultationonline.LigneOrdonnanceRepositoryArij;
import tn.esprit.services.consultationonline.ConsultationServiceArij;
import tn.esprit.services.consultationonline.OrdonnanceServiceArij;
import tn.esprit.utils.consultationonline.PdfExporterArij;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceListControllerArij {
    private static final int CURRENT_USER_ID = 1;
    private static final String CURRENT_USER_ROLE = "PATIENT";

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Button newOrdonnanceButton;

    private final ConsultationServiceArij consultationService = new ConsultationServiceArij();
    private final OrdonnanceServiceArij ordonnanceService = new OrdonnanceServiceArij();
    private final LigneOrdonnanceRepositoryArij ligneRepo = new LigneOrdonnanceRepositoryArij();

    @FXML
    private void initialize() {
        newOrdonnanceButton.setVisible("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));
        loadOrdonnances();
    }

    @FXML
    private void openNewOrdonnance() {
        // Placeholder for navigation; could be wired to open form.
    }

    private void loadOrdonnances() {
        cardsContainer.getChildren().clear();
        List<OrdonnanceArij> ordonnances = new ArrayList<>();
        List<ConsultationArij> consultations = consultationService.getMyConsultations();
        for (ConsultationArij c : consultations) {
            OrdonnanceArij o = ordonnanceService.getByConsultationId(c.getId());
            if (o != null) {
                ordonnances.add(o);
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (OrdonnanceArij o : ordonnances) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");
            card.setPrefWidth(260);

            Label numero = new Label("Numéro: " + (o.getNumeroOrdonnance() != null ? o.getNumeroOrdonnance() : o.getId()));
            Label date = new Label("Date: " + (o.getDateEmission() != null ? o.getDateEmission().format(formatter) : "N/A"));
            Label medecin = new Label("Médecin: " + o.getDoctorId());
            Label diag = new Label("Diagnostic: " + (o.getDiagnosis() != null ? o.getDiagnosis() : ""));

            Button viewBtn = new Button("View Details");
            viewBtn.setOnAction(e -> viewDetails(o));

            Button editBtn = new Button("Edit");
            editBtn.setVisible("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));
            editBtn.setOnAction(e -> editOrdonnance(o));

            Button deleteBtn = new Button("Delete");
            deleteBtn.setVisible("DOCTOR".equalsIgnoreCase(CURRENT_USER_ROLE));
            deleteBtn.setOnAction(e -> {
                ordonnanceService.deleteOrdonnance(o.getId());
                loadOrdonnances();
            });

            Button printBtn = new Button("Print PDF");
            printBtn.setOnAction(e -> printOrdonnance(o));

            card.getChildren().addAll(numero, date, medecin, diag, viewBtn);
            if (editBtn.isVisible()) {
                card.getChildren().add(editBtn);
            }
            if (deleteBtn.isVisible()) {
                card.getChildren().add(deleteBtn);
            }
            card.getChildren().add(printBtn);

            cardsContainer.getChildren().add(card);
        }
    }

    private void viewDetails(OrdonnanceArij o) {
        // Could open a detail view; keeping lightweight per instructions.
    }

    private void editOrdonnance(OrdonnanceArij o) {
        // Hook to open form window; omitted for brevity.
    }

    private void printOrdonnance(OrdonnanceArij o) {
        List<LigneOrdonnanceArij> lignes = ligneRepo.findByOrdonnanceId(o.getId());
        String path = System.getProperty("user.home") + "/ordonnance-" + o.getId() + ".pdf";
        PdfExporterArij.exportOrdonnance(o, lignes, path);
    }
}
