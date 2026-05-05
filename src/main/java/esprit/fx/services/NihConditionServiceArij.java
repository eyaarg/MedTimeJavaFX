package esprit.fx.services;

import org.json.JSONArray;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'autocomplétion médicale via l'API NIH Clinical Tables.
 *
 * API gratuite, aucune clé requise.
 * URL : https://clinicaltables.nlm.nih.gov/api/conditions/v3/search?terms={MOT_CLE}&maxList=10
 *
 * Format de réponse JSON :
 *   [0] → nombre total de résultats (int)
 *   [1] → array de codes ICD-10
 *   [2] → null
 *   [3] → array de arrays → chaque sous-array[0] = nom de la maladie
 */
public class NihConditionServiceArij {

    // ------------------------------------------------------------------ //
    //  Classe interne DTO                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Représente une maladie retournée par l'API NIH.
     */
    public static class MaladieDto {
        public final String nom;
        public final String codeIcd10;

        public MaladieDto(String nom, String codeIcd10) {
            this.nom       = nom       != null ? nom       : "";
            this.codeIcd10 = codeIcd10 != null ? codeIcd10 : "";
        }

        /** Affiché dans la ListView JavaFX */
        @Override
        public String toString() {
            return codeIcd10.isBlank()
                    ? nom
                    : nom + "  [" + codeIcd10 + "]";
        }
    }

    // ------------------------------------------------------------------ //
    //  Constantes                                                         //
    // ------------------------------------------------------------------ //

    private static final String BASE_URL =
            "https://clinicaltables.nlm.nih.gov/api/conditions/v3/search";
    private static final int MAX_RESULTS = 10;

    // HttpClient réutilisable (thread-safe)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ------------------------------------------------------------------ //
    //  Méthode principale                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Recherche des maladies correspondant au mot-clé via l'API NIH.
     *
     * Doit être appelé depuis un thread background (Task<>) — jamais sur le thread UI.
     *
     * @param motCle terme de recherche (min. 3 caractères recommandé)
     * @return liste de MaladieDto, vide si erreur ou aucun résultat
     */
    public List<MaladieDto> rechercherMaladies(String motCle) {
        List<MaladieDto> resultats = new ArrayList<>();

        // Validation basique
        if (motCle == null || motCle.trim().length() < 2) {
            return resultats;
        }

        try {
            // Construction de l'URL avec encodage du mot-clé
            String termsEncoded = URLEncoder.encode(motCle.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "?terms=" + termsEncoded + "&maxList=" + MAX_RESULTS;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[NihConditionServiceArij] HTTP " + response.statusCode());
                return resultats;
            }

            // Parsing de la réponse JSON
            resultats = parseReponse(response.body());

        } catch (Exception e) {
            // Ne jamais propager l'exception — retourner liste vide
            System.err.println("[NihConditionServiceArij] Erreur API NIH : " + e.getMessage());
        }

        return resultats;
    }

    // ------------------------------------------------------------------ //
    //  Parsing JSON                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Parse la réponse JSON de l'API NIH.
     *
     * Structure attendue :
     *   [ totalCount, [code1, code2, ...], null, [[nom1, ...], [nom2, ...], ...] ]
     */
    private List<MaladieDto> parseReponse(String json) {
        List<MaladieDto> liste = new ArrayList<>();

        try {
            JSONArray root = new JSONArray(json);

            // Vérification de la structure minimale
            if (root.length() < 4) return liste;

            // [1] → codes ICD-10
            JSONArray codes = root.optJSONArray(1);

            // [3] → tableau de tableaux de noms
            JSONArray nomsTableaux = root.optJSONArray(3);

            if (codes == null || nomsTableaux == null) return liste;

            int count = Math.min(codes.length(), nomsTableaux.length());

            for (int i = 0; i < count; i++) {
                String code = codes.optString(i, "");

                // Chaque élément de [3] est un tableau → [0] = nom de la maladie
                JSONArray nomArray = nomsTableaux.optJSONArray(i);
                String nom = (nomArray != null && nomArray.length() > 0)
                        ? nomArray.optString(0, "")
                        : "";

                if (!nom.isBlank()) {
                    liste.add(new MaladieDto(nom, code));
                }
            }

        } catch (Exception e) {
            System.err.println("[NihConditionServiceArij] Erreur parsing JSON : " + e.getMessage());
        }

        return liste;
    }
}
