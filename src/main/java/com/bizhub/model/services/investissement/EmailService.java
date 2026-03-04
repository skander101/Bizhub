package com.bizhub.model.services.investissement;

import com.bizhub.model.services.common.config.ApiConfig;
import javafx.concurrent.Task;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", ApiConfig.SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(ApiConfig.SMTP_PORT));
        props.put("mail.smtp.ssl.trust", ApiConfig.SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(ApiConfig.getSmtpUsername(), ApiConfig.getSmtpAppPassword());
            }
        });
    }

    public void sendDealConfirmation(String toEmail, String recipientName,
                                      String projectTitle, String amount,
                                      String pdfPath) throws Exception {
        Session session = getMailSession();
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(ApiConfig.getSmtpUsername(), "BizHub Platform"));
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        mimeMessage.setSubject("BizHub - Deal Confirmed: " + projectTitle);

        MimeBodyPart textPart = new MimeBodyPart();
        String html = String.format("""
            <div style="font-family: 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto;
                        background: linear-gradient(135deg, #0A192F, #172A45); color: #E8A93A;
                        border-radius: 16px; padding: 32px;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #E8A93A; font-size: 28px; margin: 0;">BizHub</h1>
                    <p style="color: #FDB813; font-size: 14px;">Investment Platform</p>
                </div>
                <div style="background: rgba(23,42,69,0.9); border-radius: 12px; padding: 24px;
                            border: 1px solid rgba(232,169,58,0.3);">
                    <h2 style="color: #fff; margin-top: 0;">Deal Confirmed!</h2>
                    <p style="color: #E8A93A;">Dear %s,</p>
                    <p style="color: #ddd;">Your investment deal for <strong style="color: #FDB813;">%s</strong>
                       has been successfully completed.</p>
                    <div style="background: rgba(232,169,58,0.1); border-radius: 8px; padding: 16px; margin: 16px 0;
                                border-left: 4px solid #E8A93A;">
                        <p style="margin: 0; color: #E8A93A;"><strong>Amount:</strong> %s EUR</p>
                        <p style="margin: 4px 0 0; color: #E8A93A;"><strong>Status:</strong> Confirmed</p>
                    </div>
                    <p style="color: #ddd;">The signed contract is attached to this email.</p>
                </div>
                <p style="text-align: center; color: #FDB813; font-size: 12px; margin-top: 24px;">
                    BizHub Investment Platform &copy; 2026
                </p>
            </div>""", recipientName, projectTitle, amount);
        textPart.setContent(html, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        if (pdfPath != null && new File(pdfPath).exists()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(pdfPath));
            attachmentPart.setFileName("BizHub_Contract.pdf");
            multipart.addBodyPart(attachmentPart);
        }

        mimeMessage.setContent(multipart);
        Transport.send(mimeMessage);
        logger.info("Deal confirmation email sent to: " + toEmail);
    }

    public void sendNegotiationNotification(String toEmail, String recipientName,
                                             String projectTitle, String senderName) throws Exception {
        Session session = getMailSession();
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(ApiConfig.getSmtpUsername(), "BizHub Platform"));
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        mimeMessage.setSubject("BizHub - New Negotiation Message: " + projectTitle);

        String html = String.format("""
            <div style="font-family: 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto;
                        background: linear-gradient(135deg, #0A192F, #172A45); color: #E8A93A;
                        border-radius: 16px; padding: 32px;">
                <h1 style="text-align: center; color: #E8A93A;">BizHub</h1>
                <div style="background: rgba(23,42,69,0.9); border-radius: 12px; padding: 24px;
                            border: 1px solid rgba(232,169,58,0.3);">
                    <p style="color: #E8A93A;">Dear %s,</p>
                    <p style="color: #ddd;"><strong style="color: #FDB813;">%s</strong> has sent you a new message
                       regarding the project <strong style="color: #FDB813;">%s</strong>.</p>
                    <p style="color: #ddd;">Log in to BizHub to view and respond.</p>
                </div>
            </div>""", recipientName, senderName, projectTitle);

        mimeMessage.setContent(html, "text/html; charset=utf-8");
        Transport.send(mimeMessage);
        logger.info("Negotiation notification sent to: " + toEmail);
    }

    public Task<Void> sendDealConfirmationAsync(String toEmail, String name,
                                                  String projectTitle, String amount, String pdfPath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                sendDealConfirmation(toEmail, name, projectTitle, amount, pdfPath);
                return null;
            }
        };
    }
}
