package esprit.fx.services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service Advice Slip - Conseils de santé aléatoires gratuits.
 *
 * API gratuite, aucune clé requise.
 * URL : https://api.adviceslip.com/advice
 *
 * Réponse JSON :
 *   {
 *     "slip": {
 *       "id": 42,
 *       "advice": "Take care of your health and it will take care of you."
 *     }
 *   }
 */
public class AdviceSlipServiceArij {

    private static final String API_URL = "https://api.adviceslip.com/advice";
    private static final String FALLBACK_ADVICE = "Prenez soin de vous aujourd'hui 🌿";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Récupère un conseil de santé aléatoire.
     *
     * @return conseil en anglais, ou fallback si erreur
     */
    public String getConseil() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[AdviceSlipServiceArij] HTTP " + response.statusCode());
                return FALLBACK_ADVICE;
            }

            // Parser JSON
            JSONObject root = new JSONObject(response.body());
            JSONObject slip = root.optJSONObject("slip");

            if (slip != null) {
                String advice = slip.optString("advice", "");
                if (!advice.isBlank()) {
                    return advice;
                }
            }

            return FALLBACK_ADVICE;

        } catch (Exception e) {
            System.err.println("[AdviceSlipServiceArij] Erreur API : " + e.getMessage());
            return FALLBACK_ADVICE;
        }
    }

    /**
     * Traduit un conseil en français (traduction simple).
     *
     * @param conseilEnAnglais conseil à traduire
     * @return conseil traduit
     */
    public String traduireEnFrancais(String conseilEnAnglais) {
        if (conseilEnAnglais == null || conseilEnAnglais.isBlank()) {
            return FALLBACK_ADVICE;
        }

        // Dictionnaire simple de traductions courantes
        String traduit = conseilEnAnglais;

        // Remplacements simples
        traduit = traduit.replaceAll("(?i)take care of your health", "Prenez soin de votre santé");
        traduit = traduit.replaceAll("(?i)drink water", "Buvez de l'eau");
        traduit = traduit.replaceAll("(?i)sleep", "Dormez");
        traduit = traduit.replaceAll("(?i)exercise", "Faites de l'exercice");
        traduit = traduit.replaceAll("(?i)eat healthy", "Mangez sainement");
        traduit = traduit.replaceAll("(?i)stress", "Stress");
        traduit = traduit.replaceAll("(?i)relax", "Détendez-vous");
        traduit = traduit.replaceAll("(?i)smile", "Souriez");
        traduit = traduit.replaceAll("(?i)walk", "Marchez");
        traduit = traduit.replaceAll("(?i)breathe", "Respirez");
        traduit = traduit.replaceAll("(?i)health", "santé");
        traduit = traduit.replaceAll("(?i)body", "corps");
        traduit = traduit.replaceAll("(?i)mind", "esprit");
        traduit = traduit.replaceAll("(?i)happy", "heureux");
        traduit = traduit.replaceAll("(?i)good", "bon");

        return traduit.isBlank() ? conseilEnAnglais : traduit;
    }

    /**
     * Détermine la couleur de fond selon le contenu du conseil.
     *
     * - Alimentation/nutrition → vert (#E8F5E9)
     * - Stress/mental/sommeil → bleu clair (#E3F2FD)
     * - Général → jaune clair (#FFFDE7)
     */
    public String determinerCouleurFond(String conseil) {
        if (conseil == null) {
            return "#FFFDE7";
        }

        String lower = conseil.toLowerCase();

        // Mots-clés alimentation/nutrition
        if (lower.contains("eat") || lower.contains("food") || lower.contains("drink")
                || lower.contains("nutrition") || lower.contains("manger") || lower.contains("nourriture")
                || lower.contains("boire") || lower.contains("fruit") || lower.contains("légume")
                || lower.contains("water") || lower.contains("eau")) {
            return "#E8F5E9"; // vert
        }

        // Mots-clés stress/mental/sommeil
        if (lower.contains("stress") || lower.contains("sleep") || lower.contains("rest")
                || lower.contains("mind") || lower.contains("mental") || lower.contains("anxiety")
                || lower.contains("sommeil") || lower.contains("repos") || lower.contains("esprit")
                || lower.contains("anxiété") || lower.contains("détente") || lower.contains("relax")) {
            return "#E3F2FD"; // bleu clair
        }

        // Général
        return "#FFFDE7"; // jaune clair
    }
}
