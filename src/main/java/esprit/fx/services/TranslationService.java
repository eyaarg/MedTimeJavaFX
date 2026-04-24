package esprit.fx.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Service de traduction via l'API gratuite MyMemory.
 * Aucune clé API requise (limite : 5000 mots/jour).
 */
public class TranslationService {

    public enum Langue {
        FRANCAIS ("fr", "🇫🇷 Français"),
        ANGLAIS  ("en", "🇬🇧 Anglais"),
        ARABE    ("ar", "🇸🇦 Arabe"),
        ESPAGNOL ("es", "🇪🇸 Espagnol"),
        ALLEMAND ("de", "🇩🇪 Allemand");

        public final String code;
        public final String label;
        Langue(String code, String label) { this.code = code; this.label = label; }

        @Override public String toString() { return label; }
    }

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private final HttpClient client;

    public TranslationService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String traduire(String texte, Langue source, Langue cible) {
        if (texte == null || texte.isBlank()) return "";
        if (source == cible) return texte;

        String texteTronque = texte.length() > 500 ? texte.substring(0, 500) + "…" : texte;

        try {
            String encoded  = URLEncoder.encode(texteTronque, StandardCharsets.UTF_8);
            String langPair = source.code + "|" + cible.code;
            String url      = API_URL + "?q=" + encoded + "&langpair=" + langPair;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTranslation(response.body());
            } else {
                return "❌ Erreur API (" + response.statusCode() + ")";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "❌ Traduction interrompue";
        } catch (Exception e) {
            return "❌ Erreur : " + e.getMessage();
        }
    }

    private String parseTranslation(String json) {
        try {
            String key = "\"translatedText\":\"";
            int start = json.indexOf(key);
            if (start == -1) return "❌ Réponse inattendue";
            start += key.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return "❌ Réponse inattendue";
            return decodeUnicode(json.substring(start, end));
        } catch (Exception e) {
            return "❌ Erreur de parsing";
        }
    }

    private String decodeUnicode(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (i + 5 < s.length() && s.charAt(i) == '\\' && s.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(s.charAt(i++));
                }
            } else {
                sb.append(s.charAt(i++));
            }
        }
        return sb.toString();
    }
}
