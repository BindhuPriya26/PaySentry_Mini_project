package com.paysentry.backend.monitoring;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.paysentry.backend.entity.UnifiedRiskLevel;

/** Configurable unified-risk thresholds, validated once on startup. */
@Component
public class RiskThresholds {

    private final int medium;
    private final int high;
    private final int critical;

    public RiskThresholds(
        @Value("${paysentry.risk.medium-threshold:31}") int medium,
        @Value("${paysentry.risk.high-threshold:71}") int high,
        @Value("${paysentry.risk.critical-threshold:91}") int critical
    ) {
        this.medium = medium;
        this.high = high;
        this.critical = critical;
    }

    @PostConstruct
    void validate() {
        if (medium < 1 || medium >= high || high >= critical || critical > 100) {
            throw new IllegalStateException("Unified-risk thresholds must satisfy 1 <= medium < high < critical <= 100.");
        }
    }

    public UnifiedRiskLevel classify(int score) {
        if (score >= critical) {
            return UnifiedRiskLevel.CRITICAL;
        }
        if (score >= high) {
            return UnifiedRiskLevel.HIGH;
        }
        if (score >= medium) {
            return UnifiedRiskLevel.MEDIUM;
        }
        return UnifiedRiskLevel.LOW;
    }
}
