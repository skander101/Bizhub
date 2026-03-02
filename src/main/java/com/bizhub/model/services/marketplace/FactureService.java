package com.bizhub.model.services.marketplace;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.services.common.DB.MyDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FactureService — Génération PDF professionnel BizHub
 *
 * Contenu :
 *  Logo + en-tête | Numéro auto | Infos client
 *  Tableau produit | HT + TVA 19% + TTC | Statut paiement | QR code
 *
 * Dépendances à ajouter dans pom.xml :
 *
 *  <dependency>
 *    <groupId>com.itextpdf</groupId>
 *    <artifactId>itext7-core</artifactId>
 *    <version>7.2.5</version>
 *    <type>pom</type>
 *  </dependency>
 *  <dependency>
 *    <groupId>com.google.zxing</groupId>
 *    <artifactId>core</artifactId>
 *    <version>3.5.2</version>
 *  </dependency>
 *  <dependency>
 *    <groupId>com.google.zxing</groupId>
 *    <artifactId>javase</artifactId>
 *    <version>3.5.2</version>
 *  </dependency>
 */
public class FactureService {

    private static final Logger LOGGER = Logger.getLogger(FactureService.class.getName());

    private static final double TVA_RATE = 0.19; // TVA Tunisie 19%

    // Palette BizHub
    private static final DeviceRgb C_GREEN  = new DeviceRgb(58,  130, 246);  // Bleu plateforme #3A82F6
    private static final DeviceRgb C_DARK   = new DeviceRgb(17,  24,  39);
    private static final DeviceRgb C_DARK2  = new DeviceRgb(31,  41,  55);
    private static final DeviceRgb C_ROW    = new DeviceRgb(24,  33,  48);
    private static final DeviceRgb C_MUTED  = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb C_WHITE  = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb C_YELLOW = new DeviceRgb(232, 169,  58);  // Jaune plateforme #E8A93A

    private final Connection cnx;

    public FactureService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // =========================================================================
    // POINT D'ENTRÉE
    // =========================================================================

    public Path genererFacture(CommandeJoinProduit commande) throws Exception {
        ClientInfo client   = fetchClientInfo(commande.getIdClient());
        double     prixHT   = fetchPrixProduit(commande.getIdProduit());
        String     numFact  = genNumFacture(commande.getIdCommande());

        Path dir = Path.of(System.getProperty("user.home"), "Downloads", "BizHub_Factures");
        Files.createDirectories(dir);
        Path out = dir.resolve("Facture_" + numFact + ".pdf");

        try (PdfWriter  pw  = new PdfWriter(out.toFile());
             PdfDocument pd = new PdfDocument(pw);
             Document   doc = new Document(pd, PageSize.A4)) {

            doc.setMargins(40, 50, 40, 50);
            sectionHeader(doc, numFact, commande);
            sectionClient(doc, client);
            sectionTableau(doc, commande, prixHT);
            sectionTotaux(doc, commande, prixHT);
            sectionStatut(doc, commande);
            sectionQrEtFooter(doc, numFact, commande);
        }

        LOGGER.info("✅ Facture générée : " + out);
        return out;
    }

    // =========================================================================
    // 1. HEADER
    // =========================================================================

    private void sectionHeader(Document doc, String numFact, CommandeJoinProduit cmd) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(C_DARK2).setBorder(Border.NO_BORDER);

        // Gauche : logo/nom
        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(16);
        try {
            var logoStream = getClass().getClassLoader().getResourceAsStream("images/site-images/logo.png");
            if (logoStream != null) {
                left.add(new Image(ImageDataFactory.create(logoStream.readAllBytes()))
                        .setHeight(44).setAutoScale(false));
            } else {
                left.add(new Paragraph("BIZHUB").setFontColor(C_GREEN).setBold().setFontSize(28));
            }
        } catch (Exception e) {
            left.add(new Paragraph("BIZHUB").setFontColor(C_GREEN).setBold().setFontSize(28));
        }
        left.add(new Paragraph("Marketplace B2B Tunisie").setFontColor(C_MUTED).setFontSize(9).setMarginTop(2));
        left.add(new Paragraph("bizhub.app | contact@bizhub.app").setFontColor(C_MUTED).setFontSize(9));

        // Droite : numéro + date
        Cell right = new Cell().setBorder(Border.NO_BORDER).setPadding(16)
                .setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("FACTURE").setFontColor(C_GREEN).setBold().setFontSize(26));
        right.add(new Paragraph(numFact).setFontColor(C_WHITE).setBold().setFontSize(14));
        right.add(new Paragraph("Date : " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFontColor(C_MUTED).setFontSize(10));
        right.add(new Paragraph("Commande #" + cmd.getIdCommande())
                .setFontColor(C_MUTED).setFontSize(10));

        t.addCell(left).addCell(right);
        doc.add(t);
        doc.add(new LineSeparator(new SolidLine(2f)).setStrokeColor(C_GREEN));
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // =========================================================================
    // 2. CLIENT
    // =========================================================================

    private void sectionClient(Document doc, ClientInfo client) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(12)
                .setBackgroundColor(C_ROW);
        left.add(new Paragraph("FACTURÉ À").setFontColor(C_GREEN).setBold().setFontSize(9).setMarginBottom(5));
        left.add(new Paragraph(client.nom).setFontColor(C_WHITE).setBold().setFontSize(13));
        left.add(new Paragraph(client.email).setFontColor(C_MUTED).setFontSize(10));
        if (client.phone != null && !client.phone.isBlank())
            left.add(new Paragraph(client.phone).setFontColor(C_MUTED).setFontSize(10));

        Cell right = new Cell().setBorder(Border.NO_BORDER).setPadding(12);
        right.add(new Paragraph("ÉMIS PAR").setFontColor(C_GREEN).setBold().setFontSize(9).setMarginBottom(5));
        right.add(new Paragraph("BizHub SAS").setFontColor(C_WHITE).setBold().setFontSize(13));
        right.add(new Paragraph("Tunis, Tunisie").setFontColor(C_MUTED).setFontSize(10));
        right.add(new Paragraph("MF : 1234567/A/M/000").setFontColor(C_MUTED).setFontSize(10));
        right.add(new Paragraph("TVA : 19% (Code IRPP 2024)").setFontColor(C_MUTED).setFontSize(9));

        t.addCell(left).addCell(right);
        doc.add(t);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // =========================================================================
    // 3. TABLEAU PRODUIT
    // =========================================================================

    private void sectionTableau(Document doc, CommandeJoinProduit cmd, double prixHT) {
        doc.add(new Paragraph("DÉTAIL DE LA COMMANDE")
                .setFontColor(C_GREEN).setBold().setFontSize(10).setMarginBottom(6));

        Table table = new Table(UnitValue.createPercentArray(new float[]{38, 10, 17, 17, 18}))
                .setWidth(UnitValue.createPercentValue(100));

        String[] cols = {"Produit / Service", "Qté", "PU HT (TND)", "TVA/unité", "Total HT"};
        for (String h : cols) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(C_GREEN).setBorder(Border.NO_BORDER).setPadding(9)
                    .add(new Paragraph(h).setFontColor(C_DARK).setBold().setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER)));
        }

        double tvaUnit = round(prixHT * TVA_RATE);
        double totalHT = round(prixHT * cmd.getQuantiteCommande());

        String[] vals = {
                safe(cmd.getProduitNom()),
                String.valueOf(cmd.getQuantiteCommande()),
                fmt(prixHT),
                fmt(tvaUnit),
                fmt(totalHT)
        };
        for (String v : vals) {
            table.addCell(new Cell()
                    .setBackgroundColor(C_DARK2)
                    .setBorderTop(new SolidBorder(C_DARK, 0.5f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(9)
                    .add(new Paragraph(v).setFontColor(C_WHITE).setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER)));
        }
        doc.add(table);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // =========================================================================
    // 4. TOTAUX
    // =========================================================================

    private void sectionTotaux(Document doc, CommandeJoinProduit cmd, double prixHT) {
        double ht  = round(prixHT * cmd.getQuantiteCommande());
        double tva = round(ht * TVA_RATE);
        double ttc = round(ht + tva);

        Table outer = new Table(UnitValue.createPercentArray(new float[]{52, 48}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
        outer.addCell(new Cell().setBorder(Border.NO_BORDER)); // vide gauche

        Cell box = new Cell().setBorder(new SolidBorder(C_GREEN, 1.5f)) // bleu
                .setBackgroundColor(C_DARK2).setPadding(14);
        addTotalLine(box, "Sous-total HT", fmt(ht), false);
        addTotalLine(box, "TVA (19%)",      fmt(tva), false);
        box.add(new LineSeparator(new SolidLine(1f))
                .setStrokeColor(C_GREEN).setMarginTop(5).setMarginBottom(5));
        addTotalLine(box, "TOTAL TTC",      fmt(ttc), true);

        outer.addCell(box);
        doc.add(outer);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addTotalLine(Cell parent, String label, String value, boolean big) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        Paragraph pLabel = new Paragraph(label)
                .setFontColor(big ? C_YELLOW : C_MUTED)
                .setFontSize(big ? 12 : 10);
        if (big) pLabel.setBold();

        Paragraph pValue = new Paragraph(value)
                .setFontColor(big ? C_GREEN : C_WHITE)
                .setFontSize(big ? 14 : 10);
        if (big) pValue.setBold();

        row.addCell(new Cell().setBorder(Border.NO_BORDER).add(pLabel));
        row.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT).add(pValue));
        parent.add(row);
    }

    // =========================================================================
    // 5. STATUT PAIEMENT
    // =========================================================================

    private void sectionStatut(Document doc, CommandeJoinProduit cmd) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(C_GREEN, 1.5f)) // bleu
                .setBackgroundColor(C_ROW).setPadding(12);

        cell.add(new Paragraph("STATUT DU PAIEMENT")
                .setFontColor(C_GREEN).setBold().setFontSize(9).setMarginBottom(4));

        String txt = cmd.isEstPayee()
                ? "PAYEE  -  Paiement Stripe confirme"
                : "EN ATTENTE  -  Paiement non encore recu";
        cell.add(new Paragraph(txt)
                .setFontColor(cmd.isEstPayee() ? C_GREEN : C_YELLOW)
                .setBold().setFontSize(12));

        if (cmd.getPaymentRef() != null && !cmd.getPaymentRef().isBlank())
            cell.add(new Paragraph("Ref. Stripe : " + cmd.getPaymentRef())
                    .setFontColor(C_MUTED).setFontSize(9).setMarginTop(4));
        if (cmd.getPaidAt() != null)
            cell.add(new Paragraph("Date paiement : " +
                    cmd.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .setFontColor(C_MUTED).setFontSize(9));

        doc.add(new Table(1).setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER).addCell(cell));
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // =========================================================================
    // 6. QR CODE + FOOTER
    // =========================================================================

    private void sectionQrEtFooter(Document doc, String numFact, CommandeJoinProduit cmd) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        Cell left = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(8);
        left.add(new Paragraph("CONDITIONS").setFontColor(C_GREEN).setBold().setFontSize(9));
        left.add(new Paragraph("Paiement traite via la plateforme BizHub (Stripe).")
                .setFontColor(C_MUTED).setFontSize(8));
        left.add(new Paragraph("TVA au taux legal tunisien de 19% (art. 7 CIRPPIS).")
                .setFontColor(C_MUTED).setFontSize(8));
        left.add(new Paragraph("Document genere automatiquement - valeur probante.")
                .setFontColor(C_MUTED).setFontSize(8));

        Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);

        try {
            String qrData = "BIZHUB|" + numFact + "|CMD:" + cmd.getIdCommande()
                    + "|PAY:" + (cmd.isEstPayee() ? "1" : "0")
                    + "|REF:" + (cmd.getPaymentRef() != null ? cmd.getPaymentRef() : "");
            BitMatrix bm = new MultiFormatWriter().encode(qrData, BarcodeFormat.QR_CODE, 110, 110);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(bm), "PNG", baos);
            right.add(new Paragraph("Verification").setFontColor(C_MUTED).setFontSize(8));
            right.add(new Image(ImageDataFactory.create(baos.toByteArray()))
                    .setHeight(78).setAutoScale(false));
        } catch (Exception e) {
            right.add(new Paragraph("QR N/A").setFontColor(C_MUTED).setFontSize(8));
        }

        t.addCell(left).addCell(right);
        doc.add(t);

        doc.add(new LineSeparator(new SolidLine(0.5f)).setStrokeColor(C_MUTED).setMarginTop(10));
        doc.add(new Paragraph("BizHub  -  Marketplace B2B Tunisie  |  bizhub.app  |  contact@bizhub.app")
                .setFontColor(C_MUTED).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));
    }

    // =========================================================================
    // HELPERS DB
    // =========================================================================

    private ClientInfo fetchClientInfo(int id) {
        ClientInfo ci = new ClientInfo("Client #" + id, "client@bizhub.app", "");
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT nom, prenom, email, phone FROM user WHERE user_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ci.nom   = (safe(rs.getString("prenom")) + " " + safe(rs.getString("nom"))).trim();
                    ci.email = safe(rs.getString("email"));
                    ci.phone = safe(rs.getString("phone"));
                }
            }
        } catch (Exception e) { LOGGER.warning("fetchClientInfo: " + e.getMessage()); }
        return ci;
    }

    private double fetchPrixProduit(int idProduit) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT prix FROM produit_service WHERE id_produit=?")) {
            ps.setInt(1, idProduit);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("prix");
            }
        } catch (Exception e) { LOGGER.warning("fetchPrixProduit: " + e.getMessage()); }
        return 0.0;
    }

    private String genNumFacture(int id) {
        return String.format("FACT-%d-%04d", LocalDate.now().getYear(), id);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private String fmt(double v) { return String.format("%.3f TND", v); }
    private String safe(String s) { return s == null ? "" : s.trim(); }

    // ✅ Classe mutable (pas record) pour permettre la réassignation des champs
    private static class ClientInfo {
        String nom;
        String email;
        String phone;
        ClientInfo(String nom, String email, String phone) {
            this.nom   = nom;
            this.email = email;
            this.phone = phone;
        }
    }
}