package com.bizhub.model.services.marketplace.payment;

import com.bizhub.model.marketplace.CommandeJoinProduit;

public interface PaymentProvider {
    PaymentResult createCheckout(CommandeJoinProduit commande) throws Exception;
}