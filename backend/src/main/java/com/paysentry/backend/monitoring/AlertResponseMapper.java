package com.paysentry.backend.monitoring;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.paysentry.backend.dto.monitoring.MobileAlertResponse;
import com.paysentry.backend.entity.FraudAlert;

public final class AlertResponseMapper {

    private AlertResponseMapper() {
    }

    public static MobileAlertResponse toResponse(FraudAlert alert) {
        MobileAlertResponse response = new MobileAlertResponse();
        response.setId(alert.getId());
        response.setTitle(alert.getTitle());
        response.setEventType(alert.getFraudEvent().getEventType().name());
        response.setEventDetails(alert.getFraudEvent().getEventDetails());
        response.setRiskScore(alert.getRiskScore());
        response.setRiskLevel(alert.getRiskLevel().name());
        response.setReasons(splitLines(alert.getReasons()));
        response.setRecommendedAction(alert.getRecommendedAction());
        response.setStatus(alert.getStatus().name());
        response.setCreatedAt(alert.getCreatedAt());
        return response;
    }

    public static List<String> splitLines(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("\\n"))
            .filter(line -> !line.trim().isEmpty())
            .collect(Collectors.toList());
    }
}
