package esprit.fx.services;

import esprit.fx.utils.MyDB;

import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service de planification des articles.
 * Vérifie toutes les 60 secondes si des articles planifiés
 * doivent être publiés (date_publication <= NOW() et statut = 'planifié').
 */
public class SchedulerService {

    private static SchedulerService instance;
    private final ScheduledExecutorService executor;
    private boolean running = false;

    private SchedulerService() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ArticleScheduler");
            t.setDaemon(true); // ne bloque pas la fermeture de l'app
            return t;
        });
    }

    public static SchedulerService getInstance() {
        if (instance == null) instance = new SchedulerService();
        return instance;
    }

    /** Démarre le scheduler — à appeler au lancement de l'application. */
    public void start() {
        if (running) return;
        running = true;
        executor.scheduleAtFixedRate(this::publierArticlesPlanifies, 0, 60, TimeUnit.SECONDS);
        System.out.println("✅ SchedulerService démarré — vérification toutes les 60s");
    }

    /** Publie tous les articles planifiés dont la date est passée. */
    private void publierArticlesPlanifies() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            // Trouver les articles planifiés dont la date est arrivée
            String sqlSelect = "SELECT id, titre FROM article WHERE statut = 'planifié' AND date_creation <= NOW()";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sqlSelect);

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String titre = rs.getString("titre");
                // Passer le statut à "publié"
                String sqlUpdate = "UPDATE article SET statut = 'publié' WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(sqlUpdate);
                ps.setInt(1, id);
                ps.executeUpdate();
                System.out.println("📰 Article publié automatiquement : [" + id + "] " + titre);
                count++;
            }
            if (count > 0) System.out.println("✅ " + count + " article(s) publié(s) automatiquement");

        } catch (SQLException e) {
            System.err.println("❌ Erreur scheduler : " + e.getMessage());
        }
    }

    /** Retourne le nombre d'articles en attente de publication. */
    public int getNbArticlesPlanifies() {
        try {
            Connection con = MyDB.getInstance().getConnection();
            String sql = "SELECT COUNT(*) FROM article WHERE statut = 'planifié' AND date_publication > NOW()";
            ResultSet rs = con.createStatement().executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getNbArticlesPlanifies : " + e.getMessage());
        }
        return 0;
    }

    public void stop() {
        executor.shutdown();
        running = false;
    }
}
