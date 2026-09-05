package com.paysentry.backend.monitoring;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.monitoring.MobileDashboardResponse;
import com.paysentry.backend.dto.monitoring.MobileTransactionResponse;
import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.MonitoredTransaction;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.FraudAlertRepository;
import com.paysentry.backend.repository.FraudEventRepository;
import com.paysentry.backend.repository.MonitoredTransactionRepository;

@Service
public class MobileDashboardService {

    private final BankAccountService bankAccountService;
    private final MonitoredTransactionRepository monitoredTransactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudEventRepository fraudEventRepository;

    public MobileDashboardService(
        BankAccountService bankAccountService,
        MonitoredTransactionRepository monitoredTransactionRepository,
        FraudAlertRepository fraudAlertRepository,
        FraudEventRepository fraudEventRepository
    ) {
        this.bankAccountService = bankAccountService;
        this.monitoredTransactionRepository = monitoredTransactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudEventRepository = fraudEventRepository;
    }

    @Transactional(readOnly = true)
    public MobileDashboardResponse getDashboard(User user) {
        BankAccount account = bankAccountService.getForUser(user);
        List<FraudEvent> recentEvents = fraudEventRepository.findTop10ByUserOrderByOccurredAtDesc(user);

        MobileDashboardResponse response = new MobileDashboardResponse();
        response.setAccountId(account.getAccountId());
        response.setTotalBalance(account.getTotalBalance());
        response.setAvailableBalance(account.getAvailableBalance());
        response.setLienAmount(account.getLienAmount());
        response.setCurrency(account.getCurrency());
        response.setSecurityScore(calculateSecurityScore(recentEvents));
        response.setProtectionStatus(protectionStatus(recentEvents));
        response.setRecentTransactions(monitoredTransactionRepository.findTop10ByAccountOrderByTransactionTimeDesc(account)
            .stream().map(this::toTransactionResponse).collect(Collectors.toList()));
        response.setRecentAlerts(fraudAlertRepository.findTop20ByUserOrderByCreatedAtDesc(user)
            .stream().limit(5).map(AlertResponseMapper::toResponse).collect(Collectors.toList()));
        return response;
    }

    private MobileTransactionResponse toTransactionResponse(MonitoredTransaction transaction) {
        MobileTransactionResponse response = new MobileTransactionResponse();
        response.setTransactionId(transaction.getExternalTransactionId());
        response.setAmount(transaction.getAmount());
        response.setReceiver(transaction.getReceiver());
        response.setLocation(transaction.getLocation());
        response.setDevice(transaction.getDevice());
        response.setRiskScore(transaction.getRiskScore());
        response.setRiskLevel(transaction.getRiskLevel().name());
        response.setTimestamp(transaction.getTransactionTime());
        return response;
    }

    private int calculateSecurityScore(List<FraudEvent> events) {
        int highestRisk = events.stream().mapToInt(FraudEvent::getOverallRiskScore).max().orElse(0);
        return Math.max(0, 100 - highestRisk);
    }

    private String protectionStatus(List<FraudEvent> events) {
        int highestRisk = events.stream().mapToInt(FraudEvent::getOverallRiskScore).max().orElse(0);
        if (highestRisk >= 91) {
            return "CRITICAL ACTIVITY DETECTED";
        }
        if (highestRisk >= 71) {
            return "HIGH-RISK ACTIVITY DETECTED";
        }
        if (highestRisk >= 31) {
            return "MONITORING SUSPICIOUS ACTIVITY";
        }
        return "PROTECTION ACTIVE";
    }
}
