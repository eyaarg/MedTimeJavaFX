package esprit.fx.controllers;

import esprit.fx.entities.ConsultationsArij;
import esprit.fx.entities.DisponibiliteMedecinArij;
import esprit.fx.services.CalendrierServiceArij;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour le calendrier des disponibilités médicales.
 * Affiche les créneaux libres et les consultations confirmées.
 */
public class CalendrierControllerArij {

    @FXML private VBox calendarContainer;
    @FXML private Label monthYearLabel;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private Button todayBtn;
    @FXML private GridPane calendarGrid;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label creneauxLibresLabel;
    @FXML private Label consultationsLabel;
    @FXML private Button addCreneauBtn;
    @FXML private VBox legendeBox;

    private final CalendrierServiceArij calendarService = new CalendrierServiceArij();
    private int medecinId;
    private YearMonth currentMonth;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        currentMonth = YearMonth.now();
        setupLegend();
        setupButtonHandlers();
    }

    /**
     * Configure la légende des couleurs.
     */
    private void setupLegend() {
        HBox legendeContent = new HBox(15);
        legendeContent.setPadding(new Insets(10));
        legendeContent.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");

        // Créneau libre (vert)
        HBox libre = createLegendItem(Color.web("#4CAF50"), "Créneau libre");
        
        // Consultation confirmée (bleu)
        HBox confirmee = createLegendItem(Color.web("#2196F3"), "Consultation confirmée");
        
        // Indisponible (rouge)
        HBox indispo = createLegendItem(Color.web("#F44336"), "Indisponible");

        legendeContent.getChildren().addAll(libre, confirmee, indispo);
        legendeBox.getChildren().add(legendeContent);
    }

    /**
     * Crée un élément de légende.
     */
    private HBox createLegendItem(Color color, String label) {
        HBox item = new HBox(8);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Rectangle rect = new Rectangle(15, 15);
        rect.setFill(color);
        rect.setStroke(Color.GRAY);
        rect.setStrokeWidth(1);
        
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12;");
        
        item.getChildren().addAll(rect, lbl);
        return item;
    }

    /**
     * Configure les gestionnaires de boutons.
     */
    private void setupButtonHandlers() {
        prevMonthBtn.setOnAction(e -> previousMonth());
        nextMonthBtn.setOnAction(e -> nextMonth());
        todayBtn.setOnAction(e -> goToToday());
        addCreneauBtn.setOnAction(e -> openAddCreneauDialog());
    }

    // Cache des données chargées pour éviter les requêtes SQL par cellule
    private List<DisponibiliteMedecinArij> cachedDisponibilites = new java.util.ArrayList<>();
    private List<ConsultationsArij> cachedConsultations = new java.util.ArrayList<>();

    /**
     * Charge le calendrier pour un médecin.
     */
    public void loadCalendar(int medecinId) {
        this.medecinId = medecinId;
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                // Charger les données UNE SEULE FOIS
                cachedDisponibilites = calendarService.getDisponibilitesByMedecin(medecinId);
                cachedConsultations  = calendarService.getConsultationsConfirmees(medecinId);
                int creneaux = (int) cachedDisponibilites.stream()
                        .filter(d -> !d.isEstOccupee()).count();

                Platform.runLater(() -> {
                    creneauxLibresLabel.setText(String.valueOf(creneaux));
                    consultationsLabel.setText(String.valueOf(cachedConsultations.size()));
                    refreshCalendarView();
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                });
                return null;
            }
        };

        new Thread(task, "calendar-loader").start();
    }

    /**
     * Rafraîchit l'affichage du calendrier.
     */
    private void refreshCalendarView() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // En-têtes des jours
        String[] dayNames = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setStyle("-fx-font-weight:bold;-fx-text-alignment:center;-fx-font-size:12;-fx-text-fill:#555;");
            dayLabel.setPrefWidth(90);
            dayLabel.setPrefHeight(32);
            dayLabel.setAlignment(javafx.geometry.Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);
        }

        // Jours du mois
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay  = currentMonth.atEndOfMonth();
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue(); // 1=Lun … 7=Dim

        int row = 1;
        int col = firstDayOfWeek - 1;

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            // Passer les données en cache — pas de requête SQL ici
            VBox dayCell = createDayCell(date, cachedDisponibilites, cachedConsultations);
            calendarGrid.add(dayCell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    /**
     * Crée une cellule de jour — utilise les données déjà chargées.
     */
    private VBox createDayCell(LocalDate date,
                                List<DisponibiliteMedecinArij> disponibilites,
                                List<ConsultationsArij> consultations) {
        VBox cell = new VBox(3);
        cell.setPrefWidth(90);
        cell.setPrefHeight(90);
        cell.setPadding(new Insets(5));

        boolean isToday = date.equals(LocalDate.now());
        cell.setStyle(
            "-fx-border-color:#e0e0e0;-fx-border-width:1;" +
            (isToday ? "-fx-background-color:#dbeafe;" : "-fx-background-color:white;")
        );

        // Numéro du jour
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle(
            "-fx-font-weight:bold;-fx-font-size:13;" +
            (isToday ? "-fx-text-fill:#1d4ed8;" : "-fx-text-fill:#333;")
        );

        VBox eventsBox = new VBox(2);

        // Créneaux libres pour ce jour
        for (DisponibiliteMedecinArij dispo : disponibilites) {
            if (dispo.getDateDebut() != null &&
                dispo.getDateDebut().toLocalDate().equals(date) &&
                !dispo.isEstOccupee()) {
                Label event = new Label("✓ " + dispo.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
                event.setStyle("-fx-text-fill:#16a34a;-fx-font-size:10;-fx-font-weight:bold;");
                eventsBox.getChildren().add(event);
            }
        }

        // Consultations confirmées pour ce jour
        for (ConsultationsArij c : consultations) {
            if (c.getConsultationDate() != null &&
                c.getConsultationDate().toLocalDate().equals(date)) {
                Label event = new Label("📅 " + c.getConsultationDate().format(DateTimeFormatter.ofPattern("HH:mm")));
                event.setStyle("-fx-text-fill:#1d4ed8;-fx-font-size:10;-fx-font-weight:bold;");
                eventsBox.getChildren().add(event);
            }
        }

        cell.getChildren().addAll(dayNum, eventsBox);
        return cell;
    }

    /**
     * Ouvre le dialogue pour ajouter un créneau.
     */
    private void openAddCreneauDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un créneau");
        dialog.setHeaderText("Créer un nouveau créneau de disponibilité");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Spinner<Integer> heureDebut = new Spinner<>(0, 23, 9);
        Spinner<Integer> minuteDebut = new Spinner<>(0, 59, 0, 15);
        Spinner<Integer> heureFin = new Spinner<>(0, 23, 10);
        Spinner<Integer> minuteFin = new Spinner<>(0, 59, 0, 15);
        TextField titreField = new TextField();
        titreField.setPromptText("Ex: Créneau libre");
        
        content.getChildren().addAll(
            new Label("Date:"), datePicker,
            new Label("Heure début:"), new HBox(5, heureDebut, new Label("h"), minuteDebut),
            new Label("Heure fin:"), new HBox(5, heureFin, new Label("h"), minuteFin),
            new Label("Titre:"), titreField
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            LocalDateTime debut = datePicker.getValue().atTime(heureDebut.getValue(), minuteDebut.getValue());
            LocalDateTime fin = datePicker.getValue().atTime(heureFin.getValue(), minuteFin.getValue());
            String titre = titreField.getText().isEmpty() ? "Créneau libre" : titreField.getText();
            
            if (calendarService.ajouterCreneau(medecinId, debut, fin, titre)) {
                showAlert("Succès", "Créneau ajouté avec succès", Alert.AlertType.INFORMATION);
                loadCalendar(medecinId);
            } else {
                showAlert("Erreur", "Impossible d'ajouter le créneau", Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Affiche une alerte.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        refreshCalendarView();
    }

    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        refreshCalendarView();
    }

    private void goToToday() {
        currentMonth = YearMonth.now();
        refreshCalendarView();
    }
}