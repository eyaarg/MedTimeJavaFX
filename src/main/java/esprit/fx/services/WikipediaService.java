package esprit.fx.services;

import esprit.fx.entities.WikipediaResult;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Service pour récupérer les fiches maladie depuis Wikipedia.
 * API gratuite, aucune clé requise.
 * 
 * URLs:
 * - Français: https://fr.wikipedia.org/api/rest_v1/page/summary/{TITRE}
 * - Anglais: https://en.wikipedia.org/api/rest_v1/page/summary/{TITRE}
 */
public class WikipediaService {

    private static final String API_URL_FR = "https://fr.wikipedia.org/api/rest_v1/page/summary/";
    private static final String API_URL_EN = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final int EXTRACT_MAX_LENGTH = 400;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Cherche une maladie sur Wikipedia dans la langue spécifiée.
     * 
     * @param maladie Nom de la maladie à chercher
     * @param langue "fr" pour français, "en" pour anglais
     * @return WikipediaResult avec les informations trouvées
     */
    public WikipediaResult chercher(String maladie, String langue) {
        if (maladie == null || maladie.trim().isEmpty()) {
            return new WikipediaResult();
        }

        try {
            // Encoder le nom de la maladie
            String maladeieEncodee = URLEncoder.encode(maladie.trim(), StandardCharsets.UTF_8);
            
            // Construire l'URL selon la langue
            String apiUrl = "fr".equalsIgnoreCase(langue) ? API_URL_FR : API_URL_EN;
            String urlComplete = apiUrl + maladeieEncodee;

            // Créer la requête HTTP avec User-Agent obligatoire pour Wikipedia API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlComplete))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "MedTimeJavaFX/1.0 (esprit.fx; educational project) Java/21")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // Envoyer la requête
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Vérifier le code de réponse
            if (response.statusCode() == 404) {
                System.out.println("[WikipediaService] Maladie non trouvée: " + maladie + " (" + langue + ")");
                return new WikipediaResult();
            }

            if (response.statusCode() != 200) {
                System.err.println("[WikipediaService] Erreur API: " + response.statusCode());
                return new WikipediaResult();
            }

            // Parser la réponse JSON
            JSONObject json = new JSONObject(response.body());

            // Vérifier si l'extract est vide
            String extract = json.optString("extract", "").trim();
            if (extract.isEmpty()) {
                System.out.println("[WikipediaService] Extract vide pour: " + maladie);
                return new WikipediaResult();
            }

            // Récupérer les informations
            String titre = json.optString("title", maladie);
            String resume = tronquerExtract(extract);
            String imageUrl = null;
            String urlArticle = json.optJSONObject("content_urls")
                    .optJSONObject("desktop")
                    .optString("page", "");

            // Récupérer l'image si disponible
            if (json.has("thumbnail")) {
                JSONObject thumbnail = json.getJSONObject("thumbnail");
                imageUrl = thumbnail.optString("source", null);
            }

            System.out.println("[WikipediaService] Maladie trouvée: " + titre + " (" + langue + ")");
            return new WikipediaResult(titre, resume, imageUrl, urlArticle);

        } catch (Exception e) {
            System.err.println("[WikipediaService] Erreur lors de la recherche: " + e.getMessage());
            return new WikipediaResult();
        }
    }

    /**
     * Cherche une maladie avec fallback: essayer FR → EN → retourner not found.
     * 
     * @param maladie Nom de la maladie à chercher
     * @return WikipediaResult avec les informations trouvées
     */
    public WikipediaResult chercherAvecFallback(String maladie) {
        if (maladie == null || maladie.trim().isEmpty()) {
            return new WikipediaResult();
        }

        // Essayer en français d'abord
        WikipediaResult resultat = chercher(maladie, "fr");
        if (resultat.trouve) {
            return resultat;
        }

        // Si pas trouvé en français, essayer en anglais
        System.out.println("[WikipediaService] Fallback vers l'anglais pour: " + maladie);
        resultat = chercher(maladie, "en");
        if (resultat.trouve) {
            return resultat;
        }

        // Si toujours pas trouvé, retourner not found
        System.out.println("[WikipediaService] Maladie non trouvée dans aucune langue: " + maladie);
        return new WikipediaResult();
    }

    /**
     * Tronque l'extrait à 400 caractères maximum.
     * 
     * @param extract Texte à tronquer
     * @return Texte tronqué avec "..." à la fin si nécessaire
     */
    private String tronquerExtract(String extract) {
        if (extract == null || extract.length() <= EXTRACT_MAX_LENGTH) {
            return extract;
        }

        // Tronquer et ajouter "..."
        String tronque = extract.substring(0, EXTRACT_MAX_LENGTH).trim();
        
        // Trouver le dernier espace pour ne pas couper un mot
        int dernierEspace = tronque.lastIndexOf(' ');
        if (dernierEspace > 0) {
            tronque = tronque.substring(0, dernierEspace);
        }

        return tronque + "...";
    }
}
