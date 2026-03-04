package com.bizhub.model.services.investissement;

import com.bizhub.model.investissement.Deal;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Logger;

public class ContractPDFService {

    private static final Logger logger = Logger.getLogger(ContractPDFService.class.getName());
    private static final DeviceRgb GOLD = new DeviceRgb(232, 169, 58);
    private static final DeviceRgb NAVY = new DeviceRgb(10, 25, 47);

    public String generateContract(Deal deal) throws Exception {
        String dir = System.getProperty("user.home") + File.separator + "bizhub_contracts";
        new File(dir).mkdirs();
        String filePath = dir + File.separator + "contract_" + deal.getDealId() + "_"
                + System.currentTimeMillis() + ".pdf";

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);

            Paragraph title = new Paragraph("INVESTMENT CONTRACT")
                    .setFontSize(24).setBold().setFontColor(NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            doc.add(title);

            Paragraph subtitle = new Paragraph("BizHub Investment Platform")
                    .setFontSize(12).setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            doc.add(subtitle);

            doc.add(createSeparator());

            doc.add(new Paragraph("Contract Details")
                    .setFontSize(16).setBold().setFontColor(NAVY).setMarginTop(15));

            Table details = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth().setMarginTop(10);

            addRow(details, "Contract ID:", "BH-" + String.format("%06d", deal.getDealId()));
            addRow(details, "Date:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")));
            addRow(details, "Project:", deal.getProjectTitle() != null ? deal.getProjectTitle() : "Project #" + deal.getProjectId());

            NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.FRANCE);
            addRow(details, "Amount:", cf.format(deal.getAmount()) + " EUR");

            doc.add(details);

            doc.add(createSeparator());

            doc.add(new Paragraph("Parties")
                    .setFontSize(16).setBold().setFontColor(NAVY).setMarginTop(15));

            Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth().setMarginTop(10);

            Cell buyerCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            buyerCell.add(new Paragraph("INVESTOR (Buyer)").setBold().setFontColor(GOLD));
            buyerCell.add(new Paragraph(deal.getBuyerName() != null ? deal.getBuyerName() : "Investor #" + deal.getBuyerId()));
            buyerCell.add(new Paragraph(deal.getBuyerEmail() != null ? deal.getBuyerEmail() : ""));
            parties.addCell(buyerCell);

            Cell sellerCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            sellerCell.add(new Paragraph("STARTUP (Seller)").setBold().setFontColor(GOLD));
            sellerCell.add(new Paragraph(deal.getSellerName() != null ? deal.getSellerName() : "Startup #" + deal.getSellerId()));
            sellerCell.add(new Paragraph(deal.getSellerEmail() != null ? deal.getSellerEmail() : ""));
            parties.addCell(sellerCell);

            doc.add(parties);

            doc.add(createSeparator());

            doc.add(new Paragraph("Terms and Conditions")
                    .setFontSize(16).setBold().setFontColor(NAVY).setMarginTop(15));

            String terms = """
                1. The Investor agrees to invest the specified amount into the Project.
                2. The Startup agrees to use the funds exclusively for the stated project purposes.
                3. Both parties acknowledge that investments carry inherent risks.
                4. Quarterly progress reports shall be provided by the Startup to the Investor.
                5. This contract is governed by the applicable laws of the jurisdiction.
                6. Any disputes shall be resolved through arbitration.""";

            doc.add(new Paragraph(terms).setFontSize(10).setMarginTop(10));

            doc.add(createSeparator());

            Table signatures = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth().setMarginTop(30);

            Cell sigBuyer = new Cell().setBorder(Border.NO_BORDER).setPadding(20);
            sigBuyer.add(new Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER));
            sigBuyer.add(new Paragraph("Investor Signature").setTextAlignment(TextAlignment.CENTER).setFontSize(10).setFontColor(GOLD));

            Cell sigSeller = new Cell().setBorder(Border.NO_BORDER).setPadding(20);
            sigSeller.add(new Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER));
            sigSeller.add(new Paragraph("Startup Signature").setTextAlignment(TextAlignment.CENTER).setFontSize(10).setFontColor(GOLD));

            signatures.addCell(sigBuyer);
            signatures.addCell(sigSeller);
            doc.add(signatures);

            Paragraph footer = new Paragraph("Generated by BizHub Platform - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFontSize(8).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(30);
            doc.add(footer);
        }

        logger.info("Contract PDF generated: " + filePath);
        return filePath;
    }

    private LineSeparator createSeparator() {
        return new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1))
                .setMarginTop(10).setMarginBottom(10);
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label).setBold().setFontSize(11)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value).setFontSize(11)));
    }
}
