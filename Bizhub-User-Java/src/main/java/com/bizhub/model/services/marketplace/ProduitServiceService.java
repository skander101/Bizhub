package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.marketplace.ProduitServiceRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProduitServiceService {

    private final ProduitServiceRepository repo = new ProduitServiceRepository();

    public List<ProduitService> getAll() throws SQLException {
        return repo.findAll();
    }

    public List<ProduitService> getAllDisponibles() throws SQLException {
        List<ProduitService> all = repo.findAll();
        List<ProduitService> out = new ArrayList<>();
        for (ProduitService p : all) if (p.isDisponible()) out.add(p);
        return out;
    }

    public void add(ProduitService p) throws SQLException {
        repo.add(p);
    }

    public void update(ProduitService p) throws SQLException {
        repo.update(p);
    }

    public void delete(int idProduit) throws SQLException {
        repo.delete(idProduit);
    }
}
