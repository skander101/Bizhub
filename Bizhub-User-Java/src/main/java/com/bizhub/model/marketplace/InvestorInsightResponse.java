package com.bizhub.model.marketplace;

import java.util.ArrayList;
import java.util.List;

public class InvestorInsightResponse {

    private String summary;                 // texte global
    private List<String> anomalies = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public InvestorInsightResponse() {}

    public InvestorInsightResponse(String summary) {
        this.summary = summary;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getAnomalies() { return anomalies; }
    public void setAnomalies(List<String> anomalies) { this.anomalies = anomalies; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}