package com.bizhub.model.services.marketplace;

import com.bizhub.model.services.common.config.EnvLoader;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TwilioSmsService {

    private static final Logger LOGGER = Logger.getLogger(TwilioSmsService.class.getName());

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsService() {

        // ✅ Lire depuis .env (via EnvLoader)
        this.accountSid = EnvLoader.getRequired("TWILIO_ACCOUNT_SID");
        this.authToken  = EnvLoader.getRequired("TWILIO_AUTH_TOKEN");
        this.fromNumber = EnvLoader.getRequired("TWILIO_FROM_NUMBER");

        Twilio.init(accountSid, authToken);
        LOGGER.info("✅ TwilioSmsService initialisé (from=" + fromNumber + ")");
    }

    public boolean sendSms(String toNumber, String body) {
        try {
            if (toNumber == null || toNumber.isBlank()) return false;
            if (body == null || body.isBlank()) return false;

            Message msg = Message.creator(
                    new com.twilio.type.PhoneNumber(toNumber),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    body
            ).create();

            LOGGER.info("✅ SMS envoyé (sid=" + msg.getSid() + ") to=" + toNumber);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ SMS échec : " + e.getMessage(), e);
            return false;
        }
    }
}