package esprit.fx.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import esprit.fx.utils.ConfigLoader;
import org.json.JSONObject;
import org.json.JSONArray;

public class VisionService {

    // On récupère les infos depuis le fichier properties
    private static final String API_KEY = ConfigLoader.getProperty("gemini.api.key").trim();
    private static final String BASE_URL = ConfigLoader.getProperty("gemini.url").trim();
    
    private static final String FULL_URL = BASE_URL + "?key=" + API_KEY;

    // ... le reste de ta méthode genererDescription reste identique ...


    public String genererDescription(String base64Image) throws Exception {
        // 1. Construction du JSON selon le format Google Gemini
        JSONObject jsonBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject partText = new JSONObject();
        JSONObject partImage = new JSONObject();
        JSONObject inlineData = new JSONObject();

        // Texte du prompt
        partText.put("text", "Tu es un pharmacien expert. Analyse cette image de produit médical et rédige une description courte (3 phrases max) pour un catalogue. Sois pro et factuel.");

        // Données de l'image
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image); // Gemini veut le Base64 pur, sans le préfixe data:image/jpeg
        partImage.put("inline_data", inlineData);

        JSONArray parts = new JSONArray();
        parts.put(partText);
        parts.put(partImage);

        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", parts);
        contents.put(contentObj);

        jsonBody.put("contents", contents);

        // 2. Envoi de la requête HTTP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FULL_URL)) // <--- C'est ici que ça bloquait
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Erreur Gemini : " + response.body());
            return "Erreur API Gemini : " + response.statusCode();
        }

        return extraireContenu(response.body());
    }

    private String extraireContenu(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            // Gemini renvoie le texte dans candidates[0].content.parts[0].text
            return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();
        } catch (Exception e) {
            System.err.println("Erreur parsing Gemini : " + e.getMessage());
            return "Erreur d'analyse de la réponse IA.";
        }
    }
}