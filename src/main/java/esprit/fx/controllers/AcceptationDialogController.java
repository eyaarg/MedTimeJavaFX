package esprit.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Contrôleur pour le dialog d'acceptation de consultation.
 * Permet au médecin de choisir la date et l'heure de la consultation.
 */
public class AcceptationDialogController {

    @FXML private Label patientLabel;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> heurePicker;
    @FXML private Spinner<Integer> minutePicker;

    private String patientName;
    private LocalDateTime dateTimeSelectionnee;

    @FXML
    private void initialize() {
        // Initialiser le DatePicker
        datePicker.setValue(LocalDate.now());

        // Initialiser le Spinner d'heure (0-23)
        SpinnerValueFactory<Integer> heureFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 14);
        heurePicker.setValueFactory(heureFactory);
        heurePicker.setEditable(true);

        // Initialiser le Spinner de minutes (0, 15, 30, 45)
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15);
        minutePicker.setValueFactory(minuteFactory);
        minutePicker.setEditable(true);

        // Formater les spinners
        formatSpinner(heurePicker);
        formatSpinner(minutePicker);
    }

    /**
     * Formate un spinner pour afficher les valeurs avec 2 chiffres.
     */
    private void formatSpinner(Spinner<Integer> spinner) {
        spinner.getValueFactory().setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "00" : String.format("%02d", value);
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });
    }

    /**
     * Définit le nom du patient à afficher.
     */
    public void setPatientName(String name) {
        this.patientName = name;
        patientLabel.setText("Patient : " + name);
    }

    /**
     * Récupère la date et l'heure sélectionnées.
     */
    public LocalDateTime getDateTimeSelectionnee() {
        LocalDate date = datePicker.getValue();
        int heure = heurePicker.getValue();
        int minute = minutePicker.getValue();

        dateTimeSelectionnee = LocalDateTime.of(date, LocalTime.of(heure, minute));
        return dateTimeSelectionnee;
    }

    /**
     * Valide les données saisies.
     */
    public boolean valider() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Veuillez sélectionner une date");
            return false;
        }

        if (date.isBefore(LocalDate.now())) {
            showError("La date doit être dans le futur");
            return false;
        }

        return true;
    }

    /**
     * Affiche un message d'erreur.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
