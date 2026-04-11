package tn.esprit.controllers.consultationonline;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ChatControllerArij {

    // ── System prompt: medical-only, multilingual ──────────────────────────
    private static final String SYSTEM_PROMPT =
        "You are MedTime AI, a professional medical assistant integrated into the MedTime healthcare platform. " +
        "Your role is to help patients and doctors with ALL medical-related questions including: " +
        "symptoms, diagnoses, treatments, medications, drug interactions, side effects, dosages, " +
        "medical procedures, specialist recommendations, preventive care, nutrition advice, " +
        "mental health guidance, chronic disease management, emergency first aid, " +
        "doctor specialties and when to consult them, lab results interpretation, " +
        "and any other health or medical topic. " +
        "Always respond in the SAME LANGUAGE the user writes in (Arabic, French, English, etc.). " +
        "Be thorough, accurate, compassionate, and professional. " +
        "Always recommend consulting a qualified doctor for diagnosis and treatment decisions. " +
        "If a question is completely unrelated to health, medicine, or doctors, politely decline " +
        "and redirect the user to ask a medical question. " +
        "Format your answers clearly with sections when needed.";

    @FXML private VBox messagesBox;
    @FXML private TextField messageInput;
    @FXML private TextArea questionInput;
    @FXML private TextArea aiResponse;
    @FXML private Label loadingLabel;
    @FXML private ScrollPane chatScrollPane;

    private String apiKey;
    private String model;
    private String apiUrl;

    @FXML
    private void initialize() {
        loadConfig();
        addSystemMessage("👋 Bonjour ! Je suis MedTime IA, votre assistant médical. Posez-moi toutes vos questions sur la santé, les symptômes, les médicaments ou les médecins — dans n'importe quelle langue !");
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            System.err.println("Config load error: " + e.getMessage());
        }
        apiKey = props.getProperty("groq.api.key", "");
        model  = props.getProperty("groq.model", "llama3-70b-8192");
        apiUrl = props.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    // ── Chat panel ────────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        String question = text.trim();
        messageInput.clear();

        addUserBubble(question);

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return callGroq(question);
            }
        };
        task.setOnSucceeded(e -> addAiBubble(task.getValue()));
        task.setOnFailed(e  -> addAiBubble("⚠️ Erreur : " + task.getException().getMessage()));
        new Thread(task, "groq-chat").start();
    }

    // ── AI Assistant panel ────────────────────────────────────────────────

    @FXML private void presetExplain()      { questionInput.setText("Expliquez mon diagnostic en termes simples."); }
    @FXML private void presetSideEffects()  { questionInput.setText("Quels sont les effets secondaires courants de mes médicaments prescrits ?"); }
    @FXML private void presetNextSteps()    { questionInput.setText("Quelles sont les prochaines étapes recommandées après ma consultation ?"); }

    @FXML
    private void handleAskAi() {
        String question = questionInput.getText();
        if (question == null || question.trim().isEmpty()) return;

        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        aiResponse.clear();

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return callGroq(question.trim());
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
        new Thread(task, "groq-ai").start();
    }

    // ── Groq API call ─────────────────────────────────────────────────────

    private String callGroq(String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API key not configured. Please check config.properties.";
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            String payload = buildPayload(userMessage);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = readStream(stream);

            if (code >= 200 && code < 300) {
                return parseContent(body);
            } else {
                return "API error (" + code + "): " + body;
            }
        } catch (IOException e) {
            return "Connection error: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String buildPayload(String userMessage) {
        return "{"
            + "\"model\":\"" + model + "\","
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_PROMPT) + "\"},"
            +   "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
            + "],"
            + "\"temperature\":0.7,"
            + "\"max_tokens\":1024"
            + "}";
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private void addSystemMessage(String text) {
        Platform.runLater(() -> {
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-background-color:#eff6ff;-fx-text-fill:#1d4ed8;"
                + "-fx-padding:10 14 10 14;-fx-background-radius:10;"
                + "-fx-font-size:12px;-fx-font-style:italic;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            messagesBox.getChildren().add(lbl);
        });
    }

    private void addUserBubble(String text) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            VBox bubble = new VBox(3);
            Label msg = new Label(text);
            msg.setWrapText(true);
            msg.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;"
                + "-fx-padding:10 14 10 14;-fx-background-radius:14 14 4 14;"
                + "-fx-font-size:13px;");
            msg.setMaxWidth(380);
            Label ts = new Label(time);
            ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            bubble.getChildren().addAll(msg, ts);
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
            String clean = stripMarkdown(text);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            VBox bubble = new VBox(3);
            Label msg = new Label(clean);
            msg.setWrapText(true);
            msg.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#0f172a;"
                + "-fx-padding:10 14 10 14;-fx-background-radius:14 14 14 4;"
                + "-fx-font-size:13px;");
            msg.setMaxWidth(380);
            Label ts = new Label("🤖 MedTime IA  •  " + time);
            ts.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;");
            bubble.getChildren().addAll(msg, ts);
            bubble.setAlignment(Pos.CENTER_LEFT);

            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(bubble, new Insets(0, 60, 0, 4));
            messagesBox.getChildren().add(row);
            scrollToBottom();
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

    /** Strip basic markdown: **bold**, *italic*, # headers, bullet * → • */
    private String stripMarkdown(String text) {
        return text
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")   // **bold**
            .replaceAll("\\*(.+?)\\*", "$1")           // *italic*
            .replaceAll("(?m)^#{1,6}\\s*", "")         // # headers
            .replaceAll("(?m)^\\* ", "• ")             // * bullets → •
            .replaceAll("(?m)^- ", "• ")               // - bullets → •
            .trim();
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String parseContent(String json) {
        String key = "\"content\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return "No response received.";
        int start = idx + key.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String escapeJson(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
