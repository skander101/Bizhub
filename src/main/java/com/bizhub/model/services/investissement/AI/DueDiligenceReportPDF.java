package com.bizhub.model.services.investissement.AI;

import com.bizhub.model.investissement.Project;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

public class DueDiligenceReportPDF {
    private static final Logger logger = Logger.getLogger(DueDiligenceReportPDF.class.getName());

    private static final DeviceRgb NAVY = new DeviceRgb(10, 25, 47);
    private static final DeviceRgb CYAN = new DeviceRgb(6, 182, 212);
    private static final DeviceRgb GOLD = new DeviceRgb(232, 169, 58);
    private static final DeviceRgb GREEN = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb RED = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb BLUE = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb PURPLE = new DeviceRgb(139, 92, 246);
    private static final DeviceRgb AMBER = new DeviceRgb(245, 158, 11);
    private static final DeviceRgb LIGHT = new DeviceRgb(200, 210, 230);
    private static final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.US);

    public static File export(Project project, JsonObject report) throws Exception {
        String filename = "BizHub_DueDiligence_" + project.getProjectId() + ".pdf";
        File file = new File(System.getProperty("user.home"), filename);
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        doc.setMargins(36, 36, 36, 36);

        addHeader(doc, project);
        addProjectSummary(doc, report);
        addRiskMatrix(doc, report);
        addSwot(doc, report);
        addMarketContext(doc, report);
        addSectorOutlook(doc, report);
        addValuation(doc, report);
        addRecommendation(doc, report);
        addFooter(doc);

        doc.close();
        logger.info("Due diligence PDF exported to: " + file.getAbsolutePath());
        return file;
    }

    private static void addHeader(Document doc, Project project) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(18);

        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph("BizHub").setFontSize(24).setBold().setFontColor(CYAN));
        left.add(new Paragraph("AI Due Diligence Report").setFontSize(11).setFontColor(LIGHT));
        header.addCell(left);

        Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph(project.getTitle()).setFontSize(16).setBold().setFontColor(ColorConstants.BLACK));
        right.add(new Paragraph("Budget: " + currencyFmt.format(project.getRequiredBudget()) + " TND")
                .setFontSize(10).setFontColor(LIGHT));
        right.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .setFontSize(9).setFontColor(LIGHT));
        header.addCell(right);

        doc.add(header);
        SolidLine cyanLine = new SolidLine(1);
        cyanLine.setColor(CYAN);
        doc.add(new LineSeparator(cyanLine).setMarginBottom(14));
    }

    private static void addProjectSummary(Document doc, JsonObject report) {
        if (!report.has("projectSummary")) return;
        JsonObject ps = report.getAsJsonObject("projectSummary");
        doc.add(sectionTitle("Project Summary", CYAN));
        if (ps.has("oneLine")) {
            doc.add(new Paragraph(ps.get("oneLine").getAsString()).setItalic().setFontSize(11).setMarginBottom(6));
        }
        if (ps.has("keyNumbers") && ps.get("keyNumbers").isJsonArray()) {
            for (JsonElement el : ps.getAsJsonArray("keyNumbers")) {
                doc.add(bullet(el.getAsString()));
            }
        }
        doc.add(spacer());
    }

    private static void addRiskMatrix(Document doc, JsonObject report) {
        if (!report.has("riskMatrix")) return;
        JsonObject rm = report.getAsJsonObject("riskMatrix");
        doc.add(sectionTitle("Risk Matrix", RED));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(8);
        String[] keys = {"financial", "market", "team", "execution", "regulatory"};
        for (String key : keys) {
            int val = rm.has(key) ? rm.get(key).getAsInt() : 0;
            DeviceRgb color = val <= 3 ? GREEN : val <= 6 ? AMBER : RED;
            Cell cell = new Cell().setTextAlignment(TextAlignment.CENTER).setBorder(new SolidBorder(LIGHT, 0.5f)).setPadding(8);
            cell.add(new Paragraph(val + "/10").setFontSize(18).setBold().setFontColor(color));
            cell.add(new Paragraph(key.substring(0, 1).toUpperCase() + key.substring(1)).setFontSize(9).setFontColor(LIGHT));
            table.addCell(cell);
        }
        doc.add(table);
        if (rm.has("overallComment")) {
            doc.add(new Paragraph(rm.get("overallComment").getAsString()).setFontSize(10).setFontColor(LIGHT));
        }
        doc.add(spacer());
    }

    private static void addSwot(Document doc, JsonObject report) {
        if (!report.has("swot")) return;
        JsonObject swot = report.getAsJsonObject("swot");
        doc.add(sectionTitle("SWOT Analysis", GOLD));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(8);
        String[][] parts = {
                {"strengths", "Strengths"}, {"weaknesses", "Weaknesses"},
                {"opportunities", "Opportunities"}, {"threats", "Threats"}
        };
        DeviceRgb[] colors = {GREEN, RED, BLUE, AMBER};
        for (int i = 0; i < parts.length; i++) {
            Cell cell = new Cell().setPadding(8).setBorder(new SolidBorder(LIGHT, 0.5f));
            cell.add(new Paragraph(parts[i][1]).setBold().setFontSize(11).setFontColor(colors[i]));
            if (swot.has(parts[i][0]) && swot.get(parts[i][0]).isJsonArray()) {
                for (JsonElement el : swot.getAsJsonArray(parts[i][0])) {
                    cell.add(new Paragraph("• " + el.getAsString()).setFontSize(9));
                }
            }
            table.addCell(cell);
        }
        doc.add(table);
        doc.add(spacer());
    }

    private static void addMarketContext(Document doc, JsonObject report) {
        if (!report.has("marketContext")) return;
        JsonObject mc = report.getAsJsonObject("marketContext");
        doc.add(sectionTitle("Market Context", AMBER));
        if (mc.has("tone")) {
            String tone = mc.get("tone").getAsString();
            DeviceRgb tColor = switch (tone) {
                case "positive" -> GREEN;
                case "negative" -> RED;
                default -> AMBER;
            };
            doc.add(new Paragraph("Sentiment: " + tone.toUpperCase())
                    .setFontColor(tColor).setFontSize(11).setBold().setMarginBottom(4));
        }
        if (mc.has("headlineSummary") && mc.get("headlineSummary").isJsonArray()) {
            for (JsonElement el : mc.getAsJsonArray("headlineSummary")) {
                doc.add(bullet(el.getAsString()));
            }
        }
        if (mc.has("notes")) {
            doc.add(new Paragraph(mc.get("notes").getAsString()).setFontSize(10).setFontColor(LIGHT));
        }
        doc.add(spacer());
    }

    private static void addSectorOutlook(Document doc, JsonObject report) {
        if (!report.has("sectorOutlook")) return;
        JsonObject so = report.getAsJsonObject("sectorOutlook");
        doc.add(sectionTitle("Sector Outlook", PURPLE));
        if (so.has("keySectors") && so.get("keySectors").isJsonArray()) {
            for (JsonElement el : so.getAsJsonArray("keySectors")) {
                if (!el.isJsonObject()) continue;
                JsonObject s = el.getAsJsonObject();
                String name = s.has("name") ? s.get("name").getAsString() : "";
                double perf = s.has("performance") ? s.get("performance").getAsDouble() : 0;
                String comment = s.has("comment") ? s.get("comment").getAsString() : "";
                DeviceRgb pColor = perf >= 0 ? GREEN : RED;
                doc.add(new Paragraph(name + "  " + String.format("%+.1f%%", perf) + "  " + comment)
                        .setFontSize(10).setFontColor(pColor));
            }
        }
        if (so.has("overallComment")) {
            doc.add(new Paragraph(so.get("overallComment").getAsString()).setFontSize(10).setFontColor(LIGHT));
        }
        doc.add(spacer());
    }

    private static void addValuation(Document doc, JsonObject report) {
        if (!report.has("valuation")) return;
        JsonObject val = report.getAsJsonObject("valuation");
        doc.add(sectionTitle("Valuation Estimate", BLUE));
        double vMin = val.has("valuationMin") ? val.get("valuationMin").getAsDouble() : 0;
        double vMax = val.has("valuationMax") ? val.get("valuationMax").getAsDouble() : 0;
        doc.add(new Paragraph(currencyFmt.format(vMin) + " - " + currencyFmt.format(vMax) + " TND")
                .setFontSize(18).setBold().setFontColor(BLUE).setMarginBottom(4));
        if (val.has("comment")) {
            doc.add(new Paragraph(val.get("comment").getAsString()).setFontSize(10).setFontColor(LIGHT));
        }
        doc.add(spacer());
    }

    private static void addRecommendation(Document doc, JsonObject report) {
        if (!report.has("finalRecommendation")) return;
        JsonObject rec = report.getAsJsonObject("finalRecommendation");
        doc.add(sectionTitle("Final Recommendation", CYAN));
        if (rec.has("label")) {
            String label = rec.get("label").getAsString();
            DeviceRgb recColor = switch (label.toUpperCase()) {
                case "INVEST" -> GREEN;
                case "AVOID" -> RED;
                default -> AMBER;
            };
            doc.add(new Paragraph(label).setFontSize(16).setBold().setFontColor(recColor).setMarginBottom(6));
        }
        if (rec.has("summary")) {
            doc.add(new Paragraph(rec.get("summary").getAsString()).setFontSize(11).setMarginBottom(6));
        }
        if (rec.has("nextSteps") && rec.get("nextSteps").isJsonArray()) {
            doc.add(new Paragraph("Next Steps:").setBold().setFontSize(10).setFontColor(CYAN));
            for (JsonElement el : rec.getAsJsonArray("nextSteps")) {
                doc.add(bullet(el.getAsString()));
            }
        }
        doc.add(spacer());
    }

    private static void addFooter(Document doc) {
        SolidLine footerLine = new SolidLine(0.5f);
        footerLine.setColor(CYAN);
        doc.add(new LineSeparator(footerLine).setMarginTop(10));
        doc.add(new Paragraph("Generated by BizHub AI Due Diligence Agent")
                .setFontSize(8).setFontColor(LIGHT).setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
    }

    private static Paragraph sectionTitle(String title, DeviceRgb color) {
        return new Paragraph(title)
                .setFontSize(14).setBold().setFontColor(color)
                .setMarginTop(12).setMarginBottom(6)
                .setBorderBottom(new SolidBorder(color, 1));
    }

    private static Paragraph bullet(String text) {
        return new Paragraph("  •  " + text).setFontSize(10).setMarginLeft(8);
    }

    private static Paragraph spacer() {
        return new Paragraph(" ").setFontSize(4);
    }
}
