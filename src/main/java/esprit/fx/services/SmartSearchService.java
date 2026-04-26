package esprit.fx.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SmartSearchService {

    // Remplace par ta vraie clé Groq
    private final String API_KEY = "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";
    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String askGroq(String userInput, List<String> availableProducts) throws Exception {
        // 1. On prépare le message pour l'IA (Le Prompt)
        String systemMessage = "Tu es un assistant de vente. Voici les produits disponibles : " + availableProducts.toString();
        String userMessage = "L'utilisateur cherche : '" + userInput + "'. Réponds UNIQUEMENT avec le nom exact du produit le plus proche parmi la liste. Si rien ne correspond, réponds 'NONE'.";

        // 2. On construit le corps de la requête en JSON (Format Standard)
        String jsonBody = """
            {
                "model": "llama3-8b-8192",
                "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                ],
                "temperature": 0.1
            }
            """.formatted(systemMessage, userMessage);

        // 3. On envoie la requête avec le HttpClient de Java
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. On extrait la réponse (Nettoyage simple sans bibliothèque externe)
        return parseResponse(response.body());
    }

    private String parseResponse(String responseBody) {
        // On cherche le contenu du message dans le JSON de retour
        try {
            int start = responseBody.indexOf("\"content\":\"") + 11;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end).trim();
        } catch (Exception e) {
            return "NONE";
        }
    }
}