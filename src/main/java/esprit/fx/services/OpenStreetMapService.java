package esprit.fx.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Service de géocodage via l'API Nominatim (OpenStreetMap).
 * 100% gratuit — aucune clé API requise.
 */
public class OpenStreetMapService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search";

    /** Coordonnées GPS retournées par Nominatim. */
    public static class Coordonnees {
        public final double latitude;
        public final double longitude;
        public final String adresseComplete;

        public Coordonnees(double latitude, double longitude, String adresseComplete) {
            this.latitude      = latitude;
            this.longitude     = longitude;
            this.adresseComplete = adresseComplete;
        }
    }

    /**
     * Convertit une adresse textuelle en coordonnées GPS.
     *
     * @param adresse ex: "15 Rue de la Paix, Tunis"
     * @return Coordonnees ou null si introuvable
     */
    public Coordonnees getCoordonnees(String adresse) {
        try {
            String query = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String url   = NOMINATIM_URL + "?q=" + query
                         + "&format=json&limit=1&addressdetails=1";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    // Nominatim exige un User-Agent identifiable
                    .header("User-Agent", "MedTimeFX/1.0 (contact@medtime.tn)")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Nominatim HTTP " + response.statusCode());
                return null;
            }

            JSONArray results = new JSONArray(response.body());
            if (results.isEmpty()) {
                System.err.println("Nominatim: aucun résultat pour → " + adresse);
                return null;
            }

            JSONObject first = results.getJSONObject(0);
            double lat        = Double.parseDouble(first.getString("lat"));
            double lon        = Double.parseDouble(first.getString("lon"));
            String displayName = first.getString("display_name");

            return new Coordonnees(lat, lon, displayName);

        } catch (Exception e) {
            System.err.println("OpenStreetMapService erreur: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construit l'adresse de recherche à partir du nom et de l'adresse du médecin.
     * Fallback sur "Tunis, Tunisie" si adresse vide.
     */
    public String buildAdresseRecherche(String doctorNom, String adresse) {
        if (adresse != null && !adresse.trim().isEmpty()) {
            return adresse.trim() + ", Tunisie";
        }
        // Fallback : chercher juste "Tunis" si pas d'adresse
        return "Tunis, Tunisie";
    }
}
