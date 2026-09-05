package com.paysentry.backend.entity;

public enum UnifiedRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static UnifiedRiskLevel fromScore(int score) {
        if (score >= 91) {
            return CRITICAL;
        }
        if (score >= 71) {
            return HIGH;
        }
        if (score >= 31) {
            return MEDIUM;
        }
        return LOW;
    }
}
