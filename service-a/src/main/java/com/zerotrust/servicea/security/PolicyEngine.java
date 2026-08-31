package com.zerotrust.servicea.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Set;

@Slf4j
@Component
public class PolicyEngine {

    private static final Set<String> ALLOWED_INTERNAL_CALLERS = Set.of("service-b");

    private static final Set<String> RECOGNIZED_CLIENT_TYPES = Set.of("USER", "SERVICE");

    public boolean evaluatePolicy(Context context) {
        log.info("PDP: Evaluating policy for user: {}, role: {}", context.getUserId(), context.getRole());

        if (!context.isVerified()) {
            context.setDenialReason("Token nije validan ili je istekao");
            log.warn("DENY: Invalid token for user {}", context.getUserId());
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(context.getRole())) {
            log.info("ALLOW: Admin user {} has full access", context.getUserId());
            return true;
        }

        if ("USER".equalsIgnoreCase(context.getRole())) {
            if (context.getRequestPath().startsWith("/api/data")) {
                log.info("ALLOW: User {} can access /api/data", context.getUserId());
                return true;
            }
            context.setDenialReason("Obični korisnici mogu pristupiti samo /api/data");
            log.warn("DENY: User {} cannot access {}", context.getUserId(), context.getRequestPath());
            return false;
        }

        if ("SERVICE".equalsIgnoreCase(context.getRole())) {
            if (!context.getRequestPath().startsWith("/api/internal") || !"SERVICE".equalsIgnoreCase(context.getClientType())) {
                context.setDenialReason("Servisi mogu pristupiti samo /api/internal i moraju biti tipa SERVICE");
                log.warn("DENY: Service {} cannot access {}", context.getUserId(), context.getRequestPath());
                return false;
            }

            // Policy enforcement primer iz specifikacije: "pristup dozvoljen samo određenim servisima"
            if (!ALLOWED_INTERNAL_CALLERS.contains(context.getUserId())) {
                context.setDenialReason("Servis '" + context.getUserId() + "' nije na listi dozvoljenih pozivalaca za ovaj resurs");
                log.warn("DENY: Service {} is not whitelisted for {}", context.getUserId(), context.getRequestPath());
                return false;
            }

            log.info("ALLOW: Service {} can access /api/internal", context.getUserId());
            return true;
        }

        context.setDenialReason("Nedozvoljeni pristup - default policy je DENY");
        log.warn("DENY: Default policy - access denied for user {}", context.getUserId());
        return false;
    }

    public boolean validateContext(Context context) {
        if (context == null) {
            return false;
        }

        if (context.getTokenExpiresAt() == null || context.getTokenExpiresAt().isBefore(java.time.Instant.now())) {
            context.setDenialReason("Token je istekao");
            return false;
        }

        // Provera konteksta #1: tip klijenta — mora biti prepoznata vrednost (USER ili SERVICE)
        if (context.getClientType() == null || !RECOGNIZED_CLIENT_TYPES.contains(context.getClientType().toUpperCase())) {
            context.setDenialReason("Tip klijenta nije prepoznat ili nedostaje u tokenu");
            log.warn("DENY: Unrecognized clientType '{}' for user {}", context.getClientType(), context.getUserId());
            return false;
        }
        log.info("PDP: Klijent identifikovan kao tip '{}'", context.getClientType());

        // Provera konteksta #2: IP adresa
        if (!isTrustedInternalAddress(context.getIpAddress())) {
            context.setDenialReason("Pristup je odbijen: IP adresa nije u dozvoljenom internoj mreži");
            return false;
        }

        // Provera konteksta #3: vreme pristupa
        if (!isAllowedAccessTime()) {
            context.setDenialReason("Pristup je odbijen: van dozvoljenog vremenskog prozora");
            return false;
        }

        context.setAuthorized(true);
        return true;
    }

    private boolean isTrustedInternalAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        String normalized = ipAddress.trim();
        return normalized.startsWith("10.")
                || normalized.startsWith("192.168.")
                || normalized.startsWith("172.16.")
                || normalized.startsWith("172.17.")
                || normalized.startsWith("172.18.")
                || normalized.startsWith("172.19.")
                || normalized.startsWith("172.20.")
                || normalized.startsWith("172.21.")
                || normalized.startsWith("172.22.")
                || normalized.startsWith("172.23.")
                || normalized.startsWith("172.24.")
                || normalized.startsWith("172.25.")
                || normalized.startsWith("172.26.")
                || normalized.startsWith("172.27.")
                || normalized.startsWith("172.28.")
                || normalized.startsWith("172.29.")
                || normalized.startsWith("172.30.")
                || normalized.startsWith("172.31.")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1");
    }

    private boolean isAllowedAccessTime() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(0, 0)) && !now.isAfter(LocalTime.of(23, 0));
    }
}