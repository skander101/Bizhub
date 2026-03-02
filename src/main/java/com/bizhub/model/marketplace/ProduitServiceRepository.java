package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.DB.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitServiceRepository {

    private final Connection cnx;

    public ProduitServiceRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
        if (this.cnx == null) throw new IllegalStateException("DB connection is null");
    }

    // ============ CRUD ============

    public void add(ProduitService p) throws SQLException {
        String sql = """
            INSERT INTO produit_service
            (id_profile, owner_user_id, nom, description, prix, quantite, categorie, disponible)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdProfile());
            ps.setInt(2, p.getOwnerUserId());          // ✅ IMPORTANT
            ps.setString(3, p.getNom());
            ps.setString(4, p.getDescription());
            ps.setBigDecimal(5, p.getPrix());
            ps.setInt(6, p.getQuantite());
            ps.setString(7, p.getCategorie());
            ps.setBoolean(8, p.isDisponible());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setIdProduit(rs.getInt(1));
            }
        }
    }

    public void update(ProduitService p) throws SQLException {
        String sql = """
            UPDATE produit_service
            SET id_profile=?, owner_user_id=?, nom=?, description=?, prix=?, quantite=?, categorie=?, disponible=?
            WHERE id_produit=?
            """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdProfile());
            ps.setInt(2, p.getOwnerUserId());          // ✅ IMPORTANT
            ps.setString(3, p.getNom());
            ps.setString(4, p.getDescription());
            ps.setBigDecimal(5, p.getPrix());
            ps.setInt(6, p.getQuantite());
            ps.setString(7, p.getCategorie());
            ps.setBoolean(8, p.isDisponible());
            ps.setInt(9, p.getIdProduit());
            ps.executeUpdate();
        }
    }

    public void delete(int idProduit) throws SQLException {
        String sql = "DELETE FROM produit_service WHERE id_produit=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            ps.executeUpdate();
        }
    }

    // ============ QUERIES ============

    public List<ProduitService> findAll() throws SQLException {
        String sql = "SELECT * FROM produit_service ORDER BY id_produit DESC";
        List<ProduitService> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ProduitService> findAllByOwner(int ownerUserId) throws SQLException {
        String sql = "SELECT * FROM produit_service WHERE owner_user_id=? ORDER BY id_produit DESC";
        List<ProduitService> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<String> findAllCategories() throws SQLException {
        String sql = "SELECT DISTINCT categorie FROM produit_service WHERE categorie IS NOT NULL AND categorie <> '' ORDER BY categorie";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    private ProduitService map(ResultSet rs) throws SQLException {
        ProduitService p = new ProduitService();
        p.setIdProduit(rs.getInt("id_produit"));
        p.setIdProfile(rs.getInt("id_profile"));
        p.setOwnerUserId(rs.getInt("owner_user_id"));   // ✅ NEW
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getBigDecimal("prix"));
        p.setQuantite(rs.getInt("quantite"));
        p.setCategorie(rs.getString("categorie"));
        p.setDisponible(rs.getBoolean("disponible"));
        return p;
    }
}
