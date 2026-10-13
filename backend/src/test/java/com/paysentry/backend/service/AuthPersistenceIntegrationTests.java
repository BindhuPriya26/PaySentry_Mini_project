package com.paysentry.backend.service;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "paysentry.app.jwt-secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "paysentry.mock-bank.shared-key=test-only-mock-bank-key"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthPersistenceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void normalizedRegistrationPersistsHashedPasswordAndRejectsDuplicates() throws Exception {
        String email = "MixedCase-" + UUID.randomUUID() + "@example.test";
        JsonNode registration = register(email);

        User persisted = userRepository.findByNormalizedEmail(email.toLowerCase(Locale.ROOT)).orElseThrow();
        assertEquals(email.toLowerCase(Locale.ROOT), persisted.getEmail());
        assertTrue(passwordEncoder.matches("correct-horse-battery", persisted.getPassword()));
        assertNotEquals("correct-horse-battery", persisted.getPassword());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("Another Name", email.toUpperCase(Locale.ROOT), "correct-horse-battery")))
            .andExpect(status().isBadRequest());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody(null, email.toUpperCase(Locale.ROOT), "correct-horse-battery")))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertEquals(registration.get("accountId").asText(), login.get("accountId").asText());
    }

    @Test
    void refreshRotatesPersistedSessionAndLogoutRevokesIt() throws Exception {
        JsonNode registration = register("session-" + UUID.randomUUID() + "@example.test");
        String accessToken = registration.get("token").asText();
        String refreshToken = registration.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertNotEquals(refreshToken, refreshed.get("refreshToken").asText());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/mobile/dashboard")
                .header("Authorization", "Bearer " + refreshed.get("token").asText()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshed.get("refreshToken").asText() + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshed.get("refreshToken").asText() + "\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/mobile/dashboard")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardUsesTheAuthenticatedUsersAccountAndAdminRoutesStayProtected() throws Exception {
        JsonNode firstUser = register("first-" + UUID.randomUUID() + "@example.test");
        JsonNode secondUser = register("second-" + UUID.randomUUID() + "@example.test");

        assertNotEquals(firstUser.get("accountId").asText(), secondUser.get("accountId").asText());

        mockMvc.perform(get("/api/mobile/dashboard")
                .header("Authorization", "Bearer " + firstUser.get("token").asText()))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$.accountId").value(firstUser.get("accountId").asText()));

        mockMvc.perform(get("/api/mobile/dashboard")
                .header("Authorization", "Bearer " + secondUser.get("token").asText()))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$.accountId").value(secondUser.get("accountId").asText()));

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + firstUser.get("token").asText()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody(null, firstUser.get("email").asText(), "incorrect-password")))
            .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody("PaySentry User", email, "correct-horse-battery")))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String registrationBody(String fullName, String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new RegistrationPayload(fullName, email, password));
    }

    private static final class RegistrationPayload {
        public final String fullName;
        public final String email;
        public final String password;

        private RegistrationPayload(String fullName, String email, String password) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
        }
    }
}
