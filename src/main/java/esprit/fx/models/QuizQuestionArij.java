package esprit.fx.models;

import java.util.List;

/**
 * Classe DTO représentant une question de quiz santé.
 * Contient la question, la bonne réponse et toutes les réponses mélangées.
 */
public class QuizQuestionArij {
    public String question;
    public String bonneReponse;
    public List<String> toutesLesReponses; // Bonne réponse + mauvaises, mélangées
    public String difficulte; // easy, medium, hard

    public QuizQuestionArij(String question, String bonneReponse, List<String> toutesLesReponses, String difficulte) {
        this.question = question;
        this.bonneReponse = bonneReponse;
        this.toutesLesReponses = toutesLesReponses;
        this.difficulte = difficulte;
    }

    @Override
    public String toString() {
        return "QuizQuestion{" +
                "question='" + question + '\'' +
                ", bonneReponse='" + bonneReponse + '\'' +
                ", difficulte='" + difficulte + '\'' +
                '}';
    }
}
