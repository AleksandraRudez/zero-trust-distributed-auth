package com.zerotrust.idp.controller;

import com.zerotrust.idp.model.LoginRequest;
import com.zerotrust.idp.model.LoginResponse;
import com.zerotrust.idp.service.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for user: {} as {}", request.getUsername(), request.getClientType());

            String token = jwtTokenService.generateToken(
                    request.getUsername(),
                    request.getPassword(),
                    request.getClientType()
            );

            String role = jwtTokenService.getRoleForUser(request.getUsername(), request.getPassword());

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setType("Bearer");
            response.setExpiresIn(jwtExpiration);
            response.setUsername(request.getUsername());
            response.setRole(role);

            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");

            if (jwtTokenService.validateToken(token)) {
                var claims = jwtTokenService.getClaimsFromToken(token);
                return ResponseEntity.ok(claims);
            } else {
                return ResponseEntity.status(401).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token validation error: " + e.getMessage());
        }
    }
}