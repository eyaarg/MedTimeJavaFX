package esprit.fx.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class VisionService {

    private static final String API_KEY = "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";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String genererDescription(String base64Image) throws Exception {
        // 1. Le Prompt spécialisé pour MedTime
        String prompt = "Tu es un pharmacien expert. Analyse cette image de produit médical et rédige une description courte (3 phrases) pour un catalogue. Sois pro et factuel.";

        // 2. Construction du corps de la requête au format Multi-modal
        // On utilise le modèle Llama 3.2 Vision
        String jsonBody = """
            {
              "model": "llama-3.2-11b-vision-preview",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    { "type": "text", "text": "%s" },
                    { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,%s" } }
                  ]
                }
              ],
              "temperature": 0.5
            }
            """.formatted(prompt, base64Image);

        // 3. Envoi de la requête avec HttpClient
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Extraction de la réponse
        return extraireContenu(response.body());
    }

    private String extraireContenu(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .replace("\\n", "\n")
                    .trim();
        } catch (Exception e) {
            return "Erreur d'analyse de la réponse IA.";
        }
    }
}
