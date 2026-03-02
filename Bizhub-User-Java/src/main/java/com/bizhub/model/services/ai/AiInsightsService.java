package com.bizhub.model.services.ai;

import com.bizhub.model.marketplace.InvestorInsightResponse;
import com.bizhub.model.marketplace.StatsPoint;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

public class AiInsightsService {

    private static final Logger LOGGER = Logger.getLogger(AiInsightsService.class.getName());

    private final OpenAiClient client = new OpenAiClient();
    private final Gson gson = new Gson();

    public InvestorInsightResponse analyze(List<StatsPoint> points) throws Exception {

        String system = """
                Tu es un analyste IA pour un dashboard investisseur B2B (marketplace BizHub).
                Objectif : analyser la performance des commandes (confirmées vs annulées), détecter des anomalies et donner des recommandations actionnables.
                Réponds STRICTEMENT en JSON valide avec ce format exact (sans texte avant ou après) :
                {
                  "summary": "Résumé concis de la performance en 2-3 phrases.",
                  "anomalies": ["anomalie 1", "anomalie 2"],
                  "recommendations": ["recommandation 1", "recommandation 2", "recommandation 3"]
                }
                Langue: français. Style: professionnel, concis, orienté action.
                """;

        String user = buildUserPayload(points);
        LOGGER.info("AiInsightsService.analyze() — " + (points == null ? 0 : points.size()) + " points");

        String raw = client.generateText(system, user);
        LOGGER.info("OpenAI réponse brute: " + raw.substring(0, Math.min(200, raw.length())));

        String jsonOnly = extractJsonObject(raw);
        JsonObject obj  = gson.fromJson(jsonOnly, JsonObject.class);

        InvestorInsightResponse r = new InvestorInsightResponse();

        if (obj.has("summary"))
            r.setSummary(obj.get("summary").getAsString());

        if (obj.has("anomalies") && obj.get("anomalies").isJsonArray())
            r.setAnomalies(gson.fromJson(obj.get("anomalies"), List.class));

        if (obj.has("recommendations") && obj.get("recommendations").isJsonArray())
            r.setRecommendations(gson.fromJson(obj.get("recommendations"), List.class));

        return r;
    }

    private String buildUserPayload(List<StatsPoint> points) {
        if (points == null || points.isEmpty())
            return "Aucune donnée disponible pour la période sélectionnée.";

        // ✅ Calcul des totaux pour le contexte
        int totalConf = 0, totalAnn = 0;
        BigDecimal totalMontantConf = BigDecimal.ZERO;
        BigDecimal totalMontantAnn  = BigDecimal.ZERO;

        StringBuilder sb = new StringBuilder();
        sb.append("Données de performance investisseur (").append(points.size()).append(" jours) :\n\n");
        sb.append("DATE        | CONFIRMÉES (nb) | MONTANT CONF (TND) | ANNULÉES (nb) | MONTANT ANN (TND)\n");
        sb.append("------------|-----------------|-------------------|---------------|------------------\n");

        for (StatsPoint p : points) {
            int conf = p.getConfirmees();
            int ann  = p.getAnnulees();
            BigDecimal mc = p.getMontantConf();
            BigDecimal ma = p.getMontantAnn();

            totalConf += conf;
            totalAnn  += ann;
            totalMontantConf = totalMontantConf.add(mc);
            totalMontantAnn  = totalMontantAnn.add(ma);

            // N'inclure que les jours avec activité pour ne pas surcharger
            if (conf > 0 || ann > 0) {
                sb.append(String.format("%-12s | %-15d | %-19.2f | %-13d | %.2f%n",
                        p.getDate(), conf, mc.doubleValue(), ann, ma.doubleValue()));
            }
        }

        sb.append("\n── TOTAUX ──────────────────────────────────────────\n");
        sb.append("Total confirmées : ").append(totalConf)
                .append(" commandes | ").append(String.format("%.2f TND%n", totalMontantConf.doubleValue()));
        sb.append("Total annulées   : ").append(totalAnn)
                .append(" commandes | ").append(String.format("%.2f TND%n", totalMontantAnn.doubleValue()));

        double txAnnulation = (totalConf + totalAnn) > 0
                ? (100.0 * totalAnn / (totalConf + totalAnn)) : 0;
        sb.append(String.format("Taux d'annulation : %.1f%%%n", txAnnulation));

        sb.append("\nAnalyse cette performance, détecte les anomalies et donne des recommandations.");

        return sb.toString();
    }

    private String extractJsonObject(String s) {
        if (s == null) return "{}";
        // Enlever les blocs markdown ```json ... ```
        s = s.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a >= 0 && b > a) return s.substring(a, b + 1);
        return "{}";
    }
}