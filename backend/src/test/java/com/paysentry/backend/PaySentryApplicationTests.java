package com.paysentry.backend;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "paysentry.app.jwt-secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "paysentry.mock-bank.shared-key=test-only-mock-bank-key"
})
@ActiveProfiles("dev")
class PaySentryApplicationTests {

    @Test
    void contextLoads() {
    }
}
