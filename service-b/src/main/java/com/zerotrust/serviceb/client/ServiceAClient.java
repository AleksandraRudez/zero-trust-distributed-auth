package com.zerotrust.serviceb.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class ServiceAClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${service-a-url}")
    private String serviceAUrl;

    @Value("${idp-url}")
    private String idpUrl;

    private String serviceBToken;


    public boolean authenticateAsService() {
        try {
            log.info("Service B: Authenticating as service-b");

            String loginUrl = idpUrl + "/auth/login";
            Map<String, String> loginPayload = Map.of(
                    "username", "service-b",
                    "password", "secret-b",
                    "clientType", "SERVICE"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, String>> request = new HttpEntity<>(loginPayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                serviceBToken = (String) response.getBody().get("token");
                log.info("Service B: Successfully obtained JWT token");
                return true;
            } else {
                log.error("Service B: Failed to obtain JWT token");
                return false;
            }
        } catch (Exception e) {
            log.error("Service B: Authentication failed - {}", e.getMessage());
            return false;
        }
    }


    public ResponseEntity<Map> callServiceAInternalProcess(Map<String, String> payload) {
        try {
            if (serviceBToken == null) {
                if (!authenticateAsService()) {
                    throw new RuntimeException("Failed to authenticate Service B");
                }
            }

            log.info("Service B: Calling Service A /api/internal/process");

            String url = serviceAUrl + "/api/internal/process";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + serviceBToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("Service B: Response from Service A: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Service B: Failed to call Service A - {}", e.getMessage());
            throw new RuntimeException("Failed to call Service A: " + e.getMessage());
        }
    }

    /**
     * Pozovi Service A /api/internal/ping
     */
    public ResponseEntity<Map> ping() {
        try {
            if (serviceBToken == null) {
                if (!authenticateAsService()) {
                    throw new RuntimeException("Failed to authenticate Service B");
                }
            }

            log.info("Service B: Pinging Service A");

            String url = serviceAUrl + "/api/internal/ping";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceBToken);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            log.info("Service B: Ping response: {}", response.getBody());
            return response;
        } catch (Exception e) {
            log.error("Service B: Ping failed - {}", e.getMessage());
            throw new RuntimeException("Ping failed: " + e.getMessage());
        }
    }
}