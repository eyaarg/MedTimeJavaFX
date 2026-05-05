package esprit.fx.controllers;

import esprit.fx.services.TriviaService;
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
public class QuizSanteController {

    @FXML private VBox reponsesBox;
    @FXML private Label questionLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label bonneReponseLabel;
    @FXML private Button btnSuivante;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label difficultyBadge;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox scorePanel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar scoreProgressBar;
    @FXML private Label messageLabel;
    @FXML private Button btnRejouer;

    private final TriviaService triviaService = new TriviaService();
    private List<TriviaService.QuizQuestion> questions;
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
        loadingIndicator.setVisible(true);
        questionLabel.setVisible(false);
        reponsesBox.setVisible(false);
        feedbackLabel.setVisible(false);
        bonneReponseLabel.setVisible(false);
        btnSuivante.setVisible(false);
        scorePanel.setVisible(false);

        // Tâche asynchrone pour récupérer les questions
        Task<List<TriviaService.QuizQuestion>> task = new Task<List<TriviaService.QuizQuestion>>() {
            @Override
            protected List<TriviaService.QuizQuestion> call() throws Exception {
                return triviaService.getQuestions();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                questions = task.getValue();
                loadingIndicator.setVisible(false);
                questionLabel.setVisible(true);
                reponsesBox.setVisible(true);
                feedbackLabel.setVisible(true);
                afficherQuestion(0);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                feedbackLabel.setText("❌ Erreur lors du chargement du quiz");
                feedbackLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
            });
        });

        new Thread(task).start();
    }

    /**
     * Affiche la question à l'index spécifié.
     * Crée dynamiquement les 4 boutons de réponse.
     */
    private void afficherQuestion(int index) {
        if (index >= questions.size()) {
            afficherScore();
            return;
        }

        TriviaService.QuizQuestion q = questions.get(index);
        reponseVerifiee = false;

        // Mettre à jour la barre de progression
        progressBar.setProgress((index + 1) / 5.0);
        progressLabel.setText("Question " + (index + 1) + "/5");

        // Mettre à jour le badge de difficulté
        String difficulte = q.difficulte;
        String badgeStyle = "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;";
        String badgeText = "Facile";

        if ("medium".equalsIgnoreCase(difficulte)) {
            badgeStyle = "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;";
            badgeText = "Moyen";
        } else if ("hard".equalsIgnoreCase(difficulte)) {
            badgeStyle = "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;";
            badgeText = "Difficile";
        }

        difficultyBadge.setText(badgeText);
        difficultyBadge.setStyle(badgeStyle);

        // Afficher la question avec animation
        questionLabel.setText(q.question);
        FadeTransition fadeQuestion = new FadeTransition(Duration.millis(400), questionLabel);
        fadeQuestion.setFromValue(0);
        fadeQuestion.setToValue(1);
        fadeQuestion.play();

        // Réinitialiser les labels de feedback
        feedbackLabel.setText("");
        bonneReponseLabel.setText("");
        btnSuivante.setVisible(false);

        // Créer les boutons de réponse
        reponsesBox.getChildren().clear();
        char[] lettres = {'A', 'B', 'C', 'D'};

        for (int i = 0; i < q.toutesLesReponses.size(); i++) {
            String reponse = q.toutesLesReponses.get(i);
            Button btnReponse = creerBoutonReponse(lettres[i], reponse, q.bonneReponse);
            reponsesBox.getChildren().add(btnReponse);
        }
    }

    /**
     * Crée un bouton de réponse stylisé.
     */
    private Button creerBoutonReponse(char lettre, String reponse, String bonneReponse) {
        Button btn = new Button();
        btn.setStyle(
                "-fx-background-color: #f8fafc; -fx-text-fill: #0f172a; -fx-font-size: 12px;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1;" +
                "-fx-padding: 12 16; -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;"
        );

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Label lblLettre = new Label(String.valueOf(lettre));
        lblLettre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3b82f6; -fx-min-width: 24;");

        Label lblReponse = new Label(reponse);
        lblReponse.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a; -fx-wrap-text: true;");
        lblReponse.setMaxWidth(300);

        content.getChildren().addAll(lblLettre, lblReponse);
        btn.setGraphic(content);
        btn.setPrefHeight(50);
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            if (!reponseVerifiee) {
                verifierReponse(reponse, btn, bonneReponse);
            }
        });

        return btn;
    }

    /**
     * Vérifie la réponse sélectionnée.
     * Désactive tous les boutons et affiche le feedback.
     */
    private void verifierReponse(String reponse, Button btnClique, String bonneReponse) {
        reponseVerifiee = true;

        // Désactiver tous les boutons
        for (javafx.scene.Node node : reponsesBox.getChildren()) {
            if (node instanceof Button) {
                ((Button) node).setDisable(true);
            }
        }

        boolean estCorrect = reponse.equals(bonneReponse);

        if (estCorrect) {
            // Bonne réponse
            score++;
            btnClique.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-size: 12px;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #6ee7b7; -fx-border-width: 2;" +
                    "-fx-padding: 12 16; -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;"
            );
            feedbackLabel.setText("✅ Correct!");
            feedbackLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-weight: bold;");
            bonneReponseLabel.setText("");

            // Animation du bouton correct
            ScaleTransition scale = new ScaleTransition(Duration.millis(300), btnClique);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.setCycleCount(2);
            scale.setAutoReverse(true);
            scale.play();

        } else {
            // Mauvaise réponse
            btnClique.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #7f1d1d; -fx-font-size: 12px;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #fca5a5; -fx-border-width: 2;" +
                    "-fx-padding: 12 16; -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;"
            );
            feedbackLabel.setText("❌ Incorrect");
            feedbackLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px; -fx-font-weight: bold;");
            bonneReponseLabel.setText("Bonne réponse: " + bonneReponse);
            bonneReponseLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px; -fx-font-weight: bold;");

            // Mettre en vert le bouton correct
            for (javafx.scene.Node node : reponsesBox.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    HBox content = (HBox) btn.getGraphic();
                    Label lblReponse = (Label) content.getChildren().get(1);
                    if (lblReponse.getText().equals(bonneReponse)) {
                        btn.setStyle(
                                "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-size: 12px;" +
                                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #6ee7b7; -fx-border-width: 2;" +
                                "-fx-padding: 12 16; -fx-cursor: hand; -fx-text-alignment: left; -fx-alignment: CENTER_LEFT;"
                        );
                    }
                }
            }
        }

        // Afficher le bouton "Suivante" après 1.5 secondes
        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> btnSuivante.setVisible(true));
        pause.play();
    }

    /**
     * Passe à la question suivante.
     */
    private void suivante() {
        questionCourante++;
        btnSuivante.setVisible(false);
        afficherQuestion(questionCourante);
    }

    /**
     * Affiche le score final avec animation.
     */
    private void afficherScore() {
        // Cacher la zone question
        questionLabel.setVisible(false);
        reponsesBox.setVisible(false);
        feedbackLabel.setVisible(false);
        bonneReponseLabel.setVisible(false);
        btnSuivante.setVisible(false);

        // Afficher le score panel
        scorePanel.setVisible(true);

        // Mettre à jour le label de score
        scoreLabel.setText("Vous avez " + score + "/5 bonnes réponses!");

        // Déterminer le message selon le score
        String message;
        if (score == 5) {
            message = "🏆 Parfait! Vous êtes un expert santé!";
        } else if (score >= 3) {
            message = "👍 Bien joué! Continuez à apprendre!";
        } else {
            message = "💪 Ne vous découragez pas, continuez!";
        }
        messageLabel.setText(message);

        // Animer la barre de progression du score
        scoreProgressBar.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scoreProgressBar.progressProperty(), 0)),
                new KeyFrame(Duration.millis(1000), new KeyValue(scoreProgressBar.progressProperty(), score / 5.0))
        );
        timeline.play();
    }
}
