package com.paysentry.mockbank.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.paysentry.mockbank.dto.SimulationScenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockBankServiceTests {

    @Test
    void doesNotChangeSimulatorStateWhenBackendRejectsAnEvent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://localhost:8080/api/internal/mock-events"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Mock-Bank-Key", "test-only-key"))
            .andRespond(withServerError());
        MockBankService service = new MockBankService(restTemplate, "http://localhost:8080", "test-only-key");

        assertThrows(
            RuntimeException.class,
            () -> service.simulate("PS-TEST-001", SimulationScenario.LARGE_TRANSACTION)
        );

        server.verify();
        assertEquals(0, service.getTransactions("PS-TEST-001").size());
    }

    @Test
    void updatesSimulatorStateAfterBackendAcceptsAnEvent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://localhost:8080/api/internal/mock-events"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"accepted\":true}", MediaType.APPLICATION_JSON));
        MockBankService service = new MockBankService(restTemplate, "http://localhost:8080", "test-only-key");

        service.simulate("PS-TEST-002", SimulationScenario.LARGE_TRANSACTION);

        server.verify();
        assertEquals(1, service.getTransactions("PS-TEST-002").size());
    }
}
