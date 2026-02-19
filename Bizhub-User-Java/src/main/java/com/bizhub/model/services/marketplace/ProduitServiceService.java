package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.marketplace.ProduitServiceRepository;

import java.sql.SQLException;
import java.util.List;

public class ProduitServiceService {

    private final ProduitServiceRepository repo = new ProduitServiceRepository();

    public List<ProduitService> getAll() throws SQLException {
        return repo.findAll();
    }

    public List<ProduitService> getAllByProfile(int idProfile) throws SQLException {
        if (idProfile <= 0) throw new IllegalArgumentException("ID profile invalide");
        return repo.findAllByProfile(idProfile);
    }

    public void ajouter(ProduitService p) throws SQLException {
        validate(p, false);
        repo.add(p);
    }

    public void modifier(ProduitService p) throws SQLException {
        validate(p, true);
        repo.update(p);
    }

    public void supprimer(int idProduit) throws SQLException {
        if (idProduit <= 0) throw new IllegalArgumentException("ID produit invalide");
        repo.delete(idProduit);
    }

    private void validate(ProduitService p, boolean isUpdate) {
        if (p == null) throw new IllegalArgumentException("Produit null");
        if (isUpdate && p.getIdProduit() <= 0) throw new IllegalArgumentException("ID produit invalide");
        if (p.getIdProfile() <= 0) throw new IllegalArgumentException("id_profile invalide");
        if (p.getNom() == null || p.getNom().isBlank()) throw new IllegalArgumentException("Nom obligatoire");
        if (p.getPrix() == null) throw new IllegalArgumentException("Prix obligatoire");
        if (p.getQuantite() < 0) throw new IllegalArgumentException("Quantité invalide");
    }
}
