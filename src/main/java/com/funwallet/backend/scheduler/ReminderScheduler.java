package com.funwallet.backend.scheduler;

import com.funwallet.backend.controller.FunWalletController;
import com.funwallet.backend.model.ScheduledReminder;
import com.funwallet.backend.repository.ScheduledReminderRepository;
import com.funwallet.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    @Autowired
    private ScheduledReminderRepository reminderRepository;

    @Autowired
    private EmailService emailService;

    // Check every 30 seconds for reminders due to be sent
    @Scheduled(fixedRate = 30000)
    public void checkAndSendReminders() {
        Instant now = Instant.now();
        List<ScheduledReminder> dueReminders = reminderRepository.findBySentFalseAndScheduledTimeLessThanEqual(now);

        if (!dueReminders.isEmpty()) {
            logger.info("Found {} scheduled reminder(s) due at UTC: {}", dueReminders.size(), now);
        }

        for (ScheduledReminder reminder : dueReminders) {
            try {
                logger.info("Executing scheduled reminder id: {} for recipient: {} (scheduled for: {})",
                        reminder.getId(), reminder.getRecipientEmail(), reminder.getScheduledTime());

                String emailSubject = (reminder.getSubject() != null && !reminder.getSubject().trim().isEmpty())
                        ? reminder.getSubject().trim()
                        : "Couple's Fun Wallet Reminder ⏰💖";

                String creatorInfo = (reminder.getCreatedBy() != null && !reminder.getCreatedBy().trim().isEmpty())
                        ? reminder.getCreatedBy().trim()
                        : "Someone special";

                String emailBody = reminder.getMessage() + "\n\n---\nSent with ❤️ from " + creatorInfo + " via Couple's Fun Wallet";

                emailService.sendSimpleMessage(reminder.getRecipientEmail(), emailSubject, emailBody);

                reminder.setSent(true);
                reminder.setSentAt(now);
                reminderRepository.save(reminder);

                logger.info("Scheduled reminder id: {} successfully sent and recorded.", reminder.getId());

                // Notify connected clients via SSE stream
                FunWalletController.notifyEvent("REMINDER_SENT");
            } catch (Exception e) {
                logger.error("Failed to process scheduled reminder id: {}: {}", reminder.getId(), e.getMessage(), e);
            }
        }
    }
}
