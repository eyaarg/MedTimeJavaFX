package esprit.fx.controllers;

import esprit.fx.models.QuizQuestionArij;
import esprit.fx.services.TriviaServiceArij;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Contrôleur pour le système de Quiz Santé du dashboard patient.
 * Gère le chargement des questions, l'affichage, la vérification des réponses et le score.
 */
public class QuizSanteControllerArij {

    @FXML private VBox reponsesBox;
    @FXML private Label questionLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label bonneReponseLabel;
    @FXML private Button btnSuivante;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label difficultyBadge;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox loadingBox;
    @FXML private VBox scorePanel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar scoreProgressBar;
    @FXML private Label messageLabel;
    @FXML private Button btnRejouer;
    @FXML private VBox quizContent;
    @FXML private HBox feedbackBox;

    private final TriviaServiceArij triviaService = new TriviaServiceArij();
    private List<QuizQuestionArij> questions;
    private int questionCourante = 0;
    private int score = 0;
    private boolean reponseVerifiee = false;

    @FXML
    private void initialize() {
        chargerQuiz();
        btnSuivante.setOnAction(e -> suivante());
        btnRejouer.setOnAction(e -> chargerQuiz());
    }

    /**
     * Charge les questions du quiz depuis l'API.
     * Affiche un indicateur de chargement pendant la récupération.
     */
    private void chargerQuiz() {
        // Réinitialiser l'état
        questionCourante = 0;
        score = 0;
        reponseVerifiee = false;

        // Afficher le chargement
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        quizContent.setVisible(false);
        quizContent.setManaged(false);
        scorePanel.setVisible(false);
        scorePanel.setManaged(false);

        // Tâche asynchrone pour récupérer les questions
        Task<List<QuizQuestionArij>> task = new Task<List<QuizQuestionArij>>() {
            @Override
            protected List<QuizQuestionArij> call() throws Exception {
                return triviaService.getQuestions();
            }
        };

        task.setOnSucceeded(e -> {
            questions = task.getValue();
            Platform.runLater(() -> {
                loadingBox.setVisible(false);
                loadingBox.setManaged(false);
                quizContent.setVisible(true);
                quizContent.setManaged(true);
                afficherQuestion(0);
            });
        });

        task.setOnFailed(e -> {
            System.err.println("[QuizSanteControllerArij] Erreur chargement: " + task.getException().getMessage());
            Platform.runLater(() -> {
                loadingBox.setVisible(false);
                loadingBox.setManaged(false);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible de charger le quiz");
                alert.setContentText("Veuillez vérifier votre connexion Internet.");
                alert.showAndWait();
            });
        });

        new Thread(task, "quiz-loader").start();
    }

    /**
     * Affiche la question à l'index donné.
     * Met à jour la barre de progression et crée les boutons de réponse.
     */
    private void afficherQuestion(int index) {
        if (index >= questions.size()) {
            afficherScore();
            return;
        }

        QuizQuestionArij q = questions.get(index);
        reponseVerifiee = false;

        // Mettre à jour la progression
        progressBar.setProgress((index + 1) / 5.0);
        progressLabel.setText("Question " + (index + 1) + "/5");

        // Mettre à jour le badge de difficulté
        String difficulteText = q.difficulte.equals("easy") ? "Facile" :
                                q.difficulte.equals("medium") ? "Moyen" : "Difficile";
        String couleur = q.difficulte.equals("easy") ? "#4CAF50" :
                         q.difficulte.equals("medium") ? "#FF9800" : "#F44336";
        difficultyBadge.setText(difficulteText);
        difficultyBadge.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;-fx-padding:4 12 4 12;-fx-background-radius:20;-fx-font-size:11px;-fx-font-weight:bold;");

        // Animation de la question
        FadeTransition fade = new FadeTransition(Duration.millis(300), questionLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        questionLabel.setText(q.question);
        fade.play();

        // Créer les boutons de réponse
        reponsesBox.getChildren().clear();
        feedbackBox.setVisible(false);
        btnSuivante.setVisible(false);

        char[] lettres = {'A', 'B', 'C', 'D'};
        for (int i = 0; i < q.toutesLesReponses.size(); i++) {
            String reponse = q.toutesLesReponses.get(i);
            Button btn = creerBoutonReponse(lettres[i], reponse, q.bonneReponse);
            reponsesBox.getChildren().add(btn);
        }
    }

    /**
     * Crée un bouton de réponse stylisé.
     */
    private Button creerBoutonReponse(char lettre, String reponse, String bonneReponse) {
        // Badge lettre
        Label letterLabel = new Label(String.valueOf(lettre));
        letterLabel.setMinWidth(36);
        letterLabel.setMinHeight(36);
        letterLabel.setPrefWidth(36);
        letterLabel.setPrefHeight(36);
        letterLabel.setAlignment(Pos.CENTER);
        letterLabel.setStyle(
                "-fx-background-color:#1976d2;" +
                "-fx-background-radius:18;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:14px;" +
                "-fx-font-weight:bold;"
        );

        // Texte de la réponse
        Label textLabel = new Label(reponse);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#1a1a1a;");
        HBox.setHgrow(textLabel, Priority.ALWAYS);
        textLabel.setMaxWidth(Double.MAX_VALUE);

        // Conteneur HBox
        HBox row = new HBox(12, letterLabel, textLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(
                "-fx-background-color:white;" +
                "-fx-border-color:#d0d0d0;" +
                "-fx-border-width:1.5;" +
                "-fx-border-radius:8;" +
                "-fx-background-radius:8;"
        );

        // Bouton transparent qui enveloppe le HBox
        Button btn = new Button();
        btn.setGraphic(row);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;" +
                "-fx-padding:0;" +
                "-fx-cursor:hand;"
        );
        // Forcer le graphic à prendre toute la largeur
        btn.graphicProperty().addListener((obs, o, n) -> {});
        row.prefWidthProperty().bind(btn.widthProperty().subtract(2));

        btn.setOnAction(e -> {
            if (!reponseVerifiee) {
                verifierReponse(reponse, btn, row, bonneReponse);
            }
        });

        btn.setOnMouseEntered(e -> {
            if (!reponseVerifiee) {
                row.setStyle(
                        "-fx-background-color:#f0f4ff;" +
                        "-fx-border-color:#1976d2;" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;"
                );
            }
        });

        btn.setOnMouseExited(e -> {
            if (!reponseVerifiee) {
                row.setStyle(
                        "-fx-background-color:white;" +
                        "-fx-border-color:#d0d0d0;" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;"
                );
            }
        });

        return btn;
    }

    /**
     * Vérifie la réponse sélectionnée.
     */
    private void verifierReponse(String reponse, Button btn, HBox row, String bonneReponse) {
        reponseVerifiee = true;

        // Désactiver tous les boutons
        for (javafx.scene.Node node : reponsesBox.getChildren()) {
            if (node instanceof Button b) b.setDisable(true);
        }

        boolean estCorrect = reponse.equals(bonneReponse);

        if (estCorrect) {
            score++;
            row.setStyle(
                    "-fx-background-color:#e8f5e9;" +
                    "-fx-border-color:#4CAF50;" +
                    "-fx-border-width:2;" +
                    "-fx-border-radius:8;" +
                    "-fx-background-radius:8;"
            );
            feedbackLabel.setText("✅ Correct !");
            feedbackLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#2e7d32;");
            bonneReponseLabel.setVisible(false);
            bonneReponseLabel.setManaged(false);

        } else {
            row.setStyle(
                    "-fx-background-color:#ffebee;" +
                    "-fx-border-color:#F44336;" +
                    "-fx-border-width:2;" +
                    "-fx-border-radius:8;" +
                    "-fx-background-radius:8;"
            );
            feedbackLabel.setText("❌ Incorrect");
            feedbackLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#c62828;");
            bonneReponseLabel.setText("Bonne réponse : " + bonneReponse);
            bonneReponseLabel.setVisible(true);
            bonneReponseLabel.setManaged(true);

            // Mettre la bonne réponse en vert
            for (javafx.scene.Node node : reponsesBox.getChildren()) {
                if (node instanceof Button b && b.getGraphic() instanceof HBox h) {
                    if (h.getChildren().size() > 1 && h.getChildren().get(1) instanceof Label lbl) {
                        if (lbl.getText().equals(bonneReponse)) {
                            h.setStyle(
                                    "-fx-background-color:#e8f5e9;" +
                                    "-fx-border-color:#4CAF50;" +
                                    "-fx-border-width:2;" +
                                    "-fx-border-radius:8;" +
                                    "-fx-background-radius:8;"
                            );
                        }
                    }
                }
            }
        }

        feedbackBox.setVisible(true);
        feedbackBox.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> {
            btnSuivante.setVisible(true);
            btnSuivante.setManaged(true);
        });
        pause.play();
    }

    /**
     * Passe à la question suivante.
     */
    private void suivante() {
        questionCourante++;
        if (questionCourante < questions.size()) {
            afficherQuestion(questionCourante);
        } else {
            afficherScore();
        }
    }

    /**
     * Affiche l'écran de score final.
     */
    private void afficherScore() {
        quizContent.setVisible(false);
        quizContent.setManaged(false);
        scorePanel.setVisible(true);
        scorePanel.setManaged(true);

        // Mettre à jour le label de score
        scoreLabel.setText("Vous avez " + score + "/5 bonnes réponses !");

        // Animer la barre de progression du score
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scoreProgressBar.progressProperty(), 0)),
                new KeyFrame(Duration.millis(1000), new KeyValue(scoreProgressBar.progressProperty(), score / 5.0))
        );
        timeline.play();

        // Message personnalisé selon le score
        String message;
        if (score == 5) {
            message = "🏆 Parfait ! Vous êtes un expert santé !";
        } else if (score >= 3) {
            message = "👍 Bien joué ! Continuez à apprendre !";
        } else {
            message = "💪 Ne vous découragez pas, continuez !";
        }
        messageLabel.setText(message);
    }
}
