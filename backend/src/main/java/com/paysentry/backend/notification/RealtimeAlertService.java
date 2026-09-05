package com.paysentry.backend.notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.UnifiedRiskLevel;
import com.paysentry.backend.monitoring.AlertResponseMapper;

@Service
public class RealtimeAlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final FcmNotificationService fcmNotificationService;

    public RealtimeAlertService(
        SimpMessagingTemplate messagingTemplate,
        FcmNotificationService fcmNotificationService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.fcmNotificationService = fcmNotificationService;
    }

    public void publish(FraudAlert alert) {
        String destination = alert.getAccount() == null
            ? "/topic/users/" + alert.getUser().getId()
            : "/topic/accounts/" + alert.getAccount().getAccountId();
        messagingTemplate.convertAndSend(destination, AlertResponseMapper.toResponse(alert));

        if (alert.getRiskLevel() == UnifiedRiskLevel.HIGH || alert.getRiskLevel() == UnifiedRiskLevel.CRITICAL) {
            fcmNotificationService.sendImportantAlert(alert);
        }
    }
}
