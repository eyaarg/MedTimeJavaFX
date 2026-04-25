package esprit.fx.controllers;

import esprit.fx.services.StatistiqueDAOArij;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ============================================================
 *  DashboardStatsControllerArij — Tableau de bord analytique
 * ============================================================
 *
 *  Affiche 4 graphiques JavaFX natifs + 4 cartes KPI :
 *
 *  ┌──────────────────────────────────────────────────────┐
 *  │  [KPI Total] [KPI Taux] [KPI Patients] [KPI Revenu] │
 *  ├──────────────────────────────────────────────────────┤
 *  │  BarChart consultations/mois │ PieChart spécialités  │
 *  ├──────────────────────────────────────────────────────┤
 *  │  LineChart revenu mensuel TND                        │
 *  └──────────────────────────────────────────────────────┘
 *
 *  Chargement :
 *  - Task<DonneesStats> dans un thread background
 *  - Platform.runLater() pour mettre à jour l'UI
 *  - Animations FadeTransition + ScaleTransition à l'apparition
 *
 *  Filtre :
 *  - ComboBox année → recharge toutes les données
 */
public class DashboardStatsControllerArij {

    // ------------------------------------------------------------------ //
    //  Noms des mois (Jan → Déc) pour les axes X                        //
    // ------------------------------------------------------------------ //
    private static final String[] MOIS = {
        "Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
        "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"
    };

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //

    // Filtre
    @FXML private ComboBox<Integer>          anneeCombo;

    // Indicateur de chargement
    @FXML private HBox                       loadingBox;
    @FXML private ProgressIndicator         loadingSpinner;

    // KPI cards
    @FXML private HBox                       kpiRow;
    @FXML private VBox                       kpiTotal;
    @FXML private VBox                       kpiTaux;
    @FXML private VBox                       kpiPatients;
    @FXML private VBox                       kpiRevenu;
    @FXML private Label                      lblTotalConsultations;
    @FXML private Label                      lblTotalSub;
    @FXML private Label                      lblTauxAcceptation;
    @FXML private Label                      lblPatientsActifs;
    @FXML private Label                      lblRevenuTotal;
    @FXML private Label                      lblRevenuSub;

    // Graphiques
    @FXML private BarChart<String, Number>   barConsultations;
    @FXML private CategoryAxis               barXAxis;
    @FXML private NumberAxis                 barYAxis;
    @FXML private PieChart                   pieSpecialites;
    @FXML private LineChart<String, Number>  lineRevenu;
    @FXML private Label                      lblRevenuAnnee;

    // ------------------------------------------------------------------ //
    //  Services                                                           //
    // ------------------------------------------------------------------ //
    private final StatistiqueDAOArij statsDAO = new StatistiqueDAOArij();

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // Remplir la ComboBox avec les 5 dernières années
        int anneeActuelle = LocalDate.now().getYear();
        ObservableList<Integer> annees = FXCollections.observableArrayList();
        for (int a = anneeActuelle; a >= anneeActuelle - 4; a--) {
            annees.add(a);
        }
        anneeCombo.setItems(annees);
        anneeCombo.setValue(anneeActuelle);

        // Masquer les graphiques au départ (seront animés à l'apparition)
        kpiRow.setOpacity(0);
        barConsultations.setOpacity(0);
        pieSpecialites.setOpacity(0);
        lineRevenu.setOpacity(0);

        // Configurer les axes du BarChart
        barXAxis.setCategories(FXCollections.observableArrayList(MOIS));
        barYAxis.setLabel("Consultations");
        barYAxis.setMinorTickVisible(false);

        // Premier chargement
        chargerDonnees(anneeActuelle);
    }

    // ================================================================== //
    //  Handler filtre année                                               //
    // ================================================================== //

    @FXML
    private void handleAnneeChange() {
        Integer annee = anneeCombo.getValue();
        if (annee != null) {
            chargerDonnees(annee);
        }
    }

    // ================================================================== //
    //  Chargement asynchrone des données                                 //
    // ================================================================== //

    /**
     * Lance le chargement des statistiques dans un thread background.
     *
     * POURQUOI un Task ?
     * Les 5 requêtes SQL peuvent prendre plusieurs centaines de ms.
     * Les exécuter sur le JavaFX Thread gèlerait l'interface.
     * Task garantit que setOnSucceeded() est rappelé sur le JavaFX Thread.
     *
     * @param annee année sélectionnée dans la ComboBox
     */
    private void chargerDonnees(int annee) {
        // Afficher le spinner, masquer les graphiques
        afficherChargement(true);

        // ── Task : toutes les requêtes BDD dans le thread background ──
        Task<DonneesStats> task = new Task<>() {
            @Override
            protected DonneesStats call() {
                DonneesStats d = new DonneesStats();
                d.annee                  = annee;
                d.consultationsParMois   = statsDAO.consultationsParMois(annee);
                d.tauxAcceptation        = statsDAO.tauxAcceptation();
                d.specialites            = statsDAO.specialitesLesPlusDemandees();
                d.revenuMensuel          = statsDAO.revenuMensuel(annee);
                d.patientsActifs         = statsDAO.patientsActifs();
                d.totalConsultations     = statsDAO.totalConsultations(annee);
                d.revenuTotal            = statsDAO.revenuTotal(annee);
                return d;
            }
        };

        // ── Succès : mise à jour UI sur le JavaFX Thread ──────────────
        task.setOnSucceeded(e -> {
            DonneesStats donnees = task.getValue();
            mettreAJourKpi(donnees);
            mettreAJourBarChart(donnees);
            mettreAJourPieChart(donnees);
            mettreAJourLineChart(donnees);
            afficherChargement(false);
            // Animer l'apparition des graphiques
            animer();
        });

        // ── Échec : log + masquer spinner ─────────────────────────────
        task.setOnFailed(e -> {
            System.err.println("[DashboardStatsControllerArij] Erreur chargement : "
                + task.getException().getMessage());
            afficherChargement(false);
        });

        new Thread(task, "dashboard-stats-loader").start();
    }

    // ================================================================== //
    //  Mise à jour des KPI                                               //
    // ================================================================== //

    /**
     * Met à jour les 4 cartes KPI.
     * Appelé sur le JavaFX Thread (depuis setOnSucceeded).
     */
    private void mettreAJourKpi(DonneesStats d) {
        // Total consultations
        lblTotalConsultations.setText(String.valueOf(d.totalConsultations));
        lblTotalSub.setText("en " + d.annee);

        // Taux d'acceptation
        lblTauxAcceptation.setText(String.format("%.1f%%", d.tauxAcceptation));

        // Patients actifs
        lblPatientsActifs.setText(String.valueOf(d.patientsActifs));

        // Revenu total
        lblRevenuTotal.setText(String.format("%.0f", d.revenuTotal));
        lblRevenuSub.setText("TND en " + d.annee);

        // Couleur du taux (vert si > 70%, orange sinon)
        String couleurTaux = d.tauxAcceptation >= 70 ? "#16a34a" : "#d97706";
        lblTauxAcceptation.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;"
            + " -fx-text-fill: " + couleurTaux + ";");
    }

    // ================================================================== //
    //  BarChart — consultations par mois                                 //
    // ================================================================== //

    /**
     * Remplit le BarChart avec les consultations par mois.
     *
     * Tous les 12 mois sont affichés (valeur 0 si aucune consultation).
     * Cela garantit un axe X complet Jan → Déc.
     */
    private void mettreAJourBarChart(DonneesStats d) {
        barConsultations.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations " + d.annee);

        // Remplir les 12 mois (0 si absent dans la Map)
        for (int mois = 1; mois <= 12; mois++) {
            long valeur = d.consultationsParMois.getOrDefault(mois, 0L);
            series.getData().add(new XYChart.Data<>(MOIS[mois - 1], valeur));
        }

        barConsultations.getData().add(series);

        // Appliquer le style bleu sur les barres après rendu
        Platform.runLater(() -> {
            barConsultations.lookupAll(".bar").forEach(node ->
                node.setStyle("-fx-bar-fill: #2563eb; -fx-background-radius: 4 4 0 0;")
            );
        });
    }

    // ================================================================== //
    //  PieChart — spécialités                                            //
    // ================================================================== //

    /**
     * Remplit le PieChart avec les 5 spécialités les plus demandées.
     *
     * Chaque tranche affiche : "Spécialité (N)"
     */
    private void mettreAJourPieChart(DonneesStats d) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        if (d.specialites.isEmpty()) {
            pieData.add(new PieChart.Data("Aucune donnée", 1));
        } else {
            for (Map.Entry<String, Long> entry : d.specialites.entrySet()) {
                String label = entry.getKey() + " (" + entry.getValue() + ")";
                pieData.add(new PieChart.Data(label, entry.getValue()));
            }
        }

        pieSpecialites.setData(pieData);
        pieSpecialites.setTitle("Top 5 — " + d.annee);
    }

    // ================================================================== //
    //  LineChart — revenu mensuel                                        //
    // ================================================================== //

    /**
     * Remplit le LineChart avec le revenu mensuel en TND.
     *
     * Tous les 12 mois sont affichés (0.0 si aucun revenu ce mois).
     * La courbe est lissée avec createSymbols=true pour les points.
     */
    private void mettreAJourLineChart(DonneesStats d) {
        lineRevenu.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenu " + d.annee);

        // Remplir les 12 mois
        for (int mois = 1; mois <= 12; mois++) {
            double valeur = d.revenuMensuel.getOrDefault(mois, 0.0);
            series.getData().add(new XYChart.Data<>(MOIS[mois - 1], valeur));
        }

        lineRevenu.getData().add(series);

        // Mettre à jour le label de revenu total
        lblRevenuAnnee.setText(String.format("Total : %.0f TND", d.revenuTotal));

        // Style de la courbe (vert)
        Platform.runLater(() -> {
            lineRevenu.lookupAll(".chart-series-line").forEach(node ->
                node.setStyle("-fx-stroke: #16a34a; -fx-stroke-width: 2.5px;")
            );
            lineRevenu.lookupAll(".chart-line-symbol").forEach(node ->
                node.setStyle("-fx-background-color: #16a34a, white;"
                    + " -fx-background-radius: 5px;"
                    + " -fx-padding: 5px;")
            );
        });
    }

    // ================================================================== //
    //  Animations                                                         //
    // ================================================================== //

    /**
     * Anime l'apparition des graphiques avec FadeTransition + ScaleTransition.
     *
     * Séquence :
     *  1. KPI cards  : fade in (0 → 1) en 400ms
     *  2. BarChart   : fade in + scale (0.95 → 1) en 500ms, délai 100ms
     *  3. PieChart   : fade in + scale en 500ms, délai 200ms
     *  4. LineChart  : fade in + scale en 500ms, délai 300ms
     */
    private void animer() {
        // KPI row
        animer(kpiRow, 0);

        // BarChart
        animer(barConsultations, 100);

        // PieChart
        animer(pieSpecialites, 200);

        // LineChart
        animer(lineRevenu, 300);
    }

    /**
     * Applique une animation fade + scale sur un nœud JavaFX.
     *
     * @param node   nœud à animer
     * @param delaiMs délai avant le début de l'animation (ms)
     */
    private void animer(javafx.scene.Node node, int delaiMs) {
        // FadeTransition : opacité 0 → 1
        FadeTransition fade = new FadeTransition(Duration.millis(500), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // ScaleTransition : légère mise à l'échelle 0.95 → 1.0
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), node);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // Jouer les deux en parallèle
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.setDelay(Duration.millis(delaiMs));
        parallel.play();
    }

    // ================================================================== //
    //  Utilitaires UI                                                     //
    // ================================================================== //

    /**
     * Affiche ou masque l'indicateur de chargement.
     *
     * Platform.runLater() : peut être appelé depuis n'importe quel thread.
     */
    private void afficherChargement(boolean visible) {
        Platform.runLater(() -> {
            loadingBox.setVisible(visible);
            loadingBox.setManaged(visible);
        });
    }

    // ================================================================== //
    //  DTO interne — données chargées par le Task                       //
    // ================================================================== //

    /**
     * Conteneur de toutes les données statistiques chargées en BDD.
     * Passé du thread background au JavaFX Thread via Task.getValue().
     */
    private static class DonneesStats {
        int                  annee;
        Map<Integer, Long>   consultationsParMois = new LinkedHashMap<>();
        double               tauxAcceptation;
        Map<String, Long>    specialites          = new LinkedHashMap<>();
        Map<Integer, Double> revenuMensuel        = new LinkedHashMap<>();
        long                 patientsActifs;
        long                 totalConsultations;
        double               revenuTotal;
    }
}
