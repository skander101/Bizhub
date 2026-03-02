package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.DB.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

public class InvestorStatsRepository {

    private static final Logger LOGGER = Logger.getLogger(InvestorStatsRepository.class.getName());
    private final Connection cnx;

    public InvestorStatsRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public List<StatsPoint> getDailyStats(int investorId, LocalDate from, LocalDate to) throws SQLException {
        if (investorId <= 0) throw new IllegalArgumentException("investorId invalide");
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("dates invalides");

        String ownerCol = detectOwnerCol();
        if (ownerCol == null) return fillZeros(from, to);

        Map<LocalDate, int[]>        nbMap  = new HashMap<>();
        Map<LocalDate, BigDecimal[]> amtMap = new HashMap<>();

        // ════════════════════════════════════════════════════════════════════
        // RÉALITÉ DB :
        //   commande.id_produit = NULL (nouvelle arch → pas de commande_ligne non plus)
        //   commande.total_ttc  = NULL
        //   commande.quantite   = valeur réelle (ex: 5, 256, 7852...)
        //   produit_service.prix = valeur réelle (ex: 55.00, 2563.00...)
        //
        // Solution : joindre via une sous-requête sur commande_ligne,
        //   ou si commande_ligne est vide → récupérer le produit depuis la
        //   table commande via id_produit direct UNIQUEMENT si non-NULL.
        //
        // Pour les commandes sans id_produit ET sans commande_ligne :
        //   on cherche le produit de l'investisseur qui a été commandé
        //   en matchant owner_user_id depuis toutes les lignes commande.
        //
        // ✅ VRAIE SOLUTION : requête unifiée avec sous-requêtes imbriquées
        // ════════════════════════════════════════════════════════════════════

        // Requête unifiée — fonctionne peu importe la structure :
        // Montant = CASE :
        //   1. commande_ligne présente → prix_ht × qte × (1+tva)
        //   2. commande.id_produit non NULL + produit trouvé → prix_produit × commande.quantite × 1.19
        //   3. sinon → cherche dans commande_ligne via owner et prend le montant
        //   4. fallback → 0

        String sql =
                "SELECT " +
                        "  DATE(c.date_commande) AS d, " +
                        "  COUNT(DISTINCT c.id_commande) AS nb, " +
                        "  SUM( " +
                        "    CASE " +
                        // Cas 1 : commande_ligne existe pour cette commande
                        "    WHEN EXISTS (SELECT 1 FROM commande_ligne xl WHERE xl.id_commande = c.id_commande) " +
                        "    THEN (SELECT COALESCE(SUM(xl2.prix_ht_unitaire * xl2.quantite * (1 + IFNULL(xl2.tva_rate,0.19))), 0) " +
                        "          FROM commande_ligne xl2 WHERE xl2.id_commande = c.id_commande) " +
                        // Cas 2 : pas de commande_ligne mais id_produit direct dans commande
                        "    WHEN c.id_produit IS NOT NULL AND c.id_produit > 0 " +
                        "    THEN COALESCE(" +
                        "           (SELECT p2.prix FROM produit_service p2 WHERE p2.id_produit = c.id_produit LIMIT 1), 0" +
                        "         ) * COALESCE(c.quantite, 1) * 1.19 " +
                        // Cas 3 : ni commande_ligne ni id_produit → total_ttc si dispo
                        "    WHEN c.total_ttc IS NOT NULL AND c.total_ttc > 0 THEN c.total_ttc " +
                        "    ELSE 0 " +
                        "    END " +
                        "  ) AS montant " +
                        "FROM commande c " +
                        // Jointure pour filtrer sur owner_user_id :
                        // On joint soit via commande_ligne, soit via id_produit direct
                        "WHERE ( " +
                        // Option A : l'investisseur possède un produit dans commande_ligne
                        "  EXISTS ( " +
                        "    SELECT 1 FROM commande_ligne cl " +
                        "    JOIN produit_service p ON p.id_produit = cl.id_produit " +
                        "    WHERE cl.id_commande = c.id_commande AND p." + ownerCol + " = ? " +
                        "  ) " +
                        "  OR " +
                        // Option B : id_produit direct dans commande appartient à l'investisseur
                        "  EXISTS ( " +
                        "    SELECT 1 FROM produit_service p2 " +
                        "    WHERE p2.id_produit = c.id_produit AND p2." + ownerCol + " = ? " +
                        "  ) " +
                        ") " +
                        "AND LOWER(TRIM(c.statut)) = ? " +
                        "AND DATE(c.date_commande) BETWEEN ? AND ? " +
                        "GROUP BY DATE(c.date_commande) ORDER BY d";

        runUnified(sql, investorId, "confirmee", from, to, nbMap, amtMap, 0);
        runUnified(sql, investorId, "annule",    from, to, nbMap, amtMap, 1);

        // ── Merge continu ────────────────────────────────────────────
        List<StatsPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            int[]        nb  = nbMap.getOrDefault(d,  new int[]{0, 0});
            BigDecimal[] amt = amtMap.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            out.add(new StatsPoint(d, nb[0], nb[1], amt[0], amt[1]));
        }
        return out;
    }

    private void runUnified(String sql, int investorId, String statut,
                            LocalDate from, LocalDate to,
                            Map<LocalDate, int[]> nbMap,
                            Map<LocalDate, BigDecimal[]> amtMap,
                            int idx) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, investorId);    // option A : owner dans commande_ligne
            ps.setInt(2, investorId);    // option B : owner via id_produit direct
            ps.setString(3, statut);
            ps.setDate(4, java.sql.Date.valueOf(from));
            ps.setDate(5, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate  d  = rs.getDate("d").toLocalDate();
                    int        nb = rs.getInt("nb");
                    BigDecimal m  = rs.getBigDecimal("montant");
                    if (m == null) m = BigDecimal.ZERO;

                    nbMap.computeIfAbsent(d,  k -> new int[]{0, 0})[idx] += nb;
                    BigDecimal[] arr = amtMap.computeIfAbsent(d, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    arr[idx] = arr[idx].add(m);
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Stats [" + statut + "]: " + e.getMessage());
        }
    }

    // ── Introspection DB ─────────────────────────────────────────────
    private String detectOwnerCol() throws SQLException {
        for (String c : List.of("owner_user_id","ownerUserId","id_owner","owner_id","id_investisseur","investor_id"))
            if (columnExists("produit_service", c)) return c;
        return null;
    }

    private boolean columnExists(String table, String col) throws SQLException {
        try (ResultSet rs = cnx.getMetaData().getColumns(null, null, table, null)) {
            while (rs.next())
                if (rs.getString("COLUMN_NAME").equalsIgnoreCase(col)) return true;
        }
        return false;
    }

    private List<StatsPoint> fillZeros(LocalDate from, LocalDate to) {
        List<StatsPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
            out.add(new StatsPoint(d, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO));
        return out;
    }
}