package esprit.fx.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service d'intégration avec l'API Groq (compatible OpenAI).
 * Utilise java.net.http.HttpClient (Java 11+), aucune dépendance HTTP externe.
 */
public class GroqApiServiceArij {

    private final String apiKey;
    private final String apiUrl;
    private final String model;

    private final HttpClient httpClient;

    // ------------------------------------------------------------------ //
    //  Constructeur : charge la config depuis config.properties           //
    // ------------------------------------------------------------------ //
    public GroqApiServiceArij() {
        Properties props = loadProperties();
        this.apiKey  = props.getProperty("groq.api.key",  "YOUR_GROQ_API_KEY_HERE");
        this.apiUrl  = props.getProperty("groq.api.url",  "https://api.groq.com/openai/v1/chat/completions");
        this.model   = props.getProperty("groq.model",    "llama3-8b-8192");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Méthode principale                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Envoie une liste de messages au modèle Groq et retourne la réponse.
     *
     * @param messages liste de maps contenant "role" ("user" | "assistant" | "system")
     *                 et "content" (texte du message)
     * @return contenu textuel de choices[0].message.content
     * @throws Exception en cas d'erreur réseau ou de réponse inattendue
     */
    public String chat(List<Map<String, String>> messages) throws Exception {
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
            throw new IOException(
                "Groq API error [HTTP " + response.statusCode() + "]: " + response.body()
            );
        }

        return extractContent(response.body());
    }

    // ------------------------------------------------------------------ //
    //  Helpers privés                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Construit le corps JSON de la requête.
     */
    private String buildRequestBody(List<Map<String, String>> messages) {
        JSONArray messagesArray = new JSONArray();
        for (Map<String, String> msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role",    msg.getOrDefault("role",    "user"));
            msgObj.put("content", msg.getOrDefault("content", ""));
            messagesArray.put(msgObj);
        }

        JSONObject body = new JSONObject();
        body.put("model",      model);
        body.put("messages",   messagesArray);
        body.put("max_tokens", 1024);

        return body.toString();
    }

    /**
     * Extrait choices[0].message.content depuis la réponse JSON.
     */
    private String extractContent(String responseBody) {
        JSONObject json    = new JSONObject(responseBody);
        JSONArray  choices = json.getJSONArray("choices");

        if (choices.isEmpty()) {
            throw new IllegalStateException("Groq API returned an empty choices array.");
        }

        return choices
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    /**
     * Charge config.properties depuis le classpath.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("[GroqApiServiceArij] Impossible de charger config.properties : " + e.getMessage());
        }
        return props;
    }
}
