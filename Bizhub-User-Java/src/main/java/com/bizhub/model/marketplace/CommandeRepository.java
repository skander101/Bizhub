package com.bizhub.model.marketplace;

import com.bizhub.model.services.common.dao.MyDatabase; // ✅ corrige ici selon ton vrai package

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeRepository {

    private final Connection cnx;

    public CommandeRepository() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public void add(Commande c) throws SQLException {
        if (c == null) throw new IllegalArgumentException("Commande null");
        if (c.getIdClient() <= 0) throw new IllegalArgumentException("Id client invalide");
        if (c.getIdProduit() <= 0) throw new IllegalArgumentException("Id produit invalide");
        if (c.getQuantite() <= 0) throw new IllegalArgumentException("Quantité invalide");

        if (c.getStatut() == null || c.getStatut().isBlank()) c.setStatut("en_attente");

        String sql = "INSERT INTO commande (id_client, id_produit, quantite, statut) VALUES (?, ?, ?, ?)";

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

    public void updateStatut(int idCommande, String nouveauStatut) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");
        if (nouveauStatut == null || nouveauStatut.isBlank()) throw new IllegalArgumentException("Statut invalide");

        String sql = "UPDATE commande SET statut=? WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        }
    }

    public void delete(int idCommande) throws SQLException {
        if (idCommande <= 0) throw new IllegalArgumentException("Id commande invalide");

        String sql = "DELETE FROM commande WHERE id_commande=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ps.executeUpdate();
        }
    }

    public List<Commande> findAll() throws SQLException {
        String sql = "SELECT * FROM commande ORDER BY id_commande DESC";
        List<Commande> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCommande(rs));
        }
        return list;
    }

    public List<CommandeJoinProduit> findAllJoinProduit() throws SQLException {
        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, c.quantite AS quantite_commande, c.statut, p.nom AS produit_nom " +
                        "FROM commande c " +
                        "JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "ORDER BY c.id_commande DESC";

        List<CommandeJoinProduit> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapJoin(rs));
        }
        return list;
    }

    public List<CommandeJoinProduit> findByClientJoinProduit(int idClient) throws SQLException {
        if (idClient <= 0) throw new IllegalArgumentException("Id client invalide");

        String sql =
                "SELECT c.id_commande, c.id_client, c.id_produit, c.quantite AS quantite_commande, c.statut, p.nom AS produit_nom " +
                        "FROM commande c " +
                        "JOIN produit_service p ON p.id_produit = c.id_produit " +
                        "WHERE c.id_client = ? " +
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

    private Commande mapCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setIdCommande(rs.getInt("id_commande"));
        c.setIdClient(rs.getInt("id_client"));
        c.setIdProduit(rs.getInt("id_produit"));
        c.setQuantite(rs.getInt("quantite"));
        c.setStatut(rs.getString("statut"));
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
        return j;
    }
}
