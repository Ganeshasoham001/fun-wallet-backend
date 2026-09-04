package com.funwallet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getBrevoKey() {
        String envKey = System.getenv("BREVO_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) return envKey.trim();
        return "";
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        logger.info("Dispatching password reset email to {}. Message:\n{}", to, text);

        // 1. Try Brevo HTTPS API if BREVO_API_KEY environment variable is configured
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

        // 2. Fallback to JavaMailSender SMTP
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

