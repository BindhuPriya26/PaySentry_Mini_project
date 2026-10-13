package com.paysentry.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.auth.AuthResponse;
import com.paysentry.backend.dto.auth.ForgotPasswordRequest;
import com.paysentry.backend.dto.auth.ForgotPasswordResponse;
import com.paysentry.backend.dto.auth.LoginRequest;
import com.paysentry.backend.dto.auth.RegisterRequest;
import com.paysentry.backend.dto.auth.RefreshTokenRequest;
import com.paysentry.backend.dto.auth.ResetPasswordRequest;
import com.paysentry.backend.entity.PasswordResetToken;
import com.paysentry.backend.entity.RefreshToken;
import com.paysentry.backend.entity.Role;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.repository.PasswordResetTokenRepository;
import com.paysentry.backend.repository.RefreshTokenRepository;
import com.paysentry.backend.repository.UserRepository;
import com.paysentry.backend.security.JwtService;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final BankAccountService bankAccountService;
    private final long refreshTokenExpirationDays;

    public AuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        BankAccountService bankAccountService,
        @Value("${paysentry.app.refresh-token-expiration-days}") long refreshTokenExpirationDays
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.bankAccountService = bankAccountService;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByNormalizedEmail(email).isPresent()) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setNormalizedEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setTrustedDevices("");
        user.setTrustedLocations("");

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("An account with this email already exists.");
        }
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(Locale.ROOT),
                request.getPassword()
            )
        );

        User user = userRepository.findByNormalizedEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!user.isActive()) {
            throw new BadRequestException("This account has been disabled.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashRefreshToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository
            .findActiveForUpdate(tokenHash, LocalDateTime.now())
            .orElseThrow(() -> new BadCredentialsException("Refresh session is invalid or expired."));

        User user = storedToken.getUser();
        String sessionId = storedToken.getSessionId();
        if (!user.isActive() || storedToken.getTokenVersion() != user.getTokenVersion()) {
            refreshTokenRepository.delete(storedToken);
            throw new BadCredentialsException("Refresh session is invalid or expired.");
        }

        refreshTokenRepository.delete(storedToken);
        return buildAuthResponse(user, sessionId);
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
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(hashRefreshToken(request.getRefreshToken()))
            .ifPresent(refreshTokenRepository::delete);
    }

    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponse(user, java.util.UUID.randomUUID().toString());
    }

    private AuthResponse buildAuthResponse(User user, String sessionId) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setAccountId(bankAccountService.createForUser(user).getAccountId());
        response.setToken(jwtService.generateToken(user, sessionId));
        response.setRefreshToken(createRefreshToken(user, sessionId));
        return response;
    }

    private String createRefreshToken(User user, String sessionId) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hashRefreshToken(rawToken));
        refreshToken.setSessionId(sessionId);
        refreshToken.setUser(user);
        refreshToken.setTokenVersion(user.getTokenVersion());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String hashRefreshToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
