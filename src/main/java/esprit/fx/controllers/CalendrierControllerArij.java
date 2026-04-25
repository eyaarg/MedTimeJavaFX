package esprit.fx.controllers;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.Disponibilite;
import esprit.fx.services.ServiceConsultationsArij;
import esprit.fx.services.ServiceDisponibilite;
import esprit.fx.utils.MyDB;
import esprit.fx.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================
 *  CalendrierControllerArij — Calendrier des disponibilités
 * ============================================================
 *
 *  Affiche un CalendarView (CalendarFX) en vue mois avec :
 *  - Créneaux libres  (estDisponible=true)  → Calendar STYLE7 (vert)
 *  - Créneaux occupés (estDisponible=false) → Calendar STYLE2 (rouge)
 *  - Consultations acceptées                → Calendar STYLE1 (bleu)
 *
 *  Fonctionnalités :
 *  - ComboBox médecin pour filtrer
 *  - Clic sur créneau vert → formulaire nouvelle consultation pré-rempli
 *  - Rafraîchissement automatique toutes les 60 secondes (Timeline)
 *  - Rafraîchissement manuel via bouton
 */
public class CalendrierControllerArij {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //
    @FXML private StackPane  calendarContainer;
    @FXML private ComboBox<MedecinItem> medecinCombo;
    @FXML private Button     btnRefresh;
    @FXML private Label      lblRefreshStatus;

    // ------------------------------------------------------------------ //
    //  CalendarFX                                                         //
    // ------------------------------------------------------------------ //
    private CalendarView   calendarView;

    /** Calendrier vert — créneaux libres */
    private Calendar calDisponible;
    /** Calendrier rouge — créneaux occupés */
    private Calendar calOccupe;
    /** Calendrier bleu — consultations confirmées */
    private Calendar calConsultation;

    // ------------------------------------------------------------------ //
    //  Services & état                                                    //
    // ------------------------------------------------------------------ //
    private final ServiceDisponibilite    disponibiliteService = new ServiceDisponibilite();
    private final ServiceConsultationsArij consultationService = new ServiceConsultationsArij();

    private int    patientId      = 0;
    private int    selectedDoctorId = 0; // 0 = tous les médecins

    /** Timeline pour le rafraîchissement automatique toutes les 60s */
    private Timeline refreshTimeline;

    /** Compteur de secondes pour l'indicateur visuel */
    private int secondesRestantes = 60;
    private Timeline countdownTimeline;

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // Récupérer l'id du patient connecté
        resolvePatientId();

        // Initialiser CalendarFX
        initialiserCalendar();

        // Charger la liste des médecins dans la ComboBox
        chargerMedecins();

        // Premier chargement des données
        chargerDonnees();

        // Démarrer le rafraîchissement automatique toutes les 60 secondes
        demarrerTimeline();
    }

    // ================================================================== //
    //  Initialisation CalendarFX                                         //
    // ================================================================== //

    /**
     * Crée et configure le CalendarView avec les 3 calendriers colorés.
     * Injecte la vue dans le StackPane FXML.
     */
    private void initialiserCalendar() {
        calendarView = new CalendarView();

        // ── Calendrier VERT — créneaux disponibles ────────────────────
        // STYLE7 = vert dans CalendarFX
        calDisponible = new Calendar("Disponible");
        calDisponible.setStyle(Style.STYLE7);
        calDisponible.setReadOnly(true);

        // ── Calendrier ROUGE — créneaux occupés ───────────────────────
        // STYLE2 = rouge dans CalendarFX
        calOccupe = new Calendar("Occupé");
        calOccupe.setStyle(Style.STYLE2);
        calOccupe.setReadOnly(true);

        // ── Calendrier BLEU — consultations confirmées ────────────────
        // STYLE1 = bleu dans CalendarFX
        calConsultation = new Calendar("Consultation");
        calConsultation.setStyle(Style.STYLE1);
        calConsultation.setReadOnly(true);

        // Regrouper dans une source
        CalendarSource source = new CalendarSource("MedTime");
        source.getCalendars().addAll(calDisponible, calOccupe, calConsultation);
        calendarView.getCalendarSources().add(source);

        // ── Vue mois par défaut ────────────────────────────────────────
        calendarView.showMonthPage();

        // ── Désactiver les actions de création/édition ────────────────
        // Le patient ne peut pas créer d'entrées directement
        calendarView.setEntryFactory(param -> null);
        calendarView.setEntryDetailsCallback(param -> null);

        // ── Listener : clic sur une entrée ────────────────────────────
        // Quand le patient clique sur un créneau vert → ouvrir le formulaire
        calendarView.setEntryDetailsPopOverContentCallback(param -> {
            Entry<?> entry = param.getEntry();
            if (entry != null && entry.getCalendar() == calDisponible) {
                // Ouvrir le formulaire sur le JavaFX Thread
                Platform.runLater(() -> ouvrirFormulaireConsultation(entry));
            }
            return null; // Pas de popover par défaut
        });

        // Injecter dans le FXML
        calendarContainer.getChildren().setAll(calendarView);
    }

    // ================================================================== //
    //  Chargement des données                                             //
    // ================================================================== //

    /**
     * Charge les disponibilités et consultations depuis la BDD
     * dans un thread background (Task) pour ne pas bloquer l'UI.
     */
    private void chargerDonnees() {
        Task<DonneesCalendrier> task = new Task<>() {
            @Override
            protected DonneesCalendrier call() throws Exception {
                return chargerDepuisBdd(selectedDoctorId);
            }
        };

        task.setOnSucceeded(e -> {
            // Retour sur le JavaFX Thread — mise à jour des calendriers
            DonneesCalendrier donnees = task.getValue();
            mettreAJourCalendriers(donnees);
            lblRefreshStatus.setText("✓ Mis à jour à " +
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        task.setOnFailed(e -> {
            System.err.println("[CalendrierControllerArij] Erreur chargement : "
                + task.getException().getMessage());
            lblRefreshStatus.setText("⚠ Erreur de chargement");
        });

        new Thread(task, "calendrier-loader").start();
    }

    /**
     * Requête BDD : charge disponibilités + consultations.
     * Exécuté dans le thread background.
     */
    private DonneesCalendrier chargerDepuisBdd(int doctorId) throws SQLException {
        DonneesCalendrier donnees = new DonneesCalendrier();

        // ── Disponibilités ────────────────────────────────────────────
        String sqlDispo = doctorId > 0
            ? "SELECT a.*, u.username AS doctor_nom FROM availability a " +
              "LEFT JOIN users u ON u.id = a.doctor_id WHERE a.doctor_id = ?"
            : "SELECT a.*, u.username AS doctor_nom FROM availability a " +
              "LEFT JOIN users u ON u.id = a.doctor_id";

        try (PreparedStatement ps = MyDB.getInstance().getConnection()
                .prepareStatement(sqlDispo)) {
            if (doctorId > 0) ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                donnees.disponibilites.add(mapDisponibilite(rs));
            }
        }

        // ── Consultations confirmées ───────────────────────────────────
        String sqlConsult = doctorId > 0
            ? "SELECT c.*, u.username AS doctor_nom FROM consultations c " +
              "LEFT JOIN doctors d ON d.id = c.doctor_id " +
              "LEFT JOIN users u ON u.id = d.user_id " +
              "WHERE c.doctor_id = ? AND LOWER(c.status) IN ('confirmee','terminee') " +
              "AND c.is_deleted = 0"
            : "SELECT c.*, u.username AS doctor_nom FROM consultations c " +
              "LEFT JOIN doctors d ON d.id = c.doctor_id " +
              "LEFT JOIN users u ON u.id = d.user_id " +
              "WHERE LOWER(c.status) IN ('confirmee','terminee') AND c.is_deleted = 0";

        try (PreparedStatement ps = MyDB.getInstance().getConnection()
                .prepareStatement(sqlConsult)) {
            if (doctorId > 0) ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                donnees.consultations.add(mapConsultation(rs));
            }
        }

        return donnees;
    }

    /**
     * Met à jour les 3 calendriers CalendarFX avec les données chargées.
     * Doit être appelé sur le JavaFX Application Thread.
     */
    private void mettreAJourCalendriers(DonneesCalendrier donnees) {
        // Vider les calendriers existants
        calDisponible.clear();
        calOccupe.clear();
        calConsultation.clear();

        // ── Disponibilités ────────────────────────────────────────────
        for (DispoEntry de : donnees.disponibilites) {
            if (de.dateDebut == null || de.dateFin == null) continue;

            Entry<DispoEntry> entry = new Entry<>(
                de.estDisponible
                    ? "✅ Disponible" + (de.doctorNom != null ? " — " + de.doctorNom : "")
                    : "🔴 Occupé"    + (de.doctorNom != null ? " — " + de.doctorNom : "")
            );
            entry.setInterval(de.dateDebut, de.dateFin);
            entry.setUserObject(de);

            if (de.estDisponible) {
                calDisponible.addEntry(entry);
            } else {
                calOccupe.addEntry(entry);
            }
        }

        // ── Consultations ─────────────────────────────────────────────
        for (ConsultEntry ce : donnees.consultations) {
            if (ce.dateDebut == null) continue;

            Entry<ConsultEntry> entry = new Entry<>(
                "🩺 Consultation" + (ce.doctorNom != null ? " — Dr. " + ce.doctorNom : "")
            );
            // Durée par défaut : 1 heure
            entry.setInterval(ce.dateDebut, ce.dateDebut.plusHours(1));
            entry.setUserObject(ce);
            calConsultation.addEntry(entry);
        }
    }

    // ================================================================== //
    //  Handlers FXML                                                      //
    // ================================================================== //

    /** Filtre le calendrier par médecin sélectionné. */
    @FXML
    private void handleMedecinChange() {
        MedecinItem selected = medecinCombo.getValue();
        selectedDoctorId = (selected != null) ? selected.id() : 0;
        chargerDonnees();
    }

    /** Rafraîchissement manuel. */
    @FXML
    private void handleRefresh() {
        secondesRestantes = 60;
        chargerDonnees();
    }

    // ================================================================== //
    //  Formulaire de consultation                                         //
    // ================================================================== //

    /**
     * Ouvre le formulaire de nouvelle consultation avec la date pré-remplie.
     * Appelé quand le patient clique sur un créneau vert.
     *
     * @param entry entrée CalendarFX du créneau disponible
     */
    private void ouvrirFormulaireConsultation(Entry<?> entry) {
        if (entry == null) return;

        LocalDateTime dateDebut = entry.getStartAsLocalDateTime();
        Object userObj = entry.getUserObject();

        // Récupérer le doctorId depuis l'entrée
        int doctorId = 0;
        if (userObj instanceof DispoEntry de) {
            doctorId = de.doctorId;
        }

        final int finalDoctorId = doctorId;
        final LocalDateTime finalDate = dateDebut;

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/fxml/ConsultationFormArij.fxml")));
            Parent view = loader.load();

            ConsultationFormControllerArij ctrl = loader.getController();
            ctrl.setPatientId(patientId);

            // Pré-remplir la date et le médecin
            if (finalDate != null) {
                ctrl.preFillDateTime(finalDate);
            }
            if (finalDoctorId > 0) {
                ctrl.preFillDoctor(finalDoctorId);
            }

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Nouvelle consultation — " +
                (finalDate != null ? finalDate.format(FMT) : ""));
            modal.setScene(new Scene(view));
            modal.setOnHidden(e -> chargerDonnees()); // Rafraîchir après création
            modal.show();

        } catch (IOException e) {
            System.err.println("[CalendrierControllerArij] ouvrirFormulaireConsultation: "
                + e.getMessage());
            // Fallback : afficher une alerte avec la date
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Créneau sélectionné");
            alert.setHeaderText("Créneau disponible");
            alert.setContentText("Date : " + (finalDate != null ? finalDate.format(FMT) : "—") +
                "\nVeuillez utiliser le formulaire de consultation.");
            alert.showAndWait();
        }
    }

    // ================================================================== //
    //  Timeline — rafraîchissement automatique                           //
    // ================================================================== //

    /**
     * Démarre la Timeline de rafraîchissement automatique (60 secondes).
     *
     * Utilise deux Timelines :
     * - refreshTimeline  : déclenche chargerDonnees() toutes les 60s
     * - countdownTimeline : met à jour le label de compte à rebours chaque seconde
     */
    private void demarrerTimeline() {
        // ── Timeline principale : rafraîchissement toutes les 60s ─────
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(60), e -> {
                chargerDonnees();
                secondesRestantes = 60;
            })
        );
        refreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        refreshTimeline.play();

        // ── Timeline secondaire : compte à rebours visuel ─────────────
        countdownTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                secondesRestantes--;
                if (secondesRestantes <= 0) secondesRestantes = 60;
                lblRefreshStatus.setText("⏱ Prochain rafraîchissement dans " +
                    secondesRestantes + "s");
            })
        );
        countdownTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimeline.play();
    }

    /** Arrête les Timelines proprement (à appeler au logout). */
    public void arreterTimelines() {
        if (refreshTimeline  != null) refreshTimeline.stop();
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    // ================================================================== //
    //  Chargement de la ComboBox médecins                                //
    // ================================================================== //

    private void chargerMedecins() {
        List<MedecinItem> medecins = new ArrayList<>();
        medecins.add(new MedecinItem(0, "Tous les médecins"));

        String sql = """
            SELECT d.id, COALESCE(u.username, CONCAT('Médecin #', d.id)) AS nom
            FROM doctors d
            LEFT JOIN users u ON u.id = d.user_id
            ORDER BY nom
            """;

        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                medecins.add(new MedecinItem(rs.getInt("id"), rs.getString("nom")));
            }
        } catch (SQLException e) {
            System.err.println("[CalendrierControllerArij] chargerMedecins: " + e.getMessage());
        }

        medecinCombo.setItems(FXCollections.observableArrayList(medecins));
        medecinCombo.setConverter(new StringConverter<>() {
            @Override public String toString(MedecinItem m)    { return m == null ? "" : m.nom(); }
            @Override public MedecinItem fromString(String s)  { return null; }
        });
        medecinCombo.getSelectionModel().selectFirst();
    }

    // ================================================================== //
    //  Résolution du patientId                                           //
    // ================================================================== //

    private void resolvePatientId() {
        if (UserSession.getCurrentUser() == null) return;
        int userId = UserSession.getCurrentUser().getId();
        String sql = "SELECT id FROM patients WHERE user_id = ? LIMIT 1";
        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) patientId = rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("[CalendrierControllerArij] resolvePatientId: " + e.getMessage());
        }
    }

    // ================================================================== //
    //  Mapping ResultSet → DTOs internes                                 //
    // ================================================================== //

    private DispoEntry mapDisponibilite(ResultSet rs) throws SQLException {
        DispoEntry de = new DispoEntry();
        de.id       = rs.getInt("id");
        de.doctorId = rs.getInt("doctor_id");
        de.doctorNom = rs.getString("doctor_nom");

        // Combiner start_date + start_time
        try {
            java.sql.Date sd = rs.getDate("start_date");
            java.sql.Time st = rs.getTime("start_time");
            if (sd != null && st != null)
                de.dateDebut = LocalDateTime.of(sd.toLocalDate(), st.toLocalTime());
        } catch (SQLException ignored) {}

        try {
            java.sql.Date ed = rs.getDate("end_date");
            java.sql.Time et = rs.getTime("end_time");
            if (ed != null && et != null)
                de.dateFin = LocalDateTime.of(ed.toLocalDate(), et.toLocalTime());
        } catch (SQLException ignored) {}

        // is_online = true → occupé ; is_online = false → disponible
        try { de.estDisponible = !rs.getBoolean("is_online"); }
        catch (SQLException ignored) { de.estDisponible = true; }

        return de;
    }

    private ConsultEntry mapConsultation(ResultSet rs) throws SQLException {
        ConsultEntry ce = new ConsultEntry();
        ce.id        = rs.getInt("id");
        ce.doctorNom = rs.getString("doctor_nom");
        java.sql.Timestamp ts = rs.getTimestamp("consultation_date");
        if (ts != null) ce.dateDebut = ts.toLocalDateTime();
        return ce;
    }

    // ================================================================== //
    //  DTOs internes                                                      //
    // ================================================================== //

    private static class DonneesCalendrier {
        final List<DispoEntry>   disponibilites = new ArrayList<>();
        final List<ConsultEntry> consultations  = new ArrayList<>();
    }

    private static class DispoEntry {
        int           id;
        int           doctorId;
        String        doctorNom;
        LocalDateTime dateDebut;
        LocalDateTime dateFin;
        boolean       estDisponible;
    }

    private static class ConsultEntry {
        int           id;
        String        doctorNom;
        LocalDateTime dateDebut;
    }

    /** Item de la ComboBox médecins. */
    private record MedecinItem(int id, String nom) {
        @Override public String toString() { return nom; }
    }
}
