package com.zerotrust.serviceb.security;

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
    private String clientType;
    private String ipAddress;
    private Instant tokenIssuedAt;
    private Instant tokenExpiresAt;
    private String requestPath;
    private String requestMethod;
    private boolean isVerified;
    private boolean isAuthorized;
    private String denialReason;
}