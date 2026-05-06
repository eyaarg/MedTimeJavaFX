package esprit.fx.services;

import esprit.fx.utils.ConfigLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VisionService {

    private static final String API_KEY = getConfig("groq.api.key", "");
    private static final String API_URL = getConfig("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = getConfig("groq.vision.model", "llama-3.2-90b-vision-preview");

    public String genererDescription(String base64Image) throws Exception {
        if (API_KEY.isBlank()) {
            return "Cle API Groq non configuree.";
        }

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", MODEL);
        jsonBody.put("temperature", 0.2);
        jsonBody.put("max_tokens", 180);

        JSONArray contentParts = new JSONArray();
        contentParts.put(new JSONObject()
                .put("type", "text")
                .put("text", "Tu es un pharmacien expert. Analyse cette image de produit medical et redige une description courte, professionnelle et factuelle en francais, maximum 3 phrases."));
        contentParts.put(new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject()
                        .put("url", "data:image/jpeg;base64," + base64Image)));

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", contentParts));
        jsonBody.put("messages", messages);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Erreur Groq Vision - Status: " + response.statusCode());
            System.err.println("Response body: " + response.body());
            String errorMsg = extractErrorMessage(response.body());
            return "Erreur API Groq (" + response.statusCode() + "): " + errorMsg;
        }

        return extraireContenu(response.body());
    }

    private static String getConfig(String key, String fallback) {
        String value = ConfigLoader.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String extraireContenu(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            System.err.println("Erreur parsing Groq Vision : " + e.getMessage());
            return "Erreur d'analyse de la reponse IA.";
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("error")) {
                JSONObject error = json.getJSONObject("error");
                if (error.has("message")) {
                    return error.getString("message");
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "Erreur inconnue";
    }
}

