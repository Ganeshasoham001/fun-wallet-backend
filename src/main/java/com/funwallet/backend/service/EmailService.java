package com.funwallet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getRelayUrl() {
        String envUrl = System.getenv("GMAIL_RELAY_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) return envUrl.trim();
        return "";
    }

    private String getBrevoKey() {
        String envKey = System.getenv("BREVO_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) return envKey.trim();
        return "";
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        logger.info("Dispatching password reset email to {}. Message:\n{}", to, text);

        // 1. Try Google Apps Script HTTPS Relay if configured
        String relayUrl = getRelayUrl();
        if (!relayUrl.isEmpty()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> body = new HashMap<>();
                body.put("to", to);
                body.put("subject", subject);
                body.put("text", text);

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(relayUrl, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && !response.getBody().contains("errorMessage")) {
                    logger.info("Email sent successfully via Google Apps Script HTTPS Relay to {}", to);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Google Apps Script HTTPS Relay failed: {}", e.getMessage());
            }
        }

        // 2. Try Brevo HTTPS API if BREVO_API_KEY is configured
        String brevoApiKey = getBrevoKey();
        if (!brevoApiKey.isEmpty()) {
            try {
                String url = "https://api.brevo.com/v3/smtp/email";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("sender", Map.of("name", "Couple's Fun Wallet", "email", "connectly2001@gmail.com"));
                body.put("to", List.of(Map.of("email", to)));
                body.put("subject", subject);
                body.put("textContent", text);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Email sent successfully via Brevo HTTPS API to {}", to);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Brevo HTTPS API email dispatch failed: {}", e.getMessage());
            }
        }

        // 3. Fallback to FormSubmit.co HTTPS API (100% Free Port 443 delivery)
        try {
            String formSubmitUrl = "https://formsubmit.co/" + to;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("_subject", subject);
            map.add("email", to);
            map.add("message", text);
            map.add("_captcha", "false");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(formSubmitUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email dispatched via FormSubmit HTTPS endpoint to {}", to);
                return;
            }
        } catch (Exception ex) {
            logger.warn("FormSubmit HTTPS dispatch failed: {}", ex.getMessage());
        }

        // 4. Fallback to JavaMailSender SMTP
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("connectly2001@gmail.com");
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                logger.info("Email sent via JavaMailSender fallback to {}", to);
            } catch (Exception ex) {
                logger.warn("JavaMailSender SMTP dispatch failed (cloud port block): {}", ex.getMessage());
            }
        }
    }
}



