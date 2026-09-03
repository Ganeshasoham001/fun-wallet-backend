package com.funwallet.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveTask {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveTask.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // Ping the backend's own public URL every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void pingSelf() {
        try {
            String url = "https://fun-wallet-backend.onrender.com/api/config";
            logger.info("Pinging self to prevent Render sleep: " + url);
            restTemplate.getForObject(url, String.class);
            logger.info("Ping successful.");
        } catch (Exception e) {
            logger.error("Ping failed: " + e.getMessage());
        }
    }
}
