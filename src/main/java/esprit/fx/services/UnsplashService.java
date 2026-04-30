package esprit.fx.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Service de génération d'images via l'API Unsplash.
 * Clé gratuite : 5000 requêtes/heure.
 * Sans clé    : 50 requêtes/heure (mode demo).
 *
 * Matching intelligent :
 *   1. Extrait les mots-clés du titre
 *   2. Ajoute les mots-clés de la spécialité médicale
 *   3. Appel Unsplash → meilleure image
 *   4. Fallback : image générique médicale si aucun résultat
 */
public class UnsplashService {

    // ── Remplace par ta clé Unsplash (gratuite sur unsplash.com/developers) ──
    private static final String ACCESS_KEY = "27fji82YeuR4Z9Rk7Vi9QnKEX5H6fLJB8_ryaaG3YCY";
    private static final String API_URL    = "https://api.unsplash.com/search/photos";

    // Mots-clés médicaux par spécialité — fallback intelligent
    private static final Map<String, String> SPECIALITE_KEYWORDS = new LinkedHashMap<>();
    static {
        SPECIALITE_KEYWORDS.put("cardiologie",      "heart cardiology medical");
        SPECIALITE_KEYWORDS.put("neurologie",       "brain neurology medical");
        SPECIALITE_KEYWORDS.put("pédiatrie",        "children pediatrics doctor");
        SPECIALITE_KEYWORDS.put("dermatologie",     "skin dermatology medical");
        SPECIALITE_KEYWORDS.put("orthopédie",       "bones orthopedics surgery");
        SPECIALITE_KEYWORDS.put("ophtalmologie",    "eye ophthalmology vision");
        SPECIALITE_KEYWORDS.put("gynécologie",      "women health gynecology");
        SPECIALITE_KEYWORDS.put("oncologie",        "cancer oncology research");
        SPECIALITE_KEYWORDS.put("psychiatrie",      "mental health psychology");
        SPECIALITE_KEYWORDS.put("radiologie",       "xray radiology scan");
        SPECIALITE_KEYWORDS.put("chirurgie",        "surgery operating room");
        SPECIALITE_KEYWORDS.put("médecine générale","doctor hospital medical");
        SPECIALITE_KEYWORDS.put("urgences",         "emergency hospital ambulance");
        SPECIALITE_KEYWORDS.put("pharmacologie",    "pharmacy medicine pills");
        SPECIALITE_KEYWORDS.put("nutrition",        "healthy food nutrition diet");
    }

    // Fallback final si tout échoue
    private static final String FALLBACK_URL =
        "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800";

    private final HttpClient client;

    public UnsplashService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Génère une image adaptée au contexte de l'article.
     * @param titre      titre de l'article
     * @param specialite nom de la spécialité (ex: "cardiologie")
     * @return URL de l'image Unsplash, ou URL de fallback
     */
    public String genererImage(String titre, String specialite) {
        // 1. Construire la requête de recherche
        String query = buildQuery(titre, specialite);
        System.out.println("🔍 Unsplash query: " + query);

        // 2. Appel API
        String url = rechercherImage(query);
        if (url != null) return url;

        // 3. Fallback : chercher uniquement avec la spécialité
        if (specialite != null && !specialite.isBlank()) {
            String fallbackQuery = getSpecialiteKeywords(specialite);
            url = rechercherImage(fallbackQuery);
            if (url != null) return url;
        }

        // 4. Fallback final : image médicale générique
        return FALLBACK_URL;
    }

    /**
     * Construit la requête de recherche en combinant titre + spécialité.
     */
    private String buildQuery(String titre, String specialite) {
        List<String> parts = new ArrayList<>();

        // Mots-clés de la spécialité
        if (specialite != null && !specialite.isBlank()) {
            String kw = getSpecialiteKeywords(specialite);
            if (!kw.isBlank()) parts.add(kw);
        }

        // Mots significatifs du titre (> 4 lettres, pas de mots vides)
        if (titre != null && !titre.isBlank()) {
            Set<String> stopWords = Set.of("pour", "dans", "avec", "les", "des",
                "une", "the", "and", "for", "with", "that", "this");
            Arrays.stream(titre.toLowerCase().split("\\s+"))
                  .filter(w -> w.length() > 4 && !stopWords.contains(w))
                  .limit(3)
                  .forEach(parts::add);
        }

        // Toujours ajouter "medical" pour rester dans le contexte
        parts.add("medical");

        return String.join(" ", parts);
    }

    private String getSpecialiteKeywords(String specialite) {
        if (specialite == null) return "medical doctor";
        String lower = specialite.toLowerCase();
        for (Map.Entry<String, String> e : SPECIALITE_KEYWORDS.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return "medical doctor hospital";
    }

    /**
     * Appelle l'API Unsplash et retourne l'URL de la première image.
     */
    private String rechercherImage(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String apiUrl  = API_URL + "?query=" + encoded + "&per_page=1&orientation=landscape";

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            // Ajouter la clé si elle est configurée
            if (!ACCESS_KEY.equals("UNSPLASH_ACCESS_KEY") && !ACCESS_KEY.isBlank()) {
                builder.header("Authorization", "Client-ID " + ACCESS_KEY);
            } else {
                // Mode demo — limité à 50 req/h
                builder.header("Authorization", "Client-ID demo");
            }

            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseImageUrl(response.body());
            }
            System.err.println("Unsplash status: " + response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Unsplash erreur: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse le JSON Unsplash pour extraire l'URL de l'image.
     * Format : {"results":[{"urls":{"regular":"https://..."},...},...]}
     */
    private String parseImageUrl(String json) {
        try {
            // Chercher "regular":"..."
            String key = "\"regular\":\"";
            int start = json.indexOf(key);
            if (start == -1) return null;
            start += key.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            String url = json.substring(start, end);
            // Ajouter paramètre de taille
            if (!url.contains("w=")) url += (url.contains("?") ? "&" : "?") + "w=800";
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    /** Vérifie si une clé API est configurée */
    public boolean hasApiKey() {
        return !ACCESS_KEY.equals("UNSPLASH_ACCESS_KEY") && !ACCESS_KEY.isBlank();
    }
}
