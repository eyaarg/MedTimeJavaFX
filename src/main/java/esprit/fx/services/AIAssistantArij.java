package esprit.fx.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service IA Assistant utilisant l'API Groq.
 * Fournit des réponses intelligentes pour l'application médicale.
 */
public class AIAssistantArij {

    private final String apiKey;
    private final String apiUrl = "https://api.groq.com/openai/v1/chat/completions";
    private final String model = "llama3-8b-8192";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AIAssistantArij() {
        this.apiKey = loadApiKey();
    }

    /**
     * Pose une question à l'IA et reçoit une réponse.
     *
     * @param question la question à poser
     * @param context contexte optionnel (ex: "Tu es un assistant médical")
     * @return réponse de l'IA
     */
    public String askAI(String question, String context) {
        if (question == null || question.isBlank()) {
            return "Veuillez poser une question.";
        }

        try {
            String systemPrompt = context != null && !context.isBlank()
                    ? context
                    : "Tu es un assistant médical utile et bienveillant. Réponds de manière claire et concise.";

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", question));

            String requestBody = buildRequestBody(messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[AIAssistantArij] HTTP " + response.statusCode());
                return "Désolé, l'assistant IA est temporairement indisponible.";
            }

            return extractContent(response.body());

        } catch (Exception e) {
            System.err.println("[AIAssistantArij] Erreur : " + e.getMessage());
            return "Erreur de communication avec l'assistant IA.";
        }
    }

    /**
     * Conseil médical spécifique.
     */
    public String getMedicalAdvice(String symptom) {
        String context = "Tu es un assistant médical. Fournis des conseils généraux sur le symptôme mentionné. "
                + "IMPORTANT: Rappelle toujours que ce n'est pas un diagnostic médical et qu'il faut consulter un médecin.";
        return askAI("Quels sont les conseils généraux pour " + symptom + " ?", context);
    }

    /**
     * Explication d'un diagnostic.
     */
    public String explainDiagnosis(String diagnosis) {
        String context = "Tu es un assistant médical. Explique ce diagnostic de manière simple et compréhensible.";
        return askAI("Explique-moi le diagnostic : " + diagnosis, context);
    }

    /**
     * Recommandations de traitement.
     */
    public String getTreatmentRecommendations(String condition) {
        String context = "Tu es un assistant médical. Fournis des recommandations générales de traitement. "
                + "Rappelle que seul un médecin peut prescrire un traitement.";
        return askAI("Quelles sont les recommandations générales pour traiter " + condition + " ?", context);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private String buildRequestBody(List<Map<String, String>> messages) {
        JSONArray messagesArray = new JSONArray();
        for (Map<String, String> msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getOrDefault("role", "user"));
            msgObj.put("content", msg.getOrDefault("content", ""));
            messagesArray.put(msgObj);
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messagesArray);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);

        return body.toString();
    }

    private String extractContent(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray choices = json.getJSONArray("choices");

            if (choices.isEmpty()) {
                return "Pas de réponse reçue.";
            }

            return choices
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            System.err.println("[AIAssistantArij] Erreur parsing : " + e.getMessage());
            return "Erreur lors du traitement de la réponse.";
        }
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
                return props.getProperty("groq.api.key", "");
            }
        } catch (Exception e) {
            System.err.println("[AIAssistantArij] Erreur chargement clé API : " + e.getMessage());
        }
        return "";
    }
}
