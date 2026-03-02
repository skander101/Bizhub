package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.dao.MyDatabase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierRepository {

    private final Connection cnx;

    public PanierRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ── Ajouter ou mettre à jour la quantité ──
    public void addOrUpdate(int idClient, int idProduit, int quantite) throws SQLException {
        String sql = """
            INSERT INTO panier (id_client, id_produit, quantite, date_ajout)
            VALUES (?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                quantite   = quantite + VALUES(quantite),
                date_ajout = NOW()
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.setInt(2, idProduit);
            ps.setInt(3, quantite);
            ps.executeUpdate();
        }
    }

    // ── Définir quantité exacte ──
    public void setQuantite(int idClient, int idProduit, int quantite) throws SQLException {
        if (quantite <= 0) { remove(idClient, idProduit); return; }
        String sql = """
            UPDATE panier SET quantite=?, date_ajout=NOW()
            WHERE id_client=? AND id_produit=?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setInt(2, idClient);
            ps.setInt(3, idProduit);
            ps.executeUpdate();
        }
    }

    // ── Supprimer un article ──
    public void remove(int idClient, int idProduit) throws SQLException {
        String sql = "DELETE FROM panier WHERE id_client=? AND id_produit=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.setInt(2, idProduit);
            ps.executeUpdate();
        }
    }

    // ── Vider le panier ──
    public void clear(int idClient) throws SQLException {
        String sql = "DELETE FROM panier WHERE id_client=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.executeUpdate();
        }
    }

    // ── Lire le panier (JOIN produit_service) ──
    public List<PanierItem> findByClient(int idClient) throws SQLException {
        String sql = """
            SELECT p.id_panier, p.id_client, p.id_produit, p.quantite, p.date_ajout,
                   ps.nom AS produit_nom, ps.prix AS prix_unitaire,
                   ps.categorie, ps.disponible
            FROM panier p
            JOIN produit_service ps ON ps.id_produit = p.id_produit
            WHERE p.id_client = ?
            ORDER BY p.date_ajout DESC
            """;
        List<PanierItem> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── Nombre d'articles dans le panier ──
    public int countItems(int idClient) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantite), 0) FROM panier WHERE id_client=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Mapper ──
    private PanierItem map(ResultSet rs) throws SQLException {
        PanierItem item = new PanierItem();
        item.setIdPanier(rs.getInt("id_panier"));
        item.setIdClient(rs.getInt("id_client"));
        item.setIdProduit(rs.getInt("id_produit"));
        item.setQuantite(rs.getInt("quantite"));

        Timestamp ts = rs.getTimestamp("date_ajout");
        item.setDateAjout(ts != null ? ts.toLocalDateTime() : null);

        item.setProduitNom(rs.getString("produit_nom"));
        item.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        item.setCategorie(rs.getString("categorie"));
        item.setDisponible(rs.getBoolean("disponible"));

        return item;
    }
}