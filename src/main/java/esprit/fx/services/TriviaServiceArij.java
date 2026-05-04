package esprit.fx.services;

import esprit.fx.models.QuizQuestionArij;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Service pour récupérer les questions de quiz santé depuis l'API Open Trivia DB.
 * API gratuite, aucune clé requise.
 * 
 * URL: https://opentdb.com/api.php?amount=5&category=17&type=multiple
 * Category 17 = Science & Nature (questions santé/médecine)
 */
public class TriviaServiceArij {

    private static final String API_URL = "https://opentdb.com/api.php?amount=5&category=17&type=multiple";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Récupère 5 questions de quiz santé depuis l'API Open Trivia DB.
     * 
     * @return Liste de 5 QuizQuestion
     * @throws Exception si erreur réseau
     */
    public List<QuizQuestionArij> getQuestions() throws Exception {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[TriviaServiceArij] Erreur API: " + response.statusCode());
                return getFallbackQuestions();
            }

            JSONObject json = new JSONObject(response.body());
            int responseCode = json.getInt("response_code");

            if (responseCode != 0) {
                System.err.println("[TriviaServiceArij] Erreur API response_code: " + responseCode);
                return getFallbackQuestions();
            }

            JSONArray results = json.getJSONArray("results");
            List<QuizQuestionArij> questions = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                
                // Décoder les HTML entities
                String question = unescapeHtml(result.getString("question"));
                String bonneReponse = unescapeHtml(result.getString("correct_answer"));
                String difficulte = result.getString("difficulty");

                // Récupérer les mauvaises réponses
                JSONArray incorrectAnswers = result.getJSONArray("incorrect_answers");
                List<String> toutesLesReponses = new ArrayList<>();
                toutesLesReponses.add(bonneReponse);

                for (int j = 0; j < incorrectAnswers.length(); j++) {
                    toutesLesReponses.add(unescapeHtml(incorrectAnswers.getString(j)));
                }

                // Mélanger les réponses
                Collections.shuffle(toutesLesReponses);

                questions.add(new QuizQuestionArij(question, bonneReponse, toutesLesReponses, difficulte));
            }

            return questions;

        } catch (Exception e) {
            System.err.println("[TriviaServiceArij] Exception: " + e.getMessage());
            e.printStackTrace();
            return getFallbackQuestions();
        }
    }

    /**
     * Décode les HTML entities manuellement.
     * Remplace &amp; → & &quot; → " &#039; → ' &lt; → < &gt; → >
     */
    private String unescapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ");
    }

    /**
     * Retourne 3 questions de fallback en français si l'API est indisponible.
     */
    private List<QuizQuestionArij> getFallbackQuestions() {
        List<QuizQuestionArij> fallback = new ArrayList<>();

        // Question 1
        List<String> reponses1 = new ArrayList<>();
        reponses1.add("37°C");
        reponses1.add("36°C");
        reponses1.add("38°C");
        reponses1.add("39°C");
        Collections.shuffle(reponses1);
        fallback.add(new QuizQuestionArij(
                "Quelle est la température corporelle normale d'un adulte ?",
                "37°C",
                reponses1,
                "easy"
        ));

        // Question 2
        List<String> reponses2 = new ArrayList<>();
        reponses2.add("Hypertension artérielle");
        reponses2.add("Hypotension artérielle");
        reponses2.add("Arythmie cardiaque");
        reponses2.add("Insuffisance cardiaque");
        Collections.shuffle(reponses2);
        fallback.add(new QuizQuestionArij(
                "Comment appelle-t-on une pression artérielle élevée ?",
                "Hypertension artérielle",
                reponses2,
                "medium"
        ));

        // Question 3
        List<String> reponses3 = new ArrayList<>();
        reponses3.add("Globules rouges");
        reponses3.add("Globules blancs");
        reponses3.add("Plaquettes");
        reponses3.add("Plasma");
        Collections.shuffle(reponses3);
        fallback.add(new QuizQuestionArij(
                "Quel type de cellule sanguine transporte l'oxygène ?",
                "Globules rouges",
                reponses3,
                "easy"
        ));

        System.out.println("[TriviaServiceArij] Utilisation des questions de fallback");
        return fallback;
    }
}
