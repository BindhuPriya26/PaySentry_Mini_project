package com.paysentry.backend.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.monitoring.MobileAlertResponse;
import com.paysentry.backend.entity.AlertStatus;
import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.repository.FraudAlertRepository;

@Service
public class AlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public AlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional(readOnly = true)
    public List<MobileAlertResponse> listForUser(User user) {
        return fraudAlertRepository.findTop20ByUserOrderByCreatedAtDesc(user).stream()
            .map(AlertResponseMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public MobileAlertResponse acknowledge(User user, Long alertId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
            .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Alert was not found."));
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return AlertResponseMapper.toResponse(fraudAlertRepository.save(alert));
    }
}
