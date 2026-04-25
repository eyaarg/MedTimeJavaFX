package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.services.ConsultationFiltreDAOArij;
import esprit.fx.utils.MyDB;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  ConsultationFiltreControllerArij — Filtrage avancé
 * ============================================================
 *
 *  Architecture :
 *  ──────────────
 *  1. Chargement initial BDD → ObservableList<ConsultationsArij>
 *  2. FilteredList wrappant l'ObservableList → filtrage réactif
 *     sans nouvelle requête BDD à chaque frappe
 *  3. Chaque changement de filtre → updatePredicate()
 *     → FilteredList se met à jour automatiquement
 *  4. Label "X résultats" lié à filteredList.size()
 *
 *  Filtres disponibles :
 *  ─────────────────────
 *  - DatePicker dateDebut / dateFin
 *  - ComboBox statut (Tous / EN_ATTENTE / CONFIRMEE / REFUSEE / TERMINEE / PAYEE)
 *  - ComboBox médecin
 *  - TextField recherche (nom patient, insensible à la casse)
 *  - Bouton "Réinitialiser" → vide tous les filtres
 */
public class ConsultationFiltreControllerArij {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //

    // Filtres
    @FXML private DatePicker                    dateDebutPicker;
    @FXML private DatePicker                    dateFinPicker;
    @FXML private ComboBox<String>              statutCombo;
    @FXML private ComboBox<MedecinItem>         medecinCombo;
    @FXML private TextField                     rechercheField;
    @FXML private Button                        btnReinitialiser;
    @FXML private Label                         lblResultats;
    @FXML private Label                         lblErreurDate;

    // TableView
    @FXML private TableView<ConsultationsArij>  tableConsultations;
    @FXML private TableColumn<ConsultationsArij, String> colPatient;
    @FXML private TableColumn<ConsultationsArij, String> colMedecin;
    @FXML private TableColumn<ConsultationsArij, String> colDate;
    @FXML private TableColumn<ConsultationsArij, String> colStatut;
    @FXML private TableColumn<ConsultationsArij, String> colPrix;
    @FXML private TableColumn<ConsultationsArij, Void>   colActions;

    // ------------------------------------------------------------------ //
    //  Données                                                            //
    // ------------------------------------------------------------------ //
    private final ConsultationFiltreDAOArij filtreDAO = new ConsultationFiltreDAOArij();

    /** Liste source — chargée une fois depuis la BDD. */
    private final ObservableList<ConsultationsArij> sourceList =
        FXCollections.observableArrayList();

    /**
     * FilteredList wrappant sourceList.
     * Le prédicat est mis à jour à chaque changement de filtre.
     * Aucune requête BDD supplémentaire n'est nécessaire.
     */
    private FilteredList<ConsultationsArij> filteredList;

    /** Cache : patientId → nom patient */
    private final Map<Integer, String> patientNomById  = new HashMap<>();
    /** Cache : doctorId  → nom médecin */
    private final Map<Integer, String> medecinNomById  = new HashMap<>();

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // 1. Charger les caches de noms
        chargerCaches();

        // 2. Configurer les colonnes de la TableView
        configurerColonnes();

        // 3. Configurer les filtres (ComboBox, listeners)
        configurerFiltres();

        // 4. Charger toutes les consultations depuis la BDD (Task)
        chargerToutesConsultations();
    }

    // ================================================================== //
    //  Configuration des colonnes                                        //
    // ================================================================== //

    private void configurerColonnes() {
        // Colonne Patient : lookup dans le cache
        colPatient.setCellValueFactory(cell ->
            new SimpleStringProperty(
                patientNomById.getOrDefault(cell.getValue().getPatientId(),
                    "Patient #" + cell.getValue().getPatientId())
            )
        );

        // Colonne Médecin : lookup dans le cache
        colMedecin.setCellValueFactory(cell ->
            new SimpleStringProperty(
                medecinNomById.getOrDefault(cell.getValue().getDoctorId(),
                    "Médecin #" + cell.getValue().getDoctorId())
            )
        );

        // Colonne Date
        colDate.setCellValueFactory(cell -> {
            LocalDateTime dt = cell.getValue().getConsultationDate();
            return new SimpleStringProperty(dt != null ? dt.format(FMT) : "—");
        });

        // Colonne Statut — avec badge coloré
        colStatut.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getStatus())
        );
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = badgeStatut(status);
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        // Colonne Prix
        colPrix.setCellValueFactory(cell -> {
            double fee = cell.getValue().getConsultationFee();
            return new SimpleStringProperty(fee > 0 ? String.format("%.2f", fee) : "—");
        });
        colPrix.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Colonne Actions — bouton "Voir"
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button("👁 Voir");
            {
                btnVoir.setStyle(
                    "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 7; -fx-border-radius: 7;" +
                    "-fx-border-color: #bfdbfe; -fx-padding: 5 10; -fx-cursor: hand;"
                );
                btnVoir.setOnAction(e -> {
                    ConsultationsArij c = getTableView().getItems().get(getIndex());
                    afficherDetail(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnVoir);
                setAlignment(Pos.CENTER);
            }
        });
    }

    // ================================================================== //
    //  Configuration des filtres                                         //
    // ================================================================== //

    private void configurerFiltres() {
        // ── ComboBox statut ───────────────────────────────────────────
        statutCombo.setItems(FXCollections.observableArrayList(
            "Tous", "EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE", "PAYEE"
        ));
        statutCombo.setValue("Tous");

        // ── ComboBox médecin ──────────────────────────────────────────
        List<MedecinItem> medecins = new ArrayList<>();
        medecins.add(new MedecinItem(0, "Tous les médecins"));
        medecinNomById.forEach((id, nom) -> medecins.add(new MedecinItem(id, nom)));
        medecinCombo.setItems(FXCollections.observableArrayList(medecins));
        medecinCombo.setConverter(new StringConverter<>() {
            @Override public String toString(MedecinItem m)   { return m == null ? "" : m.nom(); }
            @Override public MedecinItem fromString(String s) { return null; }
        });
        medecinCombo.getSelectionModel().selectFirst();

        // ── Listeners réactifs ────────────────────────────────────────
        // Chaque changement déclenche updatePredicate() → FilteredList se met à jour
        dateDebutPicker.valueProperty().addListener((obs, o, n) -> updatePredicate());
        dateFinPicker.valueProperty().addListener((obs, o, n)   -> updatePredicate());
        statutCombo.valueProperty().addListener((obs, o, n)     -> updatePredicate());
        medecinCombo.valueProperty().addListener((obs, o, n)    -> updatePredicate());

        // TextField : filtrage réactif à chaque frappe (pas besoin d'appuyer Entrée)
        rechercheField.textProperty().addListener((obs, o, n)   -> updatePredicate());
    }

    // ================================================================== //
    //  Chargement initial depuis la BDD                                  //
    // ================================================================== //

    /**
     * Charge toutes les consultations dans un Task background.
     * Initialise la FilteredList après chargement.
     */
    private void chargerToutesConsultations() {
        Task<List<ConsultationsArij>> task = new Task<>() {
            @Override
            protected List<ConsultationsArij> call() {
                // Chargement initial sans filtre (tous les critères null)
                return filtreDAO.filtrer(null, null, null, null, null);
            }
        };

        task.setOnSucceeded(e -> {
            sourceList.setAll(task.getValue());

            // Créer la FilteredList wrappant sourceList
            filteredList = new FilteredList<>(sourceList, c -> true);

            // Lier la TableView à la FilteredList
            tableConsultations.setItems(filteredList);

            // Lier le label "X résultats" à la taille de la FilteredList
            mettreAJourLabelResultats();

            // Appliquer les filtres initiaux (tous vides → tout afficher)
            updatePredicate();
        });

        task.setOnFailed(e ->
            System.err.println("[ConsultationFiltreControllerArij] Erreur chargement : "
                + task.getException().getMessage())
        );

        new Thread(task, "filtre-loader").start();
    }

    // ================================================================== //
    //  Mise à jour du prédicat FilteredList                             //
    // ================================================================== //

    /**
     * Construit et applique le prédicat de filtrage sur la FilteredList.
     *
     * Appelé à chaque changement de filtre (listener réactif).
     * Aucune requête BDD — le filtrage se fait en mémoire sur sourceList.
     *
     * Chaque condition n'est appliquée que si la valeur est non-null/non-vide,
     * exactement comme le StringBuilder HQL demandé.
     */
    private void updatePredicate() {
        if (filteredList == null) return;

        // Lire les valeurs courantes des filtres
        LocalDate debut    = dateDebutPicker.getValue();
        LocalDate fin      = dateFinPicker.getValue();
        String statut      = statutCombo.getValue();
        MedecinItem medecin = medecinCombo.getValue();
        String motCle      = rechercheField.getText();

        // Validation : dateDebut <= dateFin
        if (debut != null && fin != null && debut.isAfter(fin)) {
            lblErreurDate.setText("⚠ La date de début doit être avant la date de fin.");
            lblErreurDate.setVisible(true);
            lblErreurDate.setManaged(true);
        } else {
            lblErreurDate.setVisible(false);
            lblErreurDate.setManaged(false);
        }

        // Construire le prédicat dynamiquement
        filteredList.setPredicate(c -> {
            LocalDateTime dateConsult = c.getConsultationDate();

            // ── Condition 1 : date de début ───────────────────────────
            if (debut != null && dateConsult != null) {
                if (dateConsult.toLocalDate().isBefore(debut)) return false;
            }

            // ── Condition 2 : date de fin ─────────────────────────────
            if (fin != null && dateConsult != null) {
                if (dateConsult.toLocalDate().isAfter(fin)) return false;
            }

            // ── Condition 3 : statut ──────────────────────────────────
            if (statut != null && !"Tous".equalsIgnoreCase(statut)) {
                if (!statut.equalsIgnoreCase(c.getStatus())) return false;
            }

            // ── Condition 4 : médecin ─────────────────────────────────
            if (medecin != null && medecin.id() > 0) {
                if (c.getDoctorId() != medecin.id()) return false;
            }

            // ── Condition 5 : mot-clé (nom patient) ──────────────────
            if (motCle != null && !motCle.isBlank()) {
                String nomPatient = patientNomById.getOrDefault(c.getPatientId(), "")
                    .toLowerCase();
                if (!nomPatient.contains(motCle.trim().toLowerCase())) return false;
            }

            return true; // Toutes les conditions passées
        });

        mettreAJourLabelResultats();
    }

    // ================================================================== //
    //  Handler : Réinitialiser                                           //
    // ================================================================== //

    /**
     * Vide tous les filtres et réaffiche toutes les consultations.
     */
    @FXML
    private void handleReinitialiser() {
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        statutCombo.setValue("Tous");
        medecinCombo.getSelectionModel().selectFirst();
        rechercheField.clear();
        lblErreurDate.setVisible(false);
        lblErreurDate.setManaged(false);
        // updatePredicate() est appelé automatiquement par les listeners
    }

    /**
     * Handler pour les ComboBox et DatePicker (onAction dans le FXML).
     * Les TextField utilisent un listener textProperty, pas ce handler.
     */
    @FXML
    private void handleFiltreChange() {
        updatePredicate();
    }

    // ================================================================== //
    //  Label résultats                                                   //
    // ================================================================== //

    private void mettreAJourLabelResultats() {
        if (filteredList == null) {
            lblResultats.setText("0 résultat(s)");
            return;
        }
        int nb = filteredList.size();
        lblResultats.setText(nb + " résultat" + (nb > 1 ? "s" : "") + " trouvé" + (nb > 1 ? "s" : ""));
    }

    // ================================================================== //
    //  Détail d'une consultation                                         //
    // ================================================================== //

    private void afficherDetail(ConsultationsArij c) {
        String nomPatient = patientNomById.getOrDefault(c.getPatientId(),
            "Patient #" + c.getPatientId());
        String nomMedecin = medecinNomById.getOrDefault(c.getDoctorId(),
            "Médecin #" + c.getDoctorId());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la consultation");
        alert.setHeaderText("Consultation #" + c.getId());
        alert.setContentText(
            "Patient  : " + nomPatient + "\n" +
            "Médecin  : " + nomMedecin + "\n" +
            "Date     : " + (c.getConsultationDate() != null
                ? c.getConsultationDate().format(FMT) : "—") + "\n" +
            "Statut   : " + prettyStatut(c.getStatus()) + "\n" +
            "Prix     : " + (c.getConsultationFee() > 0
                ? String.format("%.2f TND", c.getConsultationFee()) : "—")
        );
        alert.showAndWait();
    }

    // ================================================================== //
    //  Chargement des caches                                             //
    // ================================================================== //

    private void chargerCaches() {
        // Cache patients
        String sqlP = """
            SELECT p.id, COALESCE(u.username, CONCAT('Patient #', p.id)) AS nom
            FROM patients p LEFT JOIN users u ON u.id = p.user_id
            """;
        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sqlP);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) patientNomById.put(rs.getInt("id"), rs.getString("nom"));
        } catch (SQLException e) {
            System.err.println("[ConsultationFiltreControllerArij] chargerCaches patients: "
                + e.getMessage());
        }

        // Cache médecins
        String sqlD = """
            SELECT d.id, COALESCE(u.username, CONCAT('Médecin #', d.id)) AS nom
            FROM doctors d LEFT JOIN users u ON u.id = d.user_id
            """;
        try (PreparedStatement ps =
                 MyDB.getInstance().getConnection().prepareStatement(sqlD);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) medecinNomById.put(rs.getInt("id"), rs.getString("nom"));
        } catch (SQLException e) {
            System.err.println("[ConsultationFiltreControllerArij] chargerCaches medecins: "
                + e.getMessage());
        }
    }

    // ================================================================== //
    //  Helpers UI                                                        //
    // ================================================================== //

    private Label badgeStatut(String status) {
        String s = status == null ? "" : status.toUpperCase();
        String text; String bg; String fg;
        switch (s) {
            case "EN_ATTENTE" -> { text = "En attente"; bg = "#fff7ed"; fg = "#9a3412"; }
            case "CONFIRMEE"  -> { text = "Confirmée";  bg = "#f0fdf4"; fg = "#166534"; }
            case "REFUSEE"    -> { text = "Refusée";    bg = "#fff1f2"; fg = "#be123c"; }
            case "TERMINEE"   -> { text = "Terminée";   bg = "#eff6ff"; fg = "#1d4ed8"; }
            case "PAYEE"      -> { text = "💳 Payée";   bg = "#f0fdf4"; fg = "#15803d"; }
            default           -> { text = s.isBlank() ? "—" : s; bg = "#f1f5f9"; fg = "#475569"; }
        }
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
            "; -fx-font-size:11px; -fx-font-weight:bold;" +
            " -fx-padding:3 10 3 10; -fx-background-radius:999;");
        return l;
    }

    private String prettyStatut(String s) {
        if (s == null) return "—";
        return switch (s.toUpperCase()) {
            case "EN_ATTENTE" -> "⏳ En attente";
            case "CONFIRMEE"  -> "✅ Confirmée";
            case "REFUSEE"    -> "❌ Refusée";
            case "TERMINEE"   -> "✔ Terminée";
            case "PAYEE"      -> "💳 Payée";
            default -> s;
        };
    }

    // ================================================================== //
    //  Record interne — item de la ComboBox médecins                    //
    // ================================================================== //
    private record MedecinItem(int id, String nom) {
        @Override public String toString() { return nom; }
    }
}
