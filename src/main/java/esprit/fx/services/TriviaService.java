package esprit.fx.services;

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
public class TriviaService {

    private static final String API_URL = "https://opentdb.com/api.php?amount=5&category=17&type=multiple";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Classe interne représentant une question de quiz.
     */
    public static class QuizQuestion {
        public String question;
        public String bonneReponse;
        public List<String> toutesLesReponses; // Bonne réponse + mauvaises, mélangées
        public String difficulte;

        public QuizQuestion(String question, String bonneReponse, List<String> toutesLesReponses, String difficulte) {
            this.question = question;
            this.bonneReponse = bonneReponse;
            this.toutesLesReponses = toutesLesReponses;
            this.difficulte = difficulte;
        }
    }

    /**
     * Récupère 5 questions de quiz santé depuis l'API Open Trivia DB.
     * 
     * @return Liste de 5 QuizQuestion
     * @throws Exception si erreur réseau
     */
    public List<QuizQuestion> getQuestions() throws Exception {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[TriviaService] Erreur API: " + response.statusCode());
                return getFallbackQuestions();
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            int responseCode = jsonResponse.getInt("response_code");

            if (responseCode != 0) {
                System.err.println("[TriviaService] Erreur API response_code: " + responseCode);
                return getFallbackQuestions();
            }

            JSONArray results = jsonResponse.getJSONArray("results");
            List<QuizQuestion> questions = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

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

                questions.add(new QuizQuestion(question, bonneReponse, toutesLesReponses, difficulte));
            }

            System.out.println("[TriviaService] " + questions.size() + " questions chargées avec succès");
            return questions;

        } catch (Exception e) {
            System.err.println("[TriviaService] Erreur lors de la récupération des questions: " + e.getMessage());
            return getFallbackQuestions();
        }
    }

    /**
     * Décode les entités HTML manuellement.
     * Remplace &amp; → & &quot; → " &#039; → ' &lt; → < &gt; → >
     */
    private String unescapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'");
    }

    /**
     * Retourne 3 questions de fallback en français si l'API est indisponible.
     */
    private List<QuizQuestion> getFallbackQuestions() {
        List<QuizQuestion> fallback = new ArrayList<>();

        // Question 1
        List<String> reponses1 = new ArrayList<>();
        reponses1.add("37°C");
        reponses1.add("36°C");
        reponses1.add("38°C");
        reponses1.add("39°C");
        Collections.shuffle(reponses1);
        fallback.add(new QuizQuestion(
                "Quelle est la température corporelle normale d'un adulte?",
                "37°C",
                reponses1,
                "easy"
        ));

        // Question 2
        List<String> reponses2 = new ArrayList<>();
        reponses2.add("206");
        reponses2.add("186");
        reponses2.add("226");
        reponses2.add("246");
        Collections.shuffle(reponses2);
        fallback.add(new QuizQuestion(
                "Combien d'os possède le corps humain adulte?",
                "206",
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
        fallback.add(new QuizQuestion(
                "Quel type de cellule sanguine transporte l'oxygène?",
                "Globules rouges",
                reponses3,
                "easy"
        ));

        System.out.println("[TriviaService] Utilisation des questions de fallback");
        return fallback;
    }
}
