package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.DB.MyDatabase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class CommandeRepository {

    private static final Logger LOGGER = Logger.getLogger(CommandeRepository.class.getName());
    private final Connection cnx;

    public CommandeRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // =====================================================
    // INSERT commande simple (depuis formulaire direct)
    // =====================================================
    public void add(Commande c) throws SQLException {
        if (c == null) throw new IllegalArgumentException("Commande null");
        if (c.getIdClient() <= 0)  throw new IllegalArgumentException("Id client invalide");
        if (c.getIdProduit() <= 0) throw new IllegalArgumentException("Id produit invalide");
        if (c.getQuantite() <= 0)  throw new IllegalArgumentException("Quantité invalide");
        if (c.getStatut() == null || c.getStatut().isBlank()) c.setStatut("en_attente");

        String sql =
                "INSERT INTO commande " +
                        "(id_client, id_produit, quantite, statut, date_commande, payment_status, est_payee) " +
                        "VALUES (?, ?, ?, ?, NOW(), 'non_initie', 0)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getIdClient());
            ps.setInt(2, c.getIdProduit());
            ps.setInt(3, c.getQuantite());
            ps.setString(4, c.getStatut());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setIdCommande(rs.getInt(1));
            }
        }
    }

    // =====================================================
    // ✅ INSERT commande depuis panier (avec totaux HT/TVA/TTC)
    //    Retourne l'id_commande généré
    // =====================================================
    public int addAndGetId(Commande c, BigDecimal totalHT, BigDecimal totalTVA, BigDecimal totalTTC)
            throws SQLException {
        if (c == null) throw new IllegalArgumentException("Commande null");
        if (c.getIdClient() <= 0)  throw new IllegalArgumentException("Id client invalide");
        if (c.getStatut() == null || c.getStatut().isBlank()) c.setStatut("en_attente");

        String sql =
                "INSERT INTO commande " +
                        "(id_client, id_produit, quantite, statut, date_commande, " +
                        " payment_status, est_payee, total_ht, total_tva, total_ttc) " +
                        "VALUES (?, ?, ?, ?, NOW(), 'non_initie', 0, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getIdClient());
            if (c.getIdProduit() > 0) ps.setInt(2, c.getIdProduit());
            else ps.setNull(2, Types.INTEGER);
            if (c.getQuantite() > 0) ps.setInt(3, c.getQuantite());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, c.getStatut());
            ps.setBigDecimal(5, totalHT.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(6, totalTVA.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(7, totalTTC.setScale(2, RoundingMode.HALF_UP));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'id de la commande créée");
    }

    // =====================================================
    // ✅ INSERT ligne dans commande_ligne
    // =====================================================
    public void addLigne(int idCommande, PanierItem item) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("idCommande invalide");
        if (item == null)    throw new IllegalArgumentException("item null");

        BigDecimal prixHT = item.getPrixUnitaire() != null
                ? item.getPrixUnitaire().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String sql =
                "INSERT INTO commande_ligne " +
                        "(id_commande, id_produit, quantite, prix_ht_unitaire, tva_rate) " +
                        "VALUES (?, ?, ?, ?, 0.19)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ps.setInt(2, item.getIdProduit());
            ps.setInt(3, item.getQuantite());
            ps.setBigDecimal(4, prixHT);
            ps.executeUpdate();
        }
    }

    // =====================================================
    // UPDATE STATUT
    // =====================================================
    public void updateStatut(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (nouveauStatut == null || nouveauStatut.isBlank())
            throw new IllegalArgumentException("Statut invalide");
        String sql = "UPDATE commande SET statut=? WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        }
    }

    public int updateStatutIfEnAttente(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql = "UPDATE commande SET statut=? WHERE id_commande=? AND statut='en_attente'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            return ps.executeUpdate();
        }
    }

    public int updateStatutIfPayable(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql =
                "UPDATE commande SET statut=? " +
                        "WHERE id_commande=? AND statut IN ('en_attente','confirmee')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            return ps.executeUpdate();
        }
    }

    // =====================================================
    // DELETE
    // =====================================================
    public void delete(int idCommande) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql = "DELETE FROM commande WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ps.executeUpdate();
        }
    }

    // =====================================================
    // FIND ALL (simple)
    // =====================================================
    public List<Commande> findAll() throws SQLException {
        String sql =
                "SELECT id_commande, id_client, id_produit, quantite, statut, date_commande " +
                        "FROM commande ORDER BY id_commande DESC";
        List<Commande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCommande(rs));
        }
        return list;
    }

    // =====================================================
    // FIND ALL JOIN — vue admin
    // =====================================================
    public List<CommandeJoinProduit> findAllJoinProduit() throws SQLException {
        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, " +
                        "       c.quantite AS quantite_commande, c.statut, " +
                        "       c.payment_status, c.payment_ref, c.payment_url, " +
                        "       c.est_payee, c.paid_at, c.date_commande, " +
                        "       IFNULL(p.nom, '(multi-articles)') AS produit_nom, " +
                        "       c.total_ht, c.total_tva, c.total_ttc " +
                        "FROM commande c " +
                        "LEFT JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "ORDER BY c.id_commande DESC";
        List<CommandeJoinProduit> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapJoin(rs));
        }
        return list;
    }

    // =====================================================
    // FIND BY CLIENT JOIN — startup voit ses commandes
    // =====================================================
    public List<CommandeJoinProduit> findByClientJoinProduit(int idClient) throws SQLException {
        if (idClient <= 0) throw new IllegalArgumentException("Id client invalide");

        String sql =
                "SELECT " +
                        "  c.id_commande, c.id_client, " +
                        "  NULL AS id_produit, " +
                        "  COALESCE(SUM(ci.quantite), c.quantite, 0) AS quantite_commande, " +
                        "  c.statut, c.payment_status, c.payment_ref, c.payment_url, " +
                        "  c.est_payee, c.paid_at, c.date_commande, " +
                        "  c.total_ht, c.total_tva, c.total_ttc, " +
                        "  COALESCE( " +
                        "     GROUP_CONCAT(CONCAT(p.nom,' x',ci.quantite) ORDER BY p.nom SEPARATOR ' + '), " +
                        "     CONCAT(IFNULL(p2.nom,'Produit inconnu'),' x', COALESCE(c.quantite,0)) " +
                        "  ) AS produit_nom " +
                        "FROM commande c " +
                        // ci = items d’une commande (panier) ; sinon NULL
                        "LEFT JOIN commande_ligne ci ON ci.id_commande = c.id_commande " +
                        "LEFT JOIN produit_service p ON p.id_produit = ci.id_produit " +
                        // p2 = produit direct (commande simple)
                        "LEFT JOIN produit_service p2 ON p2.id_produit = c.id_produit " +
                        "WHERE c.id_client = ? " +
                        "GROUP BY " +
                        "  c.id_commande, c.id_client, c.quantite, c.statut, c.payment_status, c.payment_ref, c.payment_url, " +
                        "  c.est_payee, c.paid_at, c.date_commande, c.total_ht, c.total_tva, c.total_ttc, p2.nom " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJoin(rs));
            }
        }
        return list;
    }

    // =====================================================
    // ✅ FIND BY OWNER JOIN — investisseur voit ses produits commandés
    //    (corrigé: pas de doublon panier)
    // =====================================================
    public List<CommandeJoinProduit> findByOwnerJoinProduit(int ownerId) throws SQLException {
        if (ownerId <= 0) throw new IllegalArgumentException("OwnerId invalide");

        String sql =
                "SELECT " +
                        "  c.id_commande, c.id_client, " +
                        "  NULL AS id_produit, " +
                        "  SUM(x.quantite) AS quantite_commande, " +
                        "  c.statut, c.payment_status, c.payment_ref, c.payment_url, " +
                        "  c.est_payee, c.paid_at, c.date_commande, " +
                        "  c.total_ht, c.total_tva, c.total_ttc, " +
                        "  GROUP_CONCAT(CONCAT(p.nom,' x',x.quantite) ORDER BY p.nom SEPARATOR ' + ') AS produit_nom " +
                        "FROM commande c " +

                        // x = toutes les lignes "article" d’une commande
                        // 1) panier (commande_ligne)
                        // 2) commande simple (commande.id_produit) si pas de commande_ligne
                        "JOIN ( " +
                        "   SELECT cl.id_commande, cl.id_produit, cl.quantite " +
                        "   FROM commande_ligne cl " +
                        "   UNION ALL " +
                        "   SELECT c2.id_commande, c2.id_produit, c2.quantite " +
                        "   FROM commande c2 " +
                        "   WHERE c2.id_produit IS NOT NULL " +
                        "     AND NOT EXISTS (SELECT 1 FROM commande_ligne clx WHERE clx.id_commande = c2.id_commande) " +
                        ") x ON x.id_commande = c.id_commande " +

                        "JOIN produit_service p ON p.id_produit = x.id_produit " +
                        "WHERE p.owner_user_id = ? " +

                        "GROUP BY " +
                        "  c.id_commande, c.id_client, c.statut, c.payment_status, c.payment_ref, c.payment_url, " +
                        "  c.est_payee, c.paid_at, c.date_commande, c.total_ht, c.total_tva, c.total_ttc " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJoin(rs));
            }
        }
        return list;
    }

    // =====================================================
    // PAYMENT
    // =====================================================
    public int setPaymentInitiatedIfNull(int idCommande, String ref, String url) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql =
                "UPDATE commande " +
                        "SET payment_status='en_cours', payment_ref=?, payment_url=? " +
                        "WHERE id_commande=? AND (payment_ref IS NULL OR payment_ref='')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ref);
            ps.setString(2, url);
            ps.setInt(3, idCommande);
            return ps.executeUpdate();
        }
    }

    public void updatePaymentRef(int idCommande, String ref, String url) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql =
                "UPDATE commande " +
                        "SET payment_status='en_cours', payment_ref=?, payment_url=? " +
                        "WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ref);
            ps.setString(2, url);
            ps.setInt(3, idCommande);
            ps.executeUpdate();
        }
    }

    public int markAsPaid(int idCommande, String sessionId) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (sessionId == null || sessionId.isBlank())
            throw new IllegalArgumentException("sessionId invalide");
        String sql =
                "UPDATE commande " +
                        "SET est_payee=1, paid_at=NOW(), payment_status='paid', payment_ref=? " +
                        "WHERE id_commande=? AND est_payee=0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setInt(2, idCommande);
            return ps.executeUpdate();
        }
    }

    public String getPaymentUrl(int idCommande) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        String sql = "SELECT payment_url FROM commande WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String url = rs.getString("payment_url");
                    return (url == null || url.isBlank()) ? null : url;
                }
            }
        }
        return null;
    }

    // =====================================================
    // DIVERS
    // =====================================================
    public String findStartupPhoneByCommandeId(int idCommande) throws SQLException {
        String sql =
                "SELECT u.phone FROM commande c " +
                        "JOIN user u ON u.user_id = c.id_client " +
                        "WHERE c.id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("phone") : null;
            }
        }
    }

    // =====================================================
    // MAPPERS
    // =====================================================
    private Commande mapCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setIdCommande(rs.getInt("id_commande"));
        c.setIdClient(rs.getInt("id_client"));
        c.setIdProduit(rs.getInt("id_produit"));
        c.setQuantite(rs.getInt("quantite"));
        c.setStatut(rs.getString("statut"));
        Timestamp dateTs = rs.getTimestamp("date_commande");
        c.setDateCommande(dateTs != null ? dateTs.toLocalDateTime() : null);
        return c;
    }

    private CommandeJoinProduit mapJoin(ResultSet rs) throws SQLException {
        CommandeJoinProduit j = new CommandeJoinProduit();
        j.setIdCommande(rs.getInt("id_commande"));
        j.setIdClient(rs.getInt("id_client"));
        j.setIdProduit(rs.getInt("id_produit"));
        j.setQuantiteCommande(rs.getInt("quantite_commande"));
        j.setStatut(rs.getString("statut"));
        j.setProduitNom(rs.getString("produit_nom"));
        j.setPaymentStatus(rs.getString("payment_status"));
        j.setPaymentRef(rs.getString("payment_ref"));
        j.setPaymentUrl(rs.getString("payment_url"));
        j.setEstPayee(rs.getBoolean("est_payee"));
        Timestamp paidTs = rs.getTimestamp("paid_at");
        j.setPaidAt(paidTs != null ? paidTs.toLocalDateTime() : null);
        Timestamp dateTs = rs.getTimestamp("date_commande");
        j.setDateCommande(dateTs != null ? dateTs.toLocalDateTime() : null);
        try { j.setTotalHt(rs.getBigDecimal("total_ht")); } catch (Exception ignored) {}
        try { j.setTotalTva(rs.getBigDecimal("total_tva")); } catch (Exception ignored) {}
        try { j.setTotalTtc(rs.getBigDecimal("total_ttc")); } catch (Exception ignored) {}
        return j;
    }
}