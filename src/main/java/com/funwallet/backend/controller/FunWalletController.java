package com.funwallet.backend.controller;

import com.funwallet.backend.model.AppUser;
import com.funwallet.backend.model.PointsRequest;
import com.funwallet.backend.model.ScheduledReminder;
import com.funwallet.backend.model.Streak;
import com.funwallet.backend.model.DailyQuestion;
import com.funwallet.backend.model.HeartbeatPoke;
import com.funwallet.backend.model.PolaroidMoment;
import com.funwallet.backend.model.LoveEnvelope;
import com.funwallet.backend.repository.DailyQuestionRepository;
import com.funwallet.backend.repository.HeartbeatPokeRepository;
import com.funwallet.backend.repository.PolaroidMomentRepository;
import com.funwallet.backend.repository.LoveEnvelopeRepository;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
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

    @Autowired
    private DailyQuestionRepository dailyQuestionRepository;

    @Autowired
    private HeartbeatPokeRepository heartbeatPokeRepository;

    @Autowired
    private PolaroidMomentRepository polaroidMomentRepository;

    @Autowired
    private LoveEnvelopeRepository loveEnvelopeRepository;

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

    // --- Feature 2: Daily Question of the Day (Secret Double-Reveal) ---
    private static final String[] CURATED_QUESTIONS = {
        "What is your absolute favorite memory of the two of us so far? 🌸",
        "What was the exact moment or gesture when you knew you had feelings for me? 💖",
        "If we could take off on an unexpected weekend getaway tomorrow, where would we go? ✈️",
        "What is one tiny habit of mine that secretly makes you smile? 🥰",
        "Which song always reminds you of me whenever it plays? 🎶",
        "What is our sweetest inside joke or silly moment together? 😂",
        "If you could describe our love story in three words, what would they be? ✨",
        "What is something you're excited for us to achieve or experience together in the next year? 🌟",
        "What's your dream cozy date night at home? 🍕🍿",
        "What is one thing about me that made you feel safe and comfortable right away? 🫂",
        "What's the funniest thing that has ever happened to us on a date? 🙈",
        "If we had a whole lazy Sunday with zero responsibilities, how would we spend it? ☕",
        "What is a meal or dish that will always remind you of us? 🍜",
        "What is something you admire about my personality? 💫",
        "What's one adventure or bucket-list activity you want us to try together? 🎢",
        "What is your favorite picture of us and why? 📸",
        "When was the last time I made your heart skip a beat? 💓",
        "If we built our dream home together, what is one special room or corner it must have? 🏡",
        "What do you think is our greatest strength as a couple? 💍",
        "What is a silly nickname or compliment that always works on you? 🤭"
    };

    @GetMapping("/daily-question/today")
    public ResponseEntity<?> getTodayQuestion(@RequestParam(required = false) String user) {
        try {
            String todayStr = LocalDate.now().toString();
            DailyQuestion q = dailyQuestionRepository.findByQuestionDate(todayStr).orElseGet(() -> {
                DailyQuestion newQ = new DailyQuestion();
                newQ.setQuestionDate(todayStr);
                int idx = Math.abs(LocalDate.now().getDayOfYear()) % CURATED_QUESTIONS.length;
                newQ.setQuestionText(CURATED_QUESTIONS[idx]);
                newQ.setRevealed(false);
                return dailyQuestionRepository.save(newQ);
            });

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", q.getId());
            resp.put("questionDate", q.getQuestionDate());
            resp.put("questionText", q.getQuestionText());
            resp.put("revealed", q.isRevealed());

            boolean sohamDone = q.getSohamAnswer() != null && !q.getSohamAnswer().trim().isEmpty();
            boolean sairindhriDone = q.getSairindhriAnswer() != null && !q.getSairindhriAnswer().trim().isEmpty();
            resp.put("sohamAnswered", sohamDone);
            resp.put("sairindhriAnswered", sairindhriDone);

            String reqUser = user != null ? user.trim() : "";
            if (reqUser.equalsIgnoreCase("Soham")) {
                resp.put("myAnswer", q.getSohamAnswer());
                resp.put("partnerAnswered", sairindhriDone);
                resp.put("partnerAnswer", q.isRevealed() ? q.getSairindhriAnswer() : null);
            } else if (reqUser.equalsIgnoreCase("Sairindhri")) {
                resp.put("myAnswer", q.getSairindhriAnswer());
                resp.put("partnerAnswered", sohamDone);
                resp.put("partnerAnswer", q.isRevealed() ? q.getSohamAnswer() : null);
            } else {
                resp.put("sohamAnswer", q.isRevealed() ? q.getSohamAnswer() : null);
                resp.put("sairindhriAnswer", q.isRevealed() ? q.getSairindhriAnswer() : null);
            }

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching question: " + e.getMessage());
        }
    }

    @PostMapping("/daily-question/answer")
    public ResponseEntity<?> submitDailyAnswer(@RequestBody Map<String, String> payload) {
        try {
            String user = payload.get("user");
            String answer = payload.get("answer");
            if (user == null || user.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User is required");
            }
            if (answer == null || answer.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Answer cannot be blank");
            }

            String todayStr = LocalDate.now().toString();
            DailyQuestion q = dailyQuestionRepository.findByQuestionDate(todayStr).orElseGet(() -> {
                DailyQuestion newQ = new DailyQuestion();
                newQ.setQuestionDate(todayStr);
                int idx = Math.abs(LocalDate.now().getDayOfYear()) % CURATED_QUESTIONS.length;
                newQ.setQuestionText(CURATED_QUESTIONS[idx]);
                return dailyQuestionRepository.save(newQ);
            });

            if (user.equalsIgnoreCase("Soham") || user.toLowerCase().contains("admin")) {
                q.setSohamAnswer(answer.trim());
                q.setSohamAnsweredAt(Instant.now());
            } else {
                q.setSairindhriAnswer(answer.trim());
                q.setSairindhriAnsweredAt(Instant.now());
            }

            boolean bothDone = (q.getSohamAnswer() != null && !q.getSohamAnswer().trim().isEmpty()) &&
                               (q.getSairindhriAnswer() != null && !q.getSairindhriAnswer().trim().isEmpty());

            if (bothDone) {
                q.setRevealed(true);
                dailyQuestionRepository.save(q);
                notifyEvent("QUESTION_REVEALED");
            } else {
                dailyQuestionRepository.save(q);
                notifyEvent("QUESTION_ANSWERED");
            }

            return ResponseEntity.ok(Map.of("message", "Answer saved successfully", "revealed", q.isRevealed()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit answer: " + e.getMessage());
        }
    }

    @GetMapping("/daily-question/history")
    public List<DailyQuestion> getQuestionHistory() {
        return dailyQuestionRepository.findByRevealedTrueOrderByQuestionDateDesc();
    }

    // --- Feature 3: Instant Heartbeat Poke ---
    @PostMapping("/heartbeat/poke")
    public ResponseEntity<?> sendHeartbeatPoke(@RequestBody Map<String, String> payload) {
        try {
            String fromUser = payload.get("fromUser");
            String pokeType = payload.getOrDefault("pokeType", "HEARTBEAT");
            String message = payload.getOrDefault("message", "thinking of you! 💓");

            if (fromUser == null || fromUser.trim().isEmpty()) {
                fromUser = "Someone special";
            }

            String toUser = payload.get("toUser");
            if (toUser == null || toUser.trim().isEmpty()) {
                toUser = fromUser.equalsIgnoreCase("Soham") ? "Sairindhri" : "Soham";
            }

            HeartbeatPoke poke = new HeartbeatPoke();
            poke.setFromUser(fromUser.trim());
            poke.setToUser(toUser.trim());
            poke.setPokeType(pokeType.trim());
            poke.setMessage(message.trim());
            poke.setAcknowledged(false);
            poke.setSentAt(Instant.now());

            HeartbeatPoke saved = heartbeatPokeRepository.save(poke);
            notifyEvent("HEARTBEAT_POKE");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send poke: " + e.getMessage());
        }
    }

    @GetMapping("/heartbeat/latest")
    public ResponseEntity<?> getLatestPoke(@RequestParam String user) {
        try {
            if (user == null || user.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("User parameter required");
            }
            // Pokes sent in the last 15 minutes that haven't been dismissed
            Instant fifteenMinutesAgo = Instant.now().minusSeconds(900);
            List<HeartbeatPoke> pokes = heartbeatPokeRepository.findByToUserAndSentAtAfterOrderBySentAtDesc(user.trim(), fifteenMinutesAgo);
            if (!pokes.isEmpty() && !pokes.get(0).isAcknowledged()) {
                return ResponseEntity.ok(pokes.get(0));
            }
            return ResponseEntity.ok(Map.of("active", false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/heartbeat/ack")
    public ResponseEntity<?> acknowledgePoke(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.containsKey("id")) {
                Long id = Long.valueOf(payload.get("id").toString());
                heartbeatPokeRepository.findById(id).ifPresent(p -> {
                    p.setAcknowledged(true);
                    heartbeatPokeRepository.save(p);
                });
            } else if (payload.containsKey("user")) {
                String user = payload.get("user").toString();
                List<HeartbeatPoke> pending = heartbeatPokeRepository.findByToUserAndAcknowledgedFalseOrderBySentAtDesc(user);
                for (HeartbeatPoke p : pending) {
                    p.setAcknowledged(true);
                }
                heartbeatPokeRepository.saveAll(pending);
            }
            return ResponseEntity.ok(Map.of("message", "Acknowledged"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ack error: " + e.getMessage());
        }
    }

    // --- Feature 5: Our Moments Polaroid Scrapbook ---
    @GetMapping("/moments")
    public List<PolaroidMoment> getAllMoments() {
        return polaroidMomentRepository.findAllByOrderByMemoryDateDesc();
    }

    @PostMapping("/moments")
    public ResponseEntity<?> createMoment(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.get("title");
            String caption = payload.get("caption");
            String memoryDate = payload.getOrDefault("memoryDate", LocalDate.now().toString());
            String imageUrl = payload.get("imageUrl");
            String createdBy = payload.getOrDefault("createdBy", "Soham");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }

            PolaroidMoment m = new PolaroidMoment();
            m.setTitle(title.trim());
            m.setCaption(caption != null ? caption.trim() : "");
            m.setMemoryDate(memoryDate.trim());
            m.setImageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : "https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=800&q=80");
            m.setCreatedBy(createdBy.trim());
            m.setCreatedAt(Instant.now());

            PolaroidMoment saved = polaroidMomentRepository.save(m);
            notifyEvent("MOMENT_ADDED");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save moment: " + e.getMessage());
        }
    }

    @DeleteMapping("/moments/{id}")
    public ResponseEntity<?> deleteMoment(@PathVariable Long id) {
        try {
            if (polaroidMomentRepository.existsById(id)) {
                polaroidMomentRepository.deleteById(id);
                notifyEvent("MOMENT_DELETED");
                return ResponseEntity.ok(Map.of("message", "Moment deleted"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Delete error: " + e.getMessage());
        }
    }

    // --- Feature 6: Open When Digital Love Envelopes ---
    @GetMapping("/envelopes")
    public List<LoveEnvelope> getAllEnvelopes(@RequestParam(required = false) String recipient) {
        return loveEnvelopeRepository.findAllByOrderByCreatedAtDesc();
    }

    @DeleteMapping("/envelopes/{id}")
    public ResponseEntity<?> deleteEnvelope(@PathVariable Long id) {
        try {
            if (loveEnvelopeRepository.existsById(id)) {
                loveEnvelopeRepository.deleteById(id);
                notifyEvent("ENVELOPE_DELETED");
                return ResponseEntity.ok(Map.of("message", "Envelope deleted"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Delete error: " + e.getMessage());
        }
    }

    @PostMapping("/envelopes")
    public ResponseEntity<?> createEnvelope(@RequestBody Map<String, String> payload) {
        try {
            String title = payload.get("title");
            String content = payload.get("content");
            String sender = payload.getOrDefault("sender", "Soham");
            String recipient = payload.getOrDefault("recipient", "Sairindhri");
            String unlockType = payload.getOrDefault("unlockType", "ANYTIME");
            String unlockDateStr = payload.get("unlockDate");

            if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title and message content are required");
            }

            LoveEnvelope env = new LoveEnvelope();
            env.setTitle(title.trim());
            env.setContent(content.trim());
            env.setSender(sender.trim());
            env.setRecipient(recipient.trim());
            env.setUnlockType(unlockType.trim());
            if (unlockDateStr != null && !unlockDateStr.trim().isEmpty()) {
                env.setUnlockDate(LocalDate.parse(unlockDateStr.trim()));
            }
            env.setOpened(false);
            env.setCreatedAt(Instant.now());

            LoveEnvelope saved = loveEnvelopeRepository.save(env);
            notifyEvent("ENVELOPE_ADDED");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create envelope: " + e.getMessage());
        }
    }

    @PostMapping("/envelopes/{id}/open")
    public ResponseEntity<?> openEnvelope(@PathVariable Long id) {
        try {
            LoveEnvelope env = loveEnvelopeRepository.findById(id).orElse(null);
            if (env == null) {
                return ResponseEntity.notFound().build();
            }
            env.setOpened(true);
            env.setOpenedAt(Instant.now());
            LoveEnvelope saved = loveEnvelopeRepository.save(env);
            notifyEvent("ENVELOPE_OPENED");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Open error: " + e.getMessage());
        }
    }
}

