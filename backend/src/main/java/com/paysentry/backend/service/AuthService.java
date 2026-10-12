package com.paysentry.backend.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.auth.AuthResponse;
import com.paysentry.backend.dto.auth.ForgotPasswordRequest;
import com.paysentry.backend.dto.auth.ForgotPasswordResponse;
import com.paysentry.backend.dto.auth.LoginRequest;
import com.paysentry.backend.dto.auth.RegisterRequest;
import com.paysentry.backend.dto.auth.ResetPasswordRequest;
import com.paysentry.backend.entity.PasswordResetToken;
import com.paysentry.backend.entity.Role;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.repository.PasswordResetTokenRepository;
import com.paysentry.backend.repository.UserRepository;
import com.paysentry.backend.security.JwtService;
import com.paysentry.backend.security.JwtRevocationService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtRevocationService jwtRevocationService;
    private final BankAccountService bankAccountService;

    public AuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        BankAccountService bankAccountService,
        JwtRevocationService jwtRevocationService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.bankAccountService = bankAccountService;
        this.jwtRevocationService = jwtRevocationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setTrustedDevices("");
        user.setTrustedLocations("");

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!user.isActive()) {
            throw new BadRequestException("This account has been disabled.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        ForgotPasswordResponse response = new ForgotPasswordResponse();
        response.setMessage("Password reset is unavailable because email delivery is not configured on this server.");
        return response;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
            .orElseThrow(() -> new BadRequestException("Reset token is invalid or already used."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            throw new BadRequestException("Reset token has expired.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.invalidateSessions();
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authorizationHeader.substring(7);
        jwtRevocationService.revoke(jwtService.extractTokenId(token), jwtService.extractExpiry(token));
        jwtRevocationService.removeExpired();
    }

    private AuthResponse buildAuthResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setAccountId(bankAccountService.createForUser(user).getAccountId());
        response.setToken(jwtService.generateToken(user));
        return response;
    }
}
