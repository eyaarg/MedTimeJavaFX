package esprit.fx.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SmartSearchService {

    private final String API_KEY = System.getenv("GROQ_API_KEY");
    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String askGroq(String userInput, List<String> availableProducts) throws Exception {
        // 1. Préparation du contexte
        // Dans askGroq(...)
        String systemMessage = "Tu es un pharmacien intelligent. Voici tes produits : " + String.join(", ", availableProducts);

        String userMessage = "L'utilisateur dit : '" + userInput + "'. " +
                     "1. Identifie le symptôme (ex: 'mal à la tête' = besoin d'un antalgique). " +
                     "2. Sélectionne le produit le plus adapté dans la liste fournie (ex: Doliprane). " +
                     "3. Réponds UNIQUEMENT avec le nom exact du produit trouvé dans la liste. " +
                     "4. Si aucun produit ne correspond logiquement, réponds 'NONE'.";// 2. Construction propre du JSON avec la bibliothèque org.json
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "llama-3.3-70b-versatile");
        jsonBody.put("temperature", 0.1); // Basse température pour être précis et constant

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemMessage));
        messages.put(new JSONObject().put("role", "user").put("content", userMessage));
        
        jsonBody.put("messages", messages);

        // 3. Envoi de la requête
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Vérification du code de réponse (200 = OK)
        if (response.statusCode() != 200) {
            System.err.println("Erreur API Groq : " + response.body());
            return "NONE";
        }

        return parseResponse(response.body());
    }

    private String parseResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            String content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
            
            // On retire les éventuels guillemets que l'IA pourrait ajouter
            return content.replace("\"", "");
        } catch (Exception e) {
            return "NONE";
        }
    }
}