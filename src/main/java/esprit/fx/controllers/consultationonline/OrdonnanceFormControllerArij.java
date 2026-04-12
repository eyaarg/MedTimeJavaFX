package esprit.fx.controllers.consultationonline;

import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.OrdonnanceArij;
import esprit.fx.services.ServiceFactureArij;
import esprit.fx.services.ServiceOrdonnanceArij;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceFormControllerArij {

    private static final int CURRENT_DOCTOR_ID = 1; // à remplacer par session

    @FXML private TextArea contentArea;
    @FXML private TextField diagnosisField;
    @FXML private DatePicker dateValiditePicker;
    @FXML private TextArea instructionsArea;
    @FXML private VBox medicationsContainer;
    @FXML private TextField prixConsultationField;   // ← nouveau champ prix
    @FXML private Label contentError;
    @FXML private Label diagnosisError;
    @FXML private Label dateError;
    @FXML private Label medicationsError;
    @FXML private Label prixError;

    private final ServiceOrdonnanceArij ordonnanceService = new ServiceOrdonnanceArij();
    private final ServiceFactureArij factureService = new ServiceFactureArij();

    private OrdonnanceArij ordonnance;
    private int consultationId;
    private int patientId;

    @FXML private void initialize() { addMedicationRow(); }

    /** Appelé par OrdonnanceListController avant d'afficher le formulaire */
    public void setContext(int consultationId, int patientId) {
        this.consultationId = consultationId;
        this.patientId = patientId;
    }

    public void setOrdonnance(OrdonnanceArij ordonnance) {
        this.ordonnance = ordonnance;
        if (ordonnance != null) {
            contentArea.setText(ordonnance.getContent());
            diagnosisField.setText(ordonnance.getDiagnosis());
            if (ordonnance.getDateValidite() != null)
                dateValiditePicker.setValue(ordonnance.getDateValidite().toLocalDate());
            instructionsArea.setText(ordonnance.getInstructions());
            // En mode édition, le prix n'est pas modifiable
            prixConsultationField.setDisable(true);
            prixConsultationField.setPromptText("Prix non modifiable");
        }
    }

    @FXML
    private void addMedicationRow() {
        TextField nom    = new TextField(); nom.setPromptText("Médicament");    nom.setPrefWidth(140);
        TextField dosage = new TextField(); dosage.setPromptText("Dosage");     dosage.setPrefWidth(80);
        TextField qte    = new TextField(); qte.setPromptText("Qté");           qte.setPrefWidth(50);
        TextField duree  = new TextField(); duree.setPromptText("Durée");       duree.setPrefWidth(80);
        TextField instr  = new TextField(); instr.setPromptText("Instructions"); instr.setPrefWidth(120);
        Button remove = new Button("✕");
        remove.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;-fx-background-radius:6;-fx-cursor:hand;-fx-border-color:transparent;");
        HBox row = new HBox(6, nom, dosage, qte, duree, instr, remove);
        remove.setOnAction(e -> medicationsContainer.getChildren().remove(row));
        medicationsContainer.getChildren().add(row);
    }

    @FXML
    private void handleSave() {
        hideErrors();
        boolean valid = true;

        if (contentArea.getText() == null || contentArea.getText().trim().length() < 10) {
            contentError.setVisible(true); contentError.setManaged(true); valid = false;
        }
        if (diagnosisField.getText() == null || diagnosisField.getText().trim().isEmpty()) {
            diagnosisError.setVisible(true); diagnosisError.setManaged(true); valid = false;
        }
        LocalDate validite = dateValiditePicker.getValue();
        if (validite == null) {
            dateError.setVisible(true); dateError.setManaged(true); valid = false;
        }
        List<LigneOrdonnanceArij> lignes = collectMedications();
        if (lignes.isEmpty()) {
            medicationsError.setVisible(true); medicationsError.setManaged(true); valid = false;
        }

        double prix = 0;
        boolean isNew = (ordonnance == null || ordonnance.getId() == 0);
        if (isNew) {
            try {
                prix = Double.parseDouble(prixConsultationField.getText().trim());
                if (prix <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                prixError.setVisible(true); prixError.setManaged(true); valid = false;
            }
        }

        if (!valid) return;

        if (ordonnance == null) ordonnance = new OrdonnanceArij();
        ordonnance.setContent(contentArea.getText().trim());
        ordonnance.setDiagnosis(diagnosisField.getText().trim());
        ordonnance.setDateValidite(validite.atStartOfDay());
        ordonnance.setInstructions(instructionsArea.getText());
        ordonnance.setDoctorId(CURRENT_DOCTOR_ID);
        ordonnance.setConsultationId(consultationId);

        if (isNew) {
            ordonnanceService.createOrdonnance(ordonnance, lignes);
            // Récupérer l'ordonnance créée pour avoir son id
            OrdonnanceArij saved = ordonnanceService.getByConsultationId(consultationId);
            int ordId = saved != null ? saved.getId() : 0;
            // Créer la facture automatiquement
            factureService.createFactureForConsultation(consultationId, patientId, prix, ordId);
            // Mettre à jour le prix dans la consultation
            ordonnanceService.updateConsultationFee(consultationId, prix);
        } else {
            ordonnanceService.updateOrdonnance(ordonnance, lignes);
        }
        close();
    }

    @FXML private void handleCancel() { close(); }

    private List<LigneOrdonnanceArij> collectMedications() {
        List<LigneOrdonnanceArij> list = new ArrayList<>();
        for (javafx.scene.Node node : medicationsContainer.getChildren()) {
            if (!(node instanceof HBox)) continue;
            HBox row = (HBox) node;
            TextField nom = (TextField) row.getChildren().get(0);
            if (nom.getText() == null || nom.getText().trim().isEmpty()) continue;
            LigneOrdonnanceArij l = new LigneOrdonnanceArij();
            l.setNomMedicament(nom.getText().trim());
            l.setDosage(((TextField) row.getChildren().get(1)).getText());
            try { l.setQuantite(Integer.parseInt(((TextField) row.getChildren().get(2)).getText().trim())); }
            catch (NumberFormatException ex) { l.setQuantite(1); }
            l.setDureeTraitement(((TextField) row.getChildren().get(3)).getText());
            l.setInstructions(((TextField) row.getChildren().get(4)).getText());
            list.add(l);
        }
        return list;
    }

    private void hideErrors() {
        contentError.setVisible(false);    contentError.setManaged(false);
        diagnosisError.setVisible(false);  diagnosisError.setManaged(false);
        dateError.setVisible(false);       dateError.setManaged(false);
        medicationsError.setVisible(false);medicationsError.setManaged(false);
        prixError.setVisible(false);       prixError.setManaged(false);
    }

    private void close() { ((Stage) contentArea.getScene().getWindow()).close(); }
}
