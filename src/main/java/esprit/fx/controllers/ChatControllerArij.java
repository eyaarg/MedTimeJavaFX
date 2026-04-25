package esprit.fx.controllers;

import esprit.fx.services.GroqApiServiceArij;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatControllerArij {

    // ------------------------------------------------------------------ //
    //  Prompt système MediAssist                                          //
    // ------------------------------------------------------------------ //
    private static final String SYSTEM_PROMPT =
        "Tu es MediAssist, un assistant médical intelligent et bienveillant.\n\n" +
        "RÈGLE ABSOLUE : détecte la langue du patient dans son premier message et réponds " +
        "TOUJOURS dans cette même langue (français, arabe, anglais, etc.).\n\n" +
        "Ton rôle :\n" +
        "1. Écouter les symptômes avec empathie\n" +
        "2. Poser 1-2 questions ciblées pour mieux comprendre\n" +
        "3. Donner des conseils de santé généraux adaptés\n" +
        "4. Recommander le spécialiste exact (cardiologue, dermatologue, neurologue, " +
        "généraliste, etc.)\n" +
        "5. Si urgence : recommander immédiatement les urgences\n\n" +
        "Tu ne poses PAS de diagnostic définitif.\n" +
        "Ton ton : rassurant, professionnel, empathique.";

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //
    @FXML private VBox      messagesBox;
    @FXML private TextField messageInput;
    @FXML private TextArea  questionInput;
    @FXML private TextArea  aiResponse;
    @FXML private Label     loadingLabel;
    @FXML private ScrollPane chatScrollPane;

    // ------------------------------------------------------------------ //
    //  État interne                                                       //
    // ------------------------------------------------------------------ //
    /** Historique complet de la conversation (sans le message système). */
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();

    private final GroqApiServiceArij groqService = new GroqApiServiceArij();

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        addSystemMessage(
            "👋 Bonjour ! Je suis MediAssist, votre assistant médical.\n" +
            "Décrivez vos symptômes ou posez votre question — je vous réponds dans votre langue."
        );
    }

    // ------------------------------------------------------------------ //
    //  Handlers FXML — Chat multi-tour                                   //
    // ------------------------------------------------------------------ //
    @FXML
    private void handleSend() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        String userMsg = text.trim();
        messageInput.clear();

        // Afficher la bulle utilisateur
        addUserBubble(userMsg);

        // Ajouter au contexte de conversation
        conversationHistory.add(Map.of("role", "user", "content", userMsg));

        // Appel API en arrière-plan
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return groqService.chat(buildMessages());
            }
        };
        task.setOnSucceeded(e -> {
            String reply = task.getValue();
            // Mémoriser la réponse de l'assistant pour le contexte multi-tour
            conversationHistory.add(Map.of("role", "assistant", "content", reply));
            addAiBubble(reply);
        });
        task.setOnFailed(e ->
            addAiBubble("⚠️ Erreur : " + task.getException().getMessage())
        );
        new Thread(task, "mediassist-chat").start();
    }

    // ------------------------------------------------------------------ //
    //  Handlers FXML — Panneau IA (question ponctuelle)                  //
    // ------------------------------------------------------------------ //
    @FXML private void presetExplain()     { questionInput.setText("Expliquez mon diagnostic en termes simples."); }
    @FXML private void presetSideEffects() { questionInput.setText("Quels sont les effets secondaires courants de mes médicaments ?"); }
    @FXML private void presetNextSteps()   { questionInput.setText("Quelles sont les prochaines étapes après ma consultation ?"); }

    @FXML
    private void handleAskAi() {
        String q = questionInput.getText();
        if (q == null || q.trim().isEmpty()) return;
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        aiResponse.clear();

        // Question ponctuelle : pas d'historique, juste system + user
        List<Map<String, String>> msgs = List.of(
            Map.of("role", "user", "content", q.trim())
        );

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return groqService.chat(buildMessagesFor(msgs));
            }
        };
        task.setOnSucceeded(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            aiResponse.setText(stripMarkdown(task.getValue()));
        });
        task.setOnFailed(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            aiResponse.setText("⚠️ Erreur : " + task.getException().getMessage());
        });
        new Thread(task, "mediassist-ai").start();
    }

    // ------------------------------------------------------------------ //
    //  Construction des messages envoyés à l'API                         //
    // ------------------------------------------------------------------ //

    /**
     * Construit la liste complète : [system] + historique de conversation.
     */
    private List<Map<String, String>> buildMessages() {
        List<Map<String, String>> messages = new ArrayList<>();
        // Message système en tête
        Map<String, String> system = new HashMap<>();
        system.put("role",    "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);
        // Historique complet
        messages.addAll(conversationHistory);
        return messages;
    }

    /**
     * Construit la liste pour une question ponctuelle (panneau IA).
     */
    private List<Map<String, String>> buildMessagesFor(List<Map<String, String>> userMsgs) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role",    "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);
        messages.addAll(userMsgs);
        return messages;
    }

    // ------------------------------------------------------------------ //
    //  Rendu des bulles                                                   //
    // ------------------------------------------------------------------ //
    private void addSystemMessage(String text) {
        Platform.runLater(() -> {
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.setStyle(
                "-fx-background-color:#eff6ff;-fx-text-fill:#1d4ed8;" +
                "-fx-padding:10 14 10 14;-fx-background-radius:10;" +
                "-fx-font-size:12px;-fx-font-style:italic;"
            );
            lbl.setMaxWidth(Double.MAX_VALUE);
            messagesBox.getChildren().add(lbl);
        });
    }

    private void addUserBubble(String text) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label msg = new Label(text);
            msg.setWrapText(true);
            msg.setMaxWidth(380);
            msg.setStyle(
                "-fx-background-color:#2563eb;-fx-text-fill:white;" +
                "-fx-padding:10 14 10 14;-fx-background-radius:14 14 4 14;-fx-font-size:13px;"
            );
            Label ts = new Label(time);
            ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            VBox bubble = new VBox(3, msg, ts);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_RIGHT);
            HBox.setMargin(bubble, new Insets(0, 4, 0, 60));
            messagesBox.getChildren().add(row);
            scrollToBottom();
        });
    }

    private void addAiBubble(String text) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label msg = new Label(stripMarkdown(text));
            msg.setWrapText(true);
            msg.setMaxWidth(380);
            msg.setStyle(
                "-fx-background-color:#f1f5f9;-fx-text-fill:#0f172a;" +
                "-fx-padding:10 14 10 14;-fx-background-radius:14 14 14 4;-fx-font-size:13px;"
            );
            Label ts = new Label("🩺 MediAssist  •  " + time);
            ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            VBox bubble = new VBox(3, msg, ts);
            bubble.setAlignment(Pos.CENTER_LEFT);
            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(bubble, new Insets(0, 60, 0, 4));
            messagesBox.getChildren().add(row);
            scrollToBottom();
        });
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires                                                        //
    // ------------------------------------------------------------------ //
    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private String stripMarkdown(String text) {
        return text
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
            .replaceAll("\\*(.+?)\\*", "$1")
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("(?m)^\\* ", "• ")
            .replaceAll("(?m)^- ", "• ")
            .trim();
    }
}
