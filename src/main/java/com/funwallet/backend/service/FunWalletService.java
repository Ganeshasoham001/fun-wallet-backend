package com.funwallet.backend.service;

import com.funwallet.backend.model.AppUser;
import com.funwallet.backend.model.PointsRequest;
import com.funwallet.backend.model.Streak;
import com.funwallet.backend.repository.PointsRequestRepository;
import com.funwallet.backend.repository.StreakRepository;
import com.funwallet.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FunWalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Autowired
    private PointsRequestRepository pointsRequestRepository;

    @Autowired
    private com.funwallet.backend.repository.AppConfigRepository appConfigRepository;

    @Autowired
    private com.funwallet.backend.repository.PointHistoryRepository pointHistoryRepository;

    @Autowired
    private EmailService emailService;

    private void logHistory(String username, int change, String reason) {
        com.funwallet.backend.model.PointHistory history = new com.funwallet.backend.model.PointHistory();
        history.setUsername(username);
        history.setPointsChange(change);
        history.setReason(reason);
        history.setTimestamp(java.time.LocalDateTime.now());
        pointHistoryRepository.save(history);
    }

    public com.funwallet.backend.model.AppConfig getConfig() {
        return appConfigRepository.findById(1L).orElseGet(() -> appConfigRepository.save(new com.funwallet.backend.model.AppConfig()));
    }

    public com.funwallet.backend.model.AppConfig updateConfig(com.funwallet.backend.model.AppConfig newConfig) {
        newConfig.setId(1L);
        return appConfigRepository.save(newConfig);
    }

    public boolean changePassword(String email, String oldPassword, String newPassword) {
        AppUser user = login(email, oldPassword);
        if (user != null) {
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public AppUser getUser(String name) {
        return userRepository.findByName(name).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setName(name);
            newUser.setRole("Soham".equalsIgnoreCase(name) ? "ADMIN" : "USER");
            return userRepository.save(newUser);
        });
    }

    public AppUser login(String email, String password) {
        if (email != null) email = email.trim();
        if (password != null) password = password.trim();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        return user;
    }

    public AppUser register(String name, String email, String password) {
        if (email != null) email = email.trim();
        if (password != null) password = password.trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        AppUser user = userRepository.findByName(name).orElseGet(AppUser::new);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("Soham".equalsIgnoreCase(name) ? "ADMIN" : "USER");
        return userRepository.save(user);
    }

    public void forgotPassword(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email"));
        
        // Generate random password
        String newPassword = "fun" + (int)(Math.random() * 10000);
        user.setPassword(newPassword);
        userRepository.save(user);
        
        // Send email
        String subject = "Couple's Fun Wallet - Password Reset";
        String body = "Hello " + user.getName() + ",\n\nYour new temporary password is: " + newPassword + "\n\nPlease login and keep it safe!";
        emailService.sendSimpleMessage(email, subject, body);
    }

    public AppUser deductPoints(String adminName, String userName) {
        AppUser admin = getUser(adminName);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admin can deduct points");
        }
        AppUser user = getUser(userName);
        com.funwallet.backend.model.AppConfig config = getConfig();
        int deduction = config.getAngerDeduction();
        user.setPoints(user.getPoints() - deduction);
        user = userRepository.save(user);
        
        logHistory(userName, -deduction, "Admin Deducted Points");
        
        return user;
    }

    public AppUser addProgress(String userName, String type) {
        AppUser user = getUser(userName);
        java.time.LocalDate today = java.time.LocalDate.now();
        com.funwallet.backend.model.AppConfig config = getConfig();

        if ("study".equalsIgnoreCase(type)) {
            if (today.equals(user.getLastStudyDate())) {
                throw new RuntimeException("Already added study progress today!");
            }
            user.setLastStudyDate(today);
            user.setStudyStreak(user.getStudyStreak() + 1);
            if (user.getStudyStreak() >= config.getStudyRequiredDays()) {
                createRequest(userName, "Completed " + config.getStudyRequiredDays() + " days of study", config.getStudyGain());
                user.setStudyStreak(0);
            }
        } else if ("behaviour".equalsIgnoreCase(type)) {
            if (today.equals(user.getLastBehaviourDate())) {
                throw new RuntimeException("Already added behaviour progress today!");
            }
            user.setLastBehaviourDate(today);
            user.setBehaviourStreak(user.getBehaviourStreak() + 1);
            if (user.getBehaviourStreak() >= config.getBehaviourRequiredDays()) {
                createRequest(userName, "Completed " + config.getBehaviourRequiredDays() + " days of good behaviour", config.getBehaviourGain());
                user.setBehaviourStreak(0);
            }
        } else {
            throw new RuntimeException("Unknown progress type");
        }
        return userRepository.save(user);
    }

    public Streak updateStreak(String userName, String type) {
        AppUser user = getUser(userName);
        Streak streak = streakRepository.findByUserIdAndType(user.getId(), type).orElseGet(() -> {
            Streak s = new Streak();
            s.setUserId(user.getId());
            s.setType(type);
            return s;
        });
        streak.setCurrentDays(streak.getCurrentDays() + 1);
        return streakRepository.save(streak);
    }

    public void resetStreak(String adminName, String userName, String type) {
        AppUser admin = getUser(adminName);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only Admin can reset streaks");
        }
        AppUser target = getUser(userName);
        streakRepository.findByUserIdAndType(target.getId(), type).ifPresent(streak -> {
            streak.setCurrentDays(0);
            streakRepository.save(streak);
        });
    }

    public PointsRequest createRequest(String userName, String category, int requestedPoints) {
        AppUser user = getUser(userName);
        PointsRequest req = new PointsRequest();
        req.setUserId(user.getId());
        req.setCategory(category);
        req.setRequestedPoints(requestedPoints);
        req.setStatus("PENDING");
        return pointsRequestRepository.save(req);
    }

    public PointsRequest approveRequest(String adminName, Long requestId, int grantedPoints) {
        AppUser admin = getUser(adminName);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only Admin can approve requests");
        }
        PointsRequest req = pointsRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Request is not pending");
        }
        
        req.setStatus("APPROVED");
        pointsRequestRepository.save(req);

        // Add points to user
        AppUser user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPoints(user.getPoints() + grantedPoints);
        userRepository.save(user);

        logHistory(user.getName(), grantedPoints, req.getCategory() + " (Approved by Admin)");

        return req;
    }

    public PointsRequest rejectRequest(String adminName, Long requestId) {
        AppUser admin = getUser(adminName);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only Admin can reject requests");
        }
        PointsRequest req = pointsRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus("REJECTED");
        return pointsRequestRepository.save(req);
    }

    public List<PointsRequest> getPendingRequests() {
        return pointsRequestRepository.findByStatus("PENDING");
    }

    public Streak getStreak(String userName, String type) {
        AppUser user = getUser(userName);
        return streakRepository.findByUserIdAndType(user.getId(), type).orElseGet(() -> {
            Streak s = new Streak();
            s.setUserId(user.getId());
            s.setType(type);
            s.setCurrentDays(0);
            return s;
        });
    }

    public List<com.funwallet.backend.model.PointHistory> getHistory(String userName) {
        return pointHistoryRepository.findByUsernameOrderByTimestampDesc(userName);
    }
}
