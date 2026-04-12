package esprit.fx.controllers.consultationonline;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ChatControllerArij {

    private static final String SYSTEM_PROMPT =
        "You are MedTime AI, a professional medical assistant. " +
        "Help with ALL medical questions: symptoms, diagnoses, treatments, medications, " +
        "drug interactions, dosages, procedures, specialist recommendations, preventive care, " +
        "nutrition, mental health, chronic diseases, emergency first aid, lab results. " +
        "Always respond in the SAME LANGUAGE the user writes in. " +
        "Be thorough, accurate, compassionate. Always recommend consulting a qualified doctor. " +
        "Decline non-medical questions politely.";

    @FXML private VBox messagesBox;
    @FXML private TextField messageInput;
    @FXML private TextArea questionInput;
    @FXML private TextArea aiResponse;
    @FXML private Label loadingLabel;
    @FXML private ScrollPane chatScrollPane;

    private String apiKey, model, apiUrl;

    @FXML
    private void initialize() {
        loadConfig();
        addSystemMessage("👋 Bonjour ! Je suis MedTime IA, votre assistant médical. Posez-moi toutes vos questions sur la santé — dans n'importe quelle langue !");
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
        } catch (IOException e) { System.err.println("Config: " + e.getMessage()); }
        apiKey = props.getProperty("groq.api.key", "");
        model  = props.getProperty("groq.model", "llama-3.3-70b-versatile");
        apiUrl = props.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        String q = text.trim(); messageInput.clear();
        addUserBubble(q);
        Task<String> task = new Task<>() { @Override protected String call() { return callGroq(q); } };
        task.setOnSucceeded(e -> addAiBubble(task.getValue()));
        task.setOnFailed(e -> addAiBubble("⚠️ Erreur : " + task.getException().getMessage()));
        new Thread(task, "groq-chat").start();
    }

    @FXML private void presetExplain()     { questionInput.setText("Expliquez mon diagnostic en termes simples."); }
    @FXML private void presetSideEffects() { questionInput.setText("Quels sont les effets secondaires courants de mes médicaments ?"); }
    @FXML private void presetNextSteps()   { questionInput.setText("Quelles sont les prochaines étapes après ma consultation ?"); }

    @FXML
    private void handleAskAi() {
        String q = questionInput.getText();
        if (q == null || q.trim().isEmpty()) return;
        loadingLabel.setVisible(true); loadingLabel.setManaged(true); aiResponse.clear();
        Task<String> task = new Task<>() { @Override protected String call() { return callGroq(q.trim()); } };
        task.setOnSucceeded(e -> { loadingLabel.setVisible(false); loadingLabel.setManaged(false); aiResponse.setText(stripMarkdown(task.getValue())); });
        task.setOnFailed(e -> { loadingLabel.setVisible(false); loadingLabel.setManaged(false); aiResponse.setText("⚠️ Erreur : " + task.getException().getMessage()); });
        new Thread(task, "groq-ai").start();
    }

    private String callGroq(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) return "Clé API non configurée.";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000); conn.setReadTimeout(30000); conn.setDoOutput(true);
            String payload = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_PROMPT) + "\"},{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}],\"temperature\":0.7,\"max_tokens\":1024}";
            conn.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
            int code = conn.getResponseCode();
            String body = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            return code >= 200 && code < 300 ? parseContent(body) : "Erreur API (" + code + "): " + body;
        } catch (IOException e) { return "Erreur connexion: " + e.getMessage(); }
        finally { if (conn != null) conn.disconnect(); }
    }

    private void addSystemMessage(String text) {
        Platform.runLater(() -> {
            Label lbl = new Label(text); lbl.setWrapText(true);
            lbl.setStyle("-fx-background-color:#eff6ff;-fx-text-fill:#1d4ed8;-fx-padding:10 14 10 14;-fx-background-radius:10;-fx-font-size:12px;-fx-font-style:italic;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            messagesBox.getChildren().add(lbl);
        });
    }

    private void addUserBubble(String text) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label msg = new Label(text); msg.setWrapText(true); msg.setMaxWidth(380);
            msg.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;-fx-padding:10 14 10 14;-fx-background-radius:14 14 4 14;-fx-font-size:13px;");
            Label ts = new Label(time); ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            VBox bubble = new VBox(3, msg, ts); bubble.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(bubble); row.setAlignment(Pos.CENTER_RIGHT);
            HBox.setMargin(bubble, new Insets(0, 4, 0, 60));
            messagesBox.getChildren().add(row); scrollToBottom();
        });
    }

    private void addAiBubble(String text) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label msg = new Label(stripMarkdown(text)); msg.setWrapText(true); msg.setMaxWidth(380);
            msg.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#0f172a;-fx-padding:10 14 10 14;-fx-background-radius:14 14 14 4;-fx-font-size:13px;");
            Label ts = new Label("🤖 MedTime IA  •  " + time); ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            VBox bubble = new VBox(3, msg, ts); bubble.setAlignment(Pos.CENTER_LEFT);
            HBox row = new HBox(bubble); row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(bubble, new Insets(0, 60, 0, 4));
            messagesBox.getChildren().add(row); scrollToBottom();
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> { if (chatScrollPane != null) { chatScrollPane.layout(); chatScrollPane.setVvalue(1.0); } });
    }

    private String stripMarkdown(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*","$1").replaceAll("\\*(.+?)\\*","$1")
                   .replaceAll("(?m)^#{1,6}\\s*","").replaceAll("(?m)^\\* ","• ").replaceAll("(?m)^- ","• ").trim();
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String parseContent(String json) {
        String key = "\"content\":\""; int idx = json.indexOf(key);
        if (idx == -1) return "Aucune réponse reçue.";
        int start = idx + key.length(); StringBuilder sb = new StringBuilder(); boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { switch(c){case 'n':sb.append('\n');break;case 't':sb.append('\t');break;case '\\':sb.append('\\');break;case '"':sb.append('"');break;default:sb.append(c);} escape=false; }
            else if (c=='\\') escape=true; else if (c=='"') break; else sb.append(c);
        }
        return sb.toString().trim();
    }

    private String escapeJson(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) { switch(c){case '"':sb.append("\\\"");break;case '\\':sb.append("\\\\");break;case '\n':sb.append("\\n");break;case '\r':sb.append("\\r");break;case '\t':sb.append("\\t");break;default:sb.append(c);} }
        return sb.toString();
    }
}
