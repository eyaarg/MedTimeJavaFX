package esprit.fx.services;

import esprit.fx.entities.Disponibilite;
import esprit.fx.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceDisponibilite implements IService<Disponibilite> {
    private Connection conn;

    public ServiceDisponibilite() {
        conn = MyDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(Disponibilite disponibilite) throws SQLException {
        System.out.println("\n========================================");
        System.out.println("AJOUT DISPONIBILITÃ‰ - DEBUG v2.0 (FIX DATE)");
        System.out.println("Date dÃ©but reÃ§ue: " + disponibilite.getDateDebut());
        System.out.println("Date fin reÃ§ue: " + disponibilite.getDateFin());
        System.out.println("========================================\n");
        
        // D'abord, dÃ©couvrir la structure de la table
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, "availability", null);
        
        List<String> columnNames = new ArrayList<>();
        List<String> requiredColumns = new ArrayList<>();
        
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String isNullable = columns.getString("IS_NULLABLE");
            String columnDef = columns.getString("COLUMN_DEF");
            
            columnNames.add(columnName);
            
            // Si la colonne n'est pas nullable et n'a pas de valeur par dÃ©faut, elle est requise
            if ("NO".equals(isNullable) && (columnDef == null || columnDef.isEmpty())) {
                if (!"id".equalsIgnoreCase(columnName)) { // Ignorer l'ID auto-increment
                    requiredColumns.add(columnName);
                }
            }
        }
        columns.close();
        
        System.out.println("Colonnes disponibles dans 'availability': " + columnNames);
        System.out.println("Colonnes requises: " + requiredColumns);
        
        // Construire la requÃªte dynamiquement
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO availability (");
        StringBuilder valuesBuilder = new StringBuilder(" VALUES (");
        
        List<String> fieldsToInsert = new ArrayList<>();
        
        // Ajouter les colonnes que nous connaissons
        if (columnNames.contains("doctor_id")) {
            fieldsToInsert.add("doctor_id");
        }
        
        // IMPORTANT: Pour les dates complÃ¨tes, on doit TOUJOURS sÃ©parer date et heure
        // Les colonnes start_date et end_date sont OBLIGATOIRES
        boolean hasStartDate = columnNames.contains("start_date");
        boolean hasEndDate = columnNames.contains("end_date");
        boolean hasStartTime = columnNames.contains("start_time");
        boolean hasEndTime = columnNames.contains("end_time");
        
        System.out.println("Colonnes de date dÃ©tectÃ©es: start_date=" + hasStartDate + ", end_date=" + hasEndDate + 
                          ", start_time=" + hasStartTime + ", end_time=" + hasEndTime);
        
        if (hasStartDate) {
            fieldsToInsert.add("start_date");
            System.out.println("  â†’ Ajout de start_date Ã  la requÃªte");
        }
        if (hasStartTime) {
            fieldsToInsert.add("start_time");
            System.out.println("  â†’ Ajout de start_time Ã  la requÃªte");
        }
        if (hasEndDate) {
            fieldsToInsert.add("end_date");
            System.out.println("  â†’ Ajout de end_date Ã  la requÃªte");
        }
        if (hasEndTime) {
            fieldsToInsert.add("end_time");
            System.out.println("  â†’ Ajout de end_time Ã  la requÃªte");
        }
        
        // Colonnes supplÃ©mentaires
        if (columnNames.contains("is_online")) {
            fieldsToInsert.add("is_online");
        }
        if (columnNames.contains("notes")) {
            fieldsToInsert.add("notes");
        }
        if (columnNames.contains("created_at")) {
            fieldsToInsert.add("created_at");
        }
        
        // Ajouter des valeurs par dÃ©faut pour les colonnes requises manquantes
        for (String reqCol : requiredColumns) {
            if (!fieldsToInsert.contains(reqCol)) {
                fieldsToInsert.add(reqCol);
                System.out.println("âš  Ajout de colonne requise avec valeur par dÃ©faut: " + reqCol);
            }
        }
        
        System.out.println("âœ“ LISTE FINALE DES COLONNES Ã€ INSÃ‰RER: " + fieldsToInsert);
        
        for (int i = 0; i < fieldsToInsert.size(); i++) {
            sqlBuilder.append(fieldsToInsert.get(i));
            valuesBuilder.append("?");
            if (i < fieldsToInsert.size() - 1) {
                sqlBuilder.append(", ");
                valuesBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(")");
        valuesBuilder.append(")");
        String sql = sqlBuilder.toString() + valuesBuilder.toString();
        
        System.out.println("RequÃªte SQL gÃ©nÃ©rÃ©e: " + sql);
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            
            for (String field : fieldsToInsert) {
                switch (field.toLowerCase()) {
                    case "doctor_id":
                        ps.setInt(paramIndex++, disponibilite.getDoctorId());
                        break;
                    case "start_date":
                        if (disponibilite.getDateDebut() != null) {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(disponibilite.getDateDebut().toLocalDate()));
                            System.out.println("  â†’ start_date = " + disponibilite.getDateDebut().toLocalDate());
                        } else {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()));
                        }
                        break;
                    case "end_date":
                        if (disponibilite.getDateFin() != null) {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(disponibilite.getDateFin().toLocalDate()));
                            System.out.println("  â†’ end_date = " + disponibilite.getDateFin().toLocalDate());
                        } else {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()));
                        }
                        break;
                    case "start_time":
                        if (disponibilite.getDateDebut() != null) {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(disponibilite.getDateDebut().toLocalTime()));
                            System.out.println("  â†’ start_time = " + disponibilite.getDateDebut().toLocalTime());
                        } else {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(LocalDateTime.now().toLocalTime()));
                        }
                        break;
                    case "end_time":
                        if (disponibilite.getDateFin() != null) {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(disponibilite.getDateFin().toLocalTime()));
                            System.out.println("  â†’ end_time = " + disponibilite.getDateFin().toLocalTime());
                        } else {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(LocalDateTime.now().plusHours(1).toLocalTime()));
                        }
                        break;
                    case "start_at":
                    case "end_at":
                        // Ces colonnes sont de type TIME, on met juste l'heure
                        ps.setTime(paramIndex++, java.sql.Time.valueOf("00:00:00"));
                        break;
                    case "is_online":
                        // IMPORTANT: is_online semble Ãªtre l'inverse de estDisponible
                        // Si disponible = true, alors is_online = false (consultation en cabinet)
                        // Si disponible = false, alors is_online = true (consultation en ligne)
                        boolean isOnlineValue = !disponibilite.isEstDisponible();
                        ps.setBoolean(paramIndex++, isOnlineValue);
                        System.out.println("  â†’ is_online = " + isOnlineValue + " (estDisponible = " + disponibilite.isEstDisponible() + ")");
                        break;
                    case "notes":
                        String notesValue = disponibilite.getNotes() != null ? disponibilite.getNotes() : "";
                        ps.setString(paramIndex++, notesValue);
                        System.out.println("  â†’ notes = '" + notesValue + "'");
                        break;
                    case "created_at":
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                        break;
                    default:
                        // Pour les colonnes inconnues, essayer de dÃ©terminer le type
                        System.out.println("âš  Colonne inconnue '" + field + "', tentative de dÃ©tection du type");
                        
                        // RÃ©cupÃ©rer le type de la colonne
                        DatabaseMetaData meta = conn.getMetaData();
                        ResultSet colInfo = meta.getColumns(null, null, "availability", field);
                        
                        if (colInfo.next()) {
                            int dataType = colInfo.getInt("DATA_TYPE");
                            String typeName = colInfo.getString("TYPE_NAME");
                            System.out.println("  Type dÃ©tectÃ©: " + typeName + " (code: " + dataType + ")");
                            
                            // java.sql.Types constants
                            if (dataType == java.sql.Types.INTEGER || dataType == java.sql.Types.BIGINT || 
                                dataType == java.sql.Types.SMALLINT || dataType == java.sql.Types.TINYINT) {
                                ps.setInt(paramIndex++, 0); // Valeur par dÃ©faut pour entier
                            } else if (dataType == java.sql.Types.BOOLEAN || dataType == java.sql.Types.BIT) {
                                ps.setBoolean(paramIndex++, false);
                            } else if (dataType == java.sql.Types.TIMESTAMP || dataType == java.sql.Types.DATE) {
                                ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                            } else if (dataType == java.sql.Types.DECIMAL || dataType == java.sql.Types.DOUBLE || 
                                       dataType == java.sql.Types.FLOAT) {
                                ps.setDouble(paramIndex++, 0.0);
                            } else {
                                // Par dÃ©faut, chaÃ®ne vide pour VARCHAR, TEXT, etc.
                                ps.setString(paramIndex++, "");
                            }
                        } else {
                            // Si on ne peut pas dÃ©terminer le type, utiliser NULL
                            ps.setNull(paramIndex++, java.sql.Types.VARCHAR);
                        }
                        colInfo.close();
                        break;
                }
            }
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    disponibilite.setId(rs.getInt(1));
                }
            }
            
            System.out.println("âœ“ DisponibilitÃ© ajoutÃ©e avec succÃ¨s - ID: " + disponibilite.getId());
            
        } catch (SQLException e) {
            System.err.println("âœ— Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void modifier(Disponibilite disponibilite) throws SQLException {
        // D'abord, dÃ©couvrir la structure de la table
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, "availability", null);
        
        List<String> columnNames = new ArrayList<>();
        
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            columnNames.add(columnName);
        }
        columns.close();
        
        System.out.println("Modification - Colonnes disponibles: " + columnNames);
        
        // Construire la requÃªte UPDATE dynamiquement
        StringBuilder sqlBuilder = new StringBuilder("UPDATE availability SET ");
        List<String> fieldsToUpdate = new ArrayList<>();
        
        // Ajouter les colonnes que nous connaissons (sauf id et created_at)
        if (columnNames.contains("doctor_id")) {
            fieldsToUpdate.add("doctor_id");
        }
        
        // Pour les dates complÃ¨tes, on doit sÃ©parer date et heure
        if (columnNames.contains("start_date")) {
            fieldsToUpdate.add("start_date");
        }
        if (columnNames.contains("end_date")) {
            fieldsToUpdate.add("end_date");
        }
        if (columnNames.contains("start_time")) {
            fieldsToUpdate.add("start_time");
        }
        if (columnNames.contains("end_time")) {
            fieldsToUpdate.add("end_time");
        }
        
        if (columnNames.contains("is_online")) {
            fieldsToUpdate.add("is_online");
        }
        if (columnNames.contains("notes")) {
            fieldsToUpdate.add("notes");
        }
        if (columnNames.contains("updated_at")) {
            fieldsToUpdate.add("updated_at");
        }
        
        for (int i = 0; i < fieldsToUpdate.size(); i++) {
            sqlBuilder.append(fieldsToUpdate.get(i)).append("=?");
            if (i < fieldsToUpdate.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        
        sqlBuilder.append(" WHERE id=?");
        String sql = sqlBuilder.toString();
        
        System.out.println("RequÃªte UPDATE gÃ©nÃ©rÃ©e: " + sql);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            
            for (String field : fieldsToUpdate) {
                switch (field.toLowerCase()) {
                    case "doctor_id":
                        ps.setInt(paramIndex++, disponibilite.getDoctorId());
                        break;
                    case "start_date":
                        if (disponibilite.getDateDebut() != null) {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(disponibilite.getDateDebut().toLocalDate()));
                        } else {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()));
                        }
                        break;
                    case "end_date":
                        if (disponibilite.getDateFin() != null) {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(disponibilite.getDateFin().toLocalDate()));
                        } else {
                            ps.setDate(paramIndex++, java.sql.Date.valueOf(LocalDateTime.now().toLocalDate()));
                        }
                        break;
                    case "start_time":
                        if (disponibilite.getDateDebut() != null) {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(disponibilite.getDateDebut().toLocalTime()));
                        } else {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(LocalDateTime.now().toLocalTime()));
                        }
                        break;
                    case "end_time":
                        if (disponibilite.getDateFin() != null) {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(disponibilite.getDateFin().toLocalTime()));
                        } else {
                            ps.setTime(paramIndex++, java.sql.Time.valueOf(LocalDateTime.now().plusHours(1).toLocalTime()));
                        }
                        break;
                    case "is_online":
                        boolean isOnlineValue = !disponibilite.isEstDisponible();
                        ps.setBoolean(paramIndex++, isOnlineValue);
                        System.out.println("  â†’ UPDATE is_online = " + isOnlineValue + " (estDisponible = " + disponibilite.isEstDisponible() + ")");
                        break;
                    case "notes":
                        String notesValue = disponibilite.getNotes() != null ? disponibilite.getNotes() : "";
                        ps.setString(paramIndex++, notesValue);
                        System.out.println("  â†’ UPDATE notes = '" + notesValue + "'");
                        break;
                    case "updated_at":
                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(LocalDateTime.now()));
                        break;
                }
            }
            
            // Le WHERE id=?
            ps.setInt(paramIndex, disponibilite.getId());
            
            int rowsAffected = ps.executeUpdate();
            System.out.println("âœ“ DisponibilitÃ© modifiÃ©e avec succÃ¨s - ID: " + disponibilite.getId() + " (" + rowsAffected + " ligne(s) affectÃ©e(s))");
            
        } catch (SQLException e) {
            System.err.println("âœ— Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // VÃ©rifier d'abord s'il y a des rendez-vous liÃ©s
        String checkSql = "SELECT COUNT(*) as count FROM rendez_vous WHERE disponibilite_id = ?";
        
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, id);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("count");
                if (count > 0) {
                    throw new SQLException("Impossible de supprimer cette disponibilitÃ© car " + count + 
                                         " rendez-vous y sont liÃ©s. Veuillez d'abord supprimer ou modifier ces rendez-vous.");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Si la colonne disponibilite_id n'existe pas, essayer avec availability_id
            if (e.getMessage().contains("Unknown column")) {
                System.out.println("Colonne 'disponibilite_id' non trouvÃ©e, tentative avec d'autres noms...");
                // Continuer avec la suppression normale
            } else {
                throw e;
            }
        }
        
        // Si pas de rendez-vous liÃ©s, procÃ©der Ã  la suppression
        String sql = "DELETE FROM availability WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            System.out.println("âœ“ DisponibilitÃ© supprimÃ©e - ID: " + id + " (" + rowsAffected + " ligne(s) supprimÃ©e(s))");
        } catch (SQLException e) {
            // Si l'erreur est une contrainte de clÃ© Ã©trangÃ¨re
            if (e.getMessage().contains("foreign key constraint")) {
                throw new SQLException("Cette disponibilitÃ© ne peut pas Ãªtre supprimÃ©e car elle est utilisÃ©e par d'autres Ã©lÃ©ments (rendez-vous, etc.). " +
                                     "Veuillez d'abord supprimer ou modifier ces Ã©lÃ©ments.");
            }
            throw e;
        }
    }

    @Override
    public List<Disponibilite> getAll() throws SQLException {
        String sql = """
            SELECT d.*, u.username AS doctor_nom, u.email AS doctor_email
            FROM availability d
            LEFT JOIN users u ON d.doctor_id = u.id
            ORDER BY d.id DESC
            """;

        List<Disponibilite> disponibilites = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Disponibilite d = mapResultSetToDisponibilite(rs);
                disponibilites.add(d);
            }
        }

        return disponibilites;
    }

    @Override
    public Disponibilite afficherParId(int id) throws SQLException {
        String sql = """
            SELECT d.*, u.username as doctor_nom, u.email as doctor_email
            FROM availability d
            LEFT JOIN users u ON d.doctor_id = u.id
            WHERE d.id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("=== DEBUG afficherParId ===");
                    System.out.println("ID: " + rs.getInt("id"));
                    System.out.println("Doctor ID: " + rs.getInt("doctor_id"));
                    
                    // Afficher toutes les colonnes disponibles
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    System.out.println("Colonnes disponibles:");
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String columnValue = rs.getString(i);
                        System.out.println("  " + columnName + " = " + columnValue);
                    }
                    
                    Disponibilite dispo = mapResultSetToDisponibilite(rs);
                    System.out.println("Date dÃ©but aprÃ¨s mapping: " + dispo.getDateDebut());
                    System.out.println("Date fin aprÃ¨s mapping: " + dispo.getDateFin());
                    System.out.println("=========================");
                    
                    return dispo;
                }
            }
        }
        
        return null;
    }

    public List<Disponibilite> getDisponibilitesParDocteur(int doctorId) throws SQLException {
        String sql = """
            SELECT d.*, u.username as doctor_nom, u.email as doctor_email
            FROM availability d
            LEFT JOIN users u ON d.doctor_id = u.id
            WHERE d.doctor_id = ?
            ORDER BY d.start_time ASC
            """;
        
        List<Disponibilite> disponibilites = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Disponibilite d = mapResultSetToDisponibilite(rs);
                    disponibilites.add(d);
                }
            }
        }
        
        return disponibilites;
    }

    public List<Disponibilite> getDisponibilitesLibres(LocalDateTime dateDebut, LocalDateTime dateFin) throws SQLException {
        String sql = """
            SELECT d.*, u.username as doctor_nom, u.email as doctor_email
            FROM availability d
            LEFT JOIN users u ON d.doctor_id = u.id
            WHERE d.available = true 
            AND d.start_time <= ? 
            AND d.end_time >= ?
            AND d.start_time >= NOW()
            ORDER BY d.start_time ASC
            """;
        
        List<Disponibilite> disponibilites = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(dateFin));
            ps.setTimestamp(2, Timestamp.valueOf(dateDebut));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Disponibilite d = mapResultSetToDisponibilite(rs);
                    disponibilites.add(d);
                }
            }
        }
        
        return disponibilites;
    }

    public List<Disponibilite> getDisponibilitesParDocteurEtDate(int doctorId, LocalDateTime date) throws SQLException {
        String sql = """
            SELECT d.*, u.username as doctor_nom, u.email as doctor_email
            FROM availability d
            LEFT JOIN users u ON d.doctor_id = u.id
            WHERE d.doctor_id = ? 
            AND d.available = true
            AND DATE(d.start_time) = DATE(?)
            ORDER BY d.start_time ASC
            """;
        
        List<Disponibilite> disponibilites = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setTimestamp(2, Timestamp.valueOf(date));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Disponibilite d = mapResultSetToDisponibilite(rs);
                    disponibilites.add(d);
                }
            }
        }
        
        return disponibilites;
    }

    public void marquerIndisponible(int disponibiliteId) throws SQLException {
        String sql = "UPDATE availability SET available = false WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disponibiliteId);
            ps.executeUpdate();
        }
    }

    private Disponibilite mapResultSetToDisponibilite(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("id"));
        d.setDoctorId(rs.getInt("doctor_id"));
        
        // Combiner start_date + start_time pour obtenir la date/heure complÃ¨te
        try {
            java.sql.Date startDate = rs.getDate("start_date");
            java.sql.Time startTime = rs.getTime("start_time");
            
            if (startDate != null && startTime != null) {
                d.setDateDebut(LocalDateTime.of(startDate.toLocalDate(), startTime.toLocalTime()));
            }
        } catch (SQLException e) {
            // Si les colonnes n'existent pas, utiliser la date actuelle
            d.setDateDebut(LocalDateTime.now());
        }
        
        try {
            java.sql.Date endDate = rs.getDate("end_date");
            java.sql.Time endTime = rs.getTime("end_time");
            
            if (endDate != null && endTime != null) {
                d.setDateFin(LocalDateTime.of(endDate.toLocalDate(), endTime.toLocalTime()));
            }
        } catch (SQLException e) {
            // Si les colonnes n'existent pas, utiliser la date actuelle + 1h
            d.setDateFin(LocalDateTime.now().plusHours(1));
        }
        
        // DisponibilitÃ©
        try {
            d.setEstDisponible(!rs.getBoolean("is_online"));
        } catch (SQLException e) {
            d.setEstDisponible(true);
        }
        
        // Notes
        try {
            d.setNotes(rs.getString("notes"));
        } catch (SQLException e) {
            d.setNotes("");
        }
        
        // Date de crÃ©ation
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                d.setDateCreation(createdAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }
        
        // Informations supplÃ©mentaires
        try {
            d.setDoctorNom(rs.getString("doctor_nom"));
        } catch (SQLException e) {
            d.setDoctorNom("MÃ©decin " + d.getDoctorId());
        }
        
        try {
            d.setDoctorEmail(rs.getString("doctor_email"));
        } catch (SQLException e) {
            // Ignorer
        }
        
        return d;
    }

    private Disponibilite mapResultSetToDisponibiliteSimple(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        
        try {
            d.setId(rs.getInt("id"));
        } catch (SQLException e) {
            d.setId(0);
        }
        
        try {
            d.setDoctorId(rs.getInt("doctor_id"));
        } catch (SQLException e) {
            d.setDoctorId(0);
        }
        
        // Combiner start_date + start_time pour obtenir la date/heure complÃ¨te
        LocalDateTime dateDebut = null;
        LocalDateTime dateFin = null;
        
        try {
            java.sql.Date startDate = rs.getDate("start_date");
            java.sql.Time startTime = rs.getTime("start_time");
            
            if (startDate != null && startTime != null) {
                dateDebut = LocalDateTime.of(startDate.toLocalDate(), startTime.toLocalTime());
                System.out.println("Date dÃ©but combinÃ©e: " + dateDebut);
            }
        } catch (SQLException e) {
            // Colonnes n'existent pas
        }
        
        try {
            java.sql.Date endDate = rs.getDate("end_date");
            java.sql.Time endTime = rs.getTime("end_time");
            
            if (endDate != null && endTime != null) {
                dateFin = LocalDateTime.of(endDate.toLocalDate(), endTime.toLocalTime());
                System.out.println("Date fin combinÃ©e: " + dateFin);
            }
        } catch (SQLException e) {
            // Colonnes n'existent pas
        }
        
        d.setDateDebut(dateDebut != null ? dateDebut : java.time.LocalDateTime.now());
        d.setDateFin(dateFin != null ? dateFin : java.time.LocalDateTime.now().plusHours(8));
        
        // DisponibilitÃ©
        try {
            d.setEstDisponible(!rs.getBoolean("is_online")); // is_online = inverse ?
        } catch (SQLException e) {
            d.setEstDisponible(true);
        }
        
        // Notes
        try {
            String notes = rs.getString("notes");
            d.setNotes(notes != null ? notes : "");
        } catch (SQLException e) {
            d.setNotes("");
        }
        
        d.setDoctorNom("MÃ©decin " + d.getDoctorId());
        
        return d;
    }
    /**
     * Get all users with DOCTOR role.
     * Used by DisponibiliteController formulaire.
     */
    public List<esprit.fx.entities.User> getAllDoctors() throws java.sql.SQLException {
        List<esprit.fx.entities.User> doctors = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT u.id, u.username, u.email " +
                     "FROM users u " +
                     "INNER JOIN user_roles ur ON u.id = ur.user_id " +
                     "INNER JOIN roles r ON ur.role_id = r.id " +
                     "WHERE r.name IN ('DOCTOR', 'ROLE_DOCTOR', 'Medecin', 'MEDECIN') " +
                     "AND u.is_active = 1 ORDER BY u.username";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                esprit.fx.entities.User doctor = new esprit.fx.entities.User();
                doctor.setId(rs.getInt("id"));
                doctor.setUsername(rs.getString("username"));
                doctor.setEmail(rs.getString("email"));
                doctors.add(doctor);
            }
        }
        return doctors;
    }
}