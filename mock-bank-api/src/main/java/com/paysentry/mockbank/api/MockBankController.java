package com.paysentry.mockbank.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.mockbank.dto.MockAccountResponse;
import com.paysentry.mockbank.dto.MockLienResponse;
import com.paysentry.mockbank.dto.MockTransactionResponse;
import com.paysentry.mockbank.dto.SimulationScenario;
import com.paysentry.mockbank.service.MockBankService;

@RestController
@RequestMapping("/api")
public class MockBankController {

    private final MockBankService mockBankService;

    public MockBankController(MockBankService mockBankService) {
        this.mockBankService = mockBankService;
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<MockAccountResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(mockBankService.getAccount(accountId));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<MockTransactionResponse>> getTransactions(@PathVariable String accountId) {
        return ResponseEntity.ok(mockBankService.getTransactions(accountId));
    }

    @GetMapping("/accounts/{accountId}/lien")
    public ResponseEntity<MockLienResponse> getLien(@PathVariable String accountId) {
        return ResponseEntity.ok(mockBankService.getLien(accountId));
    }

    @PostMapping("/development/events/{accountId}/{scenario}")
    public ResponseEntity<Map<String, Object>> simulateEvent(
        @PathVariable String accountId,
        @PathVariable SimulationScenario scenario
    ) {
        return ResponseEntity.ok(mockBankService.simulate(accountId, scenario));
    }
}
