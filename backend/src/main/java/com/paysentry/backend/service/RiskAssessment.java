package com.paysentry.backend.service;

import java.util.List;

import com.paysentry.backend.entity.RiskLevel;

public class RiskAssessment {

    private final RiskLevel riskLevel;
    private final int riskScore;
    private final String explanation;
    private final List<String> findings;
    private final List<String> recommendations;
    private final String extractedUrl;

    public RiskAssessment(
        RiskLevel riskLevel,
        int riskScore,
        String explanation,
        List<String> findings,
        List<String> recommendations,
        String extractedUrl
    ) {
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.explanation = explanation;
        this.findings = findings;
        this.recommendations = recommendations;
        this.extractedUrl = extractedUrl;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<String> getFindings() {
        return findings;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String getExtractedUrl() {
        return extractedUrl;
    }
}
