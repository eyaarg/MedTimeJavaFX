package esprit.fx.controllers;

import esprit.fx.entities.ChatMessageArij;
import esprit.fx.entities.ChatMessageArij.Role;
import esprit.fx.entities.ChatSessionArij;
import esprit.fx.services.GroqApiServiceArij;
import esprit.fx.services.ServiceChatArij;
import esprit.fx.utils.UserSession;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur du chatbot MediAssist avec historique de sessions persisté en BDD.
 */
public class ChatbotControllerArij {

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

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm");

    // ------------------------------------------------------------------ //
    //  FXML bindings                                                      //
    // ------------------------------------------------------------------ //
    @FXML private ListView<ChatSessionArij> sessionListView;
    @FXML private VBox                      messagesBox;
    @FXML private ScrollPane               chatScrollPane;
    @FXML private TextField                messageInput;
    @FXML private Button                   sendButton;
    @FXML private Button                   newSessionButton;
    @FXML private Button                   deleteSessionButton;
    @FXML private Label                    sessionTitleLabel;
    @FXML private Label                    typingIndicator;

    // ------------------------------------------------------------------ //
    //  Services & état                                                    //
    // ------------------------------------------------------------------ //
    private final ServiceChatArij    chatService  = new ServiceChatArij();
    private final GroqApiServiceArij groqService  = new GroqApiServiceArij();

    private final ObservableList<ChatSessionArij> sessions = FXCollections.observableArrayList();
    private ChatSessionArij currentSession = null;
    private int             patientId      = 0;

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // Récupérer l'id du patient connecté
        if (UserSession.getCurrentUser() != null) {
            patientId = UserSession.getCurrentUser().getId();
        }

        // Configurer la ListView
        sessionListView.setItems(sessions);
        sessionListView.setPlaceholder(new Label("Aucune session"));
        sessionListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, selected) -> {
                if (selected != null) loadSession(selected);
            });

        // Désactiver les contrôles tant qu'aucune session n'est sélectionnée
        setInputEnabled(false);
        deleteSessionButton.setDisable(true);

        // Charger les sessions existantes
        loadSessions();

        // Envoyer avec Entrée
        messageInput.setOnAction(e -> handleSend());
    }

    // ------------------------------------------------------------------ //
    //  Chargement des sessions                                            //
    // ------------------------------------------------------------------ //
    private void loadSessions() {
        sessions.clear();
        sessions.addAll(chatService.getSessionsByPatient(patientId));
        if (!sessions.isEmpty()) {
            sessionListView.getSelectionModel().selectFirst();
        }
    }

    /** Charge les messages d'une session et les affiche. */
    private void loadSession(ChatSessionArij session) {
        currentSession = session;
        sessionTitleLabel.setText(session.getTitle() != null ? session.getTitle() : "Session #" + session.getId());
        deleteSessionButton.setDisable(false);
        setInputEnabled(true);

        messagesBox.getChildren().clear();

        List<ChatMessageArij> messages = chatService.getMessagesBySession(session.getId());
        if (messages.isEmpty()) {
            addWelcomeBubble();
        } else {
            for (ChatMessageArij msg : messages) {
                if (msg.isUser()) {
                    renderUserBubble(msg.getContent(),
                        msg.getCreatedAt() != null ? msg.getCreatedAt().format(TIME_FMT) : "");
                } else {
                    renderAssistantBubble(msg.getContent(),
                        msg.getCreatedAt() != null ? msg.getCreatedAt().format(TIME_FMT) : "");
                }
            }
        }
        scrollToBottom();
    }

    // ------------------------------------------------------------------ //
    //  Handlers FXML                                                      //
    // ------------------------------------------------------------------ //

    /** Crée une nouvelle session vide. */
    @FXML
    private void handleNewSession() {
        ChatSessionArij session = chatService.createSession(patientId, "Nouvelle conversation");
        sessions.add(0, session);
        sessionListView.getSelectionModel().select(session);
    }

    /** Envoie le message du patient. */
    @FXML
    private void handleSend() {
        if (currentSession == null) {
            handleNewSession();
        }

        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        String userText = text.trim();
        messageInput.clear();
        setInputEnabled(false);

        // 1. Afficher la bulle utilisateur immédiatement
        String timeNow = java.time.LocalTime.now().format(TIME_FMT);
        renderUserBubble(userText, timeNow);
        scrollToBottom();

        // 2. Sauvegarder le message utilisateur en BDD
        chatService.saveMessage(currentSession.getId(), Role.user, userText);

        // Mettre à jour le titre de la session avec le premier message
        if (currentSession.getTitle() == null
                || currentSession.getTitle().equals("Nouvelle conversation")) {
            String newTitle = userText.length() > 40
                ? userText.substring(0, 40) + "…"
                : userText;
            chatService.updateSessionTitle(currentSession.getId(), newTitle);
            currentSession.setTitle(newTitle);
            // Rafraîchir la ListView
            int idx = sessions.indexOf(currentSession);
            if (idx >= 0) {
                sessions.set(idx, currentSession);
                sessionListView.getSelectionModel().select(currentSession);
            }
            sessionTitleLabel.setText(newTitle);
        }

        // 3. Charger l'historique complet pour le contexte IA
        List<ChatMessageArij> history = chatService.getMessagesBySession(currentSession.getId());

        // 4. Appel API dans un thread séparé
        showTyping(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return groqService.chat(buildMessages(history));
            }
        };

        task.setOnSucceeded(e -> {
            showTyping(false);
            String reply = task.getValue();

            // 5. Sauvegarder la réponse de l'assistant
            chatService.saveMessage(currentSession.getId(), Role.assistant, reply);

            // 6. Afficher la bulle assistant
            String t = java.time.LocalTime.now().format(TIME_FMT);
            renderAssistantBubble(reply, t);
            scrollToBottom();
            setInputEnabled(true);
            messageInput.requestFocus();
        });

        task.setOnFailed(e -> {
            showTyping(false);
            renderAssistantBubble(
                "⚠️ Erreur de connexion : " + task.getException().getMessage(), "");
            scrollToBottom();
            setInputEnabled(true);
        });

        new Thread(task, "mediassist-bot").start();
    }

    /** Supprime la session courante et tous ses messages. */
    @FXML
    private void handleDeleteSession() {
        if (currentSession == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette session et tous ses messages ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                chatService.deleteSession(currentSession.getId());
                sessions.remove(currentSession);
                currentSession = null;
                messagesBox.getChildren().clear();
                sessionTitleLabel.setText("Sélectionnez une session");
                deleteSessionButton.setDisable(true);
                setInputEnabled(false);

                if (!sessions.isEmpty()) {
                    sessionListView.getSelectionModel().selectFirst();
                }
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Construction des messages pour l'API                              //
    // ------------------------------------------------------------------ //

    /**
     * Construit la liste [system] + historique pour l'API Groq.
     */
    private List<Map<String, String>> buildMessages(List<ChatMessageArij> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt en tête
        Map<String, String> system = new HashMap<>();
        system.put("role",    "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);

        // Historique complet (multi-tour)
        for (ChatMessageArij msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role",    msg.getRole().name());
            m.put("content", msg.getContent());
            messages.add(m);
        }
        return messages;
    }

    // ------------------------------------------------------------------ //
    //  Rendu des bulles                                                   //
    // ------------------------------------------------------------------ //

    private void addWelcomeBubble() {
        renderAssistantBubble(
            "👋 Bonjour ! Je suis MediAssist, votre assistant médical.\n" +
            "Décrivez vos symptômes ou posez votre question — je vous réponds dans votre langue.",
            java.time.LocalTime.now().format(TIME_FMT)
        );
    }

    private void renderUserBubble(String text, String time) {
        Platform.runLater(() -> {
            // Texte
            Text msgText = new Text(text);
            msgText.setStyle("-fx-fill: white; -fx-font-size: 13px;");
            TextFlow flow = new TextFlow(msgText);
            flow.setMaxWidth(400);
            flow.setStyle(
                "-fx-background-color: #2563eb;" +
                "-fx-background-radius: 14 14 4 14;" +
                "-fx-padding: 10 14 10 14;"
            );

            // Timestamp
            Label ts = new Label(time);
            ts.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

            VBox bubble = new VBox(4, flow, ts);
            bubble.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_RIGHT);
            HBox.setMargin(bubble, new Insets(2, 6, 2, 80));

            messagesBox.getChildren().add(row);
        });
    }

    private void renderAssistantBubble(String text, String time) {
        Platform.runLater(() -> {
            // Avatar
            Label avatar = new Label("🩺");
            avatar.setStyle(
                "-fx-background-color: #eff6ff;" +
                "-fx-background-radius: 50%;" +
                "-fx-padding: 6 8 6 8;" +
                "-fx-font-size: 14px;"
            );

            // Texte (strip markdown basique)
            Text msgText = new Text(stripMarkdown(text));
            msgText.setStyle("-fx-fill: #0f172a; -fx-font-size: 13px;");
            TextFlow flow = new TextFlow(msgText);
            flow.setMaxWidth(400);
            flow.setStyle(
                "-fx-background-color: #f1f5f9;" +
                "-fx-background-radius: 14 14 14 4;" +
                "-fx-padding: 10 14 10 14;"
            );

            // Timestamp
            Label ts = new Label("MediAssist" + (time.isBlank() ? "" : "  •  " + time));
            ts.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

            VBox bubble = new VBox(4, flow, ts);
            bubble.setAlignment(Pos.CENTER_LEFT);

            HBox row = new HBox(10, avatar, bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(row, new Insets(2, 80, 2, 6));

            messagesBox.getChildren().add(row);
        });
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires UI                                                     //
    // ------------------------------------------------------------------ //

    private void showTyping(boolean visible) {
        Platform.runLater(() -> {
            typingIndicator.setVisible(visible);
            typingIndicator.setManaged(visible);
        });
    }

    private void setInputEnabled(boolean enabled) {
        Platform.runLater(() -> {
            messageInput.setDisable(!enabled);
            sendButton.setDisable(!enabled);
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private String stripMarkdown(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
            .replaceAll("\\*(.+?)\\*",        "$1")
            .replaceAll("(?m)^#{1,6}\\s*",    "")
            .replaceAll("(?m)^\\* ",           "• ")
            .replaceAll("(?m)^- ",             "• ")
            .trim();
    }
}
