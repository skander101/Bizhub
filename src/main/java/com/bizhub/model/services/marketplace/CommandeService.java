package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
public class CommandeService {

    private static final Logger LOGGER = Logger.getLogger(CommandeService.class.getName());
    private static final String STATUT_CONFIRMEE = "confirmee";

    private final CommandeRepository repo = new CommandeRepository();

    // lazy init: Twilio seulement si configuré
    private TwilioSmsService sms;

    public void ajouter(Commande c) throws SQLException {
        repo.add(c);
    }


    public void changerStatut(int idCommande, String statut) throws SQLException {
        repo.updateStatut(idCommande, statut);

        // ✅ SMS au startup quand INVEST confirme
        if (STATUT_CONFIRMEE.equalsIgnoreCase(safe(statut))) {
            notifyStartupCommandeConfirmee(idCommande);
        }
    }

    public int changerStatutSiEnAttente(int idCommande, String statut) throws SQLException {
        int rows = repo.updateStatutIfEnAttente(idCommande, statut);

        if (rows > 0 && STATUT_CONFIRMEE.equalsIgnoreCase(safe(statut))) {
            notifyStartupCommandeConfirmee(idCommande);
        }
        return rows;
    }

    public void supprimer(int idCommande) throws SQLException {
        repo.delete(idCommande);
    }

    public List<CommandeJoinProduit> getAllJoinProduit() throws SQLException {
        return repo.findAllJoinProduit();
    }

    public List<CommandeJoinProduit> getByClientJoinProduit(int idClient) throws SQLException {
        return repo.findByClientJoinProduit(idClient);
    }

    public List<CommandeJoinProduit> getByOwnerJoinProduit(int ownerId) throws SQLException {
        return repo.findByOwnerJoinProduit(ownerId);
    }

    public int setPaymentInitiatedIfNull(int idCommande, String ref, String url) throws SQLException {
        return repo.setPaymentInitiatedIfNull(idCommande, ref, url);
    }

    public String getPaymentUrl(int idCommande) throws SQLException {
        return repo.getPaymentUrl(idCommande);
    }

    public int markAsPaid(int idCommande, String paymentRef) throws SQLException {
        return repo.markAsPaid(idCommande, paymentRef);
    }

    // =========================
    // TWILIO SMS
    // =========================
    private void notifyStartupCommandeConfirmee(int idCommande) {
        try {
            String phone = repo.findStartupPhoneByCommandeId(idCommande);
            if (phone == null || phone.isBlank()) {
                LOGGER.warning("SMS non envoyé: phone vide pour commande #" + idCommande);
                return;
            }

            String msg = "BizHub ✅ Votre commande #" + idCommande
                    + " est confirmée. Vous pouvez procéder au paiement dans l’application.";

            if (sms == null) sms = new TwilioSmsService(); // peut throw si pas configuré
            boolean ok = sms.sendSms(phone.trim(), msg);

            if (ok) LOGGER.info("✅ SMS envoyé au startup pour commande #" + idCommande);
            else LOGGER.warning("⚠ SMS échec (Twilio) pour commande #" + idCommande);

        } catch (Exception e) {
            // IMPORTANT: ne jamais casser la confirmation si Twilio échoue
            LOGGER.log(Level.WARNING, "⚠ Erreur SMS Twilio (non bloquante) : " + e.getMessage(), e);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

}