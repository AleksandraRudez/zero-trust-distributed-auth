package com.zerotrust.serviceb.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PolicyEngine {

    public boolean evaluatePolicy(Context context) {
        log.info("PDP (Service B): Evaluating policy for user: {}, role: {}",
                context.getUserId(), context.getRole());

        if (!context.isVerified()) {
            context.setDenialReason("Token nije validan ili je istekao");
            log.warn("DENY: Invalid token for user {}", context.getUserId());
            return false;
        }

        // ADMIN ima pristup svemu
        if ("ADMIN".equalsIgnoreCase(context.getRole())) {
            log.info("ALLOW: Admin user {} has full access", context.getUserId());
            return true;
        }

        // USER može pristupiti /api/data i /api/orchestrate
        if ("USER".equalsIgnoreCase(context.getRole())) {
            if (context.getRequestPath().startsWith("/api/data") ||
                    context.getRequestPath().startsWith("/api/orchestrate")) {
                log.info("ALLOW: User {} can access {}", context.getUserId(), context.getRequestPath());
                return true;
            } else {
                context.setDenialReason("Obični korisnici mogu pristupiti samo /api/data ili /api/orchestrate");
                log.warn("DENY: User {} cannot access {}", context.getUserId(), context.getRequestPath());
                return false;
            }
        }

        // SERVICE može pristupiti /api/internal
        if ("SERVICE".equalsIgnoreCase(context.getRole())) {
            if (context.getRequestPath().startsWith("/api/internal") ||
                    context.getRequestPath().startsWith("/api/orchestrate")) {   log.info("ALLOW: Service {} can access /api/internal", context.getUserId());
                return true;
            } else {
                context.setDenialReason("Servisi mogu pristupiti samo /api/internal");
                log.warn("DENY: Service {} cannot access {}", context.getUserId(), context.getRequestPath());
                return false;
            }
        }

        context.setDenialReason("Nedozvoljeni pristup - default policy je DENY");
        log.warn("DENY: Default policy - access denied for user {}", context.getUserId());
        return false;
    }

    public boolean validateContext(Context context) {
        log.debug("Context IP: {}", context.getIpAddress());

        long currentTime = System.currentTimeMillis();
        long expiresAt = context.getTokenExpiresAt().toEpochMilli();

        if (currentTime > expiresAt) {
            context.setDenialReason("Token je istekao");
            return false;
        }

        return true;
    }
}