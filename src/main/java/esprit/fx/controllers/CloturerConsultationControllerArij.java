package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.LigneOrdonnanceArij;
import esprit.fx.entities.NotificationArij;
import esprit.fx.entities.OrdonnanceArij;
import esprit.fx.services.NotificationServiceArij;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceFactureArij;
import esprit.fx.services.ServiceOrdonnanceArij;
import esprit.fx.services.TwilioSmsServiceArij;
import esprit.fx.utils.MyDB;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 *  CloturerConsultationControllerArij
 * ============================================================
 *
 *  Formulaire de clôture de consultation pour le médecin.
 *
 *  Actions déclenchées par "Envoyer l'ordonnance et clore" :
 *  ──────────────────────────────────────────────────────────
 *  1. Validation des champs (médicaments, posologie, prix > 0)
 *  2. Créer l'Ordonnance en BDD avec accessToken UUID
 *  3. Mettre consultation.statut = "terminee" + prix saisi
 *  4. NotificationService.notifier(patient, "Ordonnance prête...", "success")
 *  5. TwilioSmsService.envoyer(patient.telephone, "Votre ordonnance...")
 *     → dans un Task background (appel réseau non bloquant)
 *  6. Alert.INFORMATION de confirmation
 *  7. Fermer la fenêtre + callback onSuccess (rafraîchir la liste)
 */
public class CloturerConsultationControllerArij {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //
    @FXML private Label    lblPatientNom;
    @FXML private Label    lblDateConsultation;
    @FXML private Label    lblConsultationId;

    @FXML private TextArea medicamentsArea;
    @FXML private TextArea posologieArea;
    @FXML private TextArea recommandationsArea;
    @FXML private TextField prixField;
    @FXML private TextField lienMeetField;

    @FXML private Label    errMedicaments;
    @FXML private Label    errPosologie;
    @FXML private Label    errPrix;
    @FXML private Label    errGlobal;
    @FXML private Button   btnCloturer;

    // ------------------------------------------------------------------ //
    //  Services                                                           //
    // ------------------------------------------------------------------ //
    private final ServiceConsultationsArij consultationService =
        new ServiceConsultationsArij();
    private final ServiceOrdonnanceArij ordonnanceService =
        new ServiceOrdonnanceArij();
    private final ServiceFactureArij factureService =
        new ServiceFactureArij();
    private final NotificationServiceArij notifService =
        NotificationServiceArij.getInstance();
    private final TwilioSmsServiceArij smsService =
        new TwilioSmsServiceArij();

    // ------------------------------------------------------------------ //
    //  Contexte injecté par le parent                                     //
    // ------------------------------------------------------------------ //
    private ConsultationsArij consultation;
    private int               doctorId;
    private String            patientNom;
    private String            patientTelephone;
    private int               patientUserId;

    /** Callback exécuté après clôture réussie (ex: refresh de la liste). */
    private Runnable onSuccess;

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // Limiter le champ prix aux chiffres et au point décimal
        prixField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9]*\\.?[0-9]*")) {
                prixField.setText(oldVal);
            }
        });
    }

    /**
     * Injecte le contexte depuis le controller parent.
     *
     * @param consultation  entité consultation à clôturer
     * @param doctorId      id du médecin connecté
     * @param onSuccess     callback appelé après clôture réussie
     */
    public void setContext(ConsultationsArij consultation,
                           int doctorId,
                           Runnable onSuccess) {
        this.consultation = consultation;
        this.doctorId     = doctorId;
        this.onSuccess    = onSuccess;

        // Charger les infos patient depuis la BDD
        chargerInfosPatient(consultation.getPatientId());

        // Remplir les labels d'info
        lblConsultationId.setText("#" + consultation.getId());
        lblPatientNom.setText(patientNom != null ? patientNom : "Patient #" + consultation.getPatientId());
        lblDateConsultation.setText(
            consultation.getConsultationDate() != null
                ? consultation.getConsultationDate().format(DATE_FMT)
                : "—"
        );

        // Pré-remplir le lien Meet si déjà défini
        if (consultation.getLienMeet() != null && !consultation.getLienMeet().isBlank()) {
            lienMeetField.setText(consultation.getLienMeet());
        }
    }

    // ================================================================== //
    //  Handler principal : clôturer la consultation                      //
    // ================================================================== //

    @FXML
    private void handleCloturer() {
        // ── 1. Validation ─────────────────────────────────────────────
        if (!valider()) return;

        double prix = Double.parseDouble(prixField.getText().trim());
        String medicaments     = medicamentsArea.getText().trim();
        String posologie       = posologieArea.getText().trim();
        String recommandations = recommandationsArea.getText().trim();
        String lienMeet        = lienMeetField.getText().trim();

        // Désactiver le bouton pendant le traitement
        btnCloturer.setDisable(true);
        btnCloturer.setText("⏳ Traitement en cours…");

        // ── 2-5. Traitement dans un Task (BDD + SMS réseau) ───────────
        //
        // On délègue à un thread background pour :
        //   - Ne pas bloquer le JavaFX Thread pendant les I/O BDD
        //   - L'appel Twilio (HTTP) est bloquant → doit être hors UI thread
        //
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return executerCloture(prix, medicaments, posologie,
                                       recommandations, lienMeet);
            }
        };

        task.setOnSucceeded(e -> {
            // Retour sur le JavaFX Thread (garanti par Task)
            btnCloturer.setDisable(false);
            btnCloturer.setText("✔  Envoyer l'ordonnance et clore");

            String numeroOrdonnance = task.getValue();

            // ── 6. Alert de confirmation ───────────────────────────────
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Consultation clôturée");
            alert.setHeaderText("✅ Ordonnance envoyée avec succès !");
            alert.setContentText(
                "Ordonnance N° " + numeroOrdonnance + " créée.\n" +
                "Consultation #" + consultation.getId() + " marquée comme terminée.\n" +
                "Prix : " + String.format("%.2f", prix) + " TND\n\n" +
                "Le patient a été notifié par notification et SMS."
            );
            alert.showAndWait();

            // ── 7. Fermer + callback ───────────────────────────────────
            fermerFenetre();
            if (onSuccess != null) {
                Platform.runLater(onSuccess);
            }
        });

        task.setOnFailed(e -> {
            btnCloturer.setDisable(false);
            btnCloturer.setText("✔  Envoyer l'ordonnance et clore");

            String cause = task.getException() != null
                ? task.getException().getMessage()
                : "Erreur inconnue";

            afficherErreurGlobal("✗ Erreur lors de la clôture : " + cause);
            System.err.println("[CloturerConsultationControllerArij] " + cause);
        });

        new Thread(task, "cloture-consultation").start();
    }

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    // ================================================================== //
    //  Logique métier (exécutée dans le thread background)               //
    // ================================================================== //

    /**
     * Exécute toutes les opérations de clôture dans l'ordre :
     *  1. Créer l'ordonnance avec accessToken UUID
     *  2. Mettre à jour consultation (statut + prix + lienMeet)
     *  3. Créer la facture
     *  4. Notifier le patient (BDD → visible Symfony)
     *  5. Envoyer le SMS Twilio
     *
     * @return numéro de l'ordonnance créée
     */
    private String executerCloture(double prix,
                                    String medicaments,
                                    String posologie,
                                    String recommandations,
                                    String lienMeet) {

        // ── Étape 1 : Créer l'ordonnance ──────────────────────────────
        OrdonnanceArij ordonnance = new OrdonnanceArij();
        ordonnance.setConsultationId(consultation.getId());
        ordonnance.setDoctorId(doctorId);

        // Contenu = médicaments + posologie concaténés
        ordonnance.setContent(medicaments);
        ordonnance.setDiagnosis(posologie);
        ordonnance.setInstructions(recommandations);

        // accessToken UUID — identifiant public pour le QR code
        ordonnance.setAccessToken(UUID.randomUUID().toString());

        // Date de validité : 3 mois par défaut
        ordonnance.setDateValidite(LocalDate.now().plusMonths(3).atStartOfDay());

        // Créer les lignes de médicaments (une ligne par médicament saisi)
        List<LigneOrdonnanceArij> lignes = parseMedicaments(medicaments, posologie);

        ordonnanceService.createOrdonnance(ordonnance, lignes);

        // Récupérer l'ordonnance sauvegardée pour avoir son id et numéro
        OrdonnanceArij saved = ordonnanceService.getByConsultationId(consultation.getId());
        int ordId = saved != null ? saved.getId() : 0;
        String numeroOrd = saved != null ? saved.getNumeroOrdonnance() : "N/A";

        // ── Étape 2 : Mettre à jour la consultation ───────────────────
        consultation.setStatus("TERMINEE");
        consultation.setConsultationFee(prix);
        if (!lienMeet.isBlank()) {
            consultation.setLienMeet(lienMeet);
        }
        consultation.setUpdatedAt(LocalDateTime.now());
        consultationService.updateConsultation(consultation);

        // Mettre à jour le prix dans la table consultations
        ordonnanceService.updateConsultationFee(consultation.getId(), prix);

        // ── Étape 3 : Créer la facture ────────────────────────────────
        if (ordId > 0) {
            factureService.createFactureForConsultation(
                consultation.getId(), consultation.getPatientId(), prix, ordId
            );
        }

        // ── Étape 4 : Notification BDD (visible Symfony immédiatement) ─
        if (patientUserId > 0) {
            String msgNotif = String.format(
                "Ordonnance prête (N° %s). Prix : %.2f TND. Procédez au paiement.",
                numeroOrd, prix
            );
            notifService.notifier(
                patientUserId,
                msgNotif,
                NotificationArij.TYPE_SUCCESS,
                null
            );
        }

        // ── Étape 5 : SMS Twilio ──────────────────────────────────────
        // Envoi non bloquant — on log l'échec sans faire échouer la clôture
        if (patientTelephone != null && !patientTelephone.isBlank()
                && !"-".equals(patientTelephone)) {
            String msgSms = String.format(
                "Votre ordonnance est disponible sur MediConsult. Prix : %.2f TND.",
                prix
            );
            boolean smsSent = smsService.envoyer(patientTelephone, msgSms);
            if (!smsSent) {
                System.err.println("[CloturerConsultationControllerArij] " +
                    "SMS non envoyé (Twilio) — clôture continue.");
            }
        } else {
            System.out.println("[CloturerConsultationControllerArij] " +
                "Pas de téléphone patient — SMS ignoré.");
        }

        return numeroOrd;
    }

    // ================================================================== //
    //  Validation                                                         //
    // ================================================================== //

    /**
     * Valide tous les champs du formulaire.
     * Affiche les messages d'erreur inline et retourne false si invalide.
     */
    private boolean valider() {
        cacherErreurs();
        boolean valid = true;

        // Médicaments obligatoires
        if (medicamentsArea.getText() == null || medicamentsArea.getText().trim().isEmpty()) {
            errMedicaments.setVisible(true);
            errMedicaments.setManaged(true);
            valid = false;
        }

        // Posologie obligatoire
        if (posologieArea.getText() == null || posologieArea.getText().trim().isEmpty()) {
            errPosologie.setVisible(true);
            errPosologie.setManaged(true);
            valid = false;
        }

        // Prix : nombre > 0 obligatoire
        try {
            double prix = Double.parseDouble(prixField.getText().trim());
            if (prix <= 0) throw new NumberFormatException("Prix doit être > 0");
        } catch (NumberFormatException ex) {
            errPrix.setVisible(true);
            errPrix.setManaged(true);
            valid = false;
        }

        return valid;
    }

    private void cacherErreurs() {
        errMedicaments.setVisible(false); errMedicaments.setManaged(false);
        errPosologie.setVisible(false);   errPosologie.setManaged(false);
        errPrix.setVisible(false);        errPrix.setManaged(false);
        errGlobal.setVisible(false);      errGlobal.setManaged(false);
    }

    private void afficherErreurGlobal(String msg) {
        Platform.runLater(() -> {
            errGlobal.setText(msg);
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        });
    }

    // ================================================================== //
    //  Helpers                                                            //
    // ================================================================== //

    /**
     * Parse le texte des médicaments (une ligne = un médicament)
     * et crée les LigneOrdonnanceArij correspondantes.
     */
    private List<LigneOrdonnanceArij> parseMedicaments(String medicaments,
                                                         String posologie) {
        List<LigneOrdonnanceArij> lignes = new java.util.ArrayList<>();
        if (medicaments == null || medicaments.isBlank()) return lignes;

        String[] lines = medicaments.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            LigneOrdonnanceArij l = new LigneOrdonnanceArij();
            l.setNomMedicament(trimmed);
            l.setDosage(posologie != null ? posologie.trim() : "");
            l.setQuantite(1);
            l.setDureeTraitement("");
            l.setInstructions("");
            lignes.add(l);
        }
        return lignes;
    }

    /**
     * Charge le nom, téléphone et userId du patient depuis la BDD.
     */
    private void chargerInfosPatient(int patientId) {
        String sql = """
            SELECT u.username, u.phone_number, u.id AS user_id
            FROM patients p
            JOIN users u ON u.id = p.user_id
            WHERE p.id = ?
            """;
        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.patientNom       = rs.getString("username");
                this.patientTelephone = rs.getString("phone_number");
                this.patientUserId    = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            System.err.println("[CloturerConsultationControllerArij] chargerInfosPatient: "
                + e.getMessage());
        }
    }

    private void fermerFenetre() {
        Platform.runLater(() -> {
            if (btnCloturer != null && btnCloturer.getScene() != null) {
                Stage stage = (Stage) btnCloturer.getScene().getWindow();
                if (stage != null) stage.close();
            }
        });
    }
}
