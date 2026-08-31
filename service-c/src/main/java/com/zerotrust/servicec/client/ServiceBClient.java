package com.zerotrust.servicec.client;

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
public class ServiceBClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${service-b-url}")
    private String serviceBUrl;

    @Value("${idp-url}")
    private String idpUrl;

    private String serviceCToken;

    public boolean authenticateAsService() {
        try {
            log.info("Service C: Authenticating as service-c");

            String loginUrl = idpUrl + "/auth/login";
            Map<String, String> loginPayload = Map.of(
                    "username", "service-c",
                    "password", "secret-c",
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
                serviceCToken = (String) response.getBody().get("token");
                log.info("Service C: Successfully obtained JWT token");
                return true;
            } else {
                log.error("Service C: Failed to obtain JWT token");
                return false;
            }
        } catch (Exception e) {
            log.error("Service C: Authentication failed - {}", e.getMessage());
            return false;
        }
    }

    public ResponseEntity<Map> callServiceBOrchestrate(Map<String, String> payload) {
        try {
            if (serviceCToken == null) {
                if (!authenticateAsService()) {
                    throw new RuntimeException("Failed to authenticate Service C");
                }
            }

            log.info("Service C: Calling Service B /api/orchestrate");

            String url = serviceBUrl + "/api/orchestrate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + serviceCToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            log.info("Service C: Response from Service B: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Service C: Failed to call Service B - {}", e.getMessage());
            throw new RuntimeException("Failed to call Service B: " + e.getMessage());
        }
    }

    public ResponseEntity<Map> callServiceBData(Map<String, String> payload) {
        try {
            if (serviceCToken == null) {
                if (!authenticateAsService()) {
                    throw new RuntimeException("Failed to authenticate Service C");
                }
            }

            log.info("Service C: Calling Service B /api/internal/data");

            String url = serviceBUrl + "/api/internal/data";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + serviceCToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("Service C: Response from Service B: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Service C: Failed to call Service B - {}", e.getMessage());
            throw new RuntimeException("Failed to call Service B: " + e.getMessage());
        }
    }
}