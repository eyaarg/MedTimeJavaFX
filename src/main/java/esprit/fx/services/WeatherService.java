package esprit.fx.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service d'appel à l'API OpenWeatherMap (forecast 5 jours).
 * Clé gratuite : jusqu'à 1000 appels/jour.
 */
public class WeatherService {

    private static final String API_KEY  = "VOTRE_CLE_API_ICI"; // ← remplacer par ta clé
    private static final String CITY     = "Tunis";
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/forecast"
            + "?q=" + CITY
            + "&appid=" + API_KEY
            + "&units=metric"
            + "&lang=fr"
            + "&cnt=40";

    /** Résultat météo simplifié. */
    public static class MeteoResult {
        public final double temperature;
        public final String description;
        public final String icone;       // code icône OWM (ex: "01d")
        public final String emoji;       // emoji correspondant
        public final String conseil;     // conseil contextuel

        public MeteoResult(double temperature, String description,
                           String icone, String emoji, String conseil) {
            this.temperature = temperature;
            this.description = description;
            this.icone       = icone;
            this.emoji       = emoji;
            this.conseil     = conseil;
        }

        /** Texte complet à afficher sous le DatePicker. */
        public String toDisplayString(LocalDate date) {
            String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String base = String.format("📅 Le %s : %s %.0f°C - %s",
                    dateStr, emoji, temperature,
                    capitalize(description));
            return conseil.isEmpty() ? base : base + "\n" + conseil;
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    /**
     * Récupère la météo prévue pour {@code date} à {@link #CITY}.
     * Utilise le créneau le plus proche de midi (12h00).
     *
     * @return MeteoResult ou null si la date est hors plage / erreur réseau
     */
    public MeteoResult getMeteo(LocalDate date) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("WeatherService HTTP " + response.statusCode());
                return null;
            }

            return parseResponse(response.body(), date);

        } catch (Exception e) {
            System.err.println("WeatherService erreur: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Parsing JSON
    // -------------------------------------------------------------------------

    private MeteoResult parseResponse(String json, LocalDate targetDate) {
        JSONObject root = new JSONObject(json);
        JSONArray list = root.getJSONArray("list");

        String targetPrefix = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        JSONObject best = null;
        // Chercher le créneau le plus proche de 12h pour la date cible
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            String dtTxt = entry.getString("dt_txt"); // "2025-05-15 12:00:00"
            if (dtTxt.startsWith(targetPrefix)) {
                best = entry;
                if (dtTxt.contains("12:00:00")) break; // créneau midi = idéal
            }
        }

        if (best == null) return null; // date hors plage des 5 jours

        double temp = best.getJSONObject("main").getDouble("temp");
        JSONObject weather = best.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        String icone       = weather.getString("icon");
        String mainWeather = weather.getString("main").toLowerCase();

        String emoji  = resolveEmoji(icone, mainWeather);
        String conseil = resolveConseil(mainWeather, temp);

        return new MeteoResult(temp, description, icone, emoji, conseil);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveEmoji(String icon, String main) {
        if (icon.startsWith("01")) return "☀️";
        if (icon.startsWith("02")) return "🌤️";
        if (icon.startsWith("03") || icon.startsWith("04")) return "☁️";
        if (icon.startsWith("09") || icon.startsWith("10")) return "🌧️";
        if (icon.startsWith("11")) return "⛈️";
        if (icon.startsWith("13")) return "❄️";
        if (icon.startsWith("50")) return "🌫️";
        return "🌡️";
    }

    private String resolveConseil(String main, double temp) {
        if (main.contains("rain") || main.contains("drizzle"))
            return "☂️ Pensez à prendre un parapluie !";
        if (main.contains("thunderstorm"))
            return "⚡ Orage prévu, restez prudent !";
        if (main.contains("snow"))
            return "🧥 Couvrez-vous bien, neige prévue !";
        if (main.contains("fog") || main.contains("mist"))
            return "🌫️ Visibilité réduite, conduisez prudemment.";
        if (temp >= 35)
            return "🥵 Forte chaleur, hydratez-vous bien !";
        if (temp <= 5)
            return "🥶 Températures froides, habillez-vous chaudement !";
        return "";
    }
}
