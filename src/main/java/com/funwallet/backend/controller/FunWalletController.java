package com.funwallet.backend.controller;

import com.funwallet.backend.model.AppUser;
import com.funwallet.backend.model.PointsRequest;
import com.funwallet.backend.model.Streak;
import com.funwallet.backend.service.FunWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow angular frontend to connect
public class FunWalletController {

    @Autowired
    private FunWalletService funWalletService;

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
    @GetMapping("/user/{name}")
    public AppUser getUser(@PathVariable String name) {
        return funWalletService.getUser(name);
    }

    // --- Config Endpoints ---
    @GetMapping("/config")
    public com.funwallet.backend.model.AppConfig getConfig() {
        return funWalletService.getConfig();
    }

    @PostMapping("/admin/config")
    public com.funwallet.backend.model.AppConfig updateConfig(@RequestBody com.funwallet.backend.model.AppConfig config) {
        return funWalletService.updateConfig(config);
    }

    // --- Wallet Endpoints ---
    @PostMapping("/admin/angry")
    public AppUser angryForOneDay(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        String targetName = payload.get("targetName"); // usually Sairindhri
        return funWalletService.deductPoints(adminName, targetName);
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
        return funWalletService.addProgress(userName, type);
    }

    @PostMapping("/admin/streak/reset")
    public void resetStreak(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        String targetName = payload.get("targetName");
        String type = payload.get("type");
        funWalletService.resetStreak(adminName, targetName, type);
    }

    // --- Request Endpoints ---
    @PostMapping("/request/create")
    public PointsRequest createRequest(@RequestBody Map<String, String> payload) {
        String userName = payload.get("userName");
        String category = payload.get("category");
        int points = Integer.parseInt(payload.get("points"));
        return funWalletService.createRequest(userName, category, points);
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
        return funWalletService.approveRequest(adminName, requestId, grantedPoints);
    }

    @PostMapping("/admin/request/reject")
    public PointsRequest rejectRequest(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        Long requestId = Long.parseLong(payload.get("requestId"));
        return funWalletService.rejectRequest(adminName, requestId);
    }

    @GetMapping("/history/{username}")
    public java.util.List<com.funwallet.backend.model.PointHistory> getHistory(@PathVariable String username) {
        return funWalletService.getHistory(username);
    }
}
