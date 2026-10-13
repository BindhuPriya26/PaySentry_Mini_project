package com.paysentry.backend.security;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.RefreshTokenRepository;
import com.paysentry.backend.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtRevocationService jwtRevocationService;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        JwtRevocationService jwtRevocationService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtRevocationService = jwtRevocationService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        try {
            String email = jwtService.extractUsername(jwt);
            String sessionId = jwtService.extractSessionId(jwt);
            if (email != null
                && sessionId != null
                && refreshTokenRepository.existsBySessionIdAndExpiresAtAfter(
                    sessionId,
                    java.time.LocalDateTime.now()
                )
                && !jwtRevocationService.isRevoked(jwtService.extractTokenId(jwt))
                && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOptional =
                    userRepository.findByNormalizedEmail(email.trim().toLowerCase(Locale.ROOT));
                if (userOptional.isPresent() && jwtService.isTokenValid(jwt, userOptional.get())) {
                    User user = userOptional.get();
                    UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
