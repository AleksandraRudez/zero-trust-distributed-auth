package com.zerotrust.servicec.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PolicyEngine policyEngine;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();

        log.info("PEP (Service C): Intercepting request {} {}", requestMethod, requestPath);

        if (isPublicEndpoint(requestPath)) {
            log.info("PEP: Public endpoint, allowing access");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("PEP: Missing or invalid Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing Authorization header\"}");
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("PEP: Invalid token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);
        String clientType = jwtTokenProvider.getClientTypeFromToken(token);

        Instant issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
        Instant expiresAt = jwtTokenProvider.getExpirationFromToken(token);

        Context context = Context.builder()
                .userId(userId)
                .role(role)
                .clientType(clientType)
                .ipAddress(request.getRemoteAddr())
                .tokenIssuedAt(issuedAt)
                .tokenExpiresAt(expiresAt != null ? expiresAt : Instant.EPOCH)
                .requestPath(requestPath)
                .requestMethod(requestMethod)
                .isVerified(true)
                .build();

        log.info("PEP: Created context for user: {}, role: {}", userId, role);

        if (!policyEngine.validateContext(context)) {
            log.warn("PEP: Context validation failed");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + context.getDenialReason() + "\"}");
            return;
        }

        if (!policyEngine.evaluatePolicy(context)) {
            log.warn("PEP: Policy evaluation failed - {}", context.getDenialReason());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + context.getDenialReason() + "\"}");
            return;
        }

        log.info("PEP: Policy approved, forwarding request");
        request.setAttribute("context", context);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.equals("/health") ||
                path.equals("/") ||
                path.startsWith("/api/public") ||
                path.startsWith("/actuator");
    }
}