package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;

import java.sql.SQLException;
import java.util.List;

public class CommandeService {

    private final CommandeRepository repo = new CommandeRepository();

    public void ajouter(Commande c) throws SQLException {
        repo.add(c);
    }

    public void changerStatut(int idCommande, String statut) throws SQLException {
        repo.updateStatut(idCommande, statut);
    }

    public List<CommandeJoinProduit> getAllJoinProduit() throws SQLException {
        return repo.findAllJoinProduit();
    }

    public List<CommandeJoinProduit> getByClientJoinProduit(int idClient) throws SQLException {
        return repo.findByClientJoinProduit(idClient);
    }

    public void supprimer(int idCommande) throws SQLException {
        try {
            repo.delete(idCommande);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
