package com.bizhub.model.services.marketplace;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwilioSmsService {

    private static final Logger LOGGER = Logger.getLogger(TwilioSmsService.class.getName());

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsService() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("twilio.properties")) {
            if (in == null) throw new IllegalStateException("twilio.properties introuvable dans src/main/resources/");
            props.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de lire twilio.properties : " + e.getMessage(), e);
        }

        accountSid = props.getProperty("twilio.account.sid", "").trim();
        authToken  = props.getProperty("twilio.auth.token", "").trim();
        fromNumber = props.getProperty("twilio.from.number", "").trim();

        if (accountSid.isEmpty() || authToken.isEmpty() || fromNumber.isEmpty()) {
            throw new IllegalStateException("twilio.properties incomplet (sid/token/from).");
        }

        Twilio.init(accountSid, authToken);
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