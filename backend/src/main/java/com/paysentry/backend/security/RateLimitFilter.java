package com.paysentry.backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Small in-process limiter for locally deployed PaySentry instances. Deployments with more than
 * one backend instance must enforce equivalent limits at their API gateway or load balancer.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;

    private final int authAttemptsPerMinute;
    private final int apiRequestsPerMinute;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public RateLimitFilter(
        @Value("${paysentry.rate-limit.auth-attempts-per-minute}") int authAttemptsPerMinute,
        @Value("${paysentry.rate-limit.api-requests-per-minute}") int apiRequestsPerMinute
    ) {
        this.authAttemptsPerMinute = authAttemptsPerMinute;
        this.apiRequestsPerMinute = apiRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || !request.getRequestURI().startsWith("/api/")
            || request.getRequestURI().equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean authRequest = path.equals("/api/auth/login")
            || path.equals("/api/auth/register")
            || path.equals("/api/auth/forgot-password")
            || path.equals("/api/auth/reset-password");
        int limit = authRequest ? authAttemptsPerMinute : apiRequestsPerMinute;
        String key = (authRequest ? "auth:" : "api:") + clientAddress(request);

        if (!allow(key, limit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again shortly.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        long cutoff = Instant.now().minusSeconds(WINDOW_SECONDS).toEpochMilli();
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= Math.max(1, limit)) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
