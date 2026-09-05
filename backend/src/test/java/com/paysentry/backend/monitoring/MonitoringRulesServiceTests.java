package com.paysentry.backend.monitoring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.paysentry.backend.dto.monitoring.MockBankEventRequest;
import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.BankingEventType;

class MonitoringRulesServiceTests {

    private final MonitoringRulesService monitoringRulesService = new MonitoringRulesService();

    @Test
    void flagsUnexpectedLargeLienWithoutReferenceAsHighRisk() {
        BankAccount account = accountWithLien(BigDecimal.ZERO);
        MockBankEventRequest request = new MockBankEventRequest();
        request.setEventType(BankingEventType.LIEN_CHANGE);
        request.setLienAmount(new BigDecimal("60000"));
        request.setPreviousLienAmount(BigDecimal.ZERO);
        request.setExpectedActivity(false);
        request.setTransactionReference("");

        DirectRiskResult result = monitoringRulesService.evaluate(request, account, 0, 0);

        assertTrue(result.getScore() >= 91);
        assertTrue(result.getReasons().stream().anyMatch(reason -> reason.contains("no matching expected activity")));
    }

    @Test
    void flagsLargeTransactionFromNewDeviceAndLocationAsHighRisk() {
        MockBankEventRequest request = new MockBankEventRequest();
        request.setEventType(BankingEventType.TRANSACTION);
        request.setAmount(new BigDecimal("125000"));
        request.setReceiver("Crypto Wallet Services");
        request.setNewDevice(true);
        request.setNewLocation(true);

        DirectRiskResult result = monitoringRulesService.evaluate(request, accountWithLien(BigDecimal.ZERO), 0, 0);

        assertTrue(result.getScore() >= 90);
        assertTrue(result.getReasons().stream().anyMatch(reason -> reason.contains("newly observed device")));
    }

    private BankAccount accountWithLien(BigDecimal lienAmount) {
        BankAccount account = new BankAccount();
        account.setLienAmount(lienAmount);
        account.setTotalBalance(new BigDecimal("250000"));
        account.setAvailableBalance(new BigDecimal("250000"));
        return account;
    }
}
