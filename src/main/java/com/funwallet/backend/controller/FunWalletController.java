package com.funwallet.backend.controller;

import com.funwallet.backend.model.AppUser;
import com.funwallet.backend.model.PointsRequest;
import com.funwallet.backend.model.ScheduledReminder;
import com.funwallet.backend.model.Streak;
import com.funwallet.backend.repository.ScheduledReminderRepository;
import com.funwallet.backend.service.EmailService;
import com.funwallet.backend.service.FunWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow angular frontend to connect
public class FunWalletController {

    @Autowired
    private FunWalletService funWalletService;

    @Autowired
    private ScheduledReminderRepository reminderRepository;

    @Autowired
    private EmailService emailService;

    private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/events/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout for real-time events
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (Exception e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public static void notifyEvent(String eventType) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("update").data(eventType));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    // --- Auth Endpoints ---
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            AppUser user = funWalletService.register(
                payload.get("name"),
                payload.get("email"),
                payload.get("password")
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            AppUser user = funWalletService.login(
                payload.get("email"),
                payload.get("password")
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            funWalletService.changePassword(request.get("email"), request.get("oldPassword"), request.get("newPassword"));
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            String tempPass = funWalletService.forgotPassword(payload.get("email"));
            return ResponseEntity.ok(Map.of("temporaryPassword", tempPass));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- User Endpoints ---
    @GetMapping("/users")
    public List<AppUser> getAllUsers() {
        return funWalletService.getAllUsers();
    }

    @GetMapping("/user/{name}")
    public AppUser getUser(@PathVariable String name) {
        return funWalletService.getUser(name);
    }
    
    @PostMapping("/user/mood")
    public ResponseEntity<?> updateMood(@RequestBody Map<String, String> payload) {
        try {
            AppUser user = funWalletService.updateMood(payload.get("username"), payload.get("moodText"));
            notifyEvent("MOOD_UPDATED");
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Config Endpoints ---
    @GetMapping("/config")
    public com.funwallet.backend.model.AppConfig getConfig() {
        return funWalletService.getConfig();
    }

    @PostMapping("/admin/config")
    public com.funwallet.backend.model.AppConfig updateConfig(@RequestBody com.funwallet.backend.model.AppConfig config) {
        com.funwallet.backend.model.AppConfig updated = funWalletService.updateConfig(config);
        notifyEvent("CONFIG_UPDATED");
        return updated;
    }

    // --- Wallet Endpoints ---
    @PostMapping("/admin/angry")
    public AppUser angryForOneDay(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        String targetName = payload.get("targetName"); // usually Sairindhri
        AppUser user = funWalletService.deductPoints(adminName, targetName);
        notifyEvent("POINTS_DEDUCTED");
        return user;
    }

    // --- Streak Endpoints ---
    @GetMapping("/streak/{userName}/{type}")
    public Streak getStreak(@PathVariable String userName, @PathVariable String type) {
        return funWalletService.getStreak(userName, type);
    }

    @PostMapping("/streak/add")
    public AppUser addProgress(@RequestBody Map<String, String> payload) {
        String userName = payload.get("userName");
        String type = payload.get("type"); // study or behaviour
        AppUser user = funWalletService.addProgress(userName, type);
        notifyEvent("PROGRESS_ADDED");
        return user;
    }

    @PostMapping("/admin/streak/reset")
    public void resetStreak(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        String targetName = payload.get("targetName");
        String type = payload.get("type");
        funWalletService.resetStreak(adminName, targetName, type);
        notifyEvent("STREAK_RESET");
    }

    // --- Request Endpoints ---
    @PostMapping("/request/create")
    public PointsRequest createRequest(@RequestBody Map<String, String> payload) {
        String userName = payload.get("userName");
        String category = payload.get("category");
        int points = Integer.parseInt(payload.get("points"));
        PointsRequest req = funWalletService.createRequest(userName, category, points);
        notifyEvent("REQUEST_CREATED");
        return req;
    }

    @GetMapping("/admin/requests/pending")
    public List<PointsRequest> getPendingRequests() {
        return funWalletService.getPendingRequests();
    }

    @PostMapping("/admin/request/approve")
    public PointsRequest approveRequest(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        Long requestId = Long.parseLong(payload.get("requestId"));
        int grantedPoints = Integer.parseInt(payload.getOrDefault("grantedPoints", "10"));
        PointsRequest req = funWalletService.approveRequest(adminName, requestId, grantedPoints);
        notifyEvent("REQUEST_APPROVED");
        return req;
    }

    @PostMapping("/admin/request/reject")
    public PointsRequest rejectRequest(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        Long requestId = Long.parseLong(payload.get("requestId"));
        PointsRequest req = funWalletService.rejectRequest(adminName, requestId);
        notifyEvent("REQUEST_REJECTED");
        return req;
    }

    @GetMapping("/history/{username}")
    public java.util.List<com.funwallet.backend.model.PointHistory> getHistory(@PathVariable String username) {
        return funWalletService.getHistory(username);
    }

    // --- Wishlist Endpoints ---
    @GetMapping("/wishlist")
    public List<com.funwallet.backend.model.WishlistItem> getAllWishlists() {
        return funWalletService.getAllWishlists();
    }

    @PostMapping("/wishlist")
    public com.funwallet.backend.model.WishlistItem createWishlist(@RequestBody Map<String, String> payload) {
        String description = payload.get("description");
        int targetMonth = Integer.parseInt(payload.get("targetMonth"));
        int targetYear = Integer.parseInt(payload.get("targetYear"));
        String createdBy = payload.get("createdBy");
        com.funwallet.backend.model.WishlistItem item = funWalletService.createWishlist(description, targetMonth, targetYear, createdBy);
        notifyEvent("WISHLIST_CREATED");
        return item;
    }

    @PostMapping({"/wishlist/{id}/mark-complete", "/wishlist/{id}/complete"})
    public com.funwallet.backend.model.WishlistItem markWishlistComplete(
            @PathVariable Long id,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String completedDate,
            @RequestParam(required = false) String completedBy,
            @RequestBody(required = false) Map<String, String> payload) {
        String by = null;
        if (payload != null && payload.get("completedBy") != null && !payload.get("completedBy").trim().isEmpty()) {
            by = payload.get("completedBy").trim();
        } else if (completedBy != null && !completedBy.trim().isEmpty()) {
            by = completedBy.trim();
        }
        if (by == null) by = "USER";

        String d = null;
        if (payload != null && payload.get("completedDate") != null && !payload.get("completedDate").trim().isEmpty()) {
            d = payload.get("completedDate").trim();
        } else if (completedDate != null && !completedDate.trim().isEmpty()) {
            d = completedDate.trim();
        } else if (date != null && !date.trim().isEmpty()) {
            d = date.trim();
        }
        if (d == null) d = "";

        com.funwallet.backend.model.WishlistItem item = funWalletService.markWishlistComplete(id, by, d);
        notifyEvent("WISHLIST_COMPLETED");
        return item;
    }

    @PostMapping("/wishlist/{id}/approve")
    public com.funwallet.backend.model.WishlistItem approveWishlistCompletion(@PathVariable Long id) {
        com.funwallet.backend.model.WishlistItem item = funWalletService.approveWishlistCompletion(id);
        notifyEvent("WISHLIST_APPROVED");
        return item;
    }

    @RequestMapping(value = {"/wishlist/{id}", "/wishlist/{id}/update"}, method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.POST})
    public com.funwallet.backend.model.WishlistItem updateWishlist(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String description = payload.get("description");
        int targetMonth = Integer.parseInt(payload.get("targetMonth"));
        int targetYear = Integer.parseInt(payload.get("targetYear"));
        com.funwallet.backend.model.WishlistItem item = funWalletService.updateWishlist(id, description, targetMonth, targetYear);
        notifyEvent("WISHLIST_UPDATED");
        return item;
    }

    // --- Scheduled Reminder Endpoints ---
    @GetMapping("/reminders")
    public List<ScheduledReminder> getReminders(@RequestParam(required = false) String user) {
        if (user != null && !user.trim().isEmpty()) {
            return reminderRepository.findByCreatedByOrderByScheduledTimeDesc(user.trim());
        }
        return reminderRepository.findAllByOrderByScheduledTimeDesc();
    }

    @PostMapping("/reminders")
    public ResponseEntity<?> createReminder(@RequestBody Map<String, String> payload) {
        try {
            String createdBy = payload.get("createdBy");
            String recipientEmail = payload.get("recipientEmail");
            String subject = payload.get("subject");
            String message = payload.get("message");
            String scheduledTimeStr = payload.get("scheduledTime");

            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Recipient email is required");
            }
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message content is required");
            }
            if (scheduledTimeStr == null || scheduledTimeStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Scheduled date and time is required");
            }

            Instant scheduledTime = Instant.parse(scheduledTimeStr.trim());

            ScheduledReminder reminder = new ScheduledReminder();
            reminder.setCreatedBy(createdBy != null ? createdBy.trim() : "Anonymous");
            reminder.setRecipientEmail(recipientEmail.trim());
            reminder.setSubject(subject != null && !subject.trim().isEmpty() ? subject.trim() : "Couple's Fun Wallet Reminder ⏰💖");
            reminder.setMessage(message.trim());
            reminder.setScheduledTime(scheduledTime);
            reminder.setSent(false);
            reminder.setCreatedAt(Instant.now());

            ScheduledReminder saved = reminderRepository.save(reminder);
            notifyEvent("REMINDER_CREATED");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to schedule reminder: " + e.getMessage());
        }
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long id) {
        try {
            if (reminderRepository.existsById(id)) {
                reminderRepository.deleteById(id);
                notifyEvent("REMINDER_DELETED");
                return ResponseEntity.ok(Map.of("message", "Reminder deleted successfully"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete reminder: " + e.getMessage());
        }
    }

    @PostMapping("/reminders/{id}/send-now")
    public ResponseEntity<?> sendReminderNow(@PathVariable Long id) {
        try {
            ScheduledReminder reminder = reminderRepository.findById(id).orElse(null);
            if (reminder == null) {
                return ResponseEntity.notFound().build();
            }
            String emailSubject = (reminder.getSubject() != null && !reminder.getSubject().trim().isEmpty())
                    ? reminder.getSubject().trim()
                    : "Couple's Fun Wallet Reminder ⏰💖";

            String creatorInfo = (reminder.getCreatedBy() != null && !reminder.getCreatedBy().trim().isEmpty())
                    ? reminder.getCreatedBy().trim()
                    : "Someone special";

            String emailBody = reminder.getMessage() + "\n\n---\nSent with ❤️ from " + creatorInfo + " via Couple's Fun Wallet";

            emailService.sendSimpleMessage(reminder.getRecipientEmail(), emailSubject, emailBody);

            reminder.setSent(true);
            reminder.setSentAt(Instant.now());
            reminderRepository.save(reminder);
            notifyEvent("REMINDER_SENT");
            return ResponseEntity.ok(reminder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send reminder now: " + e.getMessage());
        }
    }
}

