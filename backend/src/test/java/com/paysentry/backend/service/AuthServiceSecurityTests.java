package com.paysentry.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.paysentry.backend.dto.auth.ForgotPasswordRequest;
import com.paysentry.backend.dto.auth.ForgotPasswordResponse;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.repository.PasswordResetTokenRepository;
import com.paysentry.backend.repository.RefreshTokenRepository;
import com.paysentry.backend.repository.UserRepository;
import com.paysentry.backend.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceSecurityTests {

    @Test
    void forgotPasswordDoesNotExposeOrCreateResetTokensWithoutEmailDelivery() {
        UserRepository users = mock(UserRepository.class);
        PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
        AuthService authService = new AuthService(
            users,
            resetTokens,
            mock(RefreshTokenRepository.class),
            mock(PasswordEncoder.class),
            mock(AuthenticationManager.class),
            mock(JwtService.class),
            mock(BankAccountService.class),
            30
        );

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("person@example.invalid");
        ForgotPasswordResponse response = authService.forgotPassword(request);

        assertEquals(
            "Password reset is unavailable because email delivery is not configured on this server.",
            response.getMessage()
        );
        verifyNoInteractions(users, resetTokens);
    }
}
