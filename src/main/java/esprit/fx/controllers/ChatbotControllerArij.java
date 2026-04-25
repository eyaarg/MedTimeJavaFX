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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  ChatbotControllerArij — MediAssist Chatbot
 * ============================================================
 *
 *  ARCHITECTURE THREAD :
 *  ─────────────────────
 *  JavaFX est mono-thread côté UI : tout ce qui touche les
 *  composants graphiques (Label, VBox, ScrollPane…) DOIT
 *  s'exécuter sur le JavaFX Application Thread.
 *
 *  L'appel réseau vers l'API Groq peut prendre plusieurs
 *  secondes. Si on l'exécute sur le JavaFX Thread, l'interface
 *  se fige complètement (plus de scroll, plus de clic, etc.).
 *
 *  Solution : Task<String> (javafx.concurrent)
 *  ─────────────────────────────────────────────
 *  • call()          → s'exécute dans un thread background
 *                      → on peut faire des I/O bloquants ici
 *  • setOnSucceeded  → rappelé automatiquement sur le
 *                      JavaFX Application Thread quand call()
 *                      se termine sans exception
 *  • setOnFailed     → idem, en cas d'exception dans call()
 *
 *  Platform.runLater(Runnable) :
 *  ──────────────────────────────
 *  Quand on est déjà dans un callback de Task (setOnSucceeded /
 *  setOnFailed), JavaFX garantit qu'on est sur le bon thread,
 *  donc Platform.runLater() est techniquement optionnel dans
 *  ces callbacks. On l'utilise quand même par convention
 *  défensive et pour les méthodes utilitaires (showLoading,
 *  afficherBulle…) qui peuvent être appelées depuis n'importe
 *  quel thread.
 *
 *  Règle simple : toute modification d'un nœud JavaFX depuis
 *  un thread non-UI → Platform.runLater().
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
    @FXML private ScrollPane                chatScrollPane;
    @FXML private TextField                 messageInput;
    @FXML private Button                    sendButton;
    @FXML private Button                    newSessionButton;
    @FXML private Button                    deleteSessionButton;
    @FXML private Label                     sessionTitleLabel;

    // Indicateur de chargement (spinner) affiché pendant l'appel API
    @FXML private ProgressIndicator         loadingSpinner;

    // Label texte animé sous le spinner
    @FXML private Label                     typingIndicator;

    // ------------------------------------------------------------------ //
    //  Services & état                                                    //
    // ------------------------------------------------------------------ //
    private final ServiceChatArij    chatService = new ServiceChatArij();
    private final GroqApiServiceArij groqService = new GroqApiServiceArij();

    private final ObservableList<ChatSessionArij> sessions =
        FXCollections.observableArrayList();

    private ChatSessionArij currentSession = null;
    private int             patientId      = 0;

    // ------------------------------------------------------------------ //
    //  Initialisation                                                     //
    // ------------------------------------------------------------------ //
    @FXML
    private void initialize() {
        // Récupérer l'id du patient connecté via la session globale
        if (UserSession.getCurrentUser() != null) {
            patientId = UserSession.getCurrentUser().getId();
        }

        // Lier la ListView à la liste observable
        sessionListView.setItems(sessions);
        sessionListView.setPlaceholder(new Label("Aucune session"));

        // Charger la session sélectionnée quand l'utilisateur clique
        sessionListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, selected) -> {
                if (selected != null) loadSession(selected);
            });

        // Désactiver les contrôles tant qu'aucune session n'est active
        setInputEnabled(false);
        deleteSessionButton.setDisable(true);

        // Masquer le spinner au démarrage
        showLoading(false);

        // Charger les sessions existantes depuis la BDD
        loadSessions();

        // Permettre l'envoi avec la touche Entrée
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

    /** Charge et affiche tous les messages d'une session. */
    private void loadSession(ChatSessionArij session) {
        currentSession = session;
        sessionTitleLabel.setText(
            session.getTitle() != null ? session.getTitle() : "Session #" + session.getId()
        );
        deleteSessionButton.setDisable(false);
        setInputEnabled(true);
        messagesBox.getChildren().clear();

        List<ChatMessageArij> messages =
            chatService.getMessagesBySession(session.getId());

        if (messages.isEmpty()) {
            addWelcomeBubble();
        } else {
            for (ChatMessageArij msg : messages) {
                String t = msg.getCreatedAt() != null
                    ? msg.getCreatedAt().format(TIME_FMT) : "";
                afficherBulle(msg.getContent(), msg.getRole().name(), t);
            }
        }
        scrollToBottom();
    }

    // ------------------------------------------------------------------ //
    //  Handlers FXML                                                      //
    // ------------------------------------------------------------------ //

    @FXML
    private void handleNewSession() {
        ChatSessionArij session =
            chatService.createSession(patientId, "Nouvelle conversation");
        sessions.add(0, session);
        sessionListView.getSelectionModel().select(session);
    }

    /**
     * Point d'entrée principal : envoi d'un message patient.
     *
     * Séquence :
     *  1. Afficher la bulle utilisateur (UI immédiate)
     *  2. Sauvegarder en BDD (role=user)
     *  3. Lancer le Task<String> dans un thread background
     *     → appel bloquant vers l'API Groq
     *  4. Sur succès  : sauvegarder + afficher la réponse
     *  5. Sur échec   : afficher un message d'erreur
     */
    @FXML
    private void handleSend() {
        if (currentSession == null) handleNewSession();

        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;

        final String userText = text.trim();
        messageInput.clear();

        // ── Étape 1 : affichage immédiat côté patient ──────────────────
        String timeNow = LocalTime.now().format(TIME_FMT);
        afficherBulle(userText, "user", timeNow);
        scrollToBottom();

        // ── Étape 2 : persistance du message utilisateur ───────────────
        sauvegarderMessage("user", userText);

        // Mettre à jour le titre de la session avec le premier message
        if ("Nouvelle conversation".equals(currentSession.getTitle())) {
            String titre = userText.length() > 40
                ? userText.substring(0, 40) + "…" : userText;
            chatService.updateSessionTitle(currentSession.getId(), titre);
            currentSession.setTitle(titre);
            int idx = sessions.indexOf(currentSession);
            if (idx >= 0) {
                sessions.set(idx, currentSession);
                sessionListView.getSelectionModel().select(currentSession);
            }
            sessionTitleLabel.setText(titre);
        }

        // ── Étape 3 : charger l'historique complet pour le contexte IA ─
        final List<ChatMessageArij> history =
            chatService.getMessagesBySession(currentSession.getId());

        // Désactiver la saisie + afficher le spinner pendant l'appel
        setInputEnabled(false);
        showLoading(true);

        // ══════════════════════════════════════════════════════════════
        //  Task<String> — appel réseau dans un thread background
        // ══════════════════════════════════════════════════════════════
        //
        //  POURQUOI un thread séparé ?
        //  L'appel HTTP vers l'API Groq est bloquant (peut durer 2-10s).
        //  Si on l'exécute directement ici (sur le JavaFX Thread),
        //  l'interface se fige : plus de scroll, plus de clic, l'OS
        //  peut même afficher "Application ne répond pas".
        //
        //  Task<String> exécute call() dans un thread background et
        //  garantit que setOnSucceeded / setOnFailed sont rappelés
        //  sur le JavaFX Application Thread → on peut toucher l'UI
        //  directement dans ces callbacks.
        // ══════════════════════════════════════════════════════════════
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // ← Exécuté dans le thread background "mediassist-bot"
                // Appel HTTP bloquant — ne touche AUCUN composant JavaFX ici
                return groqService.chat(buildMessages(history));
            }
        };

        // ── Étape 4 : succès ───────────────────────────────────────────
        //
        //  setOnSucceeded est appelé sur le JavaFX Application Thread.
        //  Platform.runLater() est utilisé ici par convention défensive :
        //  les méthodes utilitaires (afficherBulle, showLoading…) peuvent
        //  être appelées depuis n'importe quel contexte, donc elles
        //  encapsulent elles-mêmes Platform.runLater() pour être sûres.
        // ──────────────────────────────────────────────────────────────
        task.setOnSucceeded(e -> {
            // On est sur le JavaFX Thread ici (garanti par Task)
            showLoading(false);
            String reply = task.getValue();

            // Sauvegarder la réponse de l'assistant en BDD
            sauvegarderMessage("assistant", reply);

            // Afficher la bulle assistant dans le VBox
            // Platform.runLater() dans afficherBulle garantit la sécurité
            // si cette méthode est un jour appelée depuis un autre thread
            String t = LocalTime.now().format(TIME_FMT);
            afficherBulle(reply, "assistant", t);

            scrollToBottom();
            setInputEnabled(true);

            // Remettre le focus sur le champ de saisie
            // Platform.runLater() ici car requestFocus() doit s'exécuter
            // APRÈS que le layout JavaFX ait traité les changements précédents
            Platform.runLater(() -> messageInput.requestFocus());
        });

        // ── Étape 5 : échec ────────────────────────────────────────────
        //
        //  setOnFailed est aussi appelé sur le JavaFX Application Thread.
        //  Platform.runLater() dans afficherErreur() protège contre un
        //  appel éventuel depuis un contexte non-UI.
        // ──────────────────────────────────────────────────────────────
        task.setOnFailed(e -> {
            showLoading(false);
            String cause = task.getException() != null
                ? task.getException().getMessage()
                : "Erreur inconnue";
            afficherErreur("Erreur connexion IA : " + cause);
            scrollToBottom();
            setInputEnabled(true);
        });

        // Lancer le thread background avec un nom lisible dans les logs
        new Thread(task, "mediassist-bot").start();
    }

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
                // DELETE chat_message WHERE session_id = ?
                // DELETE chat_session WHERE id = ?
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
    //  Méthodes métier nommées                                            //
    // ------------------------------------------------------------------ //

    /**
     * Affiche une bulle de message dans le VBox de chat.
     *
     * @param content  texte du message
     * @param role     "user" → bulle bleue à droite
     *                 "assistant" → bulle grise à gauche
     * @param time     horodatage affiché sous la bulle (ex: "14:32")
     *
     * Platform.runLater() : cette méthode peut être appelée depuis
     * n'importe quel thread (chargement BDD, callback Task…).
     * JavaFX interdit toute modification de l'arbre de scène hors du
     * JavaFX Application Thread → Platform.runLater() planifie
     * l'exécution sur ce thread dès qu'il est disponible.
     */
    private void afficherBulle(String content, String role, String time) {
        Platform.runLater(() -> {
            if ("user".equals(role)) {
                renderUserBubble(content, time);
            } else {
                renderAssistantBubble(content, time);
            }
        });
    }

    /**
     * Surcharge sans horodatage (utilisée par handleSend pour déléguer
     * à la version complète avec l'heure courante).
     */
    private void afficherBulle(String content, String role) {
        afficherBulle(content, role, LocalTime.now().format(TIME_FMT));
    }

    /**
     * Persiste un message en base de données.
     *
     * @param role    "user" ou "assistant"
     * @param content texte du message
     *
     * Appelé depuis le JavaFX Thread (avant le Task pour "user",
     * dans setOnSucceeded pour "assistant") — pas besoin de
     * Platform.runLater() ici car on ne touche pas l'UI.
     */
    private void sauvegarderMessage(String role, String content) {
        if (currentSession == null) return;
        chatService.saveMessage(
            currentSession.getId(),
            Role.from(role),
            content
        );
    }

    /**
     * Affiche une bulle d'erreur rouge dans le chat.
     *
     * Platform.runLater() : peut être appelée depuis setOnFailed
     * (JavaFX Thread) ou depuis un catch dans un thread background.
     * L'encapsulation garantit la sécurité dans les deux cas.
     */
    private void afficherErreur(String message) {
        Platform.runLater(() -> {
            Text errText = new Text("⚠️ " + message);
            errText.setStyle("-fx-fill: #dc2626; -fx-font-size: 12px;");
            TextFlow flow = new TextFlow(errText);
            flow.setStyle(
                "-fx-background-color: #fff1f2;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 10 14 10 14;" +
                "-fx-border-color: #fecdd3;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
            );
            flow.setMaxWidth(400);

            HBox row = new HBox(flow);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(flow, new Insets(2, 80, 2, 6));
            messagesBox.getChildren().add(row);
        });
    }

    // ------------------------------------------------------------------ //
    //  Construction des messages pour l'API Groq                         //
    // ------------------------------------------------------------------ //

    /**
     * Construit la liste [system] + historique complet de la session.
     * Le system prompt est toujours injecté en premier pour que le
     * modèle conserve son rôle MediAssist sur toute la conversation.
     */
    private List<Map<String, String>> buildMessages(List<ChatMessageArij> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System prompt — définit le comportement de MediAssist
        Map<String, String> system = new HashMap<>();
        system.put("role",    "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);

        // 2. Historique complet (multi-tour) pour le contexte de la conversation
        for (ChatMessageArij msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role",    msg.getRole().name());   // "user" ou "assistant"
            m.put("content", msg.getContent());
            messages.add(m);
        }
        return messages;
    }

    // ------------------------------------------------------------------ //
    //  Rendu des bulles                                                   //
    // ------------------------------------------------------------------ //

    private void addWelcomeBubble() {
        // Appelé depuis loadSession() qui est sur le JavaFX Thread,
        // mais afficherBulle() encapsule Platform.runLater() par sécurité
        afficherBulle(
            "👋 Bonjour ! Je suis MediAssist, votre assistant médical.\n" +
            "Décrivez vos symptômes ou posez votre question — " +
            "je vous réponds dans votre langue.",
            "assistant",
            LocalTime.now().format(TIME_FMT)
        );
    }

    /** Bulle bleue à droite — message du patient. */
    private void renderUserBubble(String text, String time) {
        Text msgText = new Text(text);
        msgText.setStyle("-fx-fill: white; -fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.setMaxWidth(400);
        flow.setStyle(
            "-fx-background-color: #2563eb;" +
            "-fx-background-radius: 14 14 4 14;" +
            "-fx-padding: 10 14 10 14;"
        );

        Label ts = new Label(time);
        ts.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        VBox bubble = new VBox(4, flow, ts);
        bubble.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        HBox.setMargin(bubble, new Insets(2, 6, 2, 80));

        messagesBox.getChildren().add(row);
    }

    /** Bulle grise à gauche — réponse de MediAssist. */
    private void renderAssistantBubble(String text, String time) {
        // Avatar emoji
        Label avatar = new Label("🩺");
        avatar.setStyle(
            "-fx-background-color: #eff6ff;" +
            "-fx-background-radius: 50%;" +
            "-fx-padding: 6 8 6 8;" +
            "-fx-font-size: 14px;"
        );

        Text msgText = new Text(stripMarkdown(text));
        msgText.setStyle("-fx-fill: #0f172a; -fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.setMaxWidth(400);
        flow.setStyle(
            "-fx-background-color: #f1f5f9;" +
            "-fx-background-radius: 14 14 14 4;" +
            "-fx-padding: 10 14 10 14;"
        );

        Label ts = new Label("MediAssist" + (time.isBlank() ? "" : "  •  " + time));
        ts.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        VBox bubble = new VBox(4, flow, ts);
        bubble.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(10, avatar, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(row, new Insets(2, 80, 2, 6));

        messagesBox.getChildren().add(row);
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires UI                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Affiche ou masque le ProgressIndicator + le label "en train d'écrire".
     *
     * Platform.runLater() : showLoading() peut être appelée depuis
     * setOnSucceeded/setOnFailed (JavaFX Thread) ou depuis un catch
     * dans un thread background → encapsulation défensive.
     */
    private void showLoading(boolean visible) {
        Platform.runLater(() -> {
            loadingSpinner.setVisible(visible);
            loadingSpinner.setManaged(visible);
            typingIndicator.setVisible(visible);
            typingIndicator.setManaged(visible);
        });
    }

    /**
     * Active ou désactive le champ de saisie et le bouton Envoyer.
     *
     * Platform.runLater() : même raison — peut être appelée depuis
     * n'importe quel thread, on délègue au JavaFX Thread.
     */
    private void setInputEnabled(boolean enabled) {
        Platform.runLater(() -> {
            messageInput.setDisable(!enabled);
            sendButton.setDisable(!enabled);
        });
    }

    /**
     * Fait défiler le ScrollPane jusqu'en bas après ajout d'une bulle.
     *
     * Platform.runLater() : on appelle layout() pour forcer JavaFX à
     * recalculer les dimensions AVANT de positionner la scrollbar à 1.0.
     * Sans ça, setVvalue(1.0) serait calculé sur l'ancienne hauteur.
     */
    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    /** Supprime le formatage Markdown basique pour un affichage propre. */
    private String stripMarkdown(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")   // **gras** → gras
            .replaceAll("\\*(.+?)\\*",        "$1")   // *italique* → italique
            .replaceAll("(?m)^#{1,6}\\s*",    "")     // ## Titre → Titre
            .replaceAll("(?m)^\\* ",           "• ")  // * item → • item
            .replaceAll("(?m)^- ",             "• ")  // - item → • item
            .trim();
    }
}
