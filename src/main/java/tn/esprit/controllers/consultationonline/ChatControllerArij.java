package tn.esprit.controllers.consultationonline;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ChatControllerArij {
    @FXML
    private VBox messagesBox;
    @FXML
    private TextField messageInput;
    @FXML
    private TextArea questionInput;
    @FXML
    private TextArea aiResponse;
    @FXML
    private Label loadingLabel;

    private final List<String> messages = new ArrayList<>();

    @FXML
    private void initialize() {
        addMessage("[System] Chat ready.", false);
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        addMessage("[You]: " + text.trim(), true);
        messageInput.clear();
    }

    @FXML
    private void presetExplain() {
        questionInput.setText("Explain my diagnosis in simple terms.");
    }

    @FXML
    private void presetSideEffects() {
        questionInput.setText("List common side effects of my prescribed drugs.");
    }

    @FXML
    private void presetNextSteps() {
        questionInput.setText("What are the next steps after this consultation?");
    }

    @FXML
    private void handleAskAi() {
        String question = questionInput.getText();
        if (question == null || question.trim().isEmpty()) {
            return;
        }
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        aiResponse.clear();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return callAi(question.trim());
            }
        };

        task.setOnSucceeded(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            aiResponse.setText(task.getValue());
        });

        task.setOnFailed(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            aiResponse.setText("Error: " + task.getException().getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void addMessage(String text, boolean isYou) {
        messages.add(text);
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: " + (isYou ? "#d1e7dd" : "#e2e3e5") + "; -fx-padding:8; -fx-background-radius:8;");
        messagesBox.getChildren().add(label);
    }

    private String callAi(String question) {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            return "Failed to load API key: " + e.getMessage();
        }

        String apiKey = props.getProperty("ai.api.key", "");
        String model = props.getProperty("ai.model", "gpt-3.5-turbo");
        if (apiKey.isEmpty()) {
            return "API key missing in config.properties";
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(question) + "\"}]}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream responseStream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String response = readStream(responseStream);
            if (code >= 200 && code < 300) {
                return parseContent(response);
            } else {
                return "API error (" + code + "): " + response;
            }
        } catch (IOException e) {
            return "HTTP error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String parseContent(String json) {
        String key = "\"content\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) {
            return "No content in response.";
        }
        int start = idx + key.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (!escape && c == '\"') {
                break;
            }
            if (escape) {
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '\"':
                        sb.append('\"');
                        break;
                    default:
                        sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
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
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
