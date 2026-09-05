package com.paysentry.backend.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.BankAccountRepository;
import com.paysentry.backend.repository.UserRepository;
import com.paysentry.backend.service.CustomUserDetailsService;

@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHENTICATED_EMAIL_ATTRIBUTE = "paysentry.authenticated.email";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public WebSocketJwtChannelInterceptor(
        JwtService jwtService,
        CustomUserDetailsService userDetailsService,
        UserRepository userRepository,
        BankAccountRepository bankAccountRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("A JWT token is required to open a PaySentry alert stream.");
        }

        String token = authorization.substring(7);
        String username = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new AccessDeniedException("The JWT token is invalid or expired.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new AccessDeniedException("Unable to establish a PaySentry alert-stream session.");
        }
        sessionAttributes.put(AUTHENTICATED_EMAIL_ATTRIBUTE, userDetails.getUsername());
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Object email = sessionAttributes == null ? null : sessionAttributes.get(AUTHENTICATED_EMAIL_ATTRIBUTE);
        if (!(email instanceof String) || ((String) email).trim().isEmpty()) {
            throw new AccessDeniedException("Authenticate before subscribing to PaySentry alerts.");
        }

        User user = userRepository.findByEmailIgnoreCase((String) email)
            .orElseThrow(() -> new AccessDeniedException("The alert-stream user does not exist."));
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("An alert-stream destination is required.");
        }

        if (destination.equals("/topic/users/" + user.getId())) {
            return;
        }
        BankAccount account = bankAccountRepository.findByUser(user)
            .orElseThrow(() -> new AccessDeniedException("No monitored account is linked to this user."));
        if (!destination.equals("/topic/accounts/" + account.getAccountId())) {
            throw new AccessDeniedException("You cannot subscribe to another account's alert stream.");
        }
    }
}
