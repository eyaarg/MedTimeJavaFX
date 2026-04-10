package tn.esprit.controllers.consultationonline;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.consultationonline.LigneOrdonnanceArij;
import tn.esprit.entities.consultationonline.OrdonnanceArij;
import tn.esprit.services.consultationonline.OrdonnanceServiceArij;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceFormControllerArij {
    private static final int CURRENT_USER_ID = 1;

    @FXML
    private TextArea contentArea;
    @FXML
    private TextField diagnosisField;
    @FXML
    private DatePicker dateValiditePicker;
    @FXML
    private TextArea instructionsArea;
    @FXML
    private VBox medicationsContainer;
    @FXML
    private Label contentError;
    @FXML
    private Label diagnosisError;
    @FXML
    private Label dateError;
    @FXML
    private Label medicationsError;

    private final OrdonnanceServiceArij ordonnanceService = new OrdonnanceServiceArij();
    private OrdonnanceArij ordonnance;

    @FXML
    private void initialize() {
        addMedicationRow();
    }

    public void setOrdonnance(OrdonnanceArij ordonnance) {
        this.ordonnance = ordonnance;
        if (ordonnance != null) {
            contentArea.setText(ordonnance.getContent());
            diagnosisField.setText(ordonnance.getDiagnosis());
            if (ordonnance.getDateValidite() != null) {
                dateValiditePicker.setValue(ordonnance.getDateValidite().toLocalDate());
            }
            instructionsArea.setText(ordonnance.getInstructions());
        }
    }

    @FXML
    private void addMedicationRow() {
        TextField nom = new TextField();
        nom.setPromptText("Nom médicament");
        TextField dosage = new TextField();
        dosage.setPromptText("Dosage");
        TextField quantite = new TextField();
        quantite.setPromptText("Quantité");
        TextField duree = new TextField();
        duree.setPromptText("Durée");
        TextField instr = new TextField();
        instr.setPromptText("Instructions");
        Button remove = new Button("X");
        HBox row = new HBox(6, nom, dosage, quantite, duree, instr, remove);
        row.setPrefWidth(800);
        remove.setOnAction(e -> medicationsContainer.getChildren().remove(row));
        medicationsContainer.getChildren().add(row);
    }

    @FXML
    private void handleSave() {
        hideErrors();
        boolean valid = true;
        if (contentArea.getText() == null || contentArea.getText().trim().length() < 10) {
            contentError.setVisible(true);
            contentError.setManaged(true);
            valid = false;
        }
        if (diagnosisField.getText() == null || diagnosisField.getText().trim().isEmpty()) {
            diagnosisError.setVisible(true);
            diagnosisError.setManaged(true);
            valid = false;
        }
        LocalDate validite = dateValiditePicker.getValue();
        if (validite == null) {
            dateError.setVisible(true);
            dateError.setManaged(true);
            valid = false;
        }

        List<LigneOrdonnanceArij> lignes = collectMedications();
        if (lignes.isEmpty()) {
            medicationsError.setVisible(true);
            medicationsError.setManaged(true);
            valid = false;
        }
        for (LigneOrdonnanceArij l : lignes) {
            if (l.getQuantite() <= 0) {
                medicationsError.setText("Quantité must be positive.");
                medicationsError.setVisible(true);
                medicationsError.setManaged(true);
                valid = false;
                break;
            }
        }

        if (!valid) return;

        if (ordonnance == null) {
            ordonnance = new OrdonnanceArij();
        }
        ordonnance.setContent(contentArea.getText().trim());
        ordonnance.setDiagnosis(diagnosisField.getText().trim());
        ordonnance.setDateValidite(validite.atStartOfDay());
        ordonnance.setInstructions(instructionsArea.getText());
        ordonnance.setDoctorId(CURRENT_USER_ID);
        ordonnance.setConsultationId(ordonnance.getConsultationId() == 0 ? 0 : ordonnance.getConsultationId());

        if (ordonnance.getId() == 0) {
            ordonnanceService.createOrdonnance(ordonnance, lignes);
        } else {
            ordonnanceService.updateOrdonnance(ordonnance, lignes);
        }
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private List<LigneOrdonnanceArij> collectMedications() {
        List<LigneOrdonnanceArij> list = new ArrayList<>();
        for (javafx.scene.Node node : medicationsContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                TextField nom = (TextField) row.getChildren().get(0);
                TextField dosage = (TextField) row.getChildren().get(1);
                TextField quantite = (TextField) row.getChildren().get(2);
                TextField duree = (TextField) row.getChildren().get(3);
                TextField instr = (TextField) row.getChildren().get(4);

                if (nom.getText() == null || nom.getText().trim().isEmpty()) continue;

                LigneOrdonnanceArij l = new LigneOrdonnanceArij();
                l.setNomMedicament(nom.getText().trim());
                l.setDosage(dosage.getText());
                try {
                    l.setQuantite(Integer.parseInt(quantite.getText().trim()));
                } catch (NumberFormatException ex) {
                    l.setQuantite(0);
                }
                l.setDureeTraitement(duree.getText());
                l.setInstructions(instr.getText());
                list.add(l);
            }
        }
        return list;
    }

    private void hideErrors() {
        contentError.setVisible(false);
        contentError.setManaged(false);
        diagnosisError.setVisible(false);
        diagnosisError.setManaged(false);
        dateError.setVisible(false);
        dateError.setManaged(false);
        medicationsError.setVisible(false);
        medicationsError.setManaged(false);
        medicationsError.setText("Add at least one medication.");
    }

    private void close() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }
}
