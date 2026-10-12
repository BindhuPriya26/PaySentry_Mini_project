package com.paysentry.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.paysentry.backend.entity.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(
            jwtService,
            "jwtSecret",
            Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))
        );
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 60_000L);

        user = new User();
        user.setFullName("Test User");
        user.setEmail("test@example.invalid");
    }

    @Test
    void tokenIsInvalidatedWhenAllSessionsAreRevoked() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));

        user.invalidateSessions();

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenIsInvalidatedWhenAccountIsDisabled() {
        String token = jwtService.generateToken(user);
        user.setActive(false);

        assertFalse(jwtService.isTokenValid(token, user));
    }
}
