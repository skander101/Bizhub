package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeRepository;
import com.bizhub.model.marketplace.PanierItem;
import com.bizhub.model.marketplace.PanierRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

public class PanierService {

    private final PanierRepository   repo         = new PanierRepository();
    private final CommandeRepository commandeRepo = new CommandeRepository();

    public void ajouter(int idClient, int idProduit, int quantite) throws SQLException {
        if (idClient  <= 0) throw new IllegalArgumentException("idClient invalide");
        if (idProduit <= 0) throw new IllegalArgumentException("idProduit invalide");
        if (quantite  <= 0) throw new IllegalArgumentException("quantite invalide");
        repo.addOrUpdate(idClient, idProduit, quantite);
    }

    public void setQuantite(int idClient, int idProduit, int quantite) throws SQLException {
        repo.setQuantite(idClient, idProduit, quantite);
    }

    public void retirer(int idClient, int idProduit) throws SQLException {
        repo.remove(idClient, idProduit);
    }

    public void vider(int idClient) throws SQLException {
        repo.clear(idClient);
    }

    public List<PanierItem> getPanier(int idClient) throws SQLException {
        return repo.findByClient(idClient);
    }

    public int getNbArticles(int idClient) throws SQLException {
        return repo.countItems(idClient);
    }

    public BigDecimal getTotalHT(List<PanierItem> items) {
        return items.stream()
                .map(PanierItem::getPrixHT)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalTVA(List<PanierItem> items) {
        return items.stream()
                .map(PanierItem::getTVA)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalTTC(List<PanierItem> items) {
        return getTotalHT(items).add(getTotalTVA(items))
                .setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * ✅ Commander tout le panier.
     *
     * Architecture correcte :
     *  - 1 seule commande (table commande) avec les totaux HT/TVA/TTC
     *  - N lignes (table commande_ligne) une par article du panier
     *  - L'investisseur voit ses produits via JOIN commande_ligne
     *
     * @return id de la commande créée (ou 0 si panier vide)
     */
    public int commanderTout(int idClient) throws SQLException {
        List<PanierItem> items = repo.findByClient(idClient);
        if (items.isEmpty()) return 0;

        // Filtrer les articles valides
        List<PanierItem> valides = items.stream()
                .filter(i -> i.isDisponible() && i.getQuantite() > 0)
                .toList();
        if (valides.isEmpty()) return 0;

        // Calculer les totaux
        BigDecimal totalHT  = getTotalHT(valides);
        BigDecimal totalTVA = getTotalTVA(valides);
        BigDecimal totalTTC = getTotalTTC(valides);

        // ✅ Créer 1 commande principale
        Commande cmd = new Commande();
        cmd.setIdClient(idClient);
        cmd.setIdProduit(valides.get(0).getIdProduit()); // produit principal = premier article
        cmd.setQuantite(valides.get(0).getQuantite());
        cmd.setStatut("en_attente");
        cmd.setTotalHt(totalHT);
        cmd.setTotalTva(totalTVA);
        cmd.setTotalTtc(totalTTC);

        // Insère commande et récupère l'id généré
        int idCommande = commandeRepo.addAndGetId(cmd, totalHT, totalTVA, totalTTC);

        // ✅ Insérer les lignes dans commande_ligne
        for (PanierItem item : valides) {
            commandeRepo.addLigne(idCommande, item);
        }

        // Vider le panier
        repo.clear(idClient);
        return idCommande;
    }
}