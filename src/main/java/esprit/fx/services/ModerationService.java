package esprit.fx.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service de modération IA basé sur un dataset local (toxic_dataset.csv).
 * Analyse un texte sur 5 dimensions : toxicité, insulte, menace, obscénité, harcèlement.
 * Conçu pour être remplacé/complété par Google Perspective API quand la clé sera disponible.
 */
public class ModerationService {

    public static final double SEUIL_REJET = 0.70; // score au-dessus duquel le commentaire est rejeté

    // Résultat d'une analyse
    public static class AnalyseResult {
        public final double toxicity;
        public final double insult;
        public final double threat;
        public final double obscene;
        public final double harassment;
        public final boolean rejected;
        public final String rejectionReason;
        public final List<String> motsTrouves;

        public AnalyseResult(double toxicity, double insult, double threat,
                             double obscene, double harassment,
                             List<String> motsTrouves) {
            this.toxicity   = toxicity;
            this.insult     = insult;
            this.threat     = threat;
            this.obscene    = obscene;
            this.harassment = harassment;
            this.motsTrouves = motsTrouves;

            // Rejeté si au moins une dimension dépasse le seuil
            this.rejected = toxicity >= SEUIL_REJET || insult >= SEUIL_REJET
                         || threat  >= SEUIL_REJET || obscene >= SEUIL_REJET
                         || harassment >= SEUIL_REJET;

            this.rejectionReason = buildReason(toxicity, insult, threat, obscene, harassment);
        }

        private String buildReason(double tox, double ins, double thr, double obs, double har) {
            if (!rejected) return null;
            List<String> raisons = new ArrayList<>();
            if (tox >= SEUIL_REJET) raisons.add("Toxicité (" + pct(tox) + "%)");
            if (ins >= SEUIL_REJET) raisons.add("Insulte (" + pct(ins) + "%)");
            if (thr >= SEUIL_REJET) raisons.add("Menace (" + pct(thr) + "%)");
            if (obs >= SEUIL_REJET) raisons.add("Obscénité (" + pct(obs) + "%)");
            if (har >= SEUIL_REJET) raisons.add("Harcèlement (" + pct(har) + "%)");
            return String.join(", ", raisons);
        }

        private int pct(double v) { return (int) Math.round(v * 100); }

        /** Résumé lisible pour l'UI */
        public String getSummary() {
            return String.format(
                "🔍 Analyse IA :\n" +
                "  ☣ Toxicité      : %d%%\n" +
                "  🤬 Insulte       : %d%%\n" +
                "  ⚠️  Menace        : %d%%\n" +
                "  🔞 Obscénité     : %d%%\n" +
                "  😤 Harcèlement   : %d%%",
                (int)(toxicity*100), (int)(insult*100),
                (int)(threat*100),   (int)(obscene*100),
                (int)(harassment*100)
            );
        }
    }

    // ── Dataset en mémoire ─────────────────────────────────────────────────
    private record ToxicEntry(String word, double toxicity, double insult,
                               double threat, double obscene, double harassment) {}

    private final List<ToxicEntry> dataset = new ArrayList<>();

    public ModerationService() {
        loadDataset();
    }

    private void loadDataset() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                    getClass().getResourceAsStream("/toxic_dataset.csv")),
                StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length < 7) continue;
                dataset.add(new ToxicEntry(
                    parts[0].trim().toLowerCase(),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()),
                    Double.parseDouble(parts[4].trim()),
                    Double.parseDouble(parts[5].trim()),
                    Double.parseDouble(parts[6].trim())
                ));
            }
            System.out.println("✅ Dataset chargé : " + dataset.size() + " entrées");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement dataset : " + e.getMessage());
        }
    }

    // ── Analyse principale ─────────────────────────────────────────────────
    /**
     * Analyse un texte et retourne les scores sur 5 dimensions.
     * Algorithme : cherche chaque mot/expression du dataset dans le texte,
     * prend le score maximum trouvé pour chaque dimension.
     */
    public AnalyseResult analyser(String texte) {
        if (texte == null || texte.isBlank()) {
            return new AnalyseResult(0, 0, 0, 0, 0, Collections.emptyList());
        }

        String texteLower = texte.toLowerCase().trim();

        double maxTox = 0, maxIns = 0, maxThr = 0, maxObs = 0, maxHar = 0;
        List<String> motsTrouves = new ArrayList<>();

        for (ToxicEntry entry : dataset) {
            if (texteLower.contains(entry.word())) {
                motsTrouves.add(entry.word());
                maxTox = Math.max(maxTox, entry.toxicity());
                maxIns = Math.max(maxIns, entry.insult());
                maxThr = Math.max(maxThr, entry.threat());
                maxObs = Math.max(maxObs, entry.obscene());
                maxHar = Math.max(maxHar, entry.harassment());
            }
        }

        // Bonus : si plusieurs mots toxiques trouvés, augmenter légèrement les scores
        if (motsTrouves.size() > 1) {
            double bonus = Math.min(0.10, motsTrouves.size() * 0.03);
            maxTox = Math.min(1.0, maxTox + bonus);
            maxHar = Math.min(1.0, maxHar + bonus);
        }

        return new AnalyseResult(maxTox, maxIns, maxThr, maxObs, maxHar, motsTrouves);
    }

    /** Retourne le nombre de mots dans le dataset */
    public int getDatasetSize() { return dataset.size(); }
}
