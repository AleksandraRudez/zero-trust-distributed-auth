package com.zerotrust.servicea.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Context {
    private String userId;
    private String role;
    private String clientType;  // USER ili SERVICE
    private String ipAddress;
    private Instant tokenIssuedAt;
    private Instant tokenExpiresAt;
    private String requestPath;
    private String requestMethod;

    // Zero Trust specifično
    private boolean isVerified;  // Ako je token validan
    private boolean isAuthorized;  // Ako ima dozvolu za resurs
    private String denialReason;  // Zašto nije autorizovan
}