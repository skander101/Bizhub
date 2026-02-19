package com.bizhub.model.marketplace;
import com.bizhub.model.services.common.dao.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitServiceRepository {

    private final Connection cnx;

    public ProduitServiceRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ============ CRUD ============

    public void add(ProduitService p) throws SQLException {
        String sql = "INSERT INTO produit_service (id_profile, nom, description, prix, quantite, categorie, disponible) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdProfile());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getDescription());
            ps.setBigDecimal(4, p.getPrix());
            ps.setInt(5, p.getQuantite());
            ps.setString(6, p.getCategorie());
            ps.setBoolean(7, p.isDisponible());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setIdProduit(rs.getInt(1));
                }
            }
        }
    }

    public void update(ProduitService p) throws SQLException {
        String sql = "UPDATE produit_service " +
                "SET id_profile=?, nom=?, description=?, prix=?, quantite=?, categorie=?, disponible=? " +
                "WHERE id_produit=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getIdProfile());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getDescription());
            ps.setBigDecimal(4, p.getPrix());
            ps.setInt(5, p.getQuantite());
            ps.setString(6, p.getCategorie());
            ps.setBoolean(7, p.isDisponible());
            ps.setInt(8, p.getIdProduit());
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

    // ============ Queries ============

    public ProduitService findById(int idProduit) throws SQLException {
        String sql = "SELECT * FROM produit_service WHERE id_produit=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<ProduitService> findAll() throws SQLException {
        String sql = "SELECT * FROM produit_service ORDER BY id_produit DESC";
        List<ProduitService> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<ProduitService> findAllByProfile(int idProfile) throws SQLException {
        String sql = "SELECT * FROM produit_service WHERE id_profile=? ORDER BY id_produit DESC";
        List<ProduitService> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProfile);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // ============ Mapper ============

    private ProduitService map(ResultSet rs) throws SQLException {
        ProduitService p = new ProduitService();
        p.setIdProduit(rs.getInt("id_produit"));
        p.setIdProfile(rs.getInt("id_profile"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getBigDecimal("prix"));
        p.setQuantite(rs.getInt("quantite"));
        p.setCategorie(rs.getString("categorie"));
        p.setDisponible(rs.getBoolean("disponible"));
        return p;
    }
}
